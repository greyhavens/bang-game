//
// $Id$

package com.threerings.bang.bounty.client;

import java.util.ArrayList;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.util.StateSaver;
import com.threerings.bang.data.IntStat;
import com.threerings.bang.data.Stat;
import com.threerings.bang.game.data.Criterion;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bounty.data.IntStatCriterion;
import com.threerings.bang.bounty.data.OfficeCodes;

/**
 * Allows the editing of a single bounty game criterion.
 */
public abstract class CriterionEditor extends BContainer
{
    /** Enumerates the supported types of criterion. */
    public enum Type { INTSTAT };

    public static CriterionEditor createEditor (BangContext ctx, Type type)
    {
        CriterionEditor editor;
        switch (type) {
        case INTSTAT: editor = new IntStatEditor(); break;
        default: throw new IllegalArgumentException("Unknown criterion " + type + ".");
        }
        editor.init(ctx);
        return editor;
    }

    /**
     * Returns the criterion currently configured in this editor.
     */
    public abstract Criterion getCriterion ();

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
            crit.stat = (Stat.Type)_stat.getSelectedValue();
            crit.condition = (IntStatCriterion.Condition)_condition.getSelectedItem();
            crit.value = Integer.parseInt(_value.getText());
            return crit;
        }

        protected void createInterface () {
            if (_intstats.size() == 0) {
                for (Stat.Type stat : Stat.Type.values()) {
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

    protected BangContext _ctx;
    protected MessageBundle _msgs;
}
