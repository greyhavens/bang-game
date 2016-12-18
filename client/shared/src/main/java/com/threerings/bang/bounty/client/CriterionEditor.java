//
// $Id$

package com.threerings.bang.bounty.client;

import java.util.ArrayList;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.stats.data.IntStat;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.util.StateSaver;
import com.threerings.bang.data.StatType;
import com.threerings.bang.game.data.Criterion;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bounty.data.IntStatCriterion;
import com.threerings.bang.bounty.data.OfficeCodes;
import com.threerings.bang.bounty.data.RankCriterion;

/**
 * Allows the editing of a single bounty game criterion.
 */
public abstract class CriterionEditor extends BContainer
{
    /** Enumerates the supported types of criterion. */
    public enum Type { INTSTAT, RANK };

    public static CriterionEditor createEditor (BangContext ctx, Type type)
    {
        CriterionEditor editor;
        switch (type) {
        case INTSTAT: editor = new IntStatEditor(); break;
        case RANK: editor = new RankEditor(); break;
        default: throw new IllegalArgumentException("Unknown criterion " + type + ".");
        }
        editor.init(ctx);
        return editor;
    }

    public static CriterionEditor createEditor (BangContext ctx, Criterion criterion)
    {
        CriterionEditor editor;
        if (criterion instanceof IntStatCriterion) {
            editor = new IntStatEditor();
        } else if (criterion instanceof RankCriterion) {
            editor = new RankEditor();
        } else {
            throw new IllegalArgumentException("Unknown criterion " + criterion + ".");
        }
        editor.init(ctx);
        editor.setCriterion(criterion);
        return editor;
    }

    /**
     * Returns the criterion currently configured in this editor.
     */
    public abstract Criterion getCriterion ();

    /**
     * Configures this editor based on the contents of the supplied criterion.
     */
    public abstract void setCriterion (Criterion criterion);

    protected CriterionEditor ()
    {
        super(GroupLayout.makeHoriz(GroupLayout.LEFT));
    }

    protected void init (BangContext ctx)
    {
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(OfficeCodes.OFFICE_MSGS);
        BButton remove = new BButton(_msgs.get("m.remove"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                getParent().remove(CriterionEditor.this);
            }
        }, "remove");
        remove.setStyleClass("alt_button");
        add(remove);
        createInterface();
    }

    protected abstract void createInterface ();

    protected static class IntStatEditor extends CriterionEditor
    {
        public Criterion getCriterion () {
            IntStatCriterion crit = new IntStatCriterion();
            crit.stat = (StatType)_stat.getSelectedValue();
            crit.condition = (IntStatCriterion.Condition)_condition.getSelectedItem();
            crit.value = Integer.parseInt(_value.getText());
            return crit;
        }

        public void setCriterion (Criterion criterion) {
            IntStatCriterion crit = (IntStatCriterion)criterion;
            _stat.selectValue(crit.stat);
            _condition.selectItem(crit.condition);
            _value.setText(String.valueOf(crit.value));
        }

        protected void createInterface () {
            if (_intstats.size() == 0) {
                for (StatType stat : StatType.values()) {
                    if (stat.isBounty() && stat.newStat() instanceof IntStat) {
                        _intstats.add(new BComboBox.Item(stat, _msgs.xlate(stat.key())));
                    }
                }
            }
            add(_stat = new BComboBox(_intstats));
            _stat.selectItem(0);
            new StateSaver("bounty.intstat_type", _stat);
            add(_condition = new BComboBox(IntStatCriterion.Condition.values()));
            _condition.selectItem(0);
            new StateSaver("bounty.intstat_cond", _condition);
            add(_value = new BTextField());
            _value.setPreferredWidth(50);
        }

        protected BComboBox _stat, _condition;
        protected BTextField _value;

        protected static ArrayList<BComboBox.Item> _intstats = new ArrayList<BComboBox.Item>();
    }

    protected static class RankEditor extends CriterionEditor
    {
        public Criterion getCriterion () {
            RankCriterion crit = new RankCriterion();
            crit.rank = (Integer)_rank.getSelectedValue();
            return crit;
        }

        public void setCriterion (Criterion criterion) {
            RankCriterion crit = (RankCriterion)criterion;
            _rank.selectValue(crit.rank);
        }

        protected void createInterface () {
            if (_ranks.size() == 0) {
                for (int rr = 0; rr < GameCodes.MAX_PLAYERS-1; rr++) {
                    String msg = _ctx.xlate(GameCodes.GAME_MSGS, "m.rank_at" + rr);
                    _ranks.add(new BComboBox.Item(rr, msg));
                }
            }
            add(_rank = new BComboBox(_ranks));
            _rank.selectItem(0);
            new StateSaver("bounty.rank_rank", _rank);
            add(new BLabel(_msgs.get("m.rank_orbetter")));
        }

        protected BComboBox _rank;

        protected static ArrayList<BComboBox.Item> _ranks = new ArrayList<BComboBox.Item>();
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
}
