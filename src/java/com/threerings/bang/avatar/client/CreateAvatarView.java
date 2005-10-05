//
// $Id$

package com.threerings.bang.avatar.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.AvatarCodes;

/**
 * A view for creating a new avatar.
 */
public class CreateAvatarView extends BDecoratedWindow
{
    public CreateAvatarView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(),
              ctx.xlate(AvatarCodes.AVATAR_MSGS, "m.create_title"));

        BContainer editor = new BContainer(new BorderLayout(5, 5));
    }
}
