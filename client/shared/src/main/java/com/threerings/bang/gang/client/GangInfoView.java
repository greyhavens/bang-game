//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.text.IntegerDocument;
import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.RequestDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.WeightClassUpgrade;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.avatar.client.BuckleView;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.util.GangUtil;

/**
 * Displays information about the gang for its members: notoriety, statement, contents of coffers,
 * etc.
 */
public class GangInfoView extends BContainer
    implements ActionListener
{
    public GangInfoView (
        BangContext ctx, HideoutObject hideoutobj, GangObject gangobj, StatusLabel status)
    {
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(HideoutCodes.HIDEOUT_MSGS);
        _hideoutobj = hideoutobj;
        _gangobj = gangobj;
        _status = status;

        GroupLayout glay = GroupLayout.makeVert(GroupLayout.TOP);
        glay.setOffAxisJustification(GroupLayout.RIGHT);
        setLayoutManager(glay);

        add(new BLabel(new ImageIcon(_ctx.loadImage("ui/hideout/design_top.png")),
            "gang_info_design"));

        glay = GroupLayout.makeHoriz(GroupLayout.CENTER);
        glay.setOffAxisJustification(GroupLayout.BOTTOM);
        BContainer mcont = new BContainer(glay);
        add(mcont);

        BContainer tcont = new BContainer(GroupLayout.makeVert(
            GroupLayout.STRETCH, GroupLayout.TOP, GroupLayout.NONE));
        ((GroupLayout)tcont.getLayoutManager()).setOffAxisJustification(GroupLayout.RIGHT);
        tcont.setStyleClass("gang_info_content");
        mcont.add(tcont);
        tcont.add(new BLabel(gangobj.name.toString().toUpperCase(), "gang_title"),
            GroupLayout.FIXED);

        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)bcont.getLayoutManager()).setGap(0);
        ((GroupLayout)bcont.getLayoutManager()).setOffAxisPolicy(GroupLayout.STRETCH);
        ((GroupLayout)bcont.getLayoutManager()).setOffAxisJustification(GroupLayout.BOTTOM);

        tcont.add(bcont);
        BContainer fcont = GroupLayout.makeVBox(GroupLayout.BOTTOM);
        ((GroupLayout)fcont.getLayoutManager()).setOffAxisJustification(GroupLayout.RIGHT);
        fcont.add(_buckle = new BuckleView(ctx, 2));
        _buckle.setBuckle(gangobj.getBuckleInfo());
        fcont.add(new Spacer(1, -15));
        bcont.add(fcont);

        BContainer rcont = new BContainer(GroupLayout.makeVert(
            GroupLayout.STRETCH, GroupLayout.TOP, GroupLayout.NONE));
        rcont.setPreferredSize(new Dimension(300, -1));
        bcont.add(rcont);
        bcont.add(new Spacer(40, 1));

        BContainer ncont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ncont.add(_weightClass = new BLabel(getWeightClassDesc(), "gang_notoriety"));
        ncont.add(new BLabel(new ImageIcon(_ctx.loadImage("ui/hideout/diamond.png"))));
        ncont.add(_notoriety = new BLabel(getNotorietyDesc(), "gang_notoriety"));
        rcont.add(ncont, GroupLayout.FIXED);

        BContainer scont = GroupLayout.makeVBox(GroupLayout.CENTER);
        scont.add(_statement = new BLabel(GangUtil.quoteStatement(_ctx, _gangobj.statement, true),
            "gang_statement"));
        _statement.setTooltipText(GangUtil.quoteStatement(_ctx, _gangobj.statement, false));
        BContainer pcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        pcont.add(_page = new BButton(_msgs.get("m.home_page"), this, "page"));
        _page.setTooltipText(gangobj.url);
        _page.setStyleClass("gang_page_button");
        _page.setVisible(gangobj.getURL() != null);
        if (_ctx.getUserObject().gangRank == GangCodes.LEADER_RANK) {
            BButton edit = new BButton(_msgs.get("m.edit"), this, "edit_statement");
            edit.setStyleClass("alt_button");
            pcont.add(edit);
        }
        scont.add(pcont);
        rcont.add(scont);

        BContainer ccont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ccont.add(new BLabel(_msgs.get("m.coffers"), "coffer_label"));
        ccont.add(_coffers = new CofferLabel(ctx, gangobj));
        BButton donate = new BButton(_msgs.get("m.donate"), this, "donate");
        donate.setStyleClass("alt_button");
        ccont.add(donate);
        rcont.add(ccont, GroupLayout.FIXED);

        add(new BLabel(new ImageIcon(_ctx.loadImage("ui/hideout/design_bottom.png")),
            "gang_info_design"));
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("page")) {
            _ctx.showURL(_gangobj.getURL());
        } else if (action.equals("edit_statement")) {
            _ctx.getBangClient().displayPopup(new StatementDialog(_ctx, _status), true, 400);
        } else if (action.equals("donate")) {
            _ctx.getBangClient().displayPopup(new DonateDialog(_ctx, _status), true, 400);
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _gangobj.addListener(_updater);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _gangobj.removeListener(_updater);
    }

    protected String getWeightClassDesc ()
    {
        String msg = "m.weight_class." + _gangobj.getWeightClass();
        return _ctx.xlate(GangCodes.GANG_MSGS, msg);
    }

    protected String getNotorietyDesc ()
    {
        String msg = "m.notoriety." + _gangobj.notoriety;
        return "\"" + _ctx.xlate(GangCodes.GANG_MSGS, msg) + "\"";
    }

    protected class Updater extends SetAdapter<DSet.Entry>
        implements AttributeChangeListener
    {
        // documentation inherited from interface AttributeChangeListener
        public void attributeChanged (AttributeChangedEvent event)
        {
            String name = event.getName();
            if (name.equals(GangObject.NOTORIETY)) {
                _notoriety.setText(getNotorietyDesc());
            } else if (name.equals(GangObject.STATEMENT)) {
                _statement.setText(GangUtil.quoteStatement(_ctx, _gangobj.statement, true));
                _statement.setTooltipText(GangUtil.quoteStatement(_ctx, _gangobj.statement, false));
            } else if (name.equals(GangObject.URL)) {
                _page.setVisible(_gangobj.getURL() != null);
                _page.setTooltipText(_gangobj.url);
            } else if (name.equals(GangObject.BUCKLE)) {
                _buckle.setBuckle(_gangobj.getBuckleInfo());
            }
        }

        @Override // from SetAdapter
        public void entryAdded (EntryAddedEvent<DSet.Entry> event)
        {
            if (event.getName().equals(GangObject.INVENTORY) &&
                event.getEntry() instanceof WeightClassUpgrade) {
                _weightClass.setText(getWeightClassDesc());
            }
        }
    }

    protected class StatementDialog extends RequestDialog
    {
        public StatementDialog (BangContext ctx, StatusLabel status)
        {
            super(ctx, HideoutCodes.HIDEOUT_MSGS, "m.statement_tip", "m.update", "m.cancel",
                "m.statement_updated", status);

            BContainer scont = GroupLayout.makeHBox(GroupLayout.CENTER);
            scont.add(new BLabel(_msgs.get("m.statement")), GroupLayout.FIXED);
            scont.add(_statement = new BTextField(
                _gangobj.statement, HideoutCodes.MAX_STATEMENT_LENGTH));
            _statement.setPreferredWidth(300);
            add(1, scont);

            BContainer ucont = GroupLayout.makeHBox(GroupLayout.CENTER);
            ucont.add(new BLabel(_msgs.get("m.url")), GroupLayout.FIXED);
            ucont.add(_url = new BTextField(_gangobj.url, HideoutCodes.MAX_URL_LENGTH));
            _url.setPreferredWidth(300);
            add(2, ucont);
        }

        // documentation inherited
        protected void fireRequest (Object result)
        {
            _hideoutobj.service.setStatement(_statement.getText(), _url.getText(), this);
        }

        protected BTextField _statement, _url;
    }

    protected class DonateDialog extends RequestDialog
        implements TextListener
    {
        public DonateDialog (BangContext ctx, StatusLabel status)
        {
            super(ctx, HideoutCodes.HIDEOUT_MSGS, "m.donate_tip", "m.donate", "m.cancel",
                "m.donated", status);

            // add the amount entry panel
            BContainer acont = GroupLayout.makeHBox(GroupLayout.CENTER);
            add(1, acont);

            acont.add(new BLabel(BangUI.scripIcon));
            acont.add(_scrip = new BTextField(4));
            _scrip.setPreferredWidth(50);
            _scrip.setDocument(new IntegerDocument(true));
            _scrip.addListener(this);

            // if we are on a coin deployment, allow coin donations as well
            _coins = new BTextField(4);
            _coins.setPreferredWidth(50);
            _coins.setDocument(new IntegerDocument(true));
            _coins.addListener(this);
            if (DeploymentConfig.usesCoins()) {
                acont.add(new BLabel(_msgs.get("m.and")));
                acont.add(new BLabel(BangUI.coinIcon));
                acont.add(_coins);
            }

            _buttons[0].setEnabled(false);
        }

        // documentation inherited from interface TextListener
        public void textChanged (TextEvent event)
        {
            try {
                _buttons[0].setEnabled(parseInt(_scrip.getText()) > 0 ||
                                       parseInt(_coins.getText()) > 0);
            } catch (NumberFormatException e) {
                _buttons[0].setEnabled(false);
            }
        }

        // documentation inherited
        protected void fireRequest (Object result)
        {
            _hideoutobj.service.addToCoffers(
                parseInt(_scrip.getText()), parseInt(_coins.getText()), this);
        }

        /**
         * Parses the specified string as an integer, allowing the empty
         * string to represent zero.
         */
        protected int parseInt (String text)
        {
            return (text.length() == 0) ? 0 : Integer.parseInt(text);
        }

        protected BTextField _scrip, _coins;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;
    protected Updater _updater = new Updater();

    protected BuckleView _buckle;
    protected BLabel _weightClass, _notoriety, _statement;
    protected CofferLabel _coffers;
    protected BButton _page;

    protected StatusLabel _status;
}
