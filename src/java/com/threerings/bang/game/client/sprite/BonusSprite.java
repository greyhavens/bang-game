//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;
import com.jme.scene.Spatial;

import com.threerings.openal.SoundGroup;

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
            addController(_spinner = new Spinner(this, FastMath.PI/2));
        }
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
                    result.getLocalTranslation().set(0, 0,
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
}
