//
// $Id$

package com.threerings.bang.avatar.client;

import com.jmex.bui.BContainer;

import com.threerings.bang.util.BangContext;

/**
 * Allows the customization of looks with clothing and accessories.
 */
public class WearClothingView extends BContainer
{
    public WearClothingView (BangContext ctx)
    {
        _ctx = ctx;
    }

    protected BangContext _ctx;
}
