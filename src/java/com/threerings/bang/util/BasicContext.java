//
// $Id$

package com.threerings.bang.util;

import com.jme.scene.Spatial;

import com.jmex.bui.BImage;
import com.jmex.bui.BStyleSheet;

import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;

import com.threerings.cast.CharacterManager;
import com.threerings.jme.JmeApp;
import com.threerings.jme.JmeContext;
import com.threerings.jme.model.Model;
import com.threerings.jme.util.ImageCache;
import com.threerings.jme.util.ShaderCache;
import com.threerings.media.image.ImageManager;
import com.threerings.openal.SoundManager;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.client.GlobalKeyManager;
import com.threerings.bang.client.util.ModelCache;
import com.threerings.bang.client.util.ParticleCache;
import com.threerings.bang.client.util.TextureCache;

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

    /** Returns the stylesheet used to configure the user interface. */
    public BStyleSheet getStyleSheet ();

    /** Returns a reference to our top-level application. */
    public JmeApp getApp ();

    /** Returns a reference to the invoker object used to run tasks in the
     * background. */
    public Invoker getInvoker ();

    /** Provides a mechanism for registering global key bindings. */
    public GlobalKeyManager getKeyManager ();

    /** Returns a reference to our image manager. */
    public ImageManager getImageManager ();

    /** Returns a reference to our sound manager. */
    public SoundManager getSoundManager ();

    /** Returns a reference to our image cache. */
    public ImageCache getImageCache ();

    /** Returns a reference to our texture cache. */
    public TextureCache getTextureCache ();

    /** Returns a reference to our shader cache. */
    public ShaderCache getShaderCache ();

    /** Returns a reference to our model cache. */
    public ModelCache getModelCache ();

    /** Returns a reference to our particle effect cache. */
    public ParticleCache getParticleCache ();

    /** Returns a reference to our character manager. */
    public CharacterManager getCharacterManager ();

    /** Returns an object used to encode and decode avatar information. */
    public AvatarLogic getAvatarLogic ();

    /** Translates the specified message using the specified message bundle. */
    public String xlate (String bundle, String message);

    /** Loads a 3D model asynchronously from the cache. */
    public void loadModel (String type, String name, ResultListener<Model> rl);

    /** Loads a particle effect asynchronously from the cache. */
    public void loadParticles (String name, ResultListener<Spatial> rl);

    /** Loads an image from the cache. */
    public BImage loadImage (String rsrcPath);
}
