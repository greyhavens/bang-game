//
// $Id$

package com.threerings.bang.ranch.client;

import java.util.EnumSet;

import com.jme.renderer.ColorRGBA;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.border.CompoundBorder;
import com.jmex.bui.border.EmptyBorder;
import com.jmex.bui.border.LineBorder;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

/**
 * Displays the contents of the sign hanging down in the Ranch view. This sign
 * at times displays just text and at other times displays the details of a
 * particular unit.
 */
public class SignView extends BContainer
    implements IconPalette.Inspector
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
        BContainer details = new BContainer(
            GroupLayout.makeVert(
                GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.STRETCH));
        _unit.setText("");
        details.add(_name = new BLabel(""));
        _name.setLookAndFeel(BangUI.dtitleLNF);
        details.add(_descrip = new BLabel(""));
        details.add(_move = new BLabel(""));
        details.add(_fire = new BLabel(""));
        _inspector.add(details, BorderLayout.CENTER);

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
        if (_inspector.getParent() != null) {
            remove(_inspector);
        }
        _marquee.setText(text);
        if (_marquee.getParent() == null) {
            add(_marquee);
        }
    }

    /**
     * Returns the item id of the unit being inspected (or -1 if the
     * inspected unit is not an actual unit).
     */
    public int getItemId ()
    {
        return _itemId;
    }

    /**
     * Returns the configuration of the unit being inspected or null if no
     * unit is being inspected.
     */
    public UnitConfig getConfig ()
    {
        return _config;
    }

    /**
     * Configures the unit we're inspecting.
     */
    public void setUnit (int itemId, UnitConfig config)
    {
        if (_marquee.getParent() != null) {
            remove(_marquee);
        }

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

        if (_inspector.getParent() == null) {
            add(_inspector);
        }
    }

    // documentation inherited from interface IconPalette.Inspector
    public void iconSelected (SelectableIcon icon)
    {
        UnitIcon uicon = (UnitIcon)icon;
        setUnit(uicon.getItemId(), uicon.getUnit());
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs, _umsgs;

    protected int _itemId = -1;
    protected UnitConfig _config;

    protected BTextArea _marquee;
    protected BContainer _inspector;
    protected BLabel _unit, _name;
    protected BLabel _descrip, _move, _fire;
}
