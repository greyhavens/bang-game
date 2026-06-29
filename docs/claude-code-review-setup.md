# Set up subscription-authed automatic PR code review (Claude Code GitHub Action)

**Goal:** every pull request gets an automatic Claude code review that posts inline
comments + a summary, billed against the Claude **subscription** (no metered API cost).

Do NOT use the `code-review@claude-code-plugins` plugin — in this setup its
comment-posting path is blocked by the action's tool allow-list and it silently
posts nothing. Use the action's **native review prompt** with explicit
`--allowedTools` instead (this is the pattern from claude-code-action
`docs/solutions.md` → "Automatic PR Code Review").

## Prerequisites (verify before editing files)

1. **Claude GitHub App installed** on the repo. If not, the human must run
   `/install-github-app` in a Claude Code session (requires repo admin). You can
   choose "Skip" on the workflow step — you'll write the workflow yourself below.
2. **`CLAUDE_CODE_OAUTH_TOKEN` repo secret** exists. Check with:
   `gh secret list`
   If missing, the human generates one with `claude setup-token` (uses their
   subscription) and adds it: `gh secret set CLAUDE_CODE_OAUTH_TOKEN`.

## Step 1 — Create the workflow file

Write `.github/workflows/claude-code-review.yml` with exactly this content:

```yaml
name: Claude Code Review

on:
  pull_request:
    types: [opened, synchronize, ready_for_review, reopened]

jobs:
  claude-review:
    runs-on: ubuntu-latest
    # pull-requests: write is required so `gh pr comment` and the inline-comment
    # tool can post the review back onto the PR via GITHUB_TOKEN.
    permissions:
      contents: read
      pull-requests: write
      id-token: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          fetch-depth: 1

      - name: Run Claude Code Review
        id: claude-review
        uses: anthropics/claude-code-action@v1
        with:
          # Authenticates as the Claude subscription, so review usage is covered
          # by the plan (no metered API charge).
          claude_code_oauth_token: ${{ secrets.CLAUDE_CODE_OAUTH_TOKEN }}
          prompt: |
            REPO: ${{ github.repository }}
            PR NUMBER: ${{ github.event.pull_request.number }}

            Review this pull request, following the repo's CLAUDE.md
            conventions. Focus on:
            - Correctness bugs and broken edge cases
            - Security implications
            - Performance considerations
            - Code quality and project conventions

            The PR branch is already checked out in the working directory.
            Use `gh pr comment` for a short top-level summary.
            Use `mcp__github_inline_comment__create_inline_comment` (with
            `confirmed: true`) to flag specific lines.
            Only post GitHub comments - do not return the review as a message.
            If you find nothing worth flagging, post a brief "no issues found"
            summary comment.
          claude_args: |
            --allowedTools "mcp__github_inline_comment__create_inline_comment,Bash(gh pr comment:*),Bash(gh pr diff:*),Bash(gh pr view:*)"
```

## Step 2 — Merge to the default branch FIRST

⚠️ **Critical gotcha:** the action has an anti-tamper guard — it **skips entirely on
any PR that modifies a workflow file** ("must have identical content to the version
on the repository's default branch"). So:

- Commit this workflow on a branch, open a PR, and **merge it to `main`**.
- The review will NOT run on that PR (it edits the workflow → skipped). That's expected.
- The workflow only takes effect on PRs opened **after** it's on `main`.

This same rule applies to every future tweak: edit → merge to `main` → it takes effect.

## Step 3 — Verify it works

After the workflow is on `main`, open a throwaway test PR that does **not** touch any
workflow file and contains an obvious bug, e.g. a file with:

```python
def percent_return(buy, sell):
    # BUG: divides by sell instead of buy
    return (sell - buy) / sell * 100
```

Then watch the run and confirm a comment was posted:

```bash
# find the run
gh run list --workflow "Claude Code Review" --limit 1
# watch it
gh run watch <run-id> --exit-status
# confirm comments landed
gh api repos/OWNER/REPO/pulls/<pr>/comments    # inline review comments
gh api repos/OWNER/REPO/issues/<pr>/comments    # top-level summary
```

Expect: one inline comment on the buggy line + a top-level summary from `claude[bot]`.
Close the test PR and delete the file/branch afterward.

## Troubleshooting

- **Run succeeds but nothing posts:** check the run log's result JSON. If
  `permission_denials_count` > 0 and you see "No buffered inline comments", a posting
  tool was denied — make sure the `--allowedTools` line is present and the prompt tells
  Claude to use `gh pr comment` / the inline-comment tool. (These denials are Claude
  Code's tool allow-list, NOT the GitHub sandbox.)
- **Action shows "skipped due to workflow validation":** you're testing on a PR that
  edits the workflow file. Merge to `main` first, then test on a separate PR.
- **No comment on a clean PR:** that's fine — a trivial diff yields a short
  "no issues found" summary, not inline comments.

## Cost note

`claude_code_oauth_token` = the Claude **subscription**, so reviews draw on the plan's
usage allowance (no per-token dollar charge; rate limits still apply). This is different
from the managed "Code Review" product (claude.ai/admin-settings), which is billed
separately via usage credits — you are NOT using that here.
