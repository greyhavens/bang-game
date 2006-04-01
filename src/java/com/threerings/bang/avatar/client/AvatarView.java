//
// $Id$

package com.threerings.bang.avatar.client;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;

import com.jme.renderer.Renderer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.IntListUtil;

import com.threerings.media.util.MultiFrameImage;

import com.threerings.cast.ActionFrames;
import com.threerings.cast.CharacterDescriptor;

import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.avatar.util.AvatarLogic.*;

/**
 * Displays an avatar.
 */
public class AvatarView extends BLabel
{
    /**
     * Returns a framable (skinnier) image for the specified avatar, scaled by
     * one over the specified factor.
     */
    public static BImage getFramableImage (
        BasicContext ctx, int[] avatar, int reduction)
    {
        BufferedImage base = getImage(ctx, avatar);
        BufferedImage cropped = base.getSubimage(
            (WIDTH-FRAMED_WIDTH)/2, 0, FRAMED_WIDTH, HEIGHT);
        return new BImage(cropped.getScaledInstance(
                              FRAMED_WIDTH/reduction, HEIGHT/reduction,
                              BufferedImage.SCALE_SMOOTH));
    }

    /**
     * Obtains and scales an image for the specified avatar. The source image
     * will be cached.
     */
    public static BImage getImage (
        BasicContext ctx, int[] avatar, int width, int height)
    {
        return new BImage(getImage(ctx, avatar).getScaledInstance(
                              width, height, BufferedImage.SCALE_SMOOTH));
    }

    /**
     * Gets an unscaled image for the specified avatar, retrieving an
     * existing image from the cache if possible but otherwise creating
     * and caching the image.
     */
    public static BufferedImage getImage (BasicContext ctx, int[] avatar)
    {
        // first check the cache
        AvatarKey key = new AvatarKey(avatar);
        WeakReference<BufferedImage> iref = _icache.get(key);
        BufferedImage image;
        if (iref != null && (image = iref.get()) != null) {
            return image;
        }
        
        CharacterDescriptor cdesc = ctx.getAvatarLogic().decodeAvatar(avatar);
        ActionFrames af;
        try {
            af = ctx.getCharacterManager().getActionFrames(cdesc, "default");
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to load action frames " +
                    "[cdesc=" + cdesc + "].", e);
            // return a blank image rather than null
            return ctx.getImageCache().createCompatibleImage(
                WIDTH, HEIGHT, true);
        }

        // composite the myriad components and render them into an image
        MultiFrameImage mfi = af.getFrames(0);
        int ox = af.getXOrigin(0, 0), oy = af.getYOrigin(0, 0);
        image = ctx.getImageManager().createImage(WIDTH, HEIGHT,
            Transparency.BITMASK);
        Graphics2D gfx = (Graphics2D)image.createGraphics();
        try {
//             gfx.setColor(java.awt.Color.black);
//             gfx.drawRect(0, 0, WIDTH-1, HEIGHT-1);
            mfi.paintFrame(gfx, 0, WIDTH/2-ox, HEIGHT-oy);
        } finally {
            gfx.dispose();
        }
        
        _icache.put(key, new WeakReference<BufferedImage>(image));
        return image;
    }

    public AvatarView (BasicContext ctx)
    {
        this(ctx, null);
    }

    public AvatarView (BasicContext ctx, int[] avatar)
    {
        super("", "avatar_view");
        _ctx = ctx;
        _frame = ctx.loadImage("ui/frames/big_frame.png");
        if (avatar != null) {
            setAvatar(avatar);
        }
    }

    /**
     * Decodes and displays the specified avatar fingerprint.
     */
    public void setAvatar (int[] avatar)
    {
        if (_avatar != null && Arrays.equals(avatar, _avatar)) {
            return;
        }
        _avatar = (int[])avatar.clone();
        setIcon(new ImageIcon(getFramableImage(_ctx, avatar, 2)));
    }

    @Override // documentation inherited
    public Dimension getPreferredSize (int whint, int hhint)
    {
        return new Dimension(_frame.getWidth(), _frame.getHeight());
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _frame.reference();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _frame.release();
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);
        _frame.render(renderer, 0, 0, _alpha);
    }

    /** Wraps avatar fingerprints for use as hash keys. */
    protected static class AvatarKey
    {
        public AvatarKey (int[] avatar)
        {
            _avatar = avatar;
        }
        
        public int hashCode ()
        {
            return IntListUtil.sum(_avatar);
        }
        
        public boolean equals (Object other)
        {
            return Arrays.equals(_avatar, ((AvatarKey)other)._avatar);
        }
        
        protected int[] _avatar;
    }

    protected BasicContext _ctx;
    protected BImage _frame;
    protected int[] _avatar;

    /** The avatar image cache. */
    protected static HashMap<AvatarKey, WeakReference<BufferedImage>> _icache =
        new HashMap<AvatarKey, WeakReference<BufferedImage>>();
}
