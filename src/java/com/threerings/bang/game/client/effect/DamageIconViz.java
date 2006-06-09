//
// $Id$

package com.threerings.bang.game.client.effect;

import java.util.Iterator;

import com.jme.util.geom.BufferUtils;
import com.jme.image.Texture;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;

import com.jme.scene.BillboardNode;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BangBoardView;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.client.BangMetrics.*;

import static com.threerings.bang.Log.*;
import com.threerings.util.RandomUtil;

/**
 * An effect visualization that floats a damage indicator and possible
 * influence icons about the sprite, letting users know how much damage was
 * caused and influence modifiers to the damage value.
 */
public class DamageIconViz extends IconViz
{
    /**
     * Creates a damage icon visualitzation for the given effect,
     * and adds it to the piece sprite.
     */
    public static void displayDamageIconViz (Piece target, int damage,
            BangContext ctx, BangBoardView view)
    {
        displayDamageIconViz(target, damage, null, ctx, view);
    }

    /**
     * Creates a damage icon visualitzation for the given effect,
     * and adds it to the piece sprite.
     */
    public static void displayDamageIconViz (Piece target, Effect effect,
            BangContext ctx, BangBoardView view)
    {
        displayDamageIconViz(target, 0, effect, ctx, view);
    }
    
    /**
     * Creates a damage icon visualitzation for the given effect,
     * and adds it to the piece sprite.
     */
    public static void displayDamageIconViz (Piece target, int damage,
            Effect effect, BangContext ctx, BangBoardView view)
    {
        if (target == null) {
            return;
        }
        PieceSprite sprite = view.getPieceSprite(target);
        if (sprite == null) {
            log.warning("Missing sprite for damage effect " +
                    "[target=" + target + ", effect=" + effect + "].");
            return;
        }
        DamageIconViz diviz = null; 
        String iname = (target.isAlive() ? "damaged" : "killed");
        if (effect == null) {
            diviz = new DamageIconViz(iname, damage);

        } else if (effect instanceof ShotEffect) {
            ShotEffect shot = (ShotEffect)effect;
            diviz = new DamageIconViz(iname, shot.baseDamage,
                    shot.attackIcon, shot.defendIcon);
        }

        if (diviz != null) {
            diviz.init(ctx, view, target, null);
            diviz.display(sprite);
        }
    }

    protected DamageIconViz (String iname, int damage)
    {
        this(iname, damage, null, null);
    }

    protected DamageIconViz (String iname, int damage,
            String attackIcon, String defendIcon)
    {
        super(iname);
        _damage = damage;
        _attackIcon = attackIcon;
        _defendIcon = defendIcon;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // create the damage readout
        _dmgTState = _ctx.getRenderer().createTextureState();
        _dmgTState.setEnabled(true);
        Vector2f[] tcoords = new Vector2f[4];
        Texture tex = RenderUtil.createTextTexture(
                _ctx, BangUI.DAMAGE_FONT, JPIECE_COLORS[_target.owner],
                DARKER_COLORS[_target.owner], String.valueOf(_damage),
                tcoords, null);
        _dmgTState.setTexture(tex);
        float width = ICON_SIZE * tcoords[2].x / tcoords[2].y;
        _readout[0] = new Quad("damage", width, ICON_SIZE);
        _readout[0].setTextureBuffer(0, BufferUtils.createFloatBuffer(tcoords));
        _readout[0].setRenderState(_dmgTState);
        _readout[0].setRenderState(RenderUtil.blendAlpha);
        _readout[0].setRenderState(RenderUtil.alwaysZBuf);
        _readout[0].updateRenderState();
        _readout[0].setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        _readout[0].setLightCombineMode(LightState.OFF);
        _readout[0].setDefaultColor(new ColorRGBA());

        // Add the attack and defend icons if available
        float offset =  DAMAGE_SIZE * 0.58f + width / 2f;
        if (_attackIcon != null) {
            _readout[1] = createIconQuad(
                    "influences/icons/" + _attackIcon + ".png", DAMAGE_SIZE);
            _readout[1].setDefaultColor(new ColorRGBA());
            _readout[1].getLocalTranslation().x = offset; 
        }

        if (_defendIcon != null) {
            _readout[2] = createIconQuad(
                    "influences/icons/" + _defendIcon + ".png", DAMAGE_SIZE);
            _readout[2].setDefaultColor(new ColorRGBA());
            _readout[2].getLocalTranslation().x = -offset;
        }
            
        for (int ii = 0; ii < _readout.length; ii++) {
            if (_readout[ii] != null) {
                _billboard.attachChild(_readout[ii]);
            }
        }
    }

    @Override // documentation inherited
    public void display (PieceSprite target)
    {
        // Calculate the y offset based on the number of damage readouts
        // already on this sprite
        Iterator iter = target.getChildren().iterator();
        int gap = 0;
        while (iter.hasNext()) {
            Spatial child = (Spatial)iter.next();
            if (DAMAGE_NAME.equals(child.getName())) {
                gap++;
            }
        }
        _yOffset = gap * DAMAGE_SIZE * 1.1f;
        
        super.display(target);
    }

    @Override // documentation inherited
    protected void billboardRise (float elapsed)
    {
        float y;
        float rising = RISE_DURATION - FALL_DURATION;
        if (elapsed >= rising) {
            float falling = RISE_DURATION - elapsed;
            y = ICON_SIZE * (1f + 0.5f * falling / FALL_DURATION);
        } else {
            y = ICON_SIZE * (1f + 0.5f * elapsed / rising);
        }
        setReadoutY(y);
    }

    @Override // documentation inherited
    protected void billboardLinger (float elapsed)
    {
        setReadoutY(ICON_SIZE);
    }

    @Override // documentation inherited
    protected void billboardDetached ()
    {
        _dmgTState.deleteAll();
    }

    /**
     * Sets the y coordinate of the local translation for the readout quads.
     */
    protected void setReadoutY (float y)
    {
        y += _yOffset;
        for (int ii = 0; ii < _readout.length; ii++) {
            if (_readout[ii] != null) {
                _readout[ii].getLocalTranslation().y = y;
            }
        }
    }

    @Override // documentation inherited
    protected void createBillboard ()
    {
        super.createBillboard();
        _billboard.setName(DAMAGE_NAME);
    }

    /** The name of the icons to display. */
    protected String _attackIcon;
    protected String _defendIcon;

    /** The amount of damage to display. */
    protected int _damage;

    /** The readout quad. */
    protected Quad[] _readout = new Quad[3];

    /** The damage indicator texture state. */
    protected TextureState _dmgTState;

    /** The yoffset used when multiple damage icons are applied. */
    protected float _yOffset = 0;

    /** The length of time the icon takes to rise and fade out. */
    protected static final float FALL_DURATION = 0.20f;

    /** The size for the damage indicator. */
    protected static final float DAMAGE_SIZE = TILE_SIZE * 0.3f;

    /** The name of damage billboards. */
    protected static final String DAMAGE_NAME = "dmgBillboard";
}
