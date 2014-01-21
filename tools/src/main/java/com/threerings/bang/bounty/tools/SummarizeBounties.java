//
// $Id$

package com.threerings.bang.bounty.tools;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import com.jme.util.export.binary.BinaryImporter;

import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.bang.data.Star;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;

import com.threerings.bang.bounty.data.BountyConfig;
import com.threerings.bang.bounty.data.OfficeCodes;

/**
 * Generates an HTML summary of all bounty configurations.
 */
public class SummarizeBounties
{
    public static void main (String[] args)
        throws IOException
    {
        if (args.length == 0) {
            System.err.println("Usage: SummarizeBounties town_id");
            System.exit(255);
        }

        System.out.println("<html><head>");
        System.out.println("<title>Bounties: " + args[0] + "</title>");
        System.out.println("</head><body>");
        System.out.println("<h2>Town Bounties</h2>");
        summarizeBounties(args[0], BountyConfig.Type.TOWN);
        System.out.println("<h2>Most Wanted Bounties</h2>");
        summarizeBounties(args[0], BountyConfig.Type.MOST_WANTED);
        System.out.println("</body></html>");
    }

    protected static void summarizeBounties (String townId, BountyConfig.Type type)
    {
        System.out.println("<table style='border-collapse: collapse; border: 1px solid'" +
                           "cellpadding=5 cellspacing=0>");
        for (BountyConfig bounty : BountyConfig.getBounties(townId, type)) {
            summarizeBounty(bounty);
        }
        System.out.println("</table>");
    }

    protected static void summarizeBounty (BountyConfig config)
    {
        System.out.println("<tr style='border: 1px solid; " +
                           "background: " + _colors.get(config.difficulty) + "'><td>");
        System.out.println("<b>" + config.title + "</b></td>");
        System.out.print("<td align='right'>" + config.reward.scrip);
        if (config.reward.badge != null) {
            System.out.print(", " + _msgs.xlate(config.reward.badge.newBadge().getName()));
        }
        if (config.reward.articles != null) {
            System.out.print(", " + _msgs.xlate(config.reward.articles[0].getName()) +
                               ", " + _msgs.xlate(config.reward.articles[1].getName()));
        }
        System.out.println("</td></tr><tr><td colspan=2>");
        System.out.println(config.description);
        for (BountyConfig.GameInfo game : config.games) {
            summarizeGame(config, game);
        }
        System.out.println("</td></tr>");
    }

    protected static void summarizeGame (BountyConfig config, BountyConfig.GameInfo game)
    {
        String path = "rsrc/" + config.getGamePath(game.ident);
        URL url = SummarizeBounties.class.getClassLoader().getResource(path);
        if (url == null) {
            System.err.println("Failed to locate '" + path + "'.");
            return;
        }

        BangConfig gconfig;
        try {
            gconfig = (BangConfig)BinaryImporter.getInstance().load(url);
        } catch (IOException ioe) {
            System.err.println("Failed to load '" + path + "': " + ioe);
            return;
        }

        System.out.println("<tr style='border-top: 1px solid'>" +
                           "<td valign='top'><i>" + game.name + "</i><br>");
        System.out.println("<font size='-1'>");
        String smsg = "m.scenario_" + gconfig.rounds.get(0).scenario;
        System.out.print("<b>" + _msgmgr.getBundle(GameCodes.GAME_MSGS).get(smsg) + "</b> " +
                         gconfig.rounds.get(0).board);
        String[] players = new String[gconfig.plist.size()];
        for (int ii = 0; ii < players.length; ii++) {
            BangAI ai = config.getOpponent(game.ident, gconfig.plist.size(), ii, new BangAI());
            String defname = (ii == 0) ? "Player" : "Tin Can";
            players[ii] = (ai.handle == null ? defname : ai.handle.toString());
        }

        System.out.println("<ol>");
        String bigShot = null;
        for (int ii = 0; ii < players.length; ii++) {
            BangConfig.Player player = gconfig.plist.get(ii);
            System.out.print("<li><u>" + players[ii] + ":</u>");
            if (player.bigShot != null) {
                String name = _msgs.xlate(UnitConfig.getName(player.bigShot));
                System.out.print(" " + name);
                if (ii == 0) {
                    bigShot = name;
                }
            }
            for (String unit : player.units) {
                System.out.print(", " + _msgs.xlate(UnitConfig.getName(unit)));
            }
        }
        System.out.println("</ol>");
        System.out.println("</font>");
        System.out.println("</td><td valign='top'>");
        System.out.println("<font size='-1'>");
        printQuote(players, "Pre-game", game.preGameQuote, bigShot);
        printQuote(players, "Failed", game.failedQuote, bigShot);
        printQuote(players, "Completed", game.completedQuote, bigShot);
        System.out.println("</font>");
        System.out.println("</td></tr>");

    }

    protected static void printQuote (String[] players, String type, BountyConfig.Quote quote,
                                      String bigShot)
    {
        int spidx = (quote.speaker == -1) ? players.length-1 : quote.speaker;
        System.out.println("<u>" + type + ":</u> " + (spidx == 0 ? bigShot : players[spidx]) +
                           "<br>" + quote.text + "<br>");
    }

    protected static MessageManager _msgmgr = new MessageManager("rsrc.i18n");
    protected static MessageBundle _msgs = _msgmgr.getBundle(OfficeCodes.OFFICE_MSGS);

    protected static HashMap<Star.Difficulty,String> _colors;
    static {
        _colors = new HashMap<Star.Difficulty,String>();
        _colors.put(Star.Difficulty.EASY, "#6699CC");
        _colors.put(Star.Difficulty.MEDIUM, "#CC6600");
        _colors.put(Star.Difficulty.HARD, "#CCCCCC");
        _colors.put(Star.Difficulty.EXTREME, "#FFCC33");
    }
}
