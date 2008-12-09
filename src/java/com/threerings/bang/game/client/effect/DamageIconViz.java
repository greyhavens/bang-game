//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.util.geom.BufferUtils;
import com.jme.image.Texture;
import com.jme.math.Vector2f;
import com.jme.renderer.ColorRGBA;

import com.jme.scene.shape.Quad;
import com.jme.scene.state.TextureState;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.IconConfig;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BangBoardView;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.MoveShootEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.client.BangMetrics.*;

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
    public static void displayDamageIconViz (Piece target, boolean showText, Effect effect,
        BangContext ctx, BangBoardView view)
    {
        if (target == null) {
            return;
        }
        displayDamageIconViz(target, getJPieceColor(target.owner),
                getDarkerPieceColor(target.owner), showText, effect, ctx, view);
    }

    /**
     * Creates a damage icon visualitzation for the given effect,
     * and adds it to the piece sprite.
     */
    public static void displayDamageIconViz (Piece target, ColorRGBA color,
        ColorRGBA dcolor, boolean showText, Effect effect, BangContext ctx, BangBoardView view)
    {
        if (target == null) {
            return;
        }
        displayDamageIconViz(target, target.isAlive() ? "damaged" : "killed",
            color, dcolor, effect.getBaseDamage(target), showText, effect, ctx, view);
    }

    /**
     * Creates a damage icon visualization for the given effect,
     * and adds it to the piece sprite.
     */
    public static void displayDamageIconViz (Piece target, String iname,
        ColorRGBA color, ColorRGBA dcolor, int damage, boolean showText, Effect effect,
        BangContext ctx, BangBoardView view)
    {
        if (target == null) {
            return;
        }

        PieceSprite sprite = view.getPieceSprite(target);
        if (sprite == null) {
            return;
        }

        DamageIconViz diviz = null;
        if (effect instanceof MoveShootEffect) {
            effect = ((MoveShootEffect)effect).shotEffect;
        }
        if (effect instanceof ShotEffect) {
            ShotEffect shot = (ShotEffect)effect;
            diviz = new DamageIconViz(iname, damage, color, dcolor, showText,
                shot.attackIcons, shot.defendIcons);
        } else {
            diviz = new DamageIconViz(iname, damage, color, dcolor, showText);
        }
        diviz.init(ctx, view, sprite, null);
        diviz.display();
    }

    protected DamageIconViz (String iname, int damage, ColorRGBA color,
        ColorRGBA dcolor, boolean showText)
    {
        this(iname, damage, color, dcolor, showText, null, null);
    }

    protected DamageIconViz (String iname, int damage, ColorRGBA color, ColorRGBA dcolor,
        boolean showText, String[] attackIcons, String[] defendIcons)
    {
        super("textures/effects/" + iname + ".png", color);
        _damage = damage;
        _dcolor = dcolor;
        _showText = showText;
        _attackIcons = attackIcons;
        _defendIcons = defendIcons;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // create the damage readout
        int readoutsize = 1 +
            (_attackIcons == null ? 0 : _attackIcons.length) +
            (_defendIcons == null ? 0 : _defendIcons.length);

        int readoutidx = 0;
        float offset =  DAMAGE_SIZE * 0.58f;
        _readout = new Quad[readoutsize];
        if (_showText) {
            _dmgTState = _ctx.getRenderer().createTextureState();
            _dmgTState.setEnabled(true);
            Vector2f[] tcoords = new Vector2f[4];
            Texture tex = RenderUtil.createTextTexture(
                    _ctx, BangUI.DAMAGE_FONT, _color, _dcolor,
                    String.valueOf(_damage), tcoords, null);
            _dmgTState.setTexture(tex);
            float width = ICON_SIZE * tcoords[2].x / tcoords[2].y;
            _readout[0] = IconConfig.createIcon(_dmgTState, width, ICON_SIZE);
            _readout[0].setTextureBuffer(0, BufferUtils.createFloatBuffer(tcoords));
            _readout[0].getBatch(0).getDefaultColor().set(new ColorRGBA());
            readoutidx = 1;
            offset += width / 2f;
        }

        // Add the attack and defend icons if available
        if (_attackIcons != null) {
            for (int ii = 0; ii < _attackIcons.length; ii++) {
                _readout[readoutidx] = IconConfig.createIcon(_ctx,
                    "influences/icons/" + _attackIcons[ii] + ".png",
                    DAMAGE_SIZE, DAMAGE_SIZE);
                _readout[readoutidx].getBatch(0).getDefaultColor().set(new ColorRGBA());
                _readout[readoutidx].getLocalTranslation().x =
                    offset + ii * DAMAGE_SIZE;
                readoutidx++;
            }
        }

        if (_defendIcons != null) {
            for (int ii = 0; ii < _defendIcons.length; ii++) {
                _readout[readoutidx] = IconConfig.createIcon(_ctx,
                    "influences/icons/" + _defendIcons[ii] + ".png",
                    DAMAGE_SIZE, DAMAGE_SIZE);
                _readout[readoutidx].getBatch(0).getDefaultColor().set(new ColorRGBA());
                _readout[readoutidx].getLocalTranslation().x =
                    -(offset + ii * DAMAGE_SIZE);
            }
        }

        for (int ii = 0; ii < _readout.length; ii++) {
            if (_readout[ii] != null) {
                _billboard.attachChild(_readout[ii]);
            }
        }
    }

    @Override // documentation inherited
    public void display ()
    {
        // Calculate the y offset based on the number of damage readouts
        // already on this sprite
        if (_sprite != null) {
            int gap = _sprite.damageAttach();
            attached = true;
            _yOffset = gap * DAMAGE_SIZE * 1.1f;
        }
        super.display();
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
        if (_dmgTState != null) {
            _dmgTState.deleteAll();
        }
        if (attached) {
            if (_sprite != null) {
                _sprite.damageDetach();
            }
            attached = false;
        }
    }

    @Override // documentation inherited
    protected void billboardFade ()
    {
        if (attached) {
            if (_sprite != null) {
                _sprite.damageDetach();
            }
            attached = false;
        }
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

    /** The alternate, darker color. */
    protected ColorRGBA _dcolor;

    /** The name of the icons to display. */
    protected String[] _attackIcons;
    protected String[] _defendIcons;

    /** The amount of damage to display. */
    protected int _damage;

    /** Show damage text. */
    protected boolean _showText;

    /** Set to true when we're attached. */
    protected boolean attached;

    /** The readout quad. */
    protected Quad[] _readout;

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
