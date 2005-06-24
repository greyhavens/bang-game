//
// $Id$

package com.threerings.bang.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import javax.imageio.ImageIO;

import com.jme.bui.BLookAndFeel;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.jme.JmeApp;
import com.threerings.jme.JmeContext;
import com.threerings.media.image.ImageUtil;
import com.threerings.parlor.util.ParlorContext;

import com.threerings.bang.client.ModelCache;
import com.threerings.bang.data.BangUserObject;

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

    /** Returns the look and feel used to configure the user interface. */
    public abstract BLookAndFeel getLookAndFeel ();

    /** Returns the 3D model cache. */
    public abstract ModelCache getModelCache ();

    /** Returns a reference to our top-level application. */
    public abstract JmeApp getApp ();

    /** Returns a reference to the current player's user object. Only
     * valid when we are logged onto the server. */
    public BangUserObject getUserObject ()
    {
        return (BangUserObject)getClient().getClientObject();
    }

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
