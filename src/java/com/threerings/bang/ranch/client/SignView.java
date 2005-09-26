//
// $Id$

package com.threerings.bang.ranch.client;

import java.util.EnumSet;

import com.jme.renderer.ColorRGBA;
import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.border.CompoundBorder;
import com.jmex.bui.border.EmptyBorder;
import com.jmex.bui.border.LineBorder;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

/**
 * Displays the contents of the sign hanging down in the Ranch view. This sign
 * at times displays just text and at other times displays the details of a
 * particular unit.
 */
public class SignView extends BContainer
    implements IconPalette.Inspector, ActionListener
{
    public SignView (BangContext ctx)
    {
        super(GroupLayout.makeVStretch());
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("ranch");
        _umsgs = ctx.getMessageManager().getBundle(BangCodes.UNITS_MSGS);

        setBorder(new CompoundBorder(new LineBorder(ColorRGBA.black),
                                     new EmptyBorder(5, 5, 5, 5)));

        // this is used to "inspect" a particular unit
        _inspector = new BContainer(new BorderLayout(5, 5));
        _inspector.add(_unit = new BLabel(""), BorderLayout.WEST);
        _details = new BContainer(
            GroupLayout.makeVert(
                GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.STRETCH));
        _unit.setText("");
        _details.add(_name = new BLabel(""));
        _name.setLookAndFeel(BangUI.dtitleLNF);
        _details.add(_descrip = new BLabel(""));
        _details.add(_move = new BLabel(""));
        _details.add(_fire = new BLabel(""));
        _inspector.add(_details, BorderLayout.CENTER);

        // this is shown when we're displaying recruitable big shots
        _recruit = GroupLayout.makeButtonBox(GroupLayout.LEFT);
        _recruit.add(_cost = new MoneyLabel(ctx));
        _recruit.add(new BButton(_msgs.get("m.recruit"), this, "recruit"));

        // this is shown when we're displaying recruited big shots
        _customize = GroupLayout.makeButtonBox(GroupLayout.LEFT);
        _customize.add(new BButton(_msgs.get("m.customize", this, "customize")));

        // this is used when we're simply displaying text
        _marquee = new BTextArea();
        _marquee.setLookAndFeel(BangUI.dtitleLNF);

        // start in marquee mode
        add(_marquee);
    }

    /**
     * Displays the specified text in the sign.
     */
    public void setText (String text)
    {
        setVisible(this, _inspector, false);
        setVisible(_details, _recruit, false);
        setVisible(_details, _customize, false);
        _marquee.setText(text);
        setVisible(this, _marquee, true);
    }

    /**
     * Configures the unit we're inspecting.
     */
    public void setUnit (int itemId, UnitConfig config)
    {
        setVisible(this, _marquee, false);

        _itemId = itemId;
        _config = config;

        _unit.setIcon(
            _ctx.getModelCache().getModel("units", config.type).getIcon());
        _name.setText(_umsgs.xlate(config.getName()));
        _descrip.setText(_umsgs.xlate(config.getName() + "_descrip"));
        _move.setText(_umsgs.get("m.move_range", "" + config.moveDistance));

        String fire;
        if (config.minFireDistance == config.maxFireDistance) {
            fire = "" + config.minFireDistance;
        } else {
            fire = config.minFireDistance + " - " + config.maxFireDistance;
        }
        _fire.setText(_umsgs.get("m.fire_range", fire));

        setVisible(this, _inspector, true);

        // Big Shots have some additional user interface bits
        boolean showRecruit = false, showCustomize = false;
        if (config.rank == UnitConfig.Rank.BIGSHOT) {
            if (itemId == -1) {
                showRecruit = true;
                _cost.setMoney(config.scripCost, config.goldCost, false);
            } else {
                showCustomize = true;
            }
        }
        setVisible(_details, _recruit, showRecruit);
        setVisible(_details, _customize, showCustomize);
    }

    // documentation inherited from interface IconPalette.Inspector
    public void iconSelected (SelectableIcon icon)
    {
        UnitIcon uicon = (UnitIcon)icon;
        setUnit(uicon.getItemId(), uicon.getUnit());
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("recruit".equals(event.getAction())) {
            if (_config != null && _itemId == -1 &&
                _config.rank == UnitConfig.Rank.BIGSHOT) {
                recruit(_config);
            }

        } else if ("customize".equals(event.getAction())) {
            setText("Not yet implemented. Sorry.");
        }
    }

    @Override // documentation inherited
    public Dimension getPreferredSize ()
    {
        Dimension d = super.getPreferredSize();
        d.height = Math.max(d.height, 100);
        return d;
    }

    protected void recruit (UnitConfig config)
    {
        RanchService rsvc = (RanchService)
            _ctx.getClient().requireService(RanchService.class);
        RanchService.ResultListener rl = new RanchService.ResultListener() {
            public void requestProcessed (Object result) {
                BigShotItem unit = (BigShotItem)result;
                RanchView parent = (RanchView)getParent().getParent();
                parent.unitRecruited(unit.getItemId());
                setText(_msgs.get("m.recruited_bigshot"));
            }
            public void requestFailed (String cause) {
                setText(_msgs.xlate(cause));
            }
        };
        rsvc.recruitBigShot(_ctx.getClient(), config.type, rl);
    }

    protected void setVisible (
        BContainer parent, BComponent comp, boolean visible)
    {
        if (visible && comp.getParent() == null) {
            parent.add(comp);
        } else if (!visible && comp.getParent() != null) {
            parent.remove(comp);
        }
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs, _umsgs;

    protected int _itemId = -1;
    protected UnitConfig _config;

    protected BTextArea _marquee;
    protected BContainer _inspector, _details, _recruit, _customize;
    protected MoneyLabel _cost;
    protected BLabel _unit, _name;
    protected BLabel _descrip, _move, _fire;
}
