//
// $Id$

package com.threerings.bang.avatar.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.BarberObject;
import com.threerings.bang.avatar.data.Look;

import static com.threerings.bang.Log.log;

/**
 * Allows a player to select one of their active looks for use.
 */
public class PickLookView extends BContainer
    implements ActionListener
{
    public PickLookView (BangContext ctx, boolean barberMode)
    {
        super(new AbsoluteLayout());

        _ctx = ctx;
        add(_avatar = new AvatarView(ctx, 2, true, false), new Point(0, 36));
        _looks = new LookComboBox(ctx);
        _looks.addListener(this);

        // if we have more than one look or are being used in the barber, add
        // the looks combo, otherwise add a blurb for the barber
        if (barberMode) {
            BImage icon = _ctx.loadImage("ui/barber/caption_look.png");
            add(new BLabel(new ImageIcon(icon)), new Point(20, 0));
            add(_looks, new Rectangle(79, 0, 164, 29));
        } else {
            String msg = _ctx.xlate(AvatarCodes.AVATAR_MSGS, "m.get_looks_at_barber");
            add(new BLabel(msg, "look_upsell"), new Rectangle(0, 0, 258, 29));
        }
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
        if (_selection != null) {
            _avatar.setAvatar(_selection.getAvatar(_ctx.getUserObject()));
        } else {
            _avatar.setAvatar(new AvatarInfo());
        }
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        // select their current look (which will update the display)
        PlayerObject user = _ctx.getUserObject();
        Look current = user.getLook(Look.Pose.DEFAULT);
        if (current != null) {
            if (_looks.isAdded()) {
                _looks.selectLook(current);
            } else {
                selectLook(current);
            }
        } else {
            if (user.hasCharacter()) {
                log.warning("Missing default look?", "user", user);
            }
            refreshDisplay();
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
        selectLook(_looks.getSelectedLook());
    }

    protected void selectLook (Look look)
    {
        // we don't clone our selection because whatever modifications we make
        // we'll eventually flush to the server, and when we switch back from
        // configuring our active look, we want the NewLookView to be able to
        // immediately grab the new articles which would not otherwise be
        // possible since we'd have just sent off a request to the server to
        // update them
        _selection = look;

        // but we keep track of what the selection looked like before we
        // started messing with it so that we can tell if we changed it
        _orig = (Look)_selection.clone();

        // update the interface
        refreshDisplay();
    }

    protected void flushModifiedLook ()
    {
        if (_barbobj == null || _selection == null) {
            return; // nothing doing
        }

        // compare our current look with the unmodified copy; if they differ,
        // send a request to the server to update the look
        if (!_selection.equals(_orig)) {
            _barbobj.service.configureLook(_selection.name, _selection.articles);
        }
    }

    protected BangContext _ctx;
    protected BarberObject _barbobj;

    protected AvatarView _avatar;
    protected LookComboBox _looks;
    protected Look _selection, _orig;
}
