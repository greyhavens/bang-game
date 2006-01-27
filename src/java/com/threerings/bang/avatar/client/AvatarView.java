//
// $Id$

package com.threerings.bang.avatar.client;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.logging.Level;

import com.jme.image.Image;
import com.jme.renderer.Renderer;
import com.jmex.bui.BLabel;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.RenderUtil;

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

    public AvatarView (BangContext ctx)
    {
        this(ctx, null);
    }

    public AvatarView (BangContext ctx, int[] avatar)
    {
        super("", "avatar_view");
        _ctx = ctx;
        _frame = ctx.loadImage("ui/barber/avatar_frame.png");
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
        java.awt.Image scaled = image.getScaledInstance(
            WIDTH/2, HEIGHT/2, BufferedImage.SCALE_SMOOTH);

        // TODO: fade between the two images
        setIcon(new ImageIcon(scaled));
    }

    @Override // documentation inherited
    public Dimension getPreferredSize (int whint, int hhint)
    {
        return new Dimension(_frame.getWidth(), _frame.getHeight());
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);
        RenderUtil.renderImage(_frame, 0, 0);
    }

    protected BangContext _ctx;
    protected Image _frame;
}
