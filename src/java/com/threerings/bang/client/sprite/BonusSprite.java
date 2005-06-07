//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.image.BufferedImage;

import com.jme.math.Vector3f;
import com.jme.scene.shape.Disk;

import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a bonus piece of some sort.
 */
public class BonusSprite extends PieceSprite
{
    public BonusSprite (String type)
    {
        _type = type;

        // create some simple temporary geometry
        _disk = new Disk("bonus", 10, 10, TILE_SIZE/2);
        _disk.setLocalTranslation(new Vector3f(TILE_SIZE/2, TILE_SIZE/2, 0f));
        attachChild(_disk);
    }

    @Override // documentation inherited
    public void init (BangContext ctx, Piece piece, short tick)
    {
        super.init(ctx, piece, tick);

        BufferedImage image = ctx.loadImage("bonuses/" + _type + "/icon.png");
        _disk.setRenderState(RenderUtil.createTexture(ctx, image));
        _disk.updateRenderState();
    }

    protected String _type;
    protected Disk _disk;
}
