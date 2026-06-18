//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.BillboardControl;

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
                // jME3 Quad is corner-anchored (the fork's was centered), so center each icon
                // on the billboard origin or it floats up-and-to-the-side by half its size.
                attachCentered(IconConfig.createIcon(_ctx,
                    "textures/effects/cardback.png", ICON_SIZE, ICON_SIZE), ICON_SIZE, ICON_SIZE);
                attachCentered(IconConfig.createIcon(_ctx,
                    _ipath, CARD_SIZE, CARD_SIZE), CARD_SIZE, CARD_SIZE);

            } else {
                ColorRGBA color = ColorRGBA.White;
                if (_color != null) {
                    color = _color;
                } else if (_sprite != null) {
                    color = getJPieceColor(_sprite.getPiece().owner);
                }
                attachCentered(IconConfig.createIcon(_ctx,
                    _ipath, ICON_SIZE, ICON_SIZE, color), ICON_SIZE, ICON_SIZE);
            }
        }
    }

    /**
     * Centers a corner-anchored icon quad on the billboard origin and attaches it. jME3's
     * {@code Quad} spans (0,0)-(w,h), so without this shift the icon hangs off the pivot.
     */
    protected void attachCentered (Geometry icon, float width, float height)
    {
        icon.setLocalTranslation(-width / 2f, -height / 2f, 0f);
        _billboard.attachChild(icon);
    }

    @Override // documentation inherited
    public void display ()
    {
        if (_sprite != null) {
            _sprite.attachChild(_billboard);
        } else {
            // wrap the billboard in a container node for ease of
            // transformation
            final Node xform = new Node("icon");
            xform.setLocalTranslation(_pos.clone());
            xform.setLocalRotation(PathUtil.computeRotation(
                    Vector3f.UNIT_Z, Vector3f.UNIT_Z,
                    _view.getTerrainNode().getHeightfieldNormal(_pos.x, _pos.y),
                    xform.getLocalRotation()));
            // detach the container along with the billboard when the billboard finishes
            _detachParent = xform;
            xform.attachChild(_billboard);
            _view.getPieceNode().attachChild(xform);
        }
        _billboard.updateGeometricState();
    }

    /**
     * Creates the named billboard.
     */
    protected void createBillboard ()
    {
        // jME3: the fork BillboardNode (screen-facing Node whose updateWorldData drove the
        // rise/linger/fade) becomes a plain Node carrying a BillboardControl for the screen-facing
        // orientation plus an AbstractControl that steps the animation each frame.
        _billboard = new Node("billboard");
        _billboard.addControl(new BillboardControl());
        _billboard.addControl(new AbstractControl() {
            @Override protected void controlUpdate (float time) {
                Node bb = (Node)spatial;
                float alpha;
                if ((_elapsed += time) >= RISE_DURATION + LINGER_DURATION +
                    FADE_DURATION) {
                    Node p = (_detachParent != null) ? _detachParent : bb;
                    if (p.getParent() != null) {
                        p.getParent().detachChild(p);
                    }
                    billboardDetached();
                    return;

                } else if (_elapsed >= RISE_DURATION + LINGER_DURATION) {
                    alpha = 1f - (_elapsed - RISE_DURATION - LINGER_DURATION) /
                        FADE_DURATION;
                    billboardFade();

                } else if (_elapsed >= RISE_DURATION) {
                    alpha = 1f;
                    bb.setLocalTranslation(bb.getLocalTranslation().setZ(_zOffset));
                    billboardLinger(_elapsed);

                } else {
                    alpha = _elapsed / RISE_DURATION;
                    bb.setLocalTranslation(bb.getLocalTranslation().setZ(_zOffset *
                        FastMath.interpolateLinear(alpha, _card ? 1.5f : 0.5f, 1f)));
                    billboardRise(_elapsed);
                }
                setChildAlpha(bb, alpha);
            }
            @Override protected void controlRender (RenderManager rm, ViewPort vp) {
            }
            protected float _elapsed;
        });
    }

    /**
     * Sets the alpha of all {@link Geometry} children of the supplied node via their material's
     * {@code Color} param (replaces the fork {@code getBatch(0).getDefaultColor().a}).
     */
    protected static void setChildAlpha (Node node, float alpha)
    {
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry) {
                Geometry g = (Geometry)child;
                if (g.getMaterial() != null &&
                    g.getMaterial().getParam("Color") != null) {
                    ColorRGBA c = (ColorRGBA)g.getMaterial().getParam("Color").getValue();
                    g.getMaterial().setColor("Color",
                        new ColorRGBA(c.r, c.g, c.b, alpha));
                }
            } else if (child instanceof Node) {
                setChildAlpha((Node)child, alpha);
            }
        }
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

    /** The icon billboard (a jME3 Node carrying a BillboardControl + animation control). */
    protected Node _billboard;

    /** When the icon is placed free-standing (no sprite), the container node to detach when the
     * billboard finishes (so the wrapper goes away too). */
    protected Node _detachParent;

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
