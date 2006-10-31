//
// $Id$

package com.threerings.bang.avatar.client;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
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

import com.threerings.media.util.MultiFrameImage;

import com.threerings.cast.ActionFrames;
import com.threerings.cast.CharacterDescriptor;

import com.threerings.bang.client.PlayerPopupMenu;
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
     * Obtains a framable (skinnier) image for the specified avatar, scaled by
     * one over the specified factor.
     */
    public static void getFramableImage (
        BasicContext ctx, int[] avatar, final int reduction,
        final ResultListener<BImage> receiver)
    {
        getImage(ctx, avatar, new ResultListener<BufferedImage>() {
            public void requestCompleted (BufferedImage base) {
                BufferedImage cropped = base.getSubimage(
                    (WIDTH-FRAMED_WIDTH)/2, (HEIGHT-FRAMED_HEIGHT)/2,
                    FRAMED_WIDTH, FRAMED_HEIGHT);
                receiver.requestCompleted(
                    new BImage(cropped.getScaledInstance(
                                   FRAMED_WIDTH/reduction,
                                   FRAMED_HEIGHT/reduction,
                                   BufferedImage.SCALE_SMOOTH)));
            }
            public void requestFailed (Exception cause) {
                receiver.requestFailed(cause);
            }
        });
    }

    /**
     * Obtains a coop framable (shorter) image for the specified avatar, 
     * scaled by one over the specified factor.
     */
    public static void getCoopFramableImage (
        BasicContext ctx, int[] avatar, final int reduction,
        final ResultListener<BImage> receiver)
    {
        getImage(ctx, avatar, new ResultListener<BufferedImage>() {
            public void requestCompleted (BufferedImage base) {
                BufferedImage cropped = base.getSubimage(
                    0, 0, WIDTH, HEIGHT);
                receiver.requestCompleted(
                    new BImage(cropped.getScaledInstance(
                                   WIDTH/reduction,
                                   HEIGHT/reduction,
                                   BufferedImage.SCALE_SMOOTH)));
            }
            public void requestFailed (Exception cause) {
                receiver.requestFailed(cause);
            }
        });
    }

    /**
     * Obtains and scales an image for the specified avatar. The source image
     * will be cached.
     */
    public static void getImage (
        BasicContext ctx, int[] avatar, final int width, final int height,
        final boolean mirror, final ResultListener<BImage> receiver)
    {
        getImage(ctx, avatar, new ResultListener<BufferedImage>() {
            public void requestCompleted (BufferedImage base) {
                Image scaled = base.getScaledInstance(
                    width, height, BufferedImage.SCALE_SMOOTH);
                if (mirror) {
                    receiver.requestCompleted(new BImage(scaled) {
                        public void setTextureCoords (
                            int sx, int sy, int swidth, int sheight) {
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
     * Gets an unscaled image for the specified avatar, retrieving an existing
     * image from the cache if possible but otherwise creating and caching the
     * image.
     */
    public static void getImage (
        final BasicContext ctx, int[] avatar,
        final ResultListener<BufferedImage> receiver)
    {
        // first check the cache
        final AvatarKey key = new AvatarKey(avatar);
        WeakReference<BufferedImage> iref = _icache.get(key);
        BufferedImage image;
        if (iref != null && (image = iref.get()) != null) {
            receiver.requestCompleted(image);
            return;
        }

        // TODO: handle multiple requests to getImage() for the same image
        // before the first one completes; currently the code will queue up
        // multiple composites
        final CharacterDescriptor cdesc =
            ctx.getAvatarLogic().decodeAvatar(avatar);
        ctx.getInvoker().postUnit(new Invoker.Unit() {
            public boolean invoke () {
                ActionFrames af;
                try {
                    af = ctx.getCharacterManager().getActionFrames(
                        cdesc, "default");
                } catch (Exception e) {
                    log.log(Level.WARNING, "Unable to load action frames " +
                            "[cdesc=" + cdesc + "].", e);
                    // return a blank image rather than null
                    _image = ctx.getImageCache().createCompatibleImage(
                        WIDTH, HEIGHT, true);
                    return true;
                }

                // composite the myriad components and render them into an image
                MultiFrameImage mfi = af.getFrames(0);
                int ox = af.getXOrigin(0, 0), oy = af.getYOrigin(0, 0);
                _image = ctx.getImageManager().createImage(
                    WIDTH, HEIGHT, Transparency.BITMASK);
                Graphics2D gfx = (Graphics2D)_image.createGraphics();
                try {
                    mfi.paintFrame(gfx, 0, WIDTH/2-ox, HEIGHT-oy);
                } finally {
                    gfx.dispose();
                }
                _icache.put(key, new WeakReference<BufferedImage>(_image));
                return true;
            }

            public void handleResult () {
                receiver.requestCompleted(_image);
            }

            protected BufferedImage _image;
        });
    }

    /**
     * Creates a view that can be used to display avatar images.
     *
     * @param scale the image will be one over this value times the "natural"
     * size of the avatar imagery. This should be at least 2.
     * @param framed whether to render a frame around the avatar image.
     * @param named whether to display a banner containing the name of the
     * avatar (which is set with {@link #setHandle}).
     */
    public AvatarView (BasicContext ctx, int scale,
                       boolean framed, boolean named)
    {
        super("");
        if (framed) {
            setStyleClass("avatar_view_framed_" + scale);
        } else {
            setStyleClass("avatar_view_unframed_" + scale);
        }
        setOrientation(VERTICAL);
        setWrap(false);
        setFit(true);
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

        // if we're going to display a name, load up the appropriate name
        // banner image
        if (named) {
            String type = null;
            switch (scale) {
            case 2: type = "big"; break;
            case 3: type = "medium"; break;
            case 4: type = "small"; break;
            case 8: type = "tiny"; break;
            }
            if (type != null) {
                _scroll = ctx.loadImage("ui/frames/" + type + "_scroll.png");
                phei += _scroll.getHeight()/2;
                pwid = Math.max(pwid, _scroll.getWidth());
            }
        }

        setPreferredSize(new Dimension(pwid, phei));
    }

    /**
     * Configures the handle of the avatar we're viewing. This will also
     * activate the player popup menu.
     */
    public void setHandle (Handle handle)
    {
        setHandle(_handle, _handle.toString());
    }

    /**
     * Configures the handle of the avatar we're viewing and potentially
     * modified version of that handle for display.
     */
    public void setHandle (Handle handle, String displayHandle)
    {
        _handle = handle;
        setText(displayHandle);
    }

    /**
     * Indicates whether to flip our avatar image around the y axis. This
     * should be called before any call to {@link #setAvatar}.
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
    public void setAvatar (int[] avatar)
    {
        if ((_avatar != null && Arrays.equals(avatar, _avatar)) ||
            avatar == null) {
            return;
        }
        _avatar = (int[])avatar.clone();

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

    @Override // from BComponent
    public boolean dispatchEvent (BEvent event)
    {
        // pop up a player menu if they click the mouse and we know who we're
        // looking at
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
    protected BImage _frame, _scroll, _image;
    protected int[] _avatar;
    protected int _scale;
    protected boolean _mirror;
    protected Handle _handle;

    /** Used to flip texture coordinates. */
    protected static Vector2f _tcoord = new Vector2f();

    /** The avatar image cache. */
    protected static HashMap<AvatarKey, WeakReference<BufferedImage>> _icache =
        new HashMap<AvatarKey, WeakReference<BufferedImage>>();
}
