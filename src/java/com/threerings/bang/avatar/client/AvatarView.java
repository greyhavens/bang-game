//
// $Id$

package com.threerings.bang.avatar.client;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.logging.Level;

import com.jmex.bui.BLabel;
import com.jmex.bui.ImageIcon;
import com.jmex.bui.util.Dimension;

import com.threerings.media.util.MultiFrameImage;

import com.threerings.cast.ActionFrames;
import com.threerings.cast.CharacterDescriptor;

import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Displays an avatar.
 */
public class AvatarView extends BLabel
{
    public AvatarView (BangContext ctx)
    {
        this(ctx, null);
    }

    public AvatarView (BangContext ctx, int[] avatar)
    {
        super("");
        _ctx = ctx;
        if (avatar != null) {
            setAvatar(avatar);
        }
    }

    /**
     * Decodes and displays the specified avatar fingerprint.
     */
    public void setAvatar (int[] avatar)
    {
        CharacterDescriptor cdesc = _ctx.getAvatarMetrics().decodeAvatar(avatar);
        ActionFrames af;
        try {
            af = _ctx.getCharacterManager().getActionFrames(cdesc, "default");
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to load action frames " +
                    "[cdesc=" + cdesc + "].", e);
            return;
        }

        MultiFrameImage mfi = af.getFrames(0);
        BufferedImage image = _ctx.getImageManager().createImage(
            mfi.getWidth(0), mfi.getHeight(0), Transparency.BITMASK);
        Graphics2D gfx = (Graphics2D)image.createGraphics();
        try {
            mfi.paintFrame(gfx, 0, 0, 0);
        } finally {
            gfx.dispose();
        }
        setIcon(new ImageIcon(image));
    }

    @Override // documentation inherited
    public Dimension getPreferredSize ()
    {
        return new Dimension(312, 400);
    }

    protected BangContext _ctx;
}
