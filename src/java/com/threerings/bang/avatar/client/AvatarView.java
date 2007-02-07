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
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;

import com.jme.math.Vector2f;
import com.jme.renderer.Renderer;
import com.jme.util.geom.BufferUtils;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.IntListUtil;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.media.util.MultiFrameImage;

import com.threerings.cast.ActionFrames;
import com.threerings.cast.CharacterDescriptor;

import com.threerings.bang.client.PlayerPopupMenu;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.avatar.util.AvatarLogic.*;

/**
 * Displays an avatar.
 */
public class AvatarView extends BLabel
{
    /**
     * Obtains a framable (skinnier) image for the specified avatar, scaled by one over the
     * specified factor.
     */
    public static void getFramableImage (BasicContext ctx, AvatarInfo avatar, final int reduction,
                                         final ResultListener<BImage> receiver)
    {
        getImage(ctx, avatar, new ResultListener<BufferedImage>() {
            public void requestCompleted (BufferedImage base) {
                // scale our crop frame to the size of the image we got back (which might not be
                // the canonical size)
                int fwidth = Math.round(base.getWidth() * FRAMED_WIDTH / WIDTH);
                int fheight = Math.round(base.getHeight() * FRAMED_HEIGHT / HEIGHT);
                BufferedImage cropped = base.getSubimage(
                    (base.getWidth()-fwidth)/2, (base.getHeight()-fheight)/2, fwidth, fheight);
                // compute our reduction based on the canonical width/height
                int sw = FRAMED_WIDTH/reduction, sh = FRAMED_HEIGHT/reduction; 
                receiver.requestCompleted(
                    new BImage(cropped.getScaledInstance(sw, sh, BufferedImage.SCALE_SMOOTH)));
            }
            public void requestFailed (Exception cause) {
                receiver.requestFailed(cause);
            }
        });
    }

    /**
     * Obtains a coop framable image for the specified avatar, scaled by one over the specified
     * factor.
     */
    public static void getCoopFramableImage (
        BasicContext ctx, AvatarInfo avatar, final int reduction,
        final ResultListener<BImage> receiver)
    {
        getImage(ctx, avatar, new ResultListener<BufferedImage>() {
            public void requestCompleted (BufferedImage base) {
                int sw = WIDTH/reduction, sh = HEIGHT/reduction;
                receiver.requestCompleted(
                    new BImage(base.getScaledInstance(sw, sh, BufferedImage.SCALE_SMOOTH)));
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
        BasicContext ctx, AvatarInfo avatar, final int width, final int height,
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
     * if possible but otherwise creating and caching the image. <em>Note:</em> don't use this
     * method as you almost certainly will be confused by what you get back (unscaled source avatar
     * images are not a uniform size).
     */
    public static void getImage (BasicContext ctx, AvatarInfo avatar,
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
        if (!StringUtil.isBlank(avatar.image)) {
            receiver.requestCompleted(ctx.getImageCache().getBufferedImage(avatar.image));
            return;
        }

        // if this avatar is misconfigured, stop here
        if (avatar.print == null || avatar.print.length == 0) {
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

    /**
     * Creates a view that can be used to display avatar images.
     *
     * @param scale the image will be one over this value times the "natural" size of the avatar
     * imagery. This should be at least 2.
     * @param framed whether to render a frame around the avatar image.
     * @param named whether to display a banner containing the name of the avatar (which is set
     * with {@link #setHandle}).
     */
    public AvatarView (BasicContext ctx, int scale, boolean framed, boolean named)
    {
        this(ctx, scale, framed, named, 0);
    }

    /**
     * Creates a view that can be used to display avatar images.
     *
     * @param scale the image will be one over this value times the "natural" size of the avatar
     * imagery. This should be at least 2.
     * @param framed whether to render a frame around the avatar image.
     * @param named whether to display a banner containing the name of the avatar (which is set
     * with {@link #setHandle}).
     * @param color the color index of the banner
     */
    public AvatarView (BasicContext ctx, int scale, boolean framed, boolean named, int color)
    {
        super("");
        if (framed) {
            setStyleClass("avatar_view_framed_" + scale);
        } else {
            setStyleClass("avatar_view_unframed_" + scale);
        }
        setOrientation(VERTICAL);
        setFit(Fit.SCALE);
        _ctx = ctx;
        _scale = scale;

        // set up our dimensions and frame
        int pwid, phei;
        if (framed) {
            switch (scale) {
            case 2: _frame = ctx.loadImage("ui/frames/big_frame.png"); break;
            case 3: _frame = ctx.loadImage("ui/frames/medium_frame.png"); break;
            case 4: _frame = ctx.loadImage("ui/frames/small_frame.png"); break;
            }
            pwid = _frame.getWidth();
            phei = _frame.getHeight();
        } else {
            pwid = WIDTH/scale;
            phei = HEIGHT/scale;
        }

        // if we're going to display a name, load up the appropriate name banner image
        if (named) {
            String type = null;
            switch (scale) {
            case 2: type = "big"; break;
            case 3: type = "medium"; break;
            case 4: type = "small"; break;
            case 8: type = "tiny"; break;
            }
            if (type != null) {
                _scroll = ctx.loadImage("ui/frames/" + type + "_scroll" + color + ".png");
                phei += _scroll.getHeight()/2;
                pwid = Math.max(pwid, _scroll.getWidth());
            }
        }

        setPreferredSize(new Dimension(pwid, phei));
    }

    /**
     * Configures the handle of the avatar we're viewing. This will also activate the player popup
     * menu.
     */
    public void setHandle (Handle handle)
    {
        setHandle(handle, handle.toString());
    }

    /**
     * Configures the handle of the avatar we're viewing and potentially modified version of that
     * handle for display.
     */
    public void setHandle (Handle handle, String displayHandle)
    {
        _handle = handle;
        setText(displayHandle);
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
    public void setAvatar (AvatarInfo avatar)
    {
        if (_avatar != null && _avatar.equals(avatar)) {
            return;
        }
        _avatar = (AvatarInfo)avatar.clone();

        // if we have a custom image, just use that directly
        if (_avatar.image != null) {
            setImage(_ctx.getImageCache().getBImage(_avatar.image, _scale/2, false));

        } else if (_avatar.print != null && _avatar.print.length > 0) {
            ResultListener<BImage> rl = new ResultListener<BImage>() {
                public void requestCompleted (BImage image) {
                    setImage(image);
                }
                public void requestFailed (Exception cause) {
                }
            };
            if (_frame != null) {
                getFramableImage(_ctx, avatar, _scale, rl);
            } else {
                getImage(_ctx, avatar, WIDTH/_scale, HEIGHT/_scale, _mirror, rl);
            }
        }
    }

    @Override // from BComponent
    public boolean dispatchEvent (BEvent event)
    {
        // pop up a player menu if they click the mouse and we know who we're looking at
        boolean handled = false;
        if (_handle != null && _ctx instanceof BangContext) {
            handled = PlayerPopupMenu.checkPopup(
                (BangContext)_ctx, getWindow(), event, _handle, false);
        }
        return handled || super.dispatchEvent(event);
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
            int iy = (getHeight() - _image.getHeight())/2;
            if (_scroll != null) {
                iy = _scroll.getHeight()/2;
            }
            _image.render(renderer, ix, iy, _alpha);
        }
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

    /**
     * Swaps two texture coordinates in the specified buffer.
     */
    protected static void swapInBuffer (FloatBuffer tbuf, int idx1, int idx2)
    {
        BufferUtils.populateFromBuffer(_tcoord, tbuf, idx1);
        BufferUtils.copyInternalVector2(tbuf, idx2, idx1);
        BufferUtils.setInBuffer(_tcoord, tbuf, idx2);
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

    /** Handles composition of avatar images on the invoker thread. */
    protected static class AvatarResolver extends Invoker.Unit
    {
        public ArrayList<ResultListener<BufferedImage>> receivers =
            new ArrayList<ResultListener<BufferedImage>>();

        public AvatarResolver (BasicContext ctx, AvatarInfo avatar,
                               ResultListener<BufferedImage> receiver) {
            _ctx = ctx;
            _avatar = avatar;
            _cdesc = _ctx.getAvatarLogic().decodeAvatar(avatar.print);
            receivers.add(receiver);
            _ctx.getInvoker().postUnit(this);
        }

        public boolean invoke () {
            ActionFrames af;
            try {
                af = _ctx.getCharacterManager().getActionFrames(_cdesc, "default");
            } catch (Exception e) {
                log.log(Level.WARNING, "Unable to load action frames " + _cdesc + ".", e);
                // return a blank image rather than null
                _image = _ctx.getImageCache().createCompatibleImage(WIDTH, HEIGHT, true);
                return true;
            }

            // composite the myriad components and render them into an image
            MultiFrameImage mfi = af.getFrames(0);
            int ox = af.getXOrigin(0, 0), oy = af.getYOrigin(0, 0);
            _image = _ctx.getImageManager().createImage(WIDTH, HEIGHT, Transparency.BITMASK);
            Graphics2D gfx = (Graphics2D)_image.createGraphics();
            try {
                mfi.paintFrame(gfx, 0, WIDTH/2-ox, HEIGHT-oy);
            } finally {
                gfx.dispose();
            }

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

        protected BasicContext _ctx;
        protected AvatarInfo _avatar;
        protected CharacterDescriptor _cdesc;
        protected BufferedImage _image;
    }

    protected BasicContext _ctx;
    protected BImage _frame, _scroll, _image;
    protected AvatarInfo _avatar;
    protected int _scale;
    protected boolean _mirror;
    protected Handle _handle;

    /** Used to flip texture coordinates. */
    protected static Vector2f _tcoord = new Vector2f();

    /** A mapping of active resolvers. */
    protected static HashMap<AvatarInfo, AvatarResolver> _rcache =
        new HashMap<AvatarInfo, AvatarResolver>();

    /** The avatar image cache. */
    protected static HashMap<AvatarInfo, SoftReference<BufferedImage>> _icache =
        new HashMap<AvatarInfo, SoftReference<BufferedImage>>();
}
