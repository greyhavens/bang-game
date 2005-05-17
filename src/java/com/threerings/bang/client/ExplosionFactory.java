//
// $Id$

package com.threerings.bang.client;

import java.net.URL;
import java.util.ArrayList;

import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Controller;
import com.jme.scene.TriMesh;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;
import com.jme.util.TextureManager;
import com.jmex.effects.ParticleManager;

import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

/**
 * Temporarily hijacked explosion factory.
 */
public class ExplosionFactory
{
    private static ArrayList explosions = new ArrayList();
    private static ArrayList smallExplosions = new ArrayList();
    private static AlphaState as;
    private static TextureState ts;
    private static ZBufferState zstate;

    public static void cleanExplosions() {
        int count = 0;
        for (int x = 0, tSize = explosions.size(); x < tSize; x++) {
            ParticleManager e = (ParticleManager)explosions.get(x);
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
            ParticleManager e = (ParticleManager)smallExplosions.get(x);
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

    public static ParticleManager getExplosion() {
        int count = 0, index = -1;
        for (int x = 0, tSize = explosions.size(); x < tSize; x++) {
            ParticleManager e = (ParticleManager)explosions.get(x);
            if (!e.isActive()) {
                return e;
            }
        }
        return createExplosion();
    }
    
    private static ParticleManager createExplosion() {
        DisplaySystem display = DisplaySystem.getDisplaySystem();        
        ParticleManager explosion = new ParticleManager(80, display.getRenderer().getCamera());
        explosion.setGravityForce(new Vector3f(0.0f, 0.0f, 0.0f));
        explosion.setEmissionDirection(new Vector3f(0.0f, 1.0f, 0.0f));
        explosion.setEmissionMaximumAngle(3.1415927f);
        explosion.setSpeed(0.4f);
        explosion.setParticlesMinimumLifeTime(600.0f);
        explosion.setStartSize(2.0f);
        explosion.setEndSize(5.0f);
        explosion.setStartColor(new ColorRGBA(1.0f, 0.312f, 0.121f, 1.0f));
        explosion.setEndColor(new ColorRGBA(1.0f, 0.24313726f, 0.03137255f, 0.0f));
        explosion.setRandomMod(0.0f);
        explosion.setControlFlow(false);
        explosion.setInitialVelocity(0.02f);
        explosion.setParticleSpinSpeed(0.0f);
        explosion.setRepeatType(Controller.RT_CLAMP);

        explosion.warmUp(1000);
        TriMesh particles = explosion.getParticles(); 
        particles.addController(explosion);

        particles.setRenderState(ts);
        particles.setRenderState(as);
        particles.setRenderState(zstate);
        particles.updateRenderState();
        
        explosions.add(explosion);
        
        return explosion;
    }

    public static ParticleManager getSmallExplosion() {
        int count = 0, index = -1;
        for (int x = 0, tSize = smallExplosions.size(); x < tSize; x++) {
            ParticleManager e = (ParticleManager)explosions.get(x);
            if (!e.isActive()) {
                return e;
            }
        }
        return createSmallExplosion();
    }
    
    private static ParticleManager createSmallExplosion() {
        DisplaySystem display = DisplaySystem.getDisplaySystem();        
        ParticleManager explosion = new ParticleManager(40, display.getRenderer().getCamera());
        explosion.setGravityForce(new Vector3f(0.0f, 0.0f, 0.0f));
        explosion.setEmissionDirection(new Vector3f(0.0f, 1.0f, 0.0f));
        explosion.setEmissionMaximumAngle(3.1415927f);
        explosion.setSpeed(0.7f);
        explosion.setParticlesMinimumLifeTime(600.0f);
        explosion.setStartSize(4.0f);
        explosion.setEndSize(8.0f);
        explosion.setStartColor(new ColorRGBA(1.0f, 0.312f, 0.121f, 1.0f));
        explosion.setEndColor(new ColorRGBA(1.0f, 0.24313726f, 0.03137255f, 0.0f));
        explosion.setRandomMod(0.0f);
        explosion.setControlFlow(false);
        explosion.setInitialVelocity(0.02f);
        explosion.setParticleSpinSpeed(0.0f);
        explosion.setRepeatType(Controller.RT_CLAMP);

        explosion.warmUp(1000);
        TriMesh particles = explosion.getParticles(); 
        particles.addController(explosion);

        particles.setRenderState(ts);
        particles.setRenderState(as);
        particles.setRenderState(zstate);
        particles.updateRenderState();
        
        explosions.add(explosion);
        
        return explosion;
    }

    public static void warmup(BangContext ctx) {
        DisplaySystem display = DisplaySystem.getDisplaySystem();
        as = display.getRenderer().createAlphaState();
        as.setBlendEnabled(true);
        as.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        as.setDstFunction(AlphaState.DB_ONE);
        as.setTestEnabled(true);
        as.setTestFunction(AlphaState.TF_GREATER);

        ts = RenderUtil.createTexture(ctx, ctx.loadImage("media/textures/flare.jpg"));

        zstate = display.getRenderer().createZBufferState();
        zstate.setEnabled(false);

        for (int i = 0; i < 3; i++)
            createExplosion();
        for (int i = 0; i < 5; i++)
            createSmallExplosion();
    }

}
