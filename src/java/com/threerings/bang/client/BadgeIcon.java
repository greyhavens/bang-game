//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.icon.ImageIcon;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

/**
 * Displays a badge inventory item.
 */
public class BadgeIcon extends ItemIcon
{
    public BadgeIcon ()
    {
        setStyleClass("badge_icon");
    }

    protected void configureLabel (BangContext ctx)
    {
        Badge badge = (Badge)_item;
        String id = Integer.toHexString(badge.getType().code());
        String path = "badges/" + id.substring(0,1) + "/" + id + ".png";
        setIcon(new ImageIcon(ctx.loadImage(path)));
        setText(ctx.xlate(BangCodes.BADGE_MSGS, badge.getType().key()));
    }
}
