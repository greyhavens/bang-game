//
// $Id$

package com.threerings.bang.web.logic;

import com.samskivert.velocity.Application;
import com.samskivert.velocity.InvocationContext;
import com.samskivert.velocity.Logic;

import com.threerings.user.OOOUser;

import com.threerings.bang.web.OfficeApp;

/**
 * A base class for logic classes that require admin access.
 */
public abstract class AdminLogic
    implements Logic
{
    /**
     * Admin servlets should override this method to perform their work. The
     * supplied user record will already have been authenticated as an admin.
     */
    public abstract void invoke (
        OfficeApp app, InvocationContext ctx, OOOUser user)
        throws Exception;

    // documentation inherited from interface Logic
    public void invoke (Application app, InvocationContext ctx)
        throws Exception
    {
        OfficeApp oapp = (OfficeApp)app;
        OOOUser user = (OOOUser)oapp.getUserManager().requireUser(
            ctx.getRequest(), ADMIN_TOKENS);
        ctx.put("username", user.username);
        invoke(oapp, ctx, user);
    }

    protected static final byte[] ADMIN_TOKENS = {
        OOOUser.ADMIN, OOOUser.MAINTAINER };
}
