//
// $Id$

package com.threerings.bang.client;

import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.Skybox;
import com.jme.util.TextureManager;

import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Used to display the sky.
 */
public class SkyNode extends Node
{
    public SkyNode (BangContext ctx)
    {
        super("skynode");
    
        Skybox box = new Skybox("sky", 1000, 1000, 1000);
        for (int ii = 0; ii < ORIENTS.length; ii++) {
            String path = "rsrc/media/textures/desertday" +
                SUFFIXES[ii] + ".tga";
            Texture texture = TextureManager.loadTexture(
                getClass().getClassLoader().getResource(path),
                Texture.MM_LINEAR, Texture.FM_LINEAR,
                Image.GUESS_FORMAT_NO_S3TC, 1.0f, true);
            box.setTexture(ORIENTS[ii], texture);
        }
        box.preloadTextures();
        Quaternion r1 = new Quaternion();
        r1.fromAngleAxis(-FastMath.PI/2, new Vector3f(-1,0,0));
        Quaternion r2 = new Quaternion();
        r2.fromAngleAxis(FastMath.PI/2, new Vector3f(0,1,0));
        r1.multLocal(r2);
        box.setLocalRotation(r1);
        attachChild(box);
        updateRenderState();
    }

    protected static final int[] ORIENTS = {
        Skybox.NORTH, Skybox.SOUTH, Skybox.WEST, Skybox.EAST,
        Skybox.UP /*, Skybox.DOWN */
    };
    protected static final String[] SUFFIXES = {
        "ft", "bk", "rt", "lf", "up", "dn"
    };
}
