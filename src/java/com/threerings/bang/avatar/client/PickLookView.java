//
// $Id$

package com.threerings.bang.avatar.client;

import java.util.Iterator;

import com.jme.image.Image;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.StringUtil;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.BarberObject;
import com.threerings.bang.avatar.data.Look;

/**
 * Allows a player to select one of their active looks for use.
 */
public class PickLookView extends BContainer
    implements ActionListener
{
    public PickLookView (BangContext ctx)
    {
        super(new AbsoluteLayout());

        _ctx = ctx;
        add(_avatar = new AvatarView(ctx), new Point(0, 40));
        _looks = new BComboBox();
        _looks.addListener(this);
    }

    /**
     * Returns the currently selected look.
     */
    public Look getSelection ()
    {
        return _selection;
    }

    /**
     * If the pick look view is configured with a barber object, it will issue
     * a request to {@link BarberService#configureLook} whenever its currently
     * displayed look is about to be hidden but has been modified.
     */
    public void setBarberObject (BarberObject barbobj)
    {
        _barbobj = barbobj;
    }

    /**
     * Refreshes the currently displayed look.
     */
    public void refreshDisplay ()
    {
        PlayerObject user = _ctx.getUserObject();
        if (_selection != null) {
            _avatar.setAvatar(_selection.getAvatar(user));
        }
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        // remove either the looks combo or the blurb
        if (getComponentCount() > 1) {
            remove(getComponent(1));
        }

        // we'll need this later
        _deflook = _ctx.xlate(AvatarCodes.AVATAR_MSGS, "m.default_look");

        // rebuild our available looks
        PlayerObject user = _ctx.getUserObject();
        String[] looks = new String[user.looks.size()];
        int idx = 0;
        for (Iterator iter = user.looks.iterator(); iter.hasNext(); ) {
            Look look = (Look)iter.next();
            looks[idx++] = getName(look);
        }
        _looks.setItems(looks);

        // select their current look (which will update the display)
        Look current = user.getLook();
        if (current != null) {
            _looks.selectItem(getName(current));
        }

        // if we have more than one look, add the looks combo, otherwise add a
        // blurb for the barber
        if (looks.length > 1 || _barbobj != null) {
            Image icon = _ctx.loadImage("ui/barber/caption_look.png");
            add(new BLabel(new ImageIcon(icon)), new Point(13, 0));
            add(_looks, new Rectangle(73, 0, 164, 29));
        } else {
            add(new BLabel(_ctx.xlate(AvatarCodes.AVATAR_MSGS,
                                      "m.get_looks_at_barber")),
                new Rectangle(0, 0, 234, 30));
        }
    }

    @Override // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();
        flushModifiedLook();
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        // potentially flush any changes to our old look
        flushModifiedLook();

        String name = (String)_looks.getSelectedItem();
        if (name.equals(_deflook)) {
            name = "";
        }
        PlayerObject user = _ctx.getUserObject();
        _selection = (Look)user.looks.get(name);
        // we clone our selection so that it can be modified when we're used in
        // the wear clothes view without messing up the original
        if (_selection != null) {
            _selection = (Look)_selection.clone();
        }
        refreshDisplay();

        // if we don't have a barber object, we need to tell the server that we
        // updated our preferred look
        if (_barbobj == null) {
            AvatarService asvc = (AvatarService)
                _ctx.getClient().requireService(AvatarService.class);
            asvc.selectLook(_ctx.getClient(), name);
        }
    }

    protected String getName (Look look)
    {
        return !StringUtil.isBlank(look.name) ? look.name : _deflook;
    }

    protected void flushModifiedLook ()
    {
        if (_barbobj == null || _selection == null) {
            return; // nothing doing
        }

        // compare our current look with the one in the player object; if they
        // differ, send a request to the server to update the look
        Look remote = (Look)_ctx.getUserObject().looks.get(_selection.name);
        if (!_selection.equals(remote)) {
            _barbobj.service.configureLook(
                _ctx.getClient(), _selection.name, _selection.articles);
        }
    }

    protected BangContext _ctx;
    protected BarberObject _barbobj;

    protected AvatarView _avatar;
    protected BComboBox _looks;
    protected String _deflook;
    protected Look _selection;
}
