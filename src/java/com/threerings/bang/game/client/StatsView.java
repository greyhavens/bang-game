//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollBar;
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
import com.samskivert.util.RandomUtil;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.data.StatSet;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.ScenarioCodes;
import com.threerings.bang.game.util.ScenarioUtil;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;
import com.threerings.util.MessageBundle;

/**
 * Displays game stats.
 */
public class StatsView extends SteelWindow
    implements ActionListener
{
    /**
     * The constructor the game uses.
     */
    public StatsView (BangContext ctx, BangController ctrl,
                      BangObject bangobj, boolean animate)
    {
        this((BasicContext)ctx, ctrl, bangobj, animate);
        _bctx = ctx;
    }

    /**
     * The constructor the test harness uses.
     */
    public StatsView (BasicContext ctx, BangController ctrl,
                      BangObject bangobj, boolean animate)
    {
        super(ctx, ctx.xlate(GameCodes.GAME_MSGS, "m.stats_title"));
        setLayer(1);

        _ctx = ctx;
        _ctrl = ctrl;
        _bobj = bangobj;

        _msgs = ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);

        if (ctx instanceof BangContext) {
            ((BangContext)ctx).getBangClient().fadeOutMusic(2f);
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
        _contents.setPreferredSize(CONTENT_DIMENSION);
        
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
            _bctx.getBangClient().clearPopup(this, true);
            _bctx.getBangClient().displayPopup(
                    new GameOverView(_bctx, _ctrl, _bobj), true);
        } else if (action.equals("next_round")) {
            _bctx.getBangClient().clearPopup(this, true);
            _ctrl.statsDismissed();
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
            showStats(0);
            break;
          default:
            // show the rounds in reverse order
            showStats(_bobj.roundId + 2 - page);
        }
        
        if (_bobj.state == BangObject.GAME_OVER && _bobj.roundId > 1) {
            _forward.setEnabled(page < _bobj.roundId + 1);
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
        if (ScenarioUtil.cattleRustling(_bobj.scenarioId)) {
            _objectiveIcon = new ImageIcon(_ctx.loadImage(
                        "ui/postgame/icons/cattle.png"));
            _objectiveTitle = "m.title_cattle_rustled";
            _objectivePoints = "m.cattle_points";
            _statType = Stat.Type.CATTLE_RUSTLED;
            _ppo = ScenarioCodes.POINTS_PER_COW;
        } else if (ScenarioUtil.nuggetClaiming(_bobj.scenarioId)) {
            _objectiveIcon = new ImageIcon(_ctx.loadImage(
                        "ui/postgame/icons/nugget.png"));
            _objectiveTitle = "m.title_nuggets_claimed";
            _objectivePoints = "m.nugget_points";
            _statType = Stat.Type.NUGGETS_CLAIMED;
            _ppo = ScenarioCodes.POINTS_PER_NUGGET;
        }
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
    protected int getIntStat (int pidx, Stat.Type type)
    {
        return getIntStat(pidx, _bobj.stats, type); 
    }

    /**
     * Convenience function to get int stat values for a player.
     */
    protected int getIntStat (int pidx, StatSet[] stats, Stat.Type type)
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
        if (_statType == null) {
            showPoints(false);
            return;
        }

        _contents.add(_header = new BLabel(_msgs.get(
                        "m.game_title", _msgs.xlate(_objectiveTitle)), 
                "endgame_title"), BorderLayout.NORTH);
        _header.setPreferredSize(new Dimension(300, HEADER_HEIGHT));

        setContents(_objcont = new BContainer());
        _objcont.setPreferredSize(TABLE_DIMENSION);
        _objcont.setLayoutManager(new TableLayout(3, 2, 10));

        int maxobjectives = 0;
        int iwidth = _objectiveIcon.getWidth() + 1;
        int size = _bobj.players.length;
        _labels = new BLabel[size][];

        // find the max objective count
        for (int ii = 0; ii < size; ii++) {
            maxobjectives = Math.max(maxobjectives, getIntStat(ii, _statType));
        }

        for (int ii = 0; ii < size; ii++) {
            // Add the avatar
            AvatarView aview = makeAvatarView(ii);
            _objcont.add(aview);

            // Add the objective icons
            BContainer cont = new BContainer(GroupLayout.makeHStretch());
            BContainer icont = new BContainer(new AbsoluteLayout());
            cont.add(icont, GroupLayout.FIXED);
            cont.setPreferredSize(new Dimension(385, 50));
            int objectives = getIntStat(ii, _statType);
            _labels[ii] = new BLabel[objectives + 2];
            Dimension apref = aview.getPreferredSize(-1, -1);
            int y = (apref.height - _objectiveIcon.getHeight()) / 2;
            for (int jj = 0; jj < objectives; jj++) {
                int x;
                _labels[ii][jj] = new BLabel(_objectiveIcon);
                if (maxobjectives > MAX_ICONS) {
                    x = jj * (MAX_ICONS - 1) * iwidth /
                        (maxobjectives - 1);
                } else {
                    x = jj * iwidth;
                }
                icont.add(_labels[ii][jj], new Point(x, y));
            }

            // Add the multiplier label
            _labels[ii][objectives] = new BLabel(_msgs.xlate(
                        MessageBundle.tcompose("m.multiplier", objectives)),
                    "endgame_total");
            cont.add(_labels[ii][objectives]);
            _objcont.add(cont);

            // Add the total label
            _labels[ii][objectives + 1] = new BLabel(_msgs.xlate(
                        MessageBundle.tcompose("m.equals", objectives * _ppo)),
                    "endgame_total");
            _objcont.add(_labels[ii][objectives + 1]);

            // Start everything as invisible
            for (int jj = 0; jj < _labels[ii].length; jj++) {
                _labels[ii][jj].setAlpha(0f);
            }
        }

        // Add an interval to have the icons appear in sequence after
        // a short delay
        _showing = 0;
        Interval showObjectives = new Interval(_ctx.getApp()) {
            public void expired () {
                boolean noshow = true;
                for (int ii = 0; ii < _labels.length; ii++) {
                    if (_showing < _labels[ii].length - 1) {
                        _labels[ii][_showing].setAlpha(1f);
                        noshow = false;
                    }
                }
                if (noshow) {
                    BangUI.play(BangUI.FeedbackSound.CHAT_SEND);
                    for (int ii = 0; ii < _labels.length; ii++) {
                        int length = _labels[ii].length;
                        _labels[ii][length - 1].setAlpha(1f);
                    }
                    Interval showPoints = new Interval(_ctx.getApp()) {
                        public void expired () {
                            showPoints(true);
                        }
                    };
                    showPoints.schedule(OBJECTIVE_DISPLAY);
                } else {
                    BangUI.play(BangUI.FeedbackSound.CHAT_RECEIVE);
                    _showing++;
                    this.schedule(ANIM_DELAY);
                }
            }
        };
        showObjectives.schedule(1000L);
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
            _ptscont.setPreferredSize(TABLE_DIMENSION);

            // add the titles
            if (_bobj.roundId > 1 || _bobj.state != BangObject.GAME_OVER) {
                _ptscont.add(new BLabel(_msgs.xlate(MessageBundle.tcompose(
                                    "m.stats_round_header", _bobj.roundId)),
                            "endgame_title"));
            } else {
                _ptscont.add(new Spacer(0, height));
            }
            _ptscont.add(new BLabel(_msgs.get(_objectivePoints), 
                        "endgame_smallheader"));
            _ptscont.add(new Spacer(0, height));
            _ptscont.add(new BLabel(_msgs.get("m.damage_points"),
                        "endgame_smallheader"));
            _ptscont.add(new Spacer(0, height));
            _ptscont.add(new BLabel(_msgs.get("m.star_points"),
                        "endgame_smallheader"));
            _ptscont.add(new Spacer(0, height));
            _ptscont.add(new BLabel(_msgs.get("m.total"),
                        "endgame_header"));

            BIcon damageIcon = new ImageIcon(_ctx.loadImage(
                            "ui/postgame/icons/damage.png"));
            BIcon starIcon = new ImageIcon(_ctx.loadImage(
                            "ui/postgame/icons/star.png"));

            _labels = new BLabel[size][];

            // add the data
            for (int ii = 0; ii < size; ii++) {
                BLabel[] labels = new BLabel[7];
                int objectives = getIntStat(ii, _statType);
                int points = getIntStat(ii, Stat.Type.POINTS_EARNED); 
                int objPoints = objectives * _ppo;
                int starPoints = getIntStat(ii, Stat.Type.BONUS_POINTS);
                int damagePoints = points - objPoints - starPoints;
                _ptscont.add(makeAvatarView(ii));
                _ptscont.add(labels[0] = new BLabel(
                        String.valueOf(objPoints), "endgame_smalltotal"));
                labels[0].setIcon(_objectiveIcon);
                _ptscont.add(labels[1] = new BLabel("+", "endgame_smalltotal"));
                _ptscont.add(labels[2] = new BLabel(
                        String.valueOf(damagePoints), "endgame_smalltotal"));
                labels[2].setIcon(damageIcon);
                _ptscont.add(labels[3] = new BLabel("+", "endgame_smalltotal"));
                _ptscont.add(labels[4] = new BLabel(
                        String.valueOf(starPoints), "endgame_smalltotal"));
                labels[4].setIcon(starIcon);
                _ptscont.add(labels[5] = new BLabel("=", "endgame_total"));
                _ptscont.add(labels[6] = new BLabel(
                        String.valueOf(points), "endgame_total"));
                _labels[ii] = labels;
            }
        } else {
            setContents(_ptscont);
        }
        
        // Add an interval to have the icons appear in sequence after
        // a short delay
        if (animate) {
            for (int ii = 0; ii < _labels.length; ii++) {
                for (int jj = 0; jj < _labels[ii].length; jj++) {
                    _labels[ii][jj].setAlpha(0f);
                }
            }
            _showing = 0;
            Interval showTotals = new Interval(_ctx.getApp()) {
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
                    }
                }
            };
            showTotals.schedule(1000L);
        } else {
            _forward.setEnabled(true);
            _closeBtn.setEnabled(true);
        }
    }
    /**
     * Shows the detailed stats view.
     */
    protected void showStats (int round)
    {
        if (_bobj.state != BangObject.GAME_OVER || _bobj.roundId == 1) {
            round = _bobj.roundId;
        }
        BContainer statcont = _statmap.get(round);
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
        statcont.setPreferredSize(TABLE_DIMENSION);
        setContents(statcont);
        _statmap.put(round, statcont);

        GroupLayout vlay = GroupLayout.makeVert(GroupLayout.TOP);
        vlay.setGap(2);
        BContainer avcont = new BContainer(vlay);
        statcont.add(avcont, GroupLayout.FIXED);

        // add a round header if necessary
        String roundheader = null;
        if (_bobj.roundId != 1 || _bobj.state != BangObject.GAME_OVER) {
            if (round == 0) {
                roundheader = _msgs.get("m.overall");
            } else if (_bobj.roundId > 1) {
                roundheader = _msgs.xlate(MessageBundle.tcompose(
                                "m.stats_round_header", round));
            }
        }
        if (roundheader != null) {
            avcont.add(new BLabel(roundheader, "endgame_title") {
                protected Dimension computePreferredSize (
                        int hhint, int vhint) {
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

        // Get the statSet, or generate a cummulative statSet for an 
        // overall display
        StatSet[] statSet;
        if (round == _bobj.roundId) {
            statSet = _bobj.stats;
        } else if (round == 0) {
            statSet = new StatSet[_bobj.stats.length];
            for (int ii = 1; ii <= _bobj.roundId; ii++) {
                StatSet[] tmpset = (ii == _bobj.roundId) ?
                    _bobj.stats : _ctrl.getStatSetArray(ii);
                for (int jj = 0; jj < statSet.length; jj++) {
                    if (statSet[jj] == null) {
                        statSet[jj] = new StatSet();
                    }
                    for (Stat.Type type : BASE_STAT_TYPES) {
                        statSet[jj].incrementStat(
                                type, getIntStat(jj, tmpset, type));
                    }
                }
            }
        } else {
            statSet = _ctrl.getStatSetArray(round);
        }

        // which stats are we displaying
        ArrayList<Stat.Type> statTypes = new ArrayList<Stat.Type>();
        for (Stat.Type type : BASE_STAT_TYPES) {
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
        Dimension headerdim = new Dimension(width, HEADER_HEIGHT);
        HashMap<Stat.Type, Integer> map = new HashMap<Stat.Type, Integer>();

        // Add the headers
        for (Iterator<Stat.Type> iter = statTypes.iterator(); 
                iter.hasNext(); ) {
            Stat.Type type = iter.next();
            BLabel header = new BLabel(_msgs.get("m.header_" + type.name()),
                    "endgame_smallheader") {
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
            for (Iterator<Stat.Type> iter = statTypes.iterator();
                    iter.hasNext(); ) {
                Stat.Type type = iter.next();
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
        AvatarView aview = new AvatarView(_ctx, 8, false, true);
        aview.setAvatar(_bobj.avatars[idx]);
        aview.setText(_bobj.players[idx].toString());
        aview.setStyleClass("endgame_player" + idx);
        Dimension d = aview.getPreferredSize(-1, -1);
        d.height = Math.max(GRID_HEIGHT, d.height);
        aview.setPreferredSize(d);
        return aview;
    }

    /** Reference to our various game objects. */
    protected BasicContext _ctx;
    protected BangContext _bctx;
    protected BangController _ctrl;
    protected BangObject _bobj;
    protected MessageBundle _msgs;

    /** Content layouts that can be toggled through. */
    protected BButton _back, _forward, _closeBtn;
    protected BContainer _objcont, _ptscont, _currcont;
    protected HashIntMap<BContainer> _statmap = new HashIntMap<BContainer>();
    protected BLabel _header;

    /** Used for displaying labels after a delay. */
    protected BLabel[][] _labels;

    /** Information on the game scenario. */
    protected Stat.Type _statType;
    protected BIcon _objectiveIcon;
    protected String _objectiveTitle;
    protected String _objectivePoints;
    protected int _ppo;

    /** Counter for animation steps. */
    protected int _showing;

    /** Which page is currently displayed.*/
    protected int _page = 0;

    protected static final long ANIM_DELAY = 300L;
    protected static final long OBJECTIVE_DISPLAY = 2000L;
    protected static final int MAX_ICONS = 6;

    protected static final Dimension TABLE_DIMENSION = new Dimension(630, 300);
    protected static final Dimension CONTENT_DIMENSION =
        new Dimension(630, 500);
    protected static final int HEADER_HEIGHT = 40;
    protected static final int GRID_HEIGHT = 100;
    protected static final int NUM_VIEWABLE_COLS = 6;


    protected static final Stat.Type[] BASE_STAT_TYPES = {
            Stat.Type.DAMAGE_DEALT, Stat.Type.UNITS_KILLED,
            Stat.Type.BONUSES_COLLECTED, Stat.Type.CARDS_PLAYED,
            Stat.Type.DISTANCE_MOVED, Stat.Type.SHOTS_FIRED,
            Stat.Type.UNITS_LOST, Stat.Type.CATTLE_RUSTLED,
            Stat.Type.NUGGETS_CLAIMED
    };
}
