//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ArrayUtil;

import com.threerings.bang.data.StatType;
import com.threerings.bang.util.BasicContext;

/**
 * A customized stats view for Forest Guardians scenarios:
 */
public class ForestGuardiansStatsView extends StatsView
{
    public ForestGuardiansStatsView (BasicContext ctx)
    {
        super(ctx, false);
    }
    
    @Override // documentation inherited
    protected void loadGameData ()
    {
        // record the most recent wave scores for display
        _scores = _bobj.stats[0].getIntArrayStat(StatType.WAVE_SCORES);
        if (_scores.length > MAX_WAVE_SCORES) {
            _wavenum = 1 + (_scores.length - MAX_WAVE_SCORES);
            _scores = ArrayUtil.splice(_scores, 0, _wavenum - 1);
        } else {
            _wavenum = 1;
        }
        _points = _bobj.stats[0].getIntStat(StatType.WAVE_POINTS);
        _icons = new ImageIcon[ICON_NAMES.length];
        for (int ii = 0; ii < ICON_NAMES.length; ii++) {
            _icons[ii] = new ImageIcon(_ctx.loadImage("ui/postgame/icons/" +
                ICON_NAMES[ii] + ".png"));
        }
        super.loadGameData();
    }
    
    @Override // documentation inherited
    protected int getObjectiveCount (int pidx)
    {
        return _scores.length;
    }
    
    @Override // documentation inherited
    protected int getObjectivePoints (int pidx)
    {
        return _points;   
    }
    
    @Override // documentation inherited
    protected void showObjective ()
    {
        super.showObjective();
        
        // add the wave performance headers
        GroupLayout layout = GroupLayout.makeHoriz(GroupLayout.LEFT);
        layout.setGap(0);
        BContainer hcont = new BContainer(layout);
        hcont.add(new Spacer(SECONDARY_OBJECTIVE_WIDTH, 1));
        final int iwidth = _icons[0].getWidth();
        _headers = new BLabel[_scores.length];
        for (int ii = 0; ii < _scores.length; ii++) {
            hcont.add(_headers[ii] = new BLabel(_msgs.get("m.wave_title",
                Integer.toString(_wavenum + ii)), "endgame_wave_header") {
                protected Dimension computePreferredSize (
                    int whint, int hhint) {
                    Dimension d = super.computePreferredSize(whint, hhint);
                    d.width = Math.max(iwidth, d.width);
                    return d;
                }
            });
            _headers[ii].setAlpha(0f);
        }
        
        _objcont.add(0, new Spacer());
        _objcont.add(1, hcont);
        _objcont.add(2, new Spacer());
    }
    
    @Override // documentation inherited
    protected BContainer objectiveIconContainer (
        int pidx, int secLabels, int maxobjectives, int maxIcons, int iwidth,
        int y)
    {
        GroupLayout layout = GroupLayout.makeHoriz(GroupLayout.LEFT);
        layout.setGap(0);
        BContainer icont = new BContainer(layout);
        for (int ii = 0; ii < _scores.length; ii++) {
            icont.add(_labels[pidx][secLabels + ii] =
                new BLabel(_icons[_scores[ii]]));
        }
        return icont;
    }
    
    @Override // documentation inherited
    protected boolean showObjectiveLabels (int idx)
    {
        int hidx = idx - 2;
        if (hidx >= 0 && hidx < _headers.length) {
            _headers[hidx].setAlpha(1f);
        }
        return super.showObjectiveLabels(idx);
    }
    
    /** The wave scores (the same for all players). */
    protected int[] _scores;
    
    /** The points earned in the waves. */
    protected int _points;
    
    /** The wave number of the first wave in the score list. */
    protected int _wavenum;
    
    /** The wave header labels. */
    protected BLabel[] _headers;
    
    /** The icons for the various performance levels. */
    protected ImageIcon[] _icons;
 
    /** The maximum number of waves for which we can fit performance levels. */
    protected static final int MAX_WAVE_SCORES = 5;
    
    /** The names of the icons to use for the performance levels. */   
    protected static final String[] ICON_NAMES = { "trees_stump",
        "trees_sprout", "trees_sapling", "trees_mature", "trees_elder" };
}
