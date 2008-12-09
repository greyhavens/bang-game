//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.BillboardNode;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.Spatial;

import com.threerings.jme.sprite.PathUtil;

import com.threerings.bang.util.IconConfig;

import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.effect.RepairEffect;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * An effect visualization that floats an icon above the sprite, letting users
 * know what happened in terms of gaining or losing health, switching sides,
 * etc., with consistent symbology.
 */
public class IconViz extends EffectViz
{
    /**
     * Creates an icon visualization for the given effect identifier, or
     * returns <code>null</code> if no icon is necessary.
     */
    public static IconViz createIconViz (Piece piece, String effect)
    {
        if (RepairEffect.isRepairEffect(effect)) {
            return new IconViz(RepairEffect.REPAIRED);
        } else {
            return IconConfig.haveIcon(effect) ? new IconViz(effect) : null;
        }
    }

    /**
     * Creates a visualization that drops a representation of the specified
     * card down onto the piece or coordinates.
     */
    public static IconViz createCardViz (Card card)
    {
        return new IconViz(card.getIconPath("icon"), true);
    }

    public IconViz (String ipath)
    {
        _ipath = ipath;
    }

    protected IconViz (String ipath, ColorRGBA color)
    {
        _ipath = ipath;
        _color = color;
    }

    protected IconViz (String ipath, boolean card)
    {
        _ipath = ipath;
        _card = true;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        _zOffset = (_sprite != null ? _sprite.getPiece().getHeight() : 1f) * TILE_SIZE;
        createBillboard();
        if (_ipath != null) {
            if (_card) {
                _billboard.attachChild(IconConfig.createIcon(_ctx,
                    "textures/effects/cardback.png", ICON_SIZE, ICON_SIZE));
                _billboard.attachChild(IconConfig.createIcon(_ctx,
                    _ipath, CARD_SIZE, CARD_SIZE));

            } else {
                ColorRGBA color = ColorRGBA.white;
                if (_color != null) {
                    color = _color;
                } else if (_sprite != null) {
                    color = getJPieceColor(_sprite.getPiece().owner);
                }
                _billboard.attachChild(IconConfig.createIcon(_ctx,
                    _ipath, ICON_SIZE, ICON_SIZE, color));
            }
        }
    }

    @Override // documentation inherited
    public void display ()
    {
        if (_sprite != null) {
            _sprite.attachChild(_billboard);
        } else {
            // wrap the billboard in a container node for ease of
            // transformation
            Node xform = new Node("icon") {
                public int detachChild (Spatial child) {
                    parent.detachChild(this);
                    return super.detachChild(child);
                }
            };
            xform.getLocalTranslation().set(_pos);
            PathUtil.computeRotation(Vector3f.UNIT_Z, Vector3f.UNIT_Z,
                    _view.getTerrainNode().getHeightfieldNormal(_pos.x, _pos.y),
                    xform.getLocalRotation());
            xform.attachChild(_billboard);
            _view.getPieceNode().attachChild(xform);
        }
        _billboard.updateRenderState();
        _billboard.updateGeometricState(0f, true);
    }

    /**
     * Creates the named billboard.
     */
    protected void createBillboard ()
    {
        _billboard = new BillboardNode("billboard") {
            public void updateWorldData (float time) {
                super.updateWorldData(time);
                float alpha;
                if ((_elapsed += time) >= RISE_DURATION + LINGER_DURATION +
                    FADE_DURATION) {
                    parent.detachChild(this);
                    billboardDetached();
                    return;

                } else if (_elapsed >= RISE_DURATION + LINGER_DURATION) {
                    alpha = 1f - (_elapsed - RISE_DURATION - LINGER_DURATION) /
                        FADE_DURATION;
                    billboardFade();

                } else if (_elapsed >= RISE_DURATION) {
                    alpha = 1f;
                    localTranslation.z = _zOffset;
                    billboardLinger(_elapsed);

                } else {
                    alpha = _elapsed / RISE_DURATION;
                    localTranslation.z = _zOffset *
                        FastMath.LERP(alpha, _card ? 1.5f : 0.5f, 1f);
                    billboardRise(_elapsed);
                }
                for (Spatial child : children) {
                    if (child instanceof Geometry) {
                        ((Geometry)child).getBatch(0).getDefaultColor().a =
                            alpha;
                    }
                }
            }
            protected float _elapsed;
        };
    }

    /**
     * Used to add special animation during the rise phase.
     */
    protected void billboardRise (float elapsed)
    {
        // nothing to do here
    }

    /**
     * Used to add special animation during the linger phase.
     */
    protected void billboardLinger (float elapsed)
    {
        // nothing to do here
    }

    /**
     * Called when the billboard detaches itself.
     */
    protected void billboardDetached ()
    {
        // nothing to do here
    }

    /**
     * Called when the billboard begins to fade out.
     */
    protected void billboardFade ()
    {
        // nothing to do here
    }

    /** The path of the icon to display. */
    protected String _ipath;

    /** The color in which to display the icon (or null for the default). */
    protected ColorRGBA _color;

    /** If true, this is a card, so slide it down rather than up and use the
     * card icon background. */
    protected boolean _card;

    /** The icon billboard. */
    protected BillboardNode _billboard;

    /** The z offset of the billboard. */
    protected float _zOffset;

    /** The size of the icon. */
    protected static final float ICON_SIZE = TILE_SIZE / 2;

    /** The size of the card icons (bigger because the icons are resized). */
    protected static final float CARD_SIZE = ICON_SIZE * 62 / 39;

    /** The length of time it takes for the icon to rise up and fade in. */
    protected static final float RISE_DURATION = 0.5f;

    /** The length of time the icon lingers before fading out. */
    protected static final float LINGER_DURATION = 1.25f;

    /** The length of time it takes for the icon to fade out. */
    protected static final float FADE_DURATION = 0.25f;
}
