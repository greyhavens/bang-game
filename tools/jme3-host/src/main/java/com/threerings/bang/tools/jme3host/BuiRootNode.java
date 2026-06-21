//
// Phase 5 step 3 proof harness -- ISOLATED jME3 host for BUI's jME3 render backend.
//

package com.threerings.bang.tools.jme3host;

import com.jmex.bui.BComponent;
import com.jmex.bui.BRootNode;

/**
 * A concrete {@link BRootNode} for the jME3 host harness.
 *
 * <p> Since the Phase-1 cutover {@link BRootNode} is no longer a fork scene node; its render
 * traversal is the engine-neutral {@link BRootNode#renderWindows} (walking the window list and
 * driving each window's render through the installed {@code BackendProvider} backend, here
 * {@code Jme3RenderBackend}). The {@code RenderManager} argument is a pass-through token --
 * BUI's actual drawing is routed through the backend -- so the host passes {@code null}.
 *
 * <p> Input is NOT wired: the jME3 input path ({@code RawInputListener} -> BUI events) is
 * future work (see docs/jme3-bui-host.md). This harness only proves the render path.
 */
public class BuiRootNode extends BRootNode
{
    @Override // from BRootNode
    public long getTickStamp ()
    {
        return System.currentTimeMillis();
    }

    @Override // from BRootNode
    public void rootInvalidated (BComponent root)
    {
        // The harness validates windows explicitly after building the scene; no deferred
        // revalidation queue is needed for a static demo.
        root.validate();
    }
}
