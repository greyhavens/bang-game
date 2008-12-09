//
// $Id$

package com.threerings.bang.avatar.tools;

import java.util.HashMap;

import com.samskivert.util.ComparableArrayList;
import com.samskivert.util.StringUtil;

import com.threerings.bang.data.BangCodes;

import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AspectCatalog;

/**
 * Provides support to the article and aspect summarization tools.
 */
public class SummarizeMetadata
{
    public static class Row implements Comparable<Row>
    {
        public String name;
        public int townIdx;
        public int catIdx;

        public int[] coins = new int[2];
        public int[] scrip = new int[2];

        public Row (ArticleCatalog.Article article)
        {
            name = article.name.substring(
                article.name.startsWith("male_") ?
                "male_".length() : "female_".length());
            setCost(article);
        }

        public Row (AspectCatalog.Aspect aspect, boolean isMale)
        {
            name = aspect.name;
            setCost(aspect, isMale);
        }

        public void setCost (ArticleCatalog.Article article)
        {
            int idx = article.name.startsWith("male_") ? 0 : 1;
            coins[idx] = article.coins;
            scrip[idx] = article.scrip;
        }

        public void setCost (AspectCatalog.Aspect aspect, boolean isMale)
        {
            int idx = isMale ? 0 : 1;
            coins[idx] = aspect.coins;
            scrip[idx] = aspect.scrip;
        }

        public int compareTo (Row other)
        {
            int rv;
            if ((rv = average(coins) - average(other.coins)) != 0) {
                return rv;
            }
            if ((rv = average(scrip) - average(other.scrip)) != 0) {
                return rv;
            }
            return name.compareTo(other.name);
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

    public static class RowList extends ComparableArrayList<Row>
    {
    }

    protected static void printSummary (
        String title, String what, HashMap<String,Row> allrows, String[] cats)
    {
        RowList[][][] rows =
            new RowList[BangCodes.TOWN_IDS.length][cats.length][10];
        for (Row row : allrows.values()) {
            int cidx = row.average(row.coins);
            RowList list = rows[row.townIdx][row.catIdx][cidx];
            if (list == null) {
                list = (rows[row.townIdx][row.catIdx][cidx] = new RowList());
            }
            list.add(row);
        }
        printSummary(title, what, rows, cats);
    }

    protected static void printSummary (
        String title, String what, RowList[][][] rows, String[] cats)
    {
        // now sort all of our lists
        for (int tt = 0; tt < rows.length; tt++) {
            for (int ss = 0; ss < rows[tt].length; ss++) {
                for (int cc = 0; cc < rows[tt][ss].length; cc++) {
                    if (rows[tt][ss][cc] != null) {
                        rows[tt][ss][cc].sort();
                    }
                }
            }
        };

        System.out.println("<html><head><title>" + title + "</title></head>");
        System.out.println("<body>");
        System.out.println("<h2>" + title + "</h2>");

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
        for (int ss = 0; ss < cats.length; ss++) {
            for (int tt = 0; tt < BangCodes.TOWN_IDS.length; tt++) {
                totals[tt][0] = totals[tt][1] = 0;
                ccosts[tt][0] = ccosts[tt][1] = 0;
                scosts[tt][0] = scosts[tt][1] = 0;
            }

            for (int tt = 0; tt < rows.length; tt++) {
                for (int cc = 0; cc < rows[tt][ss].length; cc++) {
                    RowList arl = rows[tt][ss][cc];
                    if (arl != null) {
                        for (Row arow : arl) {
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

            System.out.print("<tr><td>" + cats[ss] + "</td>");
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

        // now display all the rows
        System.out.println("<table cellpadding=4 border=1 " +
                           "style=\"border-collapse: collapse\">");
        System.out.print("<tr><th>Slot</th>");
        for (int ii = 0; ii < BangCodes.TOWN_IDS.length; ii++) {
            System.out.print("<th>" + what + "</th>" +
                             "<th>Coins</th><th>Scrip</th>" +
                             "<th>Coins</th><th>Scrip</th>");
        }
        System.out.println("</tr>");

        String[] cols = new String[rows.length];
        for (int ss = 0; ss < rows[0].length; ss++) {
            System.out.print("<tr style='border-top: 2px solid'>" +
                             "<th bgcolor='#CCCCCC'>" + cats[ss] + "</th>");
            for (int tt = 0; tt < BangCodes.TOWN_IDS.length; tt++) {
                System.out.print("<th bgcolor='#CCCCCC' colspan=5>" +
                                 BangCodes.TOWN_IDS[tt] + "</th>");
            }
            System.out.println("</tr>");

            for (int cc = 0; cc < rows[0][ss].length; cc++) {
                int row = 0, remain;
                boolean wrotesep = false;
                do {
                    remain = 0;
                    int wrote = 0;
                    for (int tt = 0; tt < BangCodes.TOWN_IDS.length; tt++) {
                        cols[tt] = null;
                        RowList arl = rows[tt][ss][cc];
                        if (arl != null && arl.size() > row) {
                            Row arow = arl.get(row);
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
