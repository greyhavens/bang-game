//
// $Id$

package com.threerings.bang.game.tools;

import java.util.ArrayList;

import com.samskivert.util.CollectionUtil;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.piece.Unit;

/**
 * Generates a graph of unit versus unit damage.
 */
public class PrintDamageTable
{
    public static void main (String[] args)
    {
        if (args.length == 0) {
            System.err.println("Usage: Unit town_code\n");
            System.exit(255);
        }

        ArrayList<UnitConfig> configs = new ArrayList<UnitConfig>();
        CollectionUtil.addAll(
            configs, UnitConfig.getTownUnits(args[0], UnitConfig.Rank.BIGSHOT));
        CollectionUtil.addAll(
            configs, UnitConfig.getTownUnits(args[0], UnitConfig.Rank.NORMAL));
        CollectionUtil.addAll(
            configs, UnitConfig.getTownUnits(args[0], UnitConfig.Rank.SPECIAL));

        System.out.println("<html>");
        System.out.println("<head><title>Unit Damage Table</title></head>");
        System.out.println("<body>");
        System.out.println("<table border=1 cellpadding=2 " +
                           "style='border-collapse: collapse'>");
        System.out.print("<tr><td>Unit</td><td>Base</td>");
        for (UnitConfig hconfig : configs) {
            String name = hconfig.type.substring(
                hconfig.type.lastIndexOf("/")+1);
            System.out.print("<th>" + name.substring(0, 4) + "</th>");
        }
        System.out.print("<td>Average</td><td>Move</td><td>Shoot</td></tr>");
        for (UnitConfig config : configs) {
            String name = config.type.substring(
                config.type.lastIndexOf("/")+1);
            String bits = (config.rank == UnitConfig.Rank.BIGSHOT) ?
                " style='border: 2px solid'" : "";
            System.out.print("<tr" + bits + "><td>" + name + "</td>" +
                             "<td>" + config.damage + "</td>");
            Unit attacker = Unit.getUnit(config.type);
            int total = 0, count = 0;
            for (UnitConfig oconfig : configs) {
                Unit target = Unit.getUnit(oconfig.type);
                int damage = attacker.computeScaledDamage(null, target, 1f);
                String color = "";
                if (damage >= 100) {
                    color = " bgcolor='#FF0000'";
                } else if (damage >= 50) {
                    color = " bgcolor='#FFFF00'";
                } else if (damage > 33) {
                    color = " bgcolor='#00FF00'";
                } else if (damage >= 25) {
                    color = " bgcolor='#00FFFF'";
                }
                System.out.print("<td" + color + ">" + damage + "</td>");
                total += damage;
                count++;
            }
            System.out.println("<td>" + (total/count) + "</td>" +
                               "<td>" + config.moveDistance +"</td>" +
                               "<td>" + config.getDisplayFireDistance() +
                               "</td></tr>");
        }
        System.out.println("</table>");
        System.out.println("</body>");
        System.out.println("</html>");
    }
}
