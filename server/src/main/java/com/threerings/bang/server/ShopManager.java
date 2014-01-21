//
// $Id$

package com.threerings.bang.server;

import java.lang.reflect.Field;

import com.google.inject.Inject;

import com.threerings.util.MessageBundle;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.admin.server.BangAdminManager;
import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;

import static com.threerings.bang.Log.log;

/**
 * Provides common services for all shops.
 */
public abstract class ShopManager extends PlaceManager
{
    @Override // from PlaceManager
    public String ratifyBodyEntry (BodyObject body)
    {
        PlayerObject user = (PlayerObject)body;
        if (!checkShopEnabled(user)) {
            return disabledMessage();
        }

        String msg = null;
        if (!allowAnonymous() && user.tokens.isAnonymous()) {
            msg = MessageBundle.compose(BangCodes.E_SIGN_UP, getIdent());
        } else if (requireHandle() && !user.hasCharacter()) {
            msg = BangCodes.E_CREATE_HANDLE;
        } else if (!allowUnder13() && !user.tokens.isOver13()) {
            msg = BangCodes.E_UNDER_13;
        }
        return msg;
    }

    @Override // from PlaceManager
    public boolean isValidSpeaker (DObject speakObj, ClientObject speaker, byte mode)
    {
        if (hasChat()) {
            return super.isValidSpeaker(speakObj, speaker, mode);
        }
        return false;
    }

    @Override // from PlaceManager
    protected long idleUnloadPeriod ()
    {
        // we don't want to unload
        return 0L;
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();
        _adminmgr.statobj.updatePlaceInfo(getIdent(), 0);
    }

    @Override // from PlaceManager
    protected void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);
        _adminmgr.statobj.updatePlaceInfo(getIdent(), _plobj.occupants.size());
    }

    @Override // from PlaceManager
    protected void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);
        _adminmgr.statobj.updatePlaceInfo(getIdent(), _plobj.occupants.size());
    }

    /**
     * The error string for a disabled shop.
     */
    protected String disabledMessage ()
    {
        return MessageBundle.qualify(BangCodes.BANG_MSGS,
                          MessageBundle.compose("e.shop_disabled", "m." + getIdent()));
    }

    /**
     * Checks whether the specified shop is enabled, throws an invocation exception if it is not.
     * This should be called at the start of all of a shop's invocation services. Casts and returns
     * the supplied caller to a {@link PlayerObject} for convenience.
     */
    protected PlayerObject requireShopEnabled (ClientObject caller)
        throws InvocationException
    {
        if (!checkShopEnabled((PlayerObject)caller)) {
            throw new InvocationException(disabledMessage());
        }
        return (PlayerObject)caller;
    }

    /**
     * Returns true if this shop is enabled, false if not.
     */
    protected boolean checkShopEnabled (PlayerObject user)
    {
        // admins are always allowed in
        if (user.tokens.isAdmin()) {
            return true;
        }

        try {
            Field field = RuntimeConfig.server.getClass().getField(getIdent() + "Enabled");
            return (Boolean)field.get(RuntimeConfig.server);

        } catch (Exception e) {
            log.warning("Failed to check shop enabled status", "ident", getIdent(), e);
            return false;
        }
    }

    /**
     * Returns true if the shop requires the user have a handle.
     */
    protected boolean requireHandle ()
    {
        return false;
    }

    /**
     * Returns true if the shop allows anonymous access.
     */
    protected boolean allowAnonymous ()
    {
        return true;
    }

    /**
     * Returns true if the shop allows underaged access.
     */
    protected boolean allowUnder13 ()
    {
        return true;
    }

    /**
     * Returns true if the shop has a public chat channel.
     */
    protected boolean hasChat ()
    {
        return false;
    }

    /**
     * Returns the string identifier for this shop type: eg. bank, barber, etc.
     */
    protected abstract String getIdent ();

    @Inject protected BangAdminManager _adminmgr;
}
