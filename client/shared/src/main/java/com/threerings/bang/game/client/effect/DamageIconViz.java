//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;

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
        _readout = new Geometry[readoutsize];
        if (_showText) {
            // jME3: the fork built a TextureState'd quad and animated its alpha via
            // getDefaultColor(); RenderUtil.createTextQuad yields a textured Geometry whose tint
            // (and alpha) live on its Unshaded Color material param.
            Geometry text = RenderUtil.createTextQuad(
                    _ctx, BangUI.DAMAGE_FONT, _color, _dcolor,
                    String.valueOf(_damage));
            // createTextQuad returns a quad sized in texture PIXELS (e.g. ~150 world units wide for
            // a two-digit number), which renders huge on the board. Normalize the pixel quad to a
            // fixed world height (preserving aspect) so it matches the fork (see NUMBER_SIZE below),
            // and center it on the icon-row origin. (Quad.width/height are private in jME3 3.9, so
            // measure the mesh bound: the quad spans (0,0)-(w,h).)
            com.jme3.scene.Mesh tmesh = text.getMesh();
            tmesh.updateBound();
            com.jme3.bounding.BoundingBox tb = (com.jme3.bounding.BoundingBox)tmesh.getBound();
            float tqh = tb.getYExtent() * 2f, tqw = tb.getXExtent() * 2f;
            // Match the fork's rendered number size. The fork built the quad at width x ICON_SIZE
            // but mapped the WHOLE font line-box (leading+ascent+descent) onto it via tcoords, so
            // the visible digits (which occupy roughly half a line box) rendered at ~ICON_SIZE/2.
            // BUG-4 changed createTextTexture to size to the tight glyph-ink box, so here the mesh
            // height IS the ink height; normalizing it to ICON_SIZE made the digits ~2x too tall
            // (DamageOffCenter.png). Target the ink height to NUMBER_SIZE (= ICON_SIZE/2) so the
            // number matches CorrectDamage.png / ShotgunnerDeathAndDamageCorrect.png.
            float tscale = (tqh > 0f) ? NUMBER_SIZE / tqh : 1f;
            if (tqh > 0f) {
                text.setLocalScale(tscale);
                // X-only centering: keep the BUG-8 horizontal centering so the number stays
                // centered on the icon-row origin, but leave Y at the shared baseline that
                // setReadoutY() manages for the text and all the icons together.
                text.setLocalTranslation(-tqw * tscale / 2f, 0f, 0f);
            }
            initReadoutColor(text);
            _readout[0] = text;
            readoutidx = 1;
            // advance past the rendered half-width of the number (matches the fork's
            // "offset += width/2f") so the attack/defend icons sit the correct distance
            // from a multi-digit number rather than a fixed ICON_SIZE/2.
            offset += tqw * tscale / 2f;
        }

        // Add the attack and defend icons if available
        if (_attackIcons != null) {
            for (int ii = 0; ii < _attackIcons.length; ii++) {
                _readout[readoutidx] = IconConfig.createIcon(_ctx,
                    "influences/icons/" + _attackIcons[ii] + ".png",
                    DAMAGE_SIZE, DAMAGE_SIZE);
                initReadoutColor(_readout[readoutidx]);
                // jME3 Quads are anchored at their bottom-left corner (the fork's jME1 Quads were
                // centered at the origin), so to put the icon's CENTER at x = offset + ii*DAMAGE_SIZE
                // we shift its corner left by half its width. Y centering is handled in setReadoutY.
                _readout[readoutidx].setLocalTranslation(
                    offset + ii * DAMAGE_SIZE - DAMAGE_SIZE / 2f,
                    _readout[readoutidx].getLocalTranslation().y, 0f);
                readoutidx++;
            }
        }

        if (_defendIcons != null) {
            for (int ii = 0; ii < _defendIcons.length; ii++) {
                _readout[readoutidx] = IconConfig.createIcon(_ctx,
                    "influences/icons/" + _defendIcons[ii] + ".png",
                    DAMAGE_SIZE, DAMAGE_SIZE);
                initReadoutColor(_readout[readoutidx]);
                // see attack-icon comment: corner-anchored quad, so shift left by half-width to
                // center the icon at x = -(offset + ii*DAMAGE_SIZE).
                _readout[readoutidx].setLocalTranslation(
                    -(offset + ii * DAMAGE_SIZE) - DAMAGE_SIZE / 2f,
                    _readout[readoutidx].getLocalTranslation().y, 0f);
                readoutidx++;
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

    /**
     * Initializes a readout geometry's material {@code Color} to transparent (the fork set the
     * batch default color to {@code new ColorRGBA()}); the billboard's per-frame alpha control
     * then fades it in.
     */
    protected static void initReadoutColor (Geometry geom)
    {
        if (geom.getMaterial() != null && geom.getMaterial().getParam("Color") != null) {
            geom.getMaterial().setColor("Color", new ColorRGBA(0f, 0f, 0f, 0f));
        }
    }

    @Override // documentation inherited
    protected void billboardDetached ()
    {
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
                Vector3f t = _readout[ii].getLocalTranslation();
                // jME3 Quads are anchored at their bottom-left corner and the readout geometries
                // differ in height (the number is ICON_SIZE tall, the icons DAMAGE_SIZE tall), so
                // place each one's corner half its own height below the target line. That makes the
                // number and the skull/influence icons share a single vertical center rather than a
                // common bottom edge (the latter is why the number floated higher than the skull).
                _readout[ii].setLocalTranslation(t.x, y - halfHeight(_readout[ii]), t.z);
            }
        }
    }

    /**
     * Returns the world-space half-height of a corner-anchored readout quad (mesh Y-extent scaled
     * by the geometry's local scale), used to vertically center it on the readout line.
     */
    protected static float halfHeight (Geometry geom)
    {
        com.jme3.scene.Mesh mesh = geom.getMesh();
        mesh.updateBound();
        com.jme3.bounding.BoundingVolume bv = mesh.getBound();
        if (bv instanceof com.jme3.bounding.BoundingBox) {
            return ((com.jme3.bounding.BoundingBox)bv).getYExtent() *
                geom.getLocalScale().y;
        }
        return 0f;
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

    /** The readout geometries (text + attack/defend icons). */
    protected Geometry[] _readout;

    /** The yoffset used when multiple damage icons are applied. */
    protected float _yOffset = 0;

    /** The length of time the icon takes to rise and fade out. */
    protected static final float FALL_DURATION = 0.20f;

    /** The size for the damage indicator. */
    protected static final float DAMAGE_SIZE = TILE_SIZE * 0.3f;

    /** The world height the damage number's glyph ink is normalized to. Half of ICON_SIZE: the
     * fork mapped the full font line-box onto an ICON_SIZE quad, leaving the visible digits at
     * roughly this height (see didInit). */
    protected static final float NUMBER_SIZE = ICON_SIZE / 2f;

    /** The name of damage billboards. */
    protected static final String DAMAGE_NAME = "dmgBillboard";
}
