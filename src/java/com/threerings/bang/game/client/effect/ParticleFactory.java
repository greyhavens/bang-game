//
// $Id$

package com.threerings.bang.game.client.effect;

import java.util.ArrayList;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Controller;
import com.jme.scene.TriMesh;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;
import com.jmex.effects.ParticleManager;

import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

/**
 * Temporarily hijacked explosion factory.
 */
public class ParticleFactory
{
    public static void warmup (BangContext ctx)
    {
        DisplaySystem display = DisplaySystem.getDisplaySystem();
        _astate = display.getRenderer().createAlphaState();
        _astate.setBlendEnabled(true);
        _astate.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        _astate.setDstFunction(AlphaState.DB_ONE);
        _astate.setTestEnabled(true);
        _astate.setTestFunction(AlphaState.TF_GREATER);

        _tstate = RenderUtil.createTextureState(
            ctx, ctx.loadImage("textures/effects/flare.jpg"));

        _zstate = display.getRenderer().createZBufferState();
        _zstate.setEnabled(false);

        for (int i = 0; i < 3; i++) {
            createExplosion();
        }
        for (int i = 0; i < 5; i++) {
            createSmallExplosion();
        }
    }

    public static ParticleManager getExplosion ()
    {
        int count = 0, index = -1;
        for (int x = 0, tSize = explosions.size(); x < tSize; x++) {
            ParticleManager e = explosions.get(x);
            if (!e.isActive()) {
                return e;
            }
        }
        return createExplosion();
    }

    public static ParticleManager getSmallExplosion ()
    {
        int count = 0, index = -1;
        for (int x = 0, tSize = smallExplosions.size(); x < tSize; x++) {
            ParticleManager e = smallExplosions.get(x);
            if (!e.isActive()) {
                return e;
            }
        }
        return createSmallExplosion();
    }

    public static ParticleManager getGlow ()
    {
        int count = 0, index = -1;
        for (int x = 0, tSize = glows.size(); x < tSize; x++) {
            ParticleManager e = glows.get(x);
            if (!e.isActive()) {
                return e;
            }
        }
        ParticleManager glow = createGlow();
        glows.add(glow);
        return glow;
    }

    public static void cleanup ()
    {
        int count = 0;
        for (int x = 0, tSize = explosions.size(); x < tSize; x++) {
            ParticleManager e = explosions.get(x);
            if (!e.isActive()) {
                if (e.getParticles().getParent() != null)
                    e.getParticles().removeFromParent();
                count++;
                if (count > 5) {
                    explosions.remove(x);
                    tSize--;
                }
            }
        }

        int scount = 0;
        for (int x = 0, tSize = smallExplosions.size(); x < tSize; x++) {
            ParticleManager e = smallExplosions.get(x);
            if (!e.isActive()) {
                if (e.getParticles().getParent() != null)
                    e.getParticles().removeFromParent();
                scount++;
                if (scount > 5) {
                    smallExplosions.remove(x);
                    tSize--;
                }
            }
        }
    }

    protected static ParticleManager createExplosion ()
    {
        ParticleManager explosion = createExplosion(80, 0.4f, 2f, 5f);
        explosions.add(explosion);
        return explosion;
    }

    protected static ParticleManager createSmallExplosion ()
    {
        ParticleManager explosion = createExplosion(40, 0.7f, 4f, 8f);
        smallExplosions.add(explosion);
        return explosion;
    }

    protected static ParticleManager createExplosion (
        int pcount, float speed, float startSize, float endSize)
    {
        DisplaySystem display = DisplaySystem.getDisplaySystem();
        ParticleManager explosion = new ParticleManager(pcount);
        explosion.setGravityForce(new Vector3f(0.0f, 0.0f, 0.0f));
        explosion.setEmissionDirection(new Vector3f(0.0f, 1.0f, 0.0f));
        explosion.setEmissionMaximumAngle(3.1415927f);
        explosion.setSpeed(speed);
        explosion.setParticlesMinimumLifeTime(600.0f);
        explosion.setStartSize(startSize);
        explosion.setEndSize(endSize);
        explosion.setStartColor(new ColorRGBA(1.0f, 0.312f, 0.121f, 1.0f));
        explosion.setEndColor(
            new ColorRGBA(1.0f, 0.24313726f, 0.03137255f, 0.0f));
        explosion.setRandomMod(0.0f);
        explosion.setControlFlow(false);
        explosion.setInitialVelocity(0.02f);
        explosion.setParticleSpinSpeed(0.0f);
        explosion.setRepeatType(Controller.RT_CLAMP);

        explosion.warmUp(1000);
        TriMesh particles = explosion.getParticles();
        particles.addController(explosion);

        particles.setRenderState(_tstate);
        particles.setRenderState(_astate);
        particles.setRenderState(_zstate);
        particles.updateRenderState();

        return explosion;
    }

    protected static ParticleManager createGlow ()
    {
        DisplaySystem display = DisplaySystem.getDisplaySystem();
        ParticleManager manager = new ParticleManager(50);
        manager.setGravityForce(new Vector3f(0.0f, 0.0f, 0.0f));
        manager.setEmissionDirection(new Vector3f(0.0f, 1.0f, 0.0f));
        manager.setEmissionMaximumAngle(FastMath.TWO_PI);
        manager.setSpeed(0.1f);
        manager.setParticlesMinimumLifeTime(100.0f);
        manager.setStartSize(2.0f);
        manager.setEndSize(4.0f);
        manager.setStartColor(
            new ColorRGBA(0.45490196f, 0.8901961f, 0.41568628f, 1.0f));
        manager.setEndColor(new ColorRGBA(1.0f, 0.312f, 0.121f, 0.0f));
        manager.setRandomMod(1.0f);
        manager.setControlFlow(false);
        manager.setInitialVelocity(0.02f);
        manager.setParticleSpinSpeed(0.0f);
        manager.setRepeatType(Controller.RT_CLAMP);

        manager.warmUp(1000);
        TriMesh particles = manager.getParticles();
        particles.addController(manager);

        particles.setRenderState(_tstate);
        particles.setRenderState(_astate);
        particles.setRenderState(_zstate);
        particles.updateRenderState();

        return manager;
    }

    protected static ArrayList<ParticleManager> explosions =
        new ArrayList<ParticleManager>();
    protected static ArrayList<ParticleManager> smallExplosions =
        new ArrayList<ParticleManager>();
    protected static ArrayList<ParticleManager> glows =
        new ArrayList<ParticleManager>();
    protected static AlphaState _astate;
    protected static TextureState _tstate;
    protected static ZBufferState _zstate;
}
