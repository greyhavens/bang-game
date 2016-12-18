//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import com.jme.renderer.Renderer;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;

import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ArrayUtil;

import com.threerings.openal.Sound;
import com.threerings.util.MessageBundle;

import com.threerings.bang.game.client.effect.RepairViz;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.effect.RobotWaveEffect;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Handles the effects generated at the beginning and end of waves of robots.
 */
public class RobotWaveHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        super.execute();
        RobotWaveEffect reffect = (RobotWaveEffect)_effect;
        if (reffect.difficulty >= 0) {
            new StartMarquee(reffect.wave, reffect.difficulty).display();
        } else {
            new EndMarquee(reffect.wave, reffect.living,
                reffect.treeIds.length).display();
        }
        return true;
    }

    /**
     * The superclass of the marquees for the start and end of waves.
     */
    protected abstract class WaveMarquee extends BWindow
    {
        public WaveMarquee ()
        {
            super(_ctx.getStyleSheet(), null);
        }

        /**
         * Shows the marquee.
         */
        public void display ()
        {
            _ctx.getRootNode().addWindow(this);
            setBounds(0, 0, _ctx.getDisplay().getWidth(),
                _ctx.getDisplay().getHeight());

            final BLabel buzzsaw = new BLabel(new ImageIcon(
                _ctx.loadImage("ui/wave/buzzsaw.png")));
            add(buzzsaw);
            final Dimension bsize = buzzsaw.getPreferredSize(-1, -1);
            buzzsaw.setLocation(-bsize.width, (_height - bsize.height) / 2);
            buzzsaw.setSize(bsize.width, bsize.height);

            // the container scissors out everything not yet exposed by the
            // saw
            BContainer cont = new BContainer(
                GroupLayout.makeVert(GroupLayout.CENTER)) {
                protected void renderComponent (Renderer r) {
                    GL11.glEnable(GL11.GL_SCISSOR_TEST);
                    GL11.glScissor(0, 0, buzzsaw.getX() + bsize.width,
                        WaveMarquee.this._height);
                    try {
                        super.renderComponent(r);
                    } finally {
                        GL11.glDisable(GL11.GL_SCISSOR_TEST);
                    }
                }
            };
            initContent(cont);
            add(0, cont);
            Dimension csize = cont.getPreferredSize(-1, -1);
            cont.setLocation((_width - csize.width) / 2,
                (_height - csize.height) / 2);
            cont.setSize(csize.width, csize.height);

            // the controller flies the buzzsaw across the screen, revealing
            // the text, then pauses and fades the window out before removing
            // it
            final float lingerDuration = getLingerDuration();
            final int penderId = notePender();
            _ctx.getRootNode().addController(new Controller() {
                public void update (float time) {
                    if ((_elapsed += time) < BUZZSAW_FLIGHT_DURATION) {
                        buzzsaw.setLocation(-bsize.width +
                            (int)((_elapsed / BUZZSAW_FLIGHT_DURATION) *
                                (_width + bsize.width)),
                            buzzsaw.getY());

                    } else if (_elapsed < BUZZSAW_FLIGHT_DURATION +
                        lingerDuration) {
                        lingerUpdate(_elapsed - BUZZSAW_FLIGHT_DURATION);

                    } else if (_elapsed < BUZZSAW_FLIGHT_DURATION +
                        lingerDuration + FADE_DURATION) {
                        // perform one last update to make sure everything's
                        // added
                        lingerUpdate(lingerDuration);
                        setAlpha(1f - (_elapsed - BUZZSAW_FLIGHT_DURATION -
                            lingerDuration) / FADE_DURATION);

                    } else {
                        _ctx.getRootNode().removeWindow(WaveMarquee.this);
                        _ctx.getRootNode().removeController(this);
                        maybeComplete(penderId);
                    }
                }
                protected float _elapsed;
            });
        }

        @Override // documentation inherited
        public boolean isOverlay ()
        {
            return true;
        }

        @Override // documentation inherited
        public BComponent getHitComponent (int mx, int my)
        {
            return null;
        }

        /**
         * Populates the marquee's content container.
         */
        protected abstract void initContent (BContainer cont);

        /**
         * Returns the length of time to keep the status component on the
         * screen.
         */
        protected float getLingerDuration ()
        {
            return LINGER_DURATION;
        }

        /**
         * Called regularly during the linger phase.
         *
         * @param elapsed the amount of time elapsed since the start of the
         * phase
         */
        protected void lingerUpdate (float elapsed)
        {
        }
    }

    /**
     * Displays the wave number and difficulty level at the start of a
     * wave.
     */
    protected class StartMarquee extends WaveMarquee
    {
        public StartMarquee (int wave, int difficulty)
        {
            _wave = wave;
            _difficulty = difficulty;
        }

        // documentation inherited
        protected void initContent (BContainer cont)
        {
            cont.add(new BLabel(_ctx.xlate(GameCodes.GAME_MSGS,
                MessageBundle.tcompose("m.wave_title", _wave)),
                "marquee_title"));

            BContainer scont = new BContainer(
                GroupLayout.makeHoriz(GroupLayout.CENTER));
            ImageIcon sicon = new ImageIcon(
                _ctx.loadImage("ui/wave/difficulty_saw.png"));
            for (int ii = 0; ii <= _difficulty; ii++) {
                scont.add(new BLabel(sicon));
            }
            cont.add(scont);
        }

        protected int _wave, _difficulty;
    }

    /**
     * Displays the score and performance indicator at the end of a wave.
     */
    protected class EndMarquee extends WaveMarquee
    {
        public EndMarquee (int wave, int living, int total)
        {
            _wave = wave;
            _living = living;
            _total = total;
        }

        // documentation inherited
        protected void initContent (BContainer cont)
        {
            cont.add(new BLabel(_ctx.xlate(GameCodes.GAME_MSGS,
                MessageBundle.tcompose("m.wave_end", _wave)),
                "marquee_title"));

            _tcont = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER));
            ImageIcon gicon = new ImageIcon(
                _ctx.loadImage("ui/wave/icon_tree_grown.png")),
                sicon = new ImageIcon(
                _ctx.loadImage("ui/wave/icon_tree_stump.png"));
            for (int ii = 0; ii < _total; ii++) {
                BLabel label = new BLabel(ii < _living ? gicon : sicon);
                label.setAlpha(0f);
                _tcont.add(label);
            }
            cont.add(_tcont);

            RobotWaveEffect reffect = (RobotWaveEffect)_effect;
            cont.add(_plabel = new BLabel(_ctx.xlate(GameCodes.GAME_MSGS,
                "m.wave_perf" + reffect.getPerformance()),
                "marquee_subtitle"));
            _plabel.setAlpha(0f);

            // collect the sprites to count in random order (but with
            // living sprites at the beginning and dead ones at the end)
            ArrayUtil.shuffle(reffect.treeIds);
            for (int treeId : reffect.treeIds) {
                Piece piece = _bangobj.pieces.get(treeId);
                if (piece == null) {
                    continue;
                }
                PieceSprite sprite = _view.getPieceSprite(piece);
                if (sprite == null) {
                    continue;
                }
                _tsprites.add(piece.isAlive() ? 0 : _tsprites.size(), sprite);
            }

            // load the living and dead count sounds
            _lsound = _sounds.getSound(LIVING_SOUND);
            if (_living != _total) {
                _dsound = _sounds.getSound(DEAD_SOUND);
            }
        }

        @Override // documentation inherited
        protected float getLingerDuration ()
        {
            return super.getLingerDuration() + _total * TREE_DURATION;
        }

        @Override // documentation inherited
        protected void lingerUpdate (float elapsed)
        {
            int nidx = (int)(elapsed / TREE_DURATION);
            while (_tidx < nidx) {
                if (++_tidx < _total) {
                    _tcont.getComponent(_tidx).setAlpha(1f);
                    if (_tidx < _tsprites.size()) {
                        // count the sprite and make it invisible.  if it is
                        // not removed, it will be made visible again on reset
                        PieceSprite sprite = _tsprites.get(_tidx);
                        sprite.setCullMode(Spatial.CULL_ALWAYS);
                        sprite.getHighlight().setCullMode(Spatial.CULL_ALWAYS);
                        queueEffect(sprite, sprite.getPiece(),
                            new RepairViz());
                    }
                    (_tidx < _living ? _lsound : _dsound).play(true);

                } else if (_tidx == _total) {
                    _plabel.setAlpha(1f);
                    if (_living == _total) { // perfect score
                        _sounds.getSound(PERFECT_SOUND).play(true);
                    }
                }
            }
        }

        protected int _wave, _living, _total, _tidx = -1;
        protected BContainer _tcont;
        protected BLabel _plabel;
        protected ArrayList<PieceSprite> _tsprites =
            new ArrayList<PieceSprite>();
        protected Sound _lsound, _dsound;
    }

    /** The time it takes the buzzsaw to fly across the screen, revealing the
     * marquee. */
    protected static final float BUZZSAW_FLIGHT_DURATION = 1f;

    /** The time that the wave start marquee lingers on the screen. */
    protected static final float LINGER_DURATION = 1f;

    /** The time it takes to count one tree (or display the performance). */
    protected static final float TREE_DURATION = 0.5f;

    /** The time it takes the marquee to fade out. */
    protected static final float FADE_DURATION = 0.5f;

    /** Played once for each living tree counted. */
    protected static final String LIVING_SOUND =
        "rsrc/bonuses/indian_post/totem_crown/pickedup.ogg";

    /** Played once for each dead tree counted. */
    protected static final String DEAD_SOUND =
        "rsrc/effects/indian_post/totem/pickedup.ogg";

    /** Played when the performance is announced to be perfect. */
    protected static final String PERFECT_SOUND =
        "rsrc/bonuses/indian_post/totem_crown/added.ogg";
}
