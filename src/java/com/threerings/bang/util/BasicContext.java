//
// $Id$

package com.threerings.bang.util;

import java.awt.image.BufferedImage;

import com.jmex.bui.BLookAndFeel;

import com.threerings.util.MessageManager;

import com.threerings.jme.JmeApp;
import com.threerings.jme.JmeContext;
import com.threerings.jme.tile.FringeConfiguration;
import com.threerings.media.image.ImageManager;
import com.threerings.openal.SoundManager;
import com.threerings.resource.ResourceManager;

import com.threerings.bang.client.ModelCache;

import static com.threerings.bang.Log.log;

/**
 * Provides access to the various services needed by any application associated
 * with Bang!.
 */
public interface BasicContext extends JmeContext
{
    /** Returns the resource manager used to load resources. */
    public ResourceManager getResourceManager ();

    /** Returns the message manager used to localize messages. */
    public MessageManager getMessageManager ();

    /** Returns the look and feel used to configure the user interface. */
    public BLookAndFeel getLookAndFeel ();

    /** Returns the 3D model cache. */
    public ModelCache getModelCache ();

    /** Provides access to the tile fringing configuration. */
    public FringeConfiguration getFringeConfig ();

    /** Returns a reference to our top-level application. */
    public JmeApp getApp ();

    /** Returns a reference to our image manager. */
    public ImageManager getImageManager ();

    /** Returns a reference to our sound manager. */
    public SoundManager getSoundManager ();

    /** Translates the specified message using the specified message bundle. */
    public String xlate (String bundle, String message);

    /** Convenience method to load an image from our resource bundles. */
    public BufferedImage loadImage (String rsrcPath);
}
