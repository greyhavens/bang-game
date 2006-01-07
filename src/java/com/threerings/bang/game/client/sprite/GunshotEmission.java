//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.Properties;

import com.threerings.bang.client.Model;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.client.BoardView;

import static com.threerings.bang.Log.*;

/**
 * A gunshot effect with muzzle flash and bullet trail.
 */
public class GunshotEmission extends SpriteEmission
{
    public GunshotEmission (String name, Properties props)
    {
        super(name);
    }
    
    @Override // documentation inherited
    public void init (BasicContext ctx, BoardView view, PieceSprite sprite)
    {
        super.init(ctx, view, sprite);
        
    }
    
    @Override // documentation inherited
    public void start (Model.Animation anim, Model.Binding binding)
    {
        super.start(anim, binding);
        
    }
    
    @Override // documentation inherited
    public void stop ()
    {
        super.stop();

    }
}
