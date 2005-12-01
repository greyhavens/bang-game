//
// $Id$

package com.threerings.bang.util;

import java.awt.image.BufferedImage;

import com.jme.image.Image;
import com.jmex.bui.BLookAndFeel;

import com.threerings.util.MessageManager;

import com.threerings.jme.JmeApp;
import com.threerings.jme.JmeContext;
import com.threerings.jme.tile.FringeConfiguration;
import com.threerings.media.image.ImageManager;
import com.threerings.openal.SoundManager;
import com.threerings.resource.ResourceManager;

import com.threerings.bang.client.Model;

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

    /** Loads a 3D model from the cache. */
    public Model loadModel (String type, String name);

    /** Loads an image from the cache. */
    public Image loadImage (String rsrcPath);

    /** Loads a buffered image from the cache. <em>Note:</em> you probably want
     * to use {@link #loadImage} unless you need a buffered image for some
     * special reason. Using {@link #loadImage} is (or will be) <em>much</em>
     * faster than loading a buffered image and converting that to something
     * usable by OpenGL. */
    public BufferedImage loadBufferedImage (String rsrcPath);
}
