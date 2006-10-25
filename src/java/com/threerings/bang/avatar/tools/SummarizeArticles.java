//
// $Id$

package com.threerings.bang.avatar.tools;

import java.io.IOException;
import java.util.HashMap;

import com.samskivert.util.ComparableArrayList;
import com.samskivert.util.StringUtil;

import com.threerings.resource.ResourceManager;
import com.threerings.util.CompiledConfig;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangUtil;

import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

/**
 * A tool to generate a comparsion chart of the article catalog.
 */
public class SummarizeArticles
{
    public static class ArticleRow implements Comparable<ArticleRow>
    {
        public String name;
        public int[] coins = new int[2];
        public int[] scrip = new int[2];

        public ArticleRow (ArticleCatalog.Article article)
        {
            name = article.name.substring(
                article.name.startsWith("male_") ?
                "male_".length() : "female_".length());
            setCost(article);
        }

        public void setCost (ArticleCatalog.Article article)
        {
            int idx = article.name.startsWith("male_") ? 0 : 1;
            coins[idx] = article.coins;
            scrip[idx] = article.scrip;
        }

        public int compareTo (ArticleRow other)
        {
            int rv;
            if ((rv = average(coins) - average(other.coins)) != 0) {
                return rv;
            }
            return average(scrip) - average(other.scrip);
        }

        public String toString ()
        {
            return StringUtil.fieldsToString(this);
        }

        public String toHTML ()
        {
            StringBuilder buf = new StringBuilder();
            buf.append("<td>").append(name).append("</td>");
            appendCost(buf, "#99CCFF", coins[0], scrip[0]);
            appendCost(buf, "#FF9999", coins[1], scrip[1]);
            return buf.toString();
        }

        protected int average (int[] values)
        {
            int total = 0, count = 0;
            for (int ii = 0; ii < values.length; ii++) {
                if (values[ii] != 0) {
                    total += values[ii];
                    count++;
                }
            }
            return (count == 0) ? 0 : total/count;
        }

        protected void appendCost (
            StringBuilder buf, String bgcolor, int coins, int scrip)
        {
            if (coins > 0 || scrip > 0) {
                buf.append("<td bgcolor='" + bgcolor + "'>");
                buf.append(coins).append("</td>");
                buf.append("<td bgcolor='" + bgcolor + "'>");
                buf.append(scrip).append("</td>");
            } else {
                buf.append("<td></td><td></td>");
            }
        }
    }

    public static class ArticleRowList extends ComparableArrayList<ArticleRow>
    {
    }

    public static void main (String[] args)
        throws IOException
    {
        ResourceManager rmgr = new ResourceManager("rsrc");
        rmgr.initBundles(null, "config/resource/manager.properties", null);
        ArticleCatalog artcat = (ArticleCatalog)CompiledConfig.loadConfig(
            rmgr.getResource(ArticleCatalog.CONFIG_PATH));

        ArticleRowList[][][] articles = new ArticleRowList[
            BangCodes.TOWN_IDS.length][AvatarLogic.SLOTS.length][10];
        HashMap<String,ArticleRow> allarts = new HashMap<String,ArticleRow>();

        HashMap<String,ComparableArrayList<ArticleRow>> artmap =
            new HashMap<String,ComparableArrayList<ArticleRow>>();
        for (ArticleCatalog.Article article : artcat.getArticles()) {
            int townIdx = BangUtil.getTownIndex(article.townId);
            int slotIdx = AvatarLogic.getSlotIndex(article.slot);
            ArticleRowList arts = articles[townIdx][slotIdx][article.coins];
            if (arts == null) {
                arts = (articles[townIdx][slotIdx][article.coins] =
                        new ArticleRowList());
            }
            ArticleRow row = new ArticleRow(article);
            ArticleRow orow = allarts.get(row.name);
            if (orow != null) {
                orow.setCost(article);
            } else {
                allarts.put(row.name, row);
                arts.add(row);
            }
        }

        // now sort all of our lists
        for (int tt = 0; tt < articles.length; tt++) {
            for (int ss = 0; ss < articles[tt].length; ss++) {
                for (int cc = 0; cc < articles[tt][ss].length; cc++) {
                    if (articles[tt][ss][cc] != null) {
                        articles[tt][ss][cc].sort();
                    }
                }
            }
        };

        System.out.println("<html><head><title>Article Catalog</title></head>");
        System.out.println("<body>");
        System.out.println("<h2>Article Catalog</h2>");

        // compute and print out the summary
        System.out.println("<table cellpadding=4 border=1 " +
                           "style=\"border-collapse: collapse\">");

        System.out.print("<tr><th></th>");
        for (int tt = 0; tt < BangCodes.TOWN_IDS.length; tt++) {
            System.out.print("<th colspan=6>" +
                             BangCodes.TOWN_IDS[tt] + "</th>");
        }
        System.out.println("</tr>");

        System.out.print("<tr style='border-bottom: 2px solid'><th>Slot</th>");
        for (int ii = 0; ii < BangCodes.TOWN_IDS.length; ii++) {
            System.out.print("<th>Count</th><th>Coins</th><th>Scrip</th>" +
                             "<th>Count</th><th>Coins</th><th>Scrip</th>");
        }
        System.out.println("</tr>");

        int[][] totals = new int[BangCodes.TOWN_IDS.length][2];
        int[][] ccosts = new int[BangCodes.TOWN_IDS.length][2];
        int[][] scosts = new int[BangCodes.TOWN_IDS.length][2];
        for (int ss = 0; ss < AvatarLogic.SLOTS.length; ss++) {
            for (int tt = 0; tt < BangCodes.TOWN_IDS.length; tt++) {
                totals[tt][0] = totals[tt][1] = 0;
                ccosts[tt][0] = ccosts[tt][1] = 0;
                scosts[tt][0] = scosts[tt][1] = 0;
            }

            for (int tt = 0; tt < articles.length; tt++) {
                for (int cc = 0; cc < articles[tt][ss].length; cc++) {
                    ArticleRowList arl = articles[tt][ss][cc];
                    if (arl != null) {
                        for (ArticleRow arow : arl) {
                            for (int gg = 0; gg < 2; gg++) {
                                if (arow.scrip[gg] > 0) {
                                    totals[tt][gg]++;
                                    ccosts[tt][gg] += arow.coins[gg];
                                    scosts[tt][gg] += arow.scrip[gg];
                                }
                            }
                        }
                    }
                }
            }

            System.out.print("<tr><td>" + AvatarLogic.SLOTS[ss].name + "</td>");
            for (int tt = 0; tt < BangCodes.TOWN_IDS.length; tt++) {
                for (int gg = 0; gg < 2; gg++) {
                    float total = totals[tt][gg];
                    float avgcoin = (total > 0) ? ccosts[tt][gg] / total : 0;
                    float avgscrip = (total > 0) ? scosts[tt][gg] / total : 0;
                    System.out.printf("<td align=right>%2.0f</td>" +
                                      "<td align=right>%2.1f</td>" +
                                      "<td align=right>%2.0f</td>",
                                      total, avgcoin, avgscrip);
                }
            }
            System.out.println("</tr>");
        }
        System.out.println("</table><br/><br/>");

        // now display all the articles
        System.out.println("<table cellpadding=4 border=1 " +
                           "style=\"border-collapse: collapse\">");
        System.out.print("<tr><th>Slot</th>");
        for (int ii = 0; ii < BangCodes.TOWN_IDS.length; ii++) {
            System.out.print("<th>Article</th>" +
                             "<th>Coins</th><th>Scrip</th>" +
                             "<th>Coins</th><th>Scrip</th>");
        }
        System.out.println("</tr>");

        String[] cols = new String[articles.length];
        for (int ss = 0; ss < articles[0].length; ss++) {
            System.out.print("<tr style='border-top: 2px solid'>" +
                             "<th bgcolor='#CCCCCC'>" +
                             AvatarLogic.SLOTS[ss].name + "</th>");
            for (int tt = 0; tt < BangCodes.TOWN_IDS.length; tt++) {
                System.out.print("<th bgcolor='#CCCCCC' colspan=5>" +
                                 BangCodes.TOWN_IDS[tt] + "</th>");
            }
            System.out.println("</tr>");

            for (int cc = 0; cc < articles[0][ss].length; cc++) {
                int row = 0, remain;
                boolean wrotesep = false;
                do {
                    remain = 0;
                    StringBuilder buf = new StringBuilder();
                    int wrote = 0;
                    for (int tt = 0; tt < BangCodes.TOWN_IDS.length; tt++) {
                        cols[tt] = null;
                        ArticleRowList arl = articles[tt][ss][cc];
                        if (arl != null && arl.size() > row) {
                            ArticleRow arow = arl.get(row);
                            cols[tt] = arow.toHTML();
                            wrote++;
                            if (arl.size() > row+1) {
                                remain++;
                            }
                        }
                    }
                    row++;
                    if (wrote > 0) {
                        if (wrotesep) {
                            System.out.println("<tr>");
                        } else {
                            System.out.println(
                                "<tr style='border-top: 2px solid'>");
                            wrotesep = true;
                        }
                        System.out.print("<td></td>");
                        for (String col : cols) {
                            System.out.print(
                                col == null ? "<td colspan=5></td>" : col);
                        }
                        System.out.println("</tr>");
                    }
                } while (remain > 0);
            }
        }
        System.out.println("</table>");

        System.out.println("</body>");
        System.out.println("</html>");
    }
}
