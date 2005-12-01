//
// $Id$

package com.threerings.bang.client.util;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.logging.Level;

import java.io.IOException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import com.jme.image.Image;

import com.threerings.media.image.ImageUtil;

import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Manages a weak cache of image data to make life simpler for callers that
 * don't want to worry about coordinating shared use of the same images.
 */
public class ImageCache
{
    public ImageCache (BasicContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Loads up an image from the cache if possible or from the resource
     * manager otherwise, in which case it is prepared for use by JME and
     * OpenGL.
     */
    public Image getImage (String rsrcPath)
    {
        // first check the cache
        WeakReference<Image> iref = _imgcache.get(rsrcPath);
        Image image;
        if (iref != null && (image = iref.get()) != null) {
            return image;
        }

        // load the image data from the resource manager
        BufferedImage bufimg;
        try {
            bufimg = ImageIO.read(
                _ctx.getResourceManager().getImageResource(rsrcPath));
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Unable to load image resource " +
                    "[path=" + rsrcPath + "].", ioe);
            // cope; return an error image of abitrary size
            bufimg = ImageUtil.createErrorImage(64, 64);
        }

        // convert the the image to the format we need for rendering to the
        // display (TODO: potentially use 16-bit if we're running in a 16-bit
        // display mode)
        int width = bufimg.getWidth(), height = bufimg.getHeight();
        boolean hasAlpha = bufimg.getColorModel().hasAlpha();
        BufferedImage dispimg = new BufferedImage(
            width, height, hasAlpha ? BufferedImage.TYPE_4BYTE_ABGR :
            BufferedImage.TYPE_3BYTE_BGR);

        // flip the image to convert into OpenGL's coordinate system
        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -height);

        // "convert" the image by rendering the old into the new
        Graphics2D gfx = (Graphics2D)dispimg.getGraphics();
        gfx.drawImage(bufimg, tx, null);
        gfx.dispose();

        // now extract the image data which is usable by JME
        ByteBuffer scratch = ByteBuffer.allocateDirect(4 * width * height);
        scratch.order(ByteOrder.nativeOrder());
        byte data[] = (byte[])dispimg.getRaster().getDataElements(
            0, 0, dispimg.getWidth(), dispimg.getHeight(), null);
        scratch.clear();
        scratch.put(data);
        scratch.flip();

        // create and cache a new JME image with the appropriate data
        image = new Image();
        image.setType(hasAlpha ? Image.RGBA8888 : Image.RGB888);
        image.setWidth(width);
        image.setHeight(height);
        image.setData(scratch);
        _imgcache.put(rsrcPath, new WeakReference<Image>(image));

        return image;
    }

    /**
     * Loads up a buffered image from the cache if possible or from the
     * resource manager otherwise. <em>Note:</em> these images are cached
     * separately from the normal image cache.
     */
    public BufferedImage getBufferedImage (String rsrcPath)
    {
        // first check the cache
        WeakReference<BufferedImage> iref = _bimgcache.get(rsrcPath);
        BufferedImage image;
        if (iref != null && (image = iref.get()) != null) {
            return image;
        }

        // load the image data from the resource manager
        try {
            image = ImageIO.read(
                _ctx.getResourceManager().getImageResource(rsrcPath));
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Unable to load image resource " +
                    "[path=" + rsrcPath + "].", ioe);
            // cope; return an error image of abitrary size
            image = ImageUtil.createErrorImage(64, 64);
        }

        _bimgcache.put(rsrcPath, new WeakReference<BufferedImage>(image));
        return image;
    }

    protected BasicContext _ctx;

    protected HashMap<String,WeakReference<Image>> _imgcache =
        new HashMap<String,WeakReference<Image>>();
    protected HashMap<String,WeakReference<BufferedImage>> _bimgcache =
        new HashMap<String,WeakReference<BufferedImage>>();
}
