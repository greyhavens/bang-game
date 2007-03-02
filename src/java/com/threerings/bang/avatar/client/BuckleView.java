//
// $Id$

package com.threerings.bang.avatar.client;

import com.jmex.bui.util.Dimension;

import com.threerings.bang.data.BuckleInfo;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.avatar.util.AvatarLogic;

/**
 * Displays a gang buckle.
 */
public class BuckleView extends BaseAvatarView
{
    /**
     * Creates a buckle view.
     *
     * @param scale the image will be one over this value times the "natural" size of the buckle
     * imagery. This should be at least 2.
     */
    public BuckleView (BasicContext ctx, int scale)
    {
        super(ctx, scale);
        setPreferredSize(new Dimension(
            AvatarLogic.BUCKLE_WIDTH / scale, AvatarLogic.BUCKLE_HEIGHT / scale));
    }

    /**
     * Sets the buckle to display.
     */
    public void setBuckle (BuckleInfo buckle)
    {
        setAvatar(buckle);
    }
}
