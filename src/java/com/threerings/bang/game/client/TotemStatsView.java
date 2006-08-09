//
// $Id$

package com.threerings.bang.game.client;


import com.jmex.bui.util.Point;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;

import com.threerings.bang.data.Stat;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.game.data.scenario.TotemBuildingInfo;

import com.threerings.media.image.Colorization;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Display game stats for Totem Building.
 */
public class TotemStatsView extends StatsView
{
    public TotemStatsView (BasicContext ctx)
    {
        super(ctx);
    }

    @Override // documentation inherited
    protected void loadGameData ()
    {
        TotemBuildingInfo tbi = (TotemBuildingInfo)_bobj.scenario;
        _statTypes = tbi.getObjectives();

        // create colorized totem piece icons
        _objectiveIcons = 
            new ImageIcon[_bobj.players.length][_statTypes.length];
        Colorization[] zations;
        for (int ii = 0; ii < _objectiveIcons.length; ii++) {
            zations = new Colorization[] {
                _ctx.getAvatarLogic().getColorPository().getColorization(
                        "unit", PIECE_COLOR_IDS[ii + 1] ) };
            for (int jj = 0; jj < _objectiveIcons[ii].length; jj++) {
                _objectiveIcons[ii][jj] = new ImageIcon(
                        _ctx.getImageCache().createColorizedBImage(
                            "ui/postgame/icons/" +
                            _statTypes[jj].toString().toLowerCase() + ".png",
                            zations, true));
            }
        }
        _objectiveTitle = "m.title_totems_stacked";
        _objectivePoints = "m.totems_stacked_points";
        _showMultiplier = false;

        _secStatType = tbi.getSecondaryObjective();
        String sobj = _secStatType.toString().toLowerCase();
        _objectiveIcon = _secIcon = new ImageIcon(
                _ctx.loadImage("ui/postgame/icons/" + sobj + ".png"));

        // calculate the total scenario points for each player
        _scenPoints = new int[_bobj.players.length];
        _objectives = new int[_bobj.players.length];
        int[] ppo = tbi.getPointsPerObjectives();
        int objSum;
        for (int ii = 0; ii < _scenPoints.length; ii++) {
            _scenPoints[ii] = getIntStat(ii, _secStatType);
            objSum = 0;
            for (int jj = 0; jj < _statTypes.length; jj++) {
                int objs = getIntStat(ii, _statTypes[jj]);
                objSum += objs;
                _scenPoints[ii] += ppo[jj] * objs;
            }
            _objectives[ii] = objSum;
        }
    }

    @Override // documentation inherited
    protected BContainer objectiveIconContainer (int pidx, int secLabels,
            int maxobjectives, int maxIcons, int iwidth, int y)
    {
        iwidth--;
        BContainer icont = new BContainer(new AbsoluteLayout());
        int[] totems = new int[_objectiveIcons[pidx].length];
        for (int ii = 0; ii < totems.length; ii++) {
            totems[ii] = getIntStat(pidx, _statTypes[ii]);
        }
        // Add the objective icons
        int idx = 0;
        for (int jj = 0; jj < _objectives[pidx]; jj++) {
            int x = 0;
            while (totems[idx] == 0) {
                idx++;
            }
            totems[idx]--;
            _labels[pidx][jj + secLabels] = new BLabel(
                    _objectiveIcons[pidx][idx]);
            if (maxobjectives > maxIcons) {
                x += jj * (maxIcons - 1) * iwidth /
                    (maxobjectives - 1);
            } else {
                x += jj * iwidth;
            }
            icont.add(_labels[pidx][jj + secLabels], new Point(x, y));
        }
        return icont;
    }

    protected Stat.Type[] _statTypes;
    protected ImageIcon[][] _objectiveIcons;
}
