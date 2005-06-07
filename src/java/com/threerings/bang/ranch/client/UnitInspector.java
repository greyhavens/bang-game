//
// $Id$

package com.threerings.bang.ranch.client;

import com.jme.bui.BContainer;
import com.jme.bui.BIcon;
import com.jme.bui.BLabel;
import com.jme.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.BangContext;

/**
 * Displays a large (manipulable?) rendering of a unit model and a list of
 * the unit's statistics below.
 */
public class UnitInspector extends BContainer
{
    public UnitInspector (BangContext ctx)
    {
        super(GroupLayout.makeVStretch());
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("units");

        // TODO: create texture renderer
//         add(_model = new BIcon());
        add(_name = new BLabel(""));
        add(_descrip = new BLabel(""));
        add(_stats = new BLabel(""));
    }

    /**
     * Configures the unit we're inspecting.
     */
    public void setUnit (UnitConfig config)
    {
        _name.setText(config.type);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected BIcon _model;
    protected BLabel _name;
    protected BLabel _descrip;
    protected BLabel _stats;
}
