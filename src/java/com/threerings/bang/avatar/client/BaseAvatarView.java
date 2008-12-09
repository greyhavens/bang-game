//
// $Id$

package com.threerings.bang.avatar.client;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

import java.lang.ref.SoftReference;

import java.nio.FloatBuffer;

import java.util.ArrayList;
import java.util.HashMap;

import com.jme.math.Vector2f;
import com.jme.renderer.Renderer;
import com.jme.util.geom.BufferUtils;

import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;

import com.samskivert.util.Invoker;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.cast.ActionFrames;
import com.threerings.cast.CharacterDescriptor;

import com.threerings.jme.util.ImageCache;
import com.threerings.media.util.MultiFrameImage;

import com.threerings.bang.data.BaseAvatarInfo;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Contains the base functionality common to both avatar and buckle displays.
 */
public abstract class BaseAvatarView extends BLabel
{
    /**
     * Obtains a framable (skinnier) image for the specified avatar, scaled by one over the
     * specified factor.
     */
    public static void getFramableImage (
        BasicContext ctx, final BaseAvatarInfo avatar, final int reduction,
        final ResultListener<BImage> receiver)
    {
        getFramableImage(ctx, avatar, 1f/reduction, receiver);
    }

    /**
     * Obtains a framable (skinnier) image for the specified avatar, scaled by one over the
     * specified factor.
     */
    public static void getFramableImage (
        BasicContext ctx, final BaseAvatarInfo avatar, final float reduction,
        final ResultListener<BImage> receiver)
    {
        getImage(ctx, avatar, new ResultListener<BufferedImage>() {
            public void requestCompleted (BufferedImage base) {
                // scale our crop frame to the size of the image we got back (which might not be
                // the canonical size)
                int fwidth = Math.round(
                    base.getWidth() * avatar.getFramedWidth() / avatar.getWidth());
                int fheight = Math.round(
                    base.getHeight() * avatar.getFramedHeight() / avatar.getHeight());
                BufferedImage cropped = base.getSubimage(
                    (base.getWidth()-fwidth)/2, (base.getHeight()-fheight)/2, fwidth, fheight);
                // compute our reduction based on the canonical width/height
                int sw = (int)(avatar.getFramedWidth() * reduction),
                    sh = (int)(avatar.getFramedHeight() * reduction);
                receiver.requestCompleted(
                    new BImage(cropped.getScaledInstance(sw, sh, BufferedImage.SCALE_SMOOTH)));
            }
            public void requestFailed (Exception cause) {
                receiver.requestFailed(cause);
            }
        });
    }

    /**
     * Obtains and scales an image for the specified avatar. The source image will be cached.
     */
    public static void getImage (
        BasicContext ctx, BaseAvatarInfo avatar, final int width, final int height,
        final boolean mirror, final ResultListener<BImage> receiver)
    {
        getImage(ctx, avatar, new ResultListener<BufferedImage>() {
            public void requestCompleted (BufferedImage base) {
                Image scaled = base.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH);
                if (mirror) {
                    receiver.requestCompleted(new BImage(scaled) {
                        public void setTextureCoords (int sx, int sy, int swidth, int sheight) {
                            // flip the texture coords left-to-right
                            super.setTextureCoords(sx, sy, swidth, sheight);
                            FloatBuffer tcoords = getTextureBuffer(0, 0);
                            swapInBuffer(tcoords, 0, 3);
                            swapInBuffer(tcoords, 1, 2);
                        }
                    });
                } else {
                    receiver.requestCompleted(new BImage(scaled));
                }
            }
            public void requestFailed (Exception cause) {
                receiver.requestFailed(cause);
            }
        });
    }

    /**
     * Gets an unscaled image for the specified avatar, retrieving an existing image from the cache
     * if possible but otherwise creating and caching the image.
     */
    public static void getImage (BasicContext ctx, BaseAvatarInfo avatar,
                                 ResultListener<BufferedImage> receiver)
    {
        // first check the cache
        SoftReference<BufferedImage> iref = _icache.get(avatar);
        BufferedImage image;
        if (iref != null && (image = iref.get()) != null) {
            receiver.requestCompleted(image);
            return;
        }

        // if this is a custom image avatar, get the image from the buffered image cache
        String ipath = avatar.getImage();
        if (!StringUtil.isBlank(ipath)) {
            receiver.requestCompleted(ctx.getImageCache().getBufferedImage(ipath));
            return;
        }

        // if this avatar is misconfigured, stop here
        if (!avatar.isValid()) {
            log.warning("Refusing to load blank avatar " + avatar + ".");
            receiver.requestCompleted(null);
            return;
        }

        // handle multiple pending requests to getImage() for the same fingerprint
        AvatarResolver resolver = _rcache.get(avatar);
        if (resolver != null) {
            resolver.receivers.add(receiver);
        } else {
            _rcache.put(avatar, new AvatarResolver(ctx, avatar, receiver));
        }
    }

    public BaseAvatarView (BasicContext ctx, int scale)
    {
        this(ctx, 1f / scale);
    }

    public BaseAvatarView (BasicContext ctx, float scale)
    {
        super("");
        _ctx = ctx;
        _fscale = scale;

        setOrientation(VERTICAL);
        setFit(Fit.SCALE);
    }

    /**
     * Indicates whether to flip our avatar image around the y axis. This should be called before
     * any call to {@link #setAvatar}.
     *
     * <p>TODO: this doesn't currently work for framable images.
     */
    public void setMirror (boolean mirror)
    {
        _mirror = mirror;
    }

    /**
     * Decodes and displays the specified avatar fingerprint.
     */
    protected void setAvatar (BaseAvatarInfo avatar)
    {
        if (avatar == null || ObjectUtil.equals(_avatar, avatar)) {
            return;
        }
        _avatar = (BaseAvatarInfo)avatar.clone();

        // if we have a custom image, just use that directly
        String ipath = _avatar.getImage();
        if (ipath != null) {
            setImage(_ctx.getImageCache().getBImage(ipath, 2f*_fscale, false));

        } else if (_avatar.isValid()) {
            ResultListener<BImage> rl = new ResultListener<BImage>() {
                public void requestCompleted (BImage image) {
                    setImage(image);
                }
                public void requestFailed (Exception cause) {
                }
            };
            if (_frame != null) {
                getFramableImage(_ctx, avatar, _fscale, rl);
            } else {
                getImage(_ctx, avatar, (int)(avatar.getWidth()*_fscale),
                        (int)(avatar.getHeight()*_fscale), _mirror, rl);
            }
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        if (_image != null) {
            _image.reference();
        }
        if (_frame != null) {
            _frame.reference();
        }
        if (_scroll != null) {
            _scroll.reference();
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        if (_image != null) {
            _image.release();
        }
        if (_frame != null) {
            _frame.release();
        }
        if (_scroll != null) {
            _scroll.release();
        }
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        renderImage(renderer);
        renderScroll(renderer);
        renderFrame(renderer);
        super.renderComponent(renderer);
    }

    protected void renderImage (Renderer renderer)
    {
        if (_image != null) {
            int ix = (getWidth() - _image.getWidth())/2;
            int iy = getImageY();
            _image.render(renderer, ix, iy, _alpha);
        }
    }

    protected int getImageY ()
    {
        return (_scroll == null ? (getHeight() - _image.getHeight()) : _scroll.getHeight())/2;
    }

    protected void renderScroll (Renderer renderer)
    {
        if (getText() != null && _scroll != null) {
            int ix = (getWidth() - _scroll.getWidth())/2;
            _scroll.render(renderer, ix, 0, _alpha);
        }
    }

    protected void renderFrame (Renderer renderer)
    {
        if (_frame != null) {
            _frame.render(renderer, 0, 0, _alpha);
        }
    }

    protected void setImage (BImage image)
    {
        if (isAdded() && _image != null) {
            _image.release();
        }
        _image = image;
        if (isAdded() && image != null) {
            _image.reference();
        }
    }

    /**
     * Swaps two texture coordinates in the specified buffer.
     */
    protected static void swapInBuffer (FloatBuffer tbuf, int idx1, int idx2)
    {
        BufferUtils.populateFromBuffer(_tcoord, tbuf, idx1);
        BufferUtils.copyInternalVector2(tbuf, idx2, idx1);
        BufferUtils.setInBuffer(_tcoord, tbuf, idx2);
    }

    /**
     * Renders the given frames to an image with the specified dimensions.
     */
    protected static BufferedImage renderFrame (
        BasicContext ctx, ActionFrames af, int width, int height)
    {
        MultiFrameImage mfi = af.getFrames(0);
        int ox = af.getXOrigin(0, 0), oy = af.getYOrigin(0, 0);
        BufferedImage image = ctx.getImageManager().createImage(
            width, height, Transparency.BITMASK);
        Graphics2D gfx = image.createGraphics();
        try {
            mfi.paintFrame(gfx, 0, width/2-ox, height-oy);
        } finally {
            gfx.dispose();
        }
        return image;
    }

    /** Handles composition of avatars on the invoker thread. */
    protected static class AvatarResolver extends Invoker.Unit
    {
        public ArrayList<ResultListener<BufferedImage>> receivers =
            new ArrayList<ResultListener<BufferedImage>>();

        public AvatarResolver (BasicContext ctx, BaseAvatarInfo avatar,
                               ResultListener<BufferedImage> receiver) {
            _ctx = ctx;
            _avatar = avatar;
            _cdesc = avatar.decodePrint(ctx);
            receivers.add(receiver);
            _ctx.getInvoker().postUnit(this);
        }

        public boolean invoke () {
            ActionFrames af;
            try {
                af = _ctx.getCharacterManager().getActionFrames(
                    _cdesc, _avatar.getCharacterAction());
            } catch (Exception e) {
                log.warning("Unable to load action frames " + _cdesc + ".", e);
                // return a blank image rather than null
                _image = ImageCache.createCompatibleImage(
                    _avatar.getWidth(), _avatar.getHeight(), true);
                return true;
            }

            // composite the myriad components and render them into an image
            _image = renderFrame(_ctx, af, _avatar.getWidth(), _avatar.getHeight());

            // TODO: cache composited avatars on disk
            return true;
        }

        public void handleResult () {
            _icache.put(_avatar, new SoftReference<BufferedImage>(_image));
            for (ResultListener<BufferedImage> receiver : receivers) {
                receiver.requestCompleted(_image);
            }
            _rcache.remove(_avatar);
        }

        public long getLongThreshold () {
            return 10000L; // this could take a while...
        }

        protected BasicContext _ctx;
        protected BaseAvatarInfo _avatar;
        protected CharacterDescriptor _cdesc;
        protected BufferedImage _image;
    }

    protected BasicContext _ctx;
    protected BImage _frame, _scroll, _image;
    protected BaseAvatarInfo _avatar;
    protected float _fscale;
    protected boolean _mirror;

    /** A mapping of active resolvers. */
    protected static HashMap<BaseAvatarInfo, AvatarResolver> _rcache =
        new HashMap<BaseAvatarInfo, AvatarResolver>();

    /** The image cache. */
    protected static HashMap<BaseAvatarInfo, SoftReference<BufferedImage>> _icache =
        new HashMap<BaseAvatarInfo, SoftReference<BufferedImage>>();

    /** Used to flip texture coordinates. */
    protected static Vector2f _tcoord = new Vector2f();
}
