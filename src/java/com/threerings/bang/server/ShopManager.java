//
// $Id$

package com.threerings.bang.server;

import java.lang.reflect.Field;
import java.util.logging.Level;

import com.threerings.util.MessageBundle;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;

import static com.threerings.bang.Log.log;

/**
 * Provides common services for all shops.
 */
public abstract class ShopManager extends PlaceManager
{
    @Override // documentation inherited
    public String ratifyBodyEntry (BodyObject body)
    {
        return checkShopEnabled((PlayerObject)body) ? null :
            MessageBundle.qualify(BangCodes.BANG_MSGS,
                                  MessageBundle.compose("e.shop_disabled", "m." + getIdent()));
    }

    @Override // documentation inherited
    protected long idleUnloadPeriod ()
    {
        // we don't want to unload
        return 0L;
    }

    /**
     * Checks whether the specified shop is enabled, throws an invocation exception if it is not.
     * This should be called at the start of all of a shop's invocation services. Casts and returns
     * the supplied caller to a {@link PlayerObject} for convenience.
     */
    protected PlayerObject requireShopEnabled (ClientObject caller)
        throws InvocationException
    {
        String errmsg = ratifyBodyEntry((PlayerObject)caller);
        if (errmsg != null) {
            throw new InvocationException(errmsg);
        }
        return (PlayerObject)caller;
    }

    /**
     * Returns true if this shop is enabled, false if not.
     */
    protected boolean checkShopEnabled (PlayerObject user)
    {
        try {
            Field field = RuntimeConfig.server.getClass().getField(getIdent() + "Enabled");
            return (Boolean)field.get(RuntimeConfig.server);

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to check shop enabled status " +
                    "[ident=" + getIdent() + "].", e);
            return false;
        }
    }

    /**
     * Returns the string identifier for this shop type: eg. bank, barber, etc.
     */
    protected abstract String getIdent ();
}
