//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme3.math.FastMath;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;

import com.threerings.openal.SoundGroup;

import static com.threerings.bang.Log.log;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.util.SoundUtil;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.effect.GrantCardEffect;
import com.threerings.bang.game.data.effect.ReboundEffect;
import com.threerings.bang.game.data.effect.SnareEffect;
import com.threerings.bang.game.data.effect.TrapEffect;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a bonus piece of some sort.
 */
public class BonusSprite extends ActiveSprite
{
    public BonusSprite (String type)
    {
        super("bonuses", type);
        if (!isHidden()) {
            addControl(_spinner = new Spinner(this, FastMath.PI/2));
        }
        if (DEBUG_BONUS_DIAG) {
            addControl(new BonusTexDiag());
        }
    }

    /**
     * DEBUG (-Dbang.debug.bonusdiag on the client JVM): every ~2s, logs this bonus's per-geometry
     * DiffuseMap state (texture name + backing jME3 Image id) so a white (untextured) instance can
     * be diagnosed at runtime -- a freed Image reports id &lt; 0, a missing bind reports no-diffuse.
     */
    protected class BonusTexDiag extends AbstractControl
    {
        @Override protected void controlUpdate (float tpf) {
            if ((_accum += tpf) < 2f) {
                return;
            }
            _accum = 0f;
            final StringBuilder sb = new StringBuilder();
            // Traverse only the model subtree, not the whole sprite: a PieceSprite also carries
            // shadow / tile-highlight overlay geometry built on Unshaded.j3md (a ColorMap, no
            // DiffuseMap by design), which would otherwise always show up here as a spurious
            // [no-diffuse]. The white-bonus probe is about the lit model geoms only.
            Spatial model = (_model != null) ? _model : BonusSprite.this;
            model.depthFirstTraversal(new SceneGraphVisitor() {
                public void visit (Spatial s) {
                    if (!(s instanceof Geometry)) {
                        return;
                    }
                    Material m = ((Geometry)s).getMaterial();
                    if (m == null) {
                        sb.append("[no-mat]");
                        return;
                    }
                    MatParamTexture mp = m.getTextureParam("DiffuseMap");
                    Texture t = (mp == null) ? null : mp.getTextureValue();
                    if (t == null) {
                        // a material with no DiffuseMap *slot* (e.g. Unshaded overlay) is not a
                        // model-texturing failure; only flag a Lighting material missing its bind.
                        if (m.getMaterialDef() != null &&
                            m.getMaterialDef().getMaterialParam("DiffuseMap") != null) {
                            sb.append("[no-diffuse]");
                        }
                        return;
                    }
                    Image img = t.getImage();
                    sb.append("[").append(t.getName()).append(" img=")
                      .append(img == null ? "null" : ("id" + img.getId())).append("]");
                }
            });
            log.info("BONUSDIAG", "type", _name, "at", _piece.x + "," + _piece.y,
                     "geoms", sb.toString());
        }
        @Override protected void controlRender (RenderManager rm, ViewPort vp) {}
        protected float _accum;
    }

    @Override // documentation inherited
    public Shadow getShadowType ()
    {
        return isHidden() ? Shadow.NONE : Shadow.DYNAMIC;
    }

    /**
     * Force the bonus sprite into it's default position.
     */
    public void resetLocation (BangBoard board)
    {
        moveSprite(board);
    }

    /**
     * Determines whether this bonus is hidden underground.
     */
    protected boolean isHidden ()
    {
        return BonusConfig.getConfig(_name).hidden;
    }

    @Override // documentation inherited
    protected String getHelpIdent (int pidx)
    {
        // strip off the town prefix when reporting our bonus type
        return "bonus_" + _name.substring(_name.lastIndexOf("/")+1);
    }

    @Override // documentation inherited
    protected void addProceduralActions ()
    {
        super.addProceduralActions();
        _procActions.put(GrantCardEffect.ACTIVATED_CARD,
            new ProceduralAction() {
            public float activate () {
                _spinner.setSpeed(FastMath.PI*5);
                startRiseFade(TILE_SIZE * 2, false, CARD_FLIGHT_DURATION);
                return CARD_FLIGHT_DURATION;
            }
        });
        ProceduralAction tactivate = new ProceduralAction() {
            public float activate () {
                float duration = setAction("activated");
                queueAction(REMOVED);
                return duration;
            }
        };
        _procActions.put(TrapEffect.ACTIVATED_TRAP, tactivate);
        _procActions.put(SnareEffect.ACTIVATED_SNARE, tactivate);
        _procActions.put(ReboundEffect.ACTIVATED_SPRING, tactivate);
    }

    @Override // documentation inherited
    protected float getRemoveDepth ()
    {
        // snares and springs are taller than dead units
        return TILE_SIZE;
    }

    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();

        // if the bonus emits particles while on the board, load those up
        // as well
        String effect = BonusConfig.getConfig(_name).particleEffect;
        if (effect != null && BangPrefs.isHighDetail()) {
            _ctx.loadParticles(effect, new ResultAttacher<Spatial>(this) {
                public void requestCompleted (Spatial result) {
                    super.requestCompleted(result);
                    result.setLocalTranslation(0, 0,
                        _piece.getHeight() * 0.5f * TILE_SIZE);
                }
            });
        }
    }

    @Override // documentation inherited
    protected void createSounds (SoundGroup sounds)
    {
        // preload any sound associated with activating this bonus
        String spath = "rsrc/bonuses/" + _name + "/activate.ogg";
        if (SoundUtil.haveSound(spath)) {
            sounds.preloadClip(spath);
        }
    }

    @Override // documentation inherited
    protected boolean animatedMove ()
    {
        return false;
    }

    /** The spinner that rotates the bonus. */
    protected Spinner _spinner;

    /** The time it takes for cards to fly up in the air. */
    protected static final float CARD_FLIGHT_DURATION = 0.25f;

    /** DEBUG: -Dbang.debug.bonusdiag logs each bonus's DiffuseMap/Image state (white-bonus probe). */
    protected static final boolean DEBUG_BONUS_DIAG = Boolean.getBoolean("bang.debug.bonusdiag");
}
