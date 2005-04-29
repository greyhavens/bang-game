//
// $Id$

package com.threerings.bang.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import javax.imageio.ImageIO;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.jme.JmeContext;
import com.threerings.media.image.ImageUtil;
import com.threerings.parlor.util.ParlorContext;

import static com.threerings.bang.Log.log;

/**
 * Provides access to the various services needed by the Bang client.
 */
public abstract class BangContext
    implements ParlorContext, JmeContext
{
    /** Returns the resource manager used to load resources. */
    public abstract ResourceManager getResourceManager ();

    /** Returns the message manager used to localize messages. */
    public abstract MessageManager getMessageManager ();

    /**
     * Translates the specified message using the specified message bundle.
     */
    public String xlate (String bundle, String message)
    {
        MessageBundle mb = getMessageManager().getBundle(bundle);
        return (mb == null) ? message : mb.xlate(message);
    }

    /**
     * Convenience method to load an image from our resource bundles.
     */
    public BufferedImage loadImage (String rsrcPath)
    {
        try {
            return ImageIO.read(getResourceManager().getImageResource(rsrcPath));
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Unable to load image resource " +
                    "[path=" + rsrcPath + "].", ioe);
            // cope; return an error image of abitrary size
            return ImageUtil.createErrorImage(50, 50);
        }
    }
}
