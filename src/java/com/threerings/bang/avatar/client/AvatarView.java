//
// $Id$

package com.threerings.bang.avatar.client;

import java.awt.Graphics2D;
import java.awt.Image;
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
import static com.threerings.bang.avatar.util.AvatarLogic.*;

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
        BufferedImage image = createImage(_ctx, avatar);
        if (image == null) {
            return;
        }

        // scale that image appropriately
        Image scaled = image.getScaledInstance(
            WIDTH/2, HEIGHT/2, BufferedImage.SCALE_SMOOTH);

        // TODO: fade between the two images
        setIcon(new ImageIcon(scaled));
    }

    @Override // documentation inherited
    public Dimension getPreferredSize ()
    {
        return new Dimension(WIDTH/2, HEIGHT/2);
    }

    /**
     * Creates an unscaled image for the specified avatar.
     */
    public static BufferedImage createImage (BangContext ctx, int[] avatar)
    {
        CharacterDescriptor cdesc = ctx.getAvatarLogic().decodeAvatar(avatar);
        ActionFrames af;
        try {
            af = ctx.getCharacterManager().getActionFrames(cdesc, "default");
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to load action frames " +
                    "[cdesc=" + cdesc + "].", e);
            return null;
        }

        // composite the myriad components and render them into an image
        MultiFrameImage mfi = af.getFrames(0);
        int ox = af.getXOrigin(0, 0), oy = af.getYOrigin(0, 0);
        BufferedImage image = ctx.getImageManager().createImage(
            WIDTH, HEIGHT, Transparency.BITMASK);
        Graphics2D gfx = (Graphics2D)image.createGraphics();
        try {
//             gfx.setColor(java.awt.Color.black);
//             gfx.drawRect(0, 0, WIDTH-1, HEIGHT-1);
            mfi.paintFrame(gfx, 0, WIDTH/2-ox, HEIGHT-oy);
        } finally {
            gfx.dispose();
        }
        return image;
    }

    protected BangContext _ctx;
}
