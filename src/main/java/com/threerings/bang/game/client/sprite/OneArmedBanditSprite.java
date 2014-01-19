//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.threerings.openal.SoundGroup;

import com.jme.math.Vector3f;
import com.jme.scene.Spatial;
import com.jme.scene.BillboardNode;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.TextureState;

import com.samskivert.util.ArrayUtil;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.IconConfig;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.OneArmedBandit;
import com.threerings.bang.game.data.effect.RandomInfluenceEffect;
import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.sprite.UnitSprite;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Sprite for one-armed bandit.
 */
public class OneArmedBanditSprite extends UnitSprite
{
    public GenericCounterNode counter;

    public OneArmedBanditSprite (String type)
    {
        super(type);
    }

    @Override // documentation inherited
    public void init (
        BasicContext ctx, BoardView view, BangBoard board,
        SoundGroup sounds, Piece piece, short tick)
    {
        super.init(ctx, view, board, sounds, piece, tick);
        //counter = new GenericCounterNode();
        //counter.createGeometry((CounterInterface)piece, ctx);
        //attachChild(counter);
        
        // load wildcard textures
        for (RandomInfluenceEffect.Kind kind : RandomInfluenceEffect.Kind.values()) {
            String path = _cardIconPaths[kind.ordinal()];
            _cardIconStates = ArrayUtil.append(_cardIconStates, path != null ?
                RenderUtil.createTextureState(ctx, "influences/icons/" + path + ".png") : null);
        }
    }
    
    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);
        _target.updated(piece, tick);
        
        if (piece.isAlive()) {
            //counter.updateCount((CounterInterface)piece);
            
            int cardIndex = ((OneArmedBandit)piece).card.ordinal();
            if (_cardIconStates[cardIndex] != null) {
                if (_billboard == null) {
                     _cardIcon = IconConfig.createIcon(_cardIconStates[cardIndex],
                         CARD_SIZE, CARD_SIZE);
                     _cardIcon.setLocalTranslation(new Vector3f(0f, TILE_SIZE/4, 0f));
                     _billboard = new BillboardNode("billboard");
                     _billboard.attachChild(_cardIcon);
                     attachChild(_billboard);
                 } else {
                     IconConfig.configureIcon(_cardIcon, _cardIconStates[cardIndex]);
                     _billboard.setCullMode(Spatial.CULL_DYNAMIC);
                 }
            } else if (_billboard != null) {
                _billboard.setCullMode(Spatial.CULL_ALWAYS);
            }
        } else if (_billboard != null) {
            _billboard.setCullMode(Spatial.CULL_ALWAYS);
        }
     }
     
     /** The name of the icons to display. */
     protected String[] _cardIconPaths = new String[] { null, "increase_move_distance", "increase_attack", "increase_defense", "explode"};
     
     /** The name of the icons to display. */
     protected TextureState[] _cardIconStates = new TextureState[0];
     
     /** The card icon to be displayed over the unit. */
     protected Quad _cardIcon;
     
     /** The billboard to attach the card icon to. */
     protected BillboardNode _billboard;
     
     /** The size for the wild card. */
     protected static final float CARD_SIZE = TILE_SIZE * 0.3f;     
 }