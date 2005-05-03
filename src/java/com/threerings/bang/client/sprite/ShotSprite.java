//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;

import com.jme.scene.shape.Sphere;
import com.threerings.jme.sprite.Sprite;

/**
 * Displays a fired shot.
 */
public class ShotSprite extends Sprite
{
    public ShotSprite ()
    {
        Sphere ball = new Sphere("bullet", 5, 5, 0.5f);
        attachChild(ball);
    }

//     public ShotSprite ()
//     {
//         super(5, 5);
//         _oxoff = -2;
//         _oyoff = -2;
//     }

//     public void paint (Graphics2D gfx)
//     {
//         gfx.setColor(Color.black);
//         gfx.fillOval(_ox, _oy, _bounds.width, _bounds.height);
//     }
}
