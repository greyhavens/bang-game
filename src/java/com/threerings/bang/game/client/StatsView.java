//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.Spacer;
import com.jmex.bui.background.ImageBackground;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.StringUtil;

import com.threerings.media.image.Colorization;
import com.threerings.util.MessageBundle;

import com.threerings.stats.data.StatSet;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.StatType;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays game stats.
 */
public class StatsView extends SteelWindow
    implements ActionListener
{
    /**
     * Creates a new stats view.
     *
     * @param recolor if true, recolor the primary object icons to match the
     * player colors
     */
    public StatsView (BasicContext ctx, boolean recolor)
    {
        super(ctx, ctx.xlate(GameCodes.GAME_MSGS, "m.stats_title"));
        _ctx = ctx;
        _recolor = recolor;
    }

    /**
     * Used to initialize the stats view.
     */
    public void init (BangController ctrl, BangObject bangobj, boolean animate)
    {
        setLayer(1);

        _ctrl = ctrl;
        _bobj = bangobj;
        _animate = animate;
        _msgs = _ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);

        if (_ctx instanceof BangContext) {
            _bctx = (BangContext)_ctx;
            ((BangContext)_ctx).getBangClient().fadeOutMusic(2f);
        }

        if (bangobj.state == BangObject.GAME_OVER) {
            _closeBtn = new BButton(_msgs.get("m.results"), this, "results");
        } else {
            _closeBtn = new BButton(_msgs.get("m.next_round"),
                         this, "next_round");
        }
        _closeBtn.setEnabled(false);
        _buttons.add(_closeBtn);

        _contents.setLayoutManager(new BorderLayout());
        _contents.setPreferredSize(CONTENT_DIMS);

        // add forward and back buttons
        GroupLayout hlay = GroupLayout.makeHoriz(GroupLayout.RIGHT);
        hlay.setGap(40);
        BContainer bcont = new BContainer(hlay);
        bcont.add(_back = new BButton("", this, "back"));
        _back.setStyleClass("back_button");
        _back.setEnabled(false);
        bcont.add(_forward = new BButton("", this, "forward"));
        _forward.setStyleClass("fwd_button");
        _forward.setEnabled(false);
        _contents.add(bcont, BorderLayout.SOUTH);

        loadGameData();
        if (animate) {
            showObjective();
        } else {
            showPoints(false);
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("results")) {
            _closeBtn.setEnabled(false);
            _bctx.getBangClient().clearPopup(this, true);
            _bctx.getBangClient().displayPopup(
                    new GameOverView(_bctx, _ctrl, _bobj, _animate), true);
            _animate = false;
        } else if (action.equals("next_round")) {
            startNextRound();
        } else if (action.equals("forward")) {
            showPage(++_page);
        } else if (action.equals("back")) {
            showPage(--_page);
        }
    }

    /**
     * Display a new page.
     */
    protected void showPage (int page)
    {
        switch (page) {
        case 0:
            showPoints(false);
            break;
        case 1:
            // if the game is over, show the overall stats
            if (_bobj.state == BangObject.GAME_OVER) {
                // but if this was a one round game, skip the summarizing
                showStats(_bobj.roundId > 0 ? -1 : 0);
            } else {
                // otherwise show the just finished round's stats; the server has already advanced
                // roundId to the next round, so we have to back up one
                showStats(_bobj.roundId-1);
            }
            break;
        default:
            showStats(_bobj.roundId + (2-page)); // show the rounds in reverse order
            break;
        }

        if (_bobj.state == BangObject.GAME_OVER && _bobj.roundId > 0) {
            _forward.setEnabled(page < _bobj.roundId + 2);
        } else {
            _forward.setEnabled(page < 1);
        }
        _back.setEnabled(page > 0);
    }

    /**
     * Load media associated with the game data.
     */
    protected void loadGameData ()
    {
        _statTypes = _bobj.scenario.getObjectives();

        // create (possibly colorized) objective icons
        _objectiveIcons =
            new ImageIcon[_bobj.players.length][_statTypes.length];
        Colorization[] zations = null;
        for (int ii = 0; ii < _objectiveIcons.length; ii++) {
            if (_recolor) {
                zations = new Colorization[] {
                    _ctx.getAvatarLogic().getColorPository().getColorization(
                        "unit", colorLookup[ii + 1] + 1 ) };
            }
            for (int jj = 0; jj < _objectiveIcons[ii].length; jj++) {
                String path = "ui/postgame/icons/" +
                    StringUtil.toUSLowerCase(_statTypes[jj].toString()) + ".png";
                _objectiveIcons[ii][jj] = new ImageIcon(_recolor ?
                    _ctx.getImageCache().createColorizedBImage(
                        path, zations, true) :
                    _ctx.loadImage(path));
            }
        }
        String ocode = _bobj.scenario.getObjectiveCode();
        _objectiveTitle = "m.title_" + ocode;
        _objectivePoints = "m." + ocode + "_points";
        _showMultiplier = (_statTypes.length == 1);

        if (_bobj.scenario.getSecondaryObjective() != null) {
            _secStatType = _bobj.scenario.getSecondaryObjective();
            String sobj = StringUtil.toUSLowerCase(_secStatType.toString());
            _secIcon = new ImageIcon(
                _ctx.loadImage("ui/postgame/icons/" + sobj + ".png"));
        }

        // calculate the total scenario points for each player
        _scenPoints = new int[_bobj.players.length];
        _objectives = new int[_bobj.players.length];

        for (int ii = 0; ii < _scenPoints.length; ii++) {
            _scenPoints[ii] = (_secStatType == null) ? 0 :
                getIntStat(ii, _secStatType);
            _objectives[ii] = getObjectiveCount(ii);
            _scenPoints[ii] += getObjectivePoints(ii);
        }
    }

    /**
     * Gets the total number of primary objectives attained by the
     * specified player.
     */
    protected int getObjectiveCount (int pidx)
    {
        int count = 0;
        for (StatType type : _statTypes) {
            count += getIntStat(pidx, type);
        }
        return count;
    }

    /**
     * Gets the total primary objective points earned by the specified
     * player.
     */
    protected int getObjectivePoints (int pidx)
    {
        int points = 0;
        int[] ppo = _bobj.scenario.getPointsPerObjectives();
        for (int ii = 0; ii < _statTypes.length; ii++) {
            points += ppo[ii] * getIntStat(pidx, _statTypes[ii]);
        }
        return points;
    }

    /**
     * Sets the primary contents.
     */
    protected void setContents (BContainer cont)
    {
        if (_currcont == cont) {
            return;
        } else if (_currcont != null) {
            _contents.remove(_currcont);
        }
        _currcont = cont;
        _contents.add(_currcont, BorderLayout.CENTER);
    }

    /**
     * Convenience function to get int stat values for a player.
     */
    protected int getIntStat (int pidx, StatType type)
    {
        return getIntStat(pidx, _bobj.stats, type);
    }

    /**
     * Convenience function to get int stat values for a player.
     */
    protected int getIntStat (int pidx, StatSet[] stats, StatType type)
    {
        if (stats == null || pidx >= stats.length || stats[pidx] == null) {
            return 0;
        }
        return stats[pidx].getIntStat(type);
    }

    /**
     * Show, and possibly animated, the total game objectives met by
     * the players.
     */
    protected void showObjective ()
    {
        _contents.add(_header = new BLabel(_msgs.get(
                        "m.game_title", _msgs.xlate(_objectiveTitle)),
                "endgame_title"), BorderLayout.NORTH);
        _header.setPreferredSize(new Dimension(300, HEADER_HEIGHT));

        setContents(_objcont = new BContainer());
        _objcont.setPreferredSize(TABLE_DIMS);
        _objcont.setLayoutManager(new TableLayout(3, 2, 3));

        int iwidth = _objectiveIcons[0][0].getWidth() + 1;
        int size = _bobj.players.length;
        _labels = new BLabel[size][];
        int maxobjectives = 0;
        for (int ii = 0; ii < _objectives.length; ii++) {
            maxobjectives = Math.max(maxobjectives, _objectives[ii]);
        }

        int maxIcons = MAX_ICONS;
        int secLabels = 0;
        int offset = 0;
        if (_secStatType != null) {
            maxIcons -= 2;
            secLabels = 3;
            offset = SECONDARY_OBJECTIVE_WIDTH;
        }
        if (!_showMultiplier) {
            maxIcons++;
        }

        for (int ii = 0; ii < size; ii++) {
            // Add the avatar
            AvatarView aview = makeAvatarView(ii);
            _objcont.add(aview);

            GroupLayout hlay = GroupLayout.makeHStretch();
            hlay.setGap(0);
            BContainer cont = new BContainer(hlay);
            cont.setPreferredSize(new Dimension(400, 50));
            int secondary = 0;
            _labels[ii] = new BLabel[_objectives[ii] + secLabels + (_showMultiplier ? 2 : 1)];
            Dimension apref = aview.getPreferredSize(-1, -1);
            int y = (apref.height - _objectiveIcons[0][0].getHeight()) / 2;

            // add secondary labels if available
            if (_secStatType != null) {
                secondary = getIntStat(ii, _secStatType);
                _labels[ii][0] = new BLabel(_secIcon);
                cont.add(_labels[ii][0], GroupLayout.FIXED);
                final int lwidth = offset - iwidth - 20;
                _labels[ii][1] = new BLabel("" + secondary, "endgame_total") {
                    protected Dimension computePreferredSize (int hhint, int vhint) {
                        Dimension d = super.computePreferredSize(hhint, vhint);
                        d.width = Math.max(lwidth, d.width);
                        return d;
                    }
                };
                cont.add(_labels[ii][1], GroupLayout.FIXED);
                cont.add(_labels[ii][2] = new BLabel("+", "endgame_smalltotal"), GroupLayout.FIXED);
                if (_objectives[ii] == 0) {
                    _labels[ii][2].setAlpha(0f);
                    _labels[ii][2] = new BLabel("");
                }
            }

            cont.add(objectiveIconContainer(ii, secLabels, maxobjectives, maxIcons, iwidth, y),
                     GroupLayout.FIXED);

            // Add the multiplier label
            if (_showMultiplier) {
                _labels[ii][_objectives[ii] + secLabels] = new BLabel(
                    _msgs.xlate(MessageBundle.tcompose("m.multiplier", _objectives[ii])),
                    "endgame_total");
                if (_objectives[ii] > 0 || _secStatType == null) {
                    cont.add(_labels[ii][_objectives[ii] + secLabels]);
                } else {
                    cont.add(new Spacer(1, 1));
                }
            } else {
                cont.add(new Spacer(1, 1));
            }
            _objcont.add(cont);

            // Add the total label
            _labels[ii][_labels[ii].length - 1] = new BLabel(
                _msgs.xlate(MessageBundle.tcompose("m.equals", _scenPoints[ii])), "endgame_total");
            _objcont.add(_labels[ii][_labels[ii].length - 1]);

            // Start everything as invisible
            for (int jj = 0; jj < _labels[ii].length; jj++) {
                _labels[ii][jj].setAlpha(0f);
            }
        }

        if (isAdded()) {
            startObjectiveAnimation();
        } else {
            _startAnimationWhenAdded = true;
        }
    }

    /**
     * Returns a container with all the primary and secondary objective icons and scoring values.
     */
    protected BContainer objectiveIconContainer (
        int pidx, int secLabels, int maxobjectives, int maxIcons, int iwidth, int y)
    {
        iwidth--;
        BContainer icont = new BContainer(new AbsoluteLayout());
        int[] objs = new int[_objectiveIcons[pidx].length];
        for (int ii = 0; ii < objs.length; ii++) {
            objs[ii] = getIntStat(pidx, _statTypes[ii]);
        }
        // Add the objective icons
        int idx = 0;
        for (int jj = 0; jj < _objectives[pidx]; jj++) {
            int x = 0;
            while (objs[idx] == 0) {
                idx++;
            }
            objs[idx]--;
            _labels[pidx][jj + secLabels] = new BLabel(_objectiveIcons[pidx][idx]);
            if (maxobjectives > maxIcons) {
                x += jj * (maxIcons - 1) * iwidth / (maxobjectives - 1);
            } else {
                x += jj * iwidth;
            }
            icont.add(_labels[pidx][jj + secLabels], new Point(x, y));
        }
        return icont;
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _closeBtn.setEnabled(true);
        if (_startAnimationWhenAdded) {
            _startAnimationWhenAdded = false;
            startObjectiveAnimation();
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        if (_countdown != null) {
            _countdown.cancel();
        }
    }

    /**
     * Called to animate the objective icons.
     */
    protected void startObjectiveAnimation ()
    {
        // add an interval to have the icons appear in sequence after a short delay
        _showing = 0;
        _closeBtn.setEnabled(false);
        new Interval(_ctx.getApp()) {
            public void expired () {
                if (!showObjectiveLabels(_showing)) {
                    BangUI.play(BangUI.FeedbackSound.CHAT_SEND);
                    for (int ii = 0; ii < _labels.length; ii++) {
                        int length = _labels[ii].length;
                        _labels[ii][length - 1].setAlpha(1f);
                    }
                    new Interval(_ctx.getApp()) {
                        public void expired () {
                            showPoints(true);
                        }
                    }.schedule(OBJECTIVE_DISPLAY);
                } else {
                    BangUI.play(BangUI.FeedbackSound.CHAT_RECEIVE);
                    _showing++;
                    this.schedule(ANIM_DELAY);
                }
            }
        }.schedule(1000L);
    }

    /**
     * Shows all of the objective labels at the specified index for all
     * players.
     *
     * @return true if new objective labels were made visible, false if we
     * have reached the end of the objective labels for all players
     */
    protected boolean showObjectiveLabels (int idx)
    {
        boolean showed = false;
        for (BLabel[] labels : _labels) {
            if (idx < labels.length - 1) {
                labels[idx].setAlpha(1f);
                showed = true;
            }
        }
        return showed;
    }

    /**
     * Show and possibly animate the game points.
     */
    protected void showPoints (boolean animate)
    {
        _contents.remove(_header);

        if (_ptscont == null) {
            int height = HEADER_HEIGHT - 2;
            int size = _bobj.players.length;

            setContents(_ptscont = new BContainer(new TableLayout(8, 2, 5)));
            _ptscont.setPreferredSize(TABLE_DIMS);

            // add the titles
            if (_bobj.roundId > 0 || _bobj.state != BangObject.GAME_OVER) {
                // the server starts the new round immediately, so _bangobj.roundId is already set
                // to the next round when we're showing points for the previous round; so unless
                // the game is over (in which case roundId will remain at the last round's id) we
                // need to account for the roundId mismatch
                int round = _bobj.roundId + (_bobj.state == BangObject.GAME_OVER ? 1 : 0);
                String msg = MessageBundle.tcompose("m.stats_round_header", round);
                _ptscont.add(new BLabel(_msgs.xlate(msg), "endgame_title"));
            } else {
                _ptscont.add(new Spacer(0, height));
            }
            _ptscont.add(new BLabel(_msgs.get(_objectivePoints), "endgame_smallheader"));
            _ptscont.add(new Spacer(0, height));
            _ptscont.add(new BLabel(_msgs.get("m.damage_points"), "endgame_smallheader"));
            _ptscont.add(new Spacer(0, height));
            _ptscont.add(new BLabel(_msgs.get("m.star_points"), "endgame_smallheader"));
            _ptscont.add(new Spacer(0, height));
            _ptscont.add(new BLabel(_msgs.get("m.total"), "endgame_header"));

            BIcon damageIcon = new ImageIcon(_ctx.loadImage("ui/postgame/icons/damage.png"));
            BIcon starIcon = new ImageIcon(_ctx.loadImage("ui/postgame/icons/star.png"));

            // add the data
            _labels = new BLabel[size][];
            for (int ii = 0; ii < size; ii++) {
                BLabel[] labels = new BLabel[7];
                int points = getIntStat(ii, StatType.POINTS_EARNED);
                int starPoints = getIntStat(ii, StatType.BONUS_POINTS);
                int damagePoints = points - _scenPoints[ii] - starPoints;
                _ptscont.add(makeAvatarView(ii));
                _ptscont.add(labels[0] = new BLabel(
                        String.valueOf(_scenPoints[ii]), "endgame_smalltotal"));
                labels[0].setIcon(_statTypes.length == 1 ? _objectiveIcons[0][0] : _secIcon);
                _ptscont.add(labels[1] = new BLabel("+", "endgame_smalltotal"));
                _ptscont.add(labels[2] = new BLabel(
                        String.valueOf(damagePoints), "endgame_smalltotal"));
                labels[2].setIcon(damageIcon);
                _ptscont.add(labels[3] = new BLabel("+", "endgame_smalltotal"));
                _ptscont.add(labels[4] = new BLabel(
                        String.valueOf(starPoints), "endgame_smalltotal"));
                labels[4].setIcon(starIcon);
                _ptscont.add(labels[5] = new BLabel("=", "endgame_total"));
                _ptscont.add(labels[6] = new BLabel(String.valueOf(points), "endgame_total"));
                _labels[ii] = labels;
            }
        } else {
            setContents(_ptscont);
        }

        // Add an interval to have the icons appear in sequence after a short delay
        if (animate) {
            for (int ii = 0; ii < _labels.length; ii++) {
                for (int jj = 0; jj < _labels[ii].length; jj++) {
                    _labels[ii][jj].setAlpha(0f);
                }
            }
            _showing = 0;
            new Interval(_ctx.getApp()) {
                public void expired () {
                    for (int ii = 0; ii < _labels.length; ii++) {
                        if (_showing < _labels[ii].length) {
                            _labels[ii][_showing].setAlpha(1f);
                        }
                    }
                    _showing++;
                    if (_showing < _labels[0].length) {
                        BangUI.play(BangUI.FeedbackSound.CHAT_RECEIVE);
                        this.schedule(ANIM_DELAY);
                    } else {
                        BangUI.play(BangUI.FeedbackSound.CHAT_SEND);
                        _forward.setEnabled(true);
                        _closeBtn.setEnabled(true);
                        startCountdown();
                    }
                }
            }.schedule(1000L);
        } else {
            _forward.setEnabled(true);
            _closeBtn.setEnabled(true);
            startCountdown();
        }
    }

    /**
     * Shows the detailed stats view.
     */
    protected void showStats (int roundId)
    {
        BContainer statcont = _statmap.get(roundId);
        if (statcont != null) {
            setContents(statcont);
            return;
        }

        BImage dark = _ctx.loadImage("ui/postgame/dark_box_background.png");
        BImage light = _ctx.loadImage("ui/postgame/box_background.png");
        final ImageBackground darkbg = new ImageBackground(
                ImageBackground.CENTER_XY, dark);
        final ImageBackground lightbg = new ImageBackground(
                ImageBackground.CENTER_XY, light);
        final int width = dark.getWidth();
        int height = dark.getHeight();

        GroupLayout hlay = GroupLayout.makeHoriz(GroupLayout.STRETCH,
                GroupLayout.CENTER, GroupLayout.NONE);
        hlay.setOffAxisJustification(GroupLayout.TOP);
        hlay.setGap(10);
        statcont = new BContainer(hlay);
        statcont.setPreferredSize(TABLE_DIMS);
        setContents(statcont);
        _statmap.put(roundId, statcont);

        GroupLayout vlay = GroupLayout.makeVert(GroupLayout.TOP);
        vlay.setGap(2);
        BContainer avcont = new BContainer(vlay);
        statcont.add(avcont, GroupLayout.FIXED);

        // add a round header if necessary
        String roundheader = null;
        if (_bobj.roundId != 0 || _bobj.state != BangObject.GAME_OVER) {
            if (roundId == -1) {
                roundheader = _msgs.get("m.overall");
            } else if (_bobj.roundId > 0) {
                roundheader = _msgs.xlate(MessageBundle.tcompose("m.stats_round_header", roundId+1));
            }
        }
        if (roundheader != null) {
            avcont.add(new BLabel(roundheader, "endgame_title") {
                protected Dimension computePreferredSize (int hhint, int vhint) {
                    Dimension d = super.computePreferredSize(hhint, vhint);
                    d.height = Math.max(HEADER_HEIGHT - 2, d.height);
                    return d;
                }
            }, GroupLayout.FIXED);
        } else {
            avcont.add(new Spacer(0, HEADER_HEIGHT - 2), GroupLayout.FIXED);
        }

        // add the avatars
        int size = _bobj.players.length;
        for (int ii = 0; ii < size; ii++) {
            avcont.add(makeAvatarView(ii));
        }

        statcont.add(new Spacer(1, 1));

        // get the statSet for this round, or generate a cummulative set for an overall display
        StatSet[] statSet;
        if (roundId == -1) {
            statSet = new StatSet[_bobj.stats.length];
            for (int ii = 0; ii < statSet.length; ii++) {
                statSet[ii] = new StatSet();
            }
            for (int rr = 0; rr <= _bobj.roundId; rr++) {
                StatSet[] tmpset = _ctrl.getStatSetArray(rr);
                for (int ii = 0; ii < statSet.length; ii++) {
                    for (StatType type : BASE_STAT_TYPES) {
                        statSet[ii].incrementStat(type, getIntStat(ii, tmpset, type));
                    }
                }
            }
        } else {
            statSet = _ctrl.getStatSetArray(roundId);
        }
        if (statSet == null) {
            return;
        }

        // which stats are we displaying
        ArrayList<StatType> statTypes = new ArrayList<StatType>();
        for (StatType type : BASE_STAT_TYPES) {
            boolean interesting = false;
            for (int ii = 0; ii < size; ii++) {
                if (getIntStat(ii, statSet, type) > 0) {
                    interesting = true;
                    break;
                }
            }
            if (interesting) {
                statTypes.add(type);
            }
        }

        // setup our scrollable stats grid
        BContainer stats = new BContainer(
                new TableLayout(statTypes.size(), 0, 0));
        Dimension statsize = new Dimension(
                width * statTypes.size(), height * size + HEADER_HEIGHT + 2);
        stats.setPreferredSize(statsize);
        BScrollPane scrollpane = new BScrollPane(stats, false, true, width) {
            protected Dimension computePreferredSize (int hhint, int vhint)
            {
                Dimension d = super.computePreferredSize(hhint, vhint);
                d.width = Math.min(width * NUM_VIEWABLE_COLS, d.width);
                return d;
            }
        };
        statcont.add(scrollpane, GroupLayout.FIXED);
        statcont.add(new Spacer(20, 0), GroupLayout.FIXED);

        Dimension boxdim = new Dimension(width, height);
        HashMap<StatType, Integer> map = new HashMap<StatType, Integer>();

        // Add the headers
        for (Iterator<StatType> iter = statTypes.iterator();
                iter.hasNext(); ) {
            StatType type = iter.next();
            String key = "m.header_" + StringUtil.toUSLowerCase(type.name());
            BLabel header = new BLabel(_msgs.get(key), "endgame_smallheader") {
                protected Dimension computePreferredSize (
                        int hhint, int vhint) {
                    Dimension d = super.computePreferredSize(hhint, vhint);
                    d.height = Math.max(HEADER_HEIGHT + 2, d.height);
                    return d;
                }
            };
            stats.add(header);
            int max = 0;
            for (int ii = 0; ii < size; ii++) {
                max = Math.max(max, getIntStat(ii, statSet, type));
            }
            map.put(type, new Integer(max));
        }

        // Add the stat details
        for (int ii = 0; ii < size; ii++) {
            for (Iterator<StatType> iter = statTypes.iterator();
                    iter.hasNext(); ) {
                StatType type = iter.next();
                final boolean isDark = (ii % 2 == 0);
                BContainer cont = new BContainer(new BorderLayout()) {
                    protected void wasAdded() {
                        super.wasAdded();
                        setBackground(DEFAULT, isDark ? darkbg : lightbg);
                    }
                };
                cont.setPreferredSize(boxdim);
                stats.add(cont);
                int value = getIntStat(ii, statSet, type);
                String styleclass = "endgame_stattotal";
                if (value == map.get(type).intValue()) {
                    styleclass += "high";
                }
                cont.add(new BLabel(String.valueOf(value), styleclass),
                         BorderLayout.CENTER);
            }
        }
    }

    /**
     * Convenience function for generating an AvatarView.
     */
    protected AvatarView makeAvatarView (int idx)
    {
        AvatarView aview = new AvatarView(_ctx, 8, false, true, colorLookup[idx + 1]);
        aview.setAvatar(_bobj.playerInfo[idx].avatar);
        aview.setText(_bobj.players[idx].toString());
        aview.setStyleClass("endgame_player");
        Dimension d = aview.getPreferredSize(-1, -1);
        d.height = Math.max(GRID_HEIGHT, d.height);
        aview.setPreferredSize(d);

        return aview;
    }

    /**
     * Starts the auto advance to next round countdown.
     */
    protected void startCountdown ()
    {
        // no need to countdown if we've already started or the game is over
        if (_bobj.state == BangObject.GAME_OVER || _countdown != null) {
            return;
        }

        // start our countdown timer
        _countdown = new Interval(_ctx.getApp()) {
            public void expired () {
                startNextRound();
            }
        };
        _countdown.schedule(GameCodes.STATS_TIMEOUT, false);
    }

    protected void startNextRound ()
    {
        _closeBtn.setEnabled(false);
        _bctx.getBangClient().clearPopup(this, true);
        _ctrl.statsDismissed();
    }

    /** Reference to our various game objects. */
    protected BasicContext _ctx;
    protected BangContext _bctx;
    protected BangController _ctrl;
    protected BangObject _bobj;
    protected MessageBundle _msgs;

    /** Whether or not to recolor primary objective icons. */
    protected boolean _recolor;

    /** Content layouts that can be toggled through. */
    protected BButton _back, _forward, _closeBtn;
    protected BContainer _objcont, _ptscont, _currcont;
    protected HashIntMap<BContainer> _statmap = new HashIntMap<BContainer>();
    protected BLabel _header;

    /** Used for displaying labels after a delay. */
    protected BLabel[][] _labels;

    /** Information on the game scenario. */
    protected StatType[] _statTypes;
    protected StatType _secStatType;
    protected ImageIcon[][] _objectiveIcons;
    protected BIcon _secIcon;
    protected String _objectiveTitle;
    protected String _objectivePoints;
    protected int[] _scenPoints;
    protected int[] _objectives;
    protected boolean _showMultiplier = true;

    /** Counter for animation steps. */
    protected int _showing;

    /** If we're in animation mode. */
    protected boolean _animate;

    /** Which page is currently displayed.*/
    protected int _page = 0;

    /** Interval that will auto advance the next round. */
    protected Interval _countdown;

    /** Set to true if we animate when the window is added to the hierarchy. */
    protected boolean _startAnimationWhenAdded = false;

    protected static final long ANIM_DELAY = 300L;
    protected static final long OBJECTIVE_DISPLAY = 2000L;
    protected static final int MAX_ICONS = 6;

    protected static final Dimension TABLE_DIMS = new Dimension(630, 300);
    protected static final Dimension CONTENT_DIMS = new Dimension(630, 500);
    protected static final int HEADER_HEIGHT = 40;
    protected static final int GRID_HEIGHT = 100;
    protected static final int NUM_VIEWABLE_COLS = 6;

    /** The width of the secondary objective icon, points, and plus sign. */
    protected static final int SECONDARY_OBJECTIVE_WIDTH = 120;

    protected static final StatType[] BASE_STAT_TYPES = {
        // Global Stats
        StatType.DAMAGE_DEALT, StatType.UNITS_KILLED,
        StatType.BONUSES_COLLECTED, StatType.CARDS_PLAYED,
        StatType.DISTANCE_MOVED, StatType.SHOTS_FIRED,
        StatType.UNITS_LOST,
        // Cattle Rustling
        StatType.CATTLE_RUSTLED, StatType.BRAND_POINTS,
        // Claim Jumping & Gold Rush
        StatType.NUGGETS_CLAIMED,
        // Land Grab
        StatType.STEADS_CLAIMED, StatType.STEAD_POINTS,
        // Totem Building
        StatType.TOTEMS_SMALL, StatType.TOTEMS_MEDIUM,
        StatType.TOTEMS_LARGE, StatType.TOTEMS_CROWN,
        StatType.TOTEM_POINTS,
        // Wendigo Attack
        StatType.WENDIGO_SURVIVALS, StatType.TALISMAN_POINTS,
        // Forest Guardians
        StatType.TREES_SAPLING, StatType.TREES_MATURE,
        StatType.TREES_ELDER, StatType.TREE_POINTS,
        // Warpath
        StatType.HERO_LEVEL,
    };
}
