//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;

import static com.threerings.bang.Log.log;

/**
 * Displays the hideout interface, where players can form or manage gangs,
 * see what other gang members are up to, view rankings within the gang,
 * etc.
 */
public class HideoutView extends ShopView
{
    public HideoutView (BangContext ctx)
    {
        super(ctx, HideoutCodes.HIDEOUT_MSGS);

        // add our various interface components
        add(new BLabel(_msgs.get("m.intro_tip"), "shop_status"),
            new Rectangle(232, 656, 570, 35));

        add(new WalletLabel(_ctx, true), new Rectangle(25, 40, 150, 40));
        add(createHelpButton(), new Point(780, 25));
        add(new TownButton(ctx), new Point(870, 25));
        add(_status = new StatusLabel(ctx), new Rectangle(250, 10, 520, 50));
        _status.setStyleClass("shop_status");

        // start with a random shop tip
        _status.setStatus(getShopTip(), false);
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        _hideoutobj = (HideoutObject)plobj;
        
        // subscribe to the gang object and update the ui
        updateGangObject();
        
        // listen for changes in gang membership
        _ctx.getUserObject().addListener(_userlist);
    }
    
    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
        // stop listening to the user
        _ctx.getUserObject().removeListener(_userlist);
        
        // unsubscribe from the gang object
        unsubscribeFromGang();
    }
    
    @Override // documentation inherited
    protected Point getShopkeepNameLocation ()
    {
        return new Point(23, 548);
    }
    
    /**
     * Updates the UI when first entering or when the player joins or leaves a gang.
     */
    protected void updateGangObject ()
    {
        // if the user is not in a gang, make sure that we are not subscribed
        PlayerObject player = _ctx.getUserObject();
        if (player.gangOid <= 0) {
            unsubscribeFromGang();
            return;
        }
        
        // subscribe to the gang object
        (_gangsub = new SafeSubscriber<GangObject>(
            player.gangOid, new Subscriber<GangObject>() {
            public void objectAvailable (GangObject gangobj) {
                _gangobj = gangobj;
                _ctx.getChatDirector().addAuxiliarySource(_gangobj, ChatCodes.PLACE_CHAT_TYPE);        
            }
            public void requestFailed (int oid, ObjectAccessException cause) {
                log.warning("Failed to subscribe to gang object [oid=" + oid +
                    ", cause=" + cause + "].");
                _status.setStatus(_msgs.get("m.internal_error"), true);
            }
        })).subscribe(_ctx.getDObjectManager());
    }
    
    /**
     * Unsubscribes from the gang object and stops listening.
     */
    protected void unsubscribeFromGang ()
    {
        if (_gangsub != null) {
            _gangsub.unsubscribe(_ctx.getDObjectManager());
            _gangsub = null;
        }
        if (_gangobj != null) {
            _ctx.getChatDirector().removeAuxiliarySource(_gangobj);
            _gangobj = null;
        }
    }
    
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;
    
    protected StatusLabel _status;
    
    protected SafeSubscriber<GangObject> _gangsub;
    
    /** Listens to the user object for changes in gang membership. */
    protected AttributeChangeListener _userlist = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(PlayerObject.GANG_OID)) {
                updateGangObject();
            }
        }
    };
}
