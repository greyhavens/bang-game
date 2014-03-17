//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.input.Cursor;

import com.jme.bounding.BoundingBox;
import com.jme.light.DirectionalLight;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;

import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BCursor;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.InputEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.RandomUtil;

import com.threerings.util.MessageBundle;
import com.threerings.util.StreamablePoint;

import com.threerings.media.util.AStarPathUtil;

import com.threerings.jme.camera.CameraPath;
import com.threerings.jme.camera.SwingPath;
import com.threerings.jme.effect.FadeInOutEffect;
import com.threerings.jme.model.Model;
import com.threerings.jme.sprite.Sprite;
import com.threerings.jme.util.ImageCache;
import com.threerings.jme.util.LinearTimeFunction;

import com.threerings.openal.Sound;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.client.BuckleView;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.Config;
import com.threerings.bang.client.bui.WindowFader;
import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.ParticleUtil;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.effect.IconViz;
import com.threerings.bang.game.client.sprite.ActiveSprite;
import com.threerings.bang.game.client.sprite.BonusSprite;
import com.threerings.bang.game.client.sprite.Bouncer;
import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.Spinner;
import com.threerings.bang.game.client.sprite.Targetable;
import com.threerings.bang.game.client.sprite.UnitSprite;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;
import com.threerings.bang.data.Handle;

/**
 * Displays the main game board.
 */
public class BangBoardView extends BoardView
    implements MouseListener
{
    public BangBoardView (BangContext ctx, BangController ctrl)
    {
        super(ctx, false);
        _ctx = ctx;
        _ctrl = ctrl;

        // this is used to show a unit's attack range
        _rngstate = RenderUtil.createTextureState(
            ctx, "textures/ustatus/crosshairs_shotrange.png");

        addListener(this);
    }

    /**
     * Enables or disables user interaction. This is used by the tutorial to disable user
     * interaction when we don't want the player messing with things.
     */
    public void setInteractive (boolean interactive)
    {
        if (interactive) {
            // don't misbehave if we're already interactive
            if (!isInteractive()) {
                addListener(this);
            }
        } else {
            removeListener(this);
        }
    }

    /**
     * Returns true if the board view is currently interactive.
     */
    public boolean isInteractive ()
    {
        return _listeners.contains(this);
    }

    /**
     * Requests that the specified card be enabled for placement. The area
     * of effect of the card will be rendered around the cursor and a left
     * click will activate the card at the specified coordinates while a
     * right click will cancel the placement.
     */
    public void placeCard (Card card)
    {
        // clear any current selection
        clearSelection();

        // set up the display of our card attack set
        _card = card;
        updatePlacingCard(_mouse.x, _mouse.y);
        createCardCursor(card);
        _ctx.getChatDirector().displayFeedback(
            "cards", "m.placement_" + _card.getPlacementMode().name());
        log.info("Placing " + _card);
    }

    /**
     * Returns the currently active card.
     */
    public Card getCard ()
    {
        return _card;
    }

    /**
     * Clears out the card currently being placed.
     */
    public boolean clearPlacingCard ()
    {
        resetCursor();
        if (_card == null) {
            return false;
        }

        log.info("Clearing " + _card);
        _card = null;
        clearAttackSet();
        return true;
    }

    /**
     * Called by the controller when we know one way or another regarding a
     * move and shoot request.
     */
    public void clearPendingShot (int targetId)
    {
        Piece piece = _bangobj.pieces.get(targetId);
        if (piece != null) {
            Targetable t = getTargetableSprite(piece);
            if (t != null) {
                t.setPendingShot(false);
            }
        }
    }

    /**
     * Swings around the board and calls back to the controller to let it know
     * that everything is ready for the selection phase to start.
     */
    public void doPreSelectBoardTour ()
    {
        _haveDoneTour = true;
        doBoardTour(null);
    }

    /**
     * Fades in a "round over" marquee, fades out the board, sets up the fade
     * in for the new board and calls back to the controller to let it know
     * that everything is ready for the next round to start.
     */
    public void doInterRoundMarqueeFade ()
    {
        if (noActions()) {
            showInterRoundMarquee();
        } else {
            _pendingMarquee = MarqueeMode.ROUND;
        }
    }

    protected void showInterRoundMarquee ()
    {
        createMarquee(_ctx.xlate(GameCodes.GAME_MSGS, "m.round_over"));
        _marquee.setAlpha(0f);

        // fade in the marquee
        new ComponentFader(_marquee, new LinearTimeFunction(0f, 1f, 2f)) {
            public void fadeComplete () {
                // clear the marquee and let the controller know to show the stats page
                clearMarquee(0f);
                _ctrl.interRoundMarqueeFadeComplete();
            }
        }.start();
    }

    /**
     * Fades out the the board and sets up the fade in for the new board.
     * Calls the controller to let it know that everything's ready for
     * the next round to start.
     */
    public void doInterRoundBoardFade ()
    {
        // ...and fade the whole screen to black
        _ctx.getInterface().attachChild(
            new FadeInOutEffect(
                ColorRGBA.black, 0f, 1f, 2f, true) {
                protected void fadeComplete () {
                    super.fadeComplete();
                    // and prepare to fade the new board in
                    createPausedFadeIn();
                    _ctx.getInterface().attachChild(_fadein);
                    // let the controller start up the next phase
                    _ctrl.interRoundFadeComplete();
                }
            });

    }

    /**
     * Fades in a "Game Over" marqee, then notifies the controller that it's OK
     * to drop in the post-game stats display.
     */
    public void doPostGameMarqueeFade ()
    {
        if (noActions()) {
            showPostGameMarquee();
        } else {
            _pendingMarquee = MarqueeMode.GAME;
        }
    }

    protected void showPostGameMarquee ()
    {
        createMarquee(_ctx.xlate(GameCodes.GAME_MSGS, "m.game_over"));
        _marquee.setAlpha(0f);

        // fade in the marquee
        new ComponentFader(_marquee, new LinearTimeFunction(0f, 1f, 3f)) {
            public void fadeComplete () {
                clearMarquee(0f);
                _ctrl.postGameFadeComplete();
            }
        }.start();
    }

    /**
     * Fades a marquee in and out with the specified fade in and out time.
     */
    public void fadeMarqueeInOut (String message, final float halftime)
    {
        createMarquee(_ctx.xlate(GameCodes.GAME_MSGS, message));
        _marquee.setAlpha(0f);

        new ComponentFader(_marquee, new LinearTimeFunction(0f, 1f, halftime)) {
            public void fadeComplete () {
                clearMarquee(halftime);
            }
        }.start();
    }

    /**
     * Returns true if the specified piece is OK to be selected, false if it is
     * currently animating or has a pending animation.
     */
    public boolean isSelectable (Piece piece)
    {
        if (_card != null) {
            return _card.isValidPiece(_bangobj, piece);
        }
        PieceSprite psprite;
        return (piece instanceof Unit) && (piece.owner == _pidx) &&
            piece.isAlive() && (_pendmap.get(piece.pieceId) <= 0) &&
            ((psprite = getPieceSprite(piece)) != null) && !psprite.isMoving();
    }

    /**
     * Notes that the specified piece will be moving when its action is up for
     * exection.
     */
    public void notePendingMove (int pieceId)
    {
//         Piece p = (Piece)_bangobj.pieces.get(pieceId);
//         if (p != null && p.owner == _pidx) {
//             log.info("Noting pending " + p);
//         }
        if (ACTION_DEBUG) {
            log.info("Note Pending Move", "pieceId", pieceId);
        }
        _pendmap.increment(pieceId, 1);
    }

    /**
     * Notes that the specified piece no longer has a pending move action.
     */
    public void clearPendingMove (int pieceId)
    {
//         Piece p = (Piece)_bangobj.pieces.get(pieceId);
//         if (p != null && p.owner == _pidx) {
//             log.info("Clearing pending " + p);
//         }
        if (ACTION_DEBUG) {
            log.info("Clear Pending Move", "pieceId", pieceId);
        }
        _pendmap.increment(pieceId, -1);
    }

    // from interface MouseListener
    public void mouseClicked (MouseEvent e)
    {
        // nothing doing, we handle this ourselves
    }

    // from interface MouseListener
    public void mousePressed (MouseEvent e)
    {
        // skip to the end of the board tour path, if active
        if (_tpath != null) {
            _ctx.getCameraHandler().skipPath();
        }

        // keep track of our shift down state
        _shiftDown = (e.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0;

        // update our mouse coordinates
        updateHoverState(e);

        switch (_downButton = e.getButton()) {
        case MouseEvent.BUTTON2:
            handleRightPress(e.getX(), e.getY());
            break;

        case MouseEvent.BUTTON1:
            handleLeftPress(e.getX(), e.getY());
            break;
        }
    }

    // from interface MouseListener
    public void mouseReleased (MouseEvent e)
    {
        if (_downButton == MouseEvent.BUTTON3) {
            clearAttackSet();
        }
        _downButton = -1;
    }

    // from interface MouseListener
    public void mouseEntered (MouseEvent e)
    {
        // nada
    }

    // from interface MouseListener
    public void mouseExited (MouseEvent e)
    {
        // nada
    }

    @Override // documentation inherited
    public void prepareForRound (BangObject bangobj, BangConfig cfg, int pidx)
    {
        // we'll need this in createMarquee() which is called by super
        _bconfig = cfg;

        super.prepareForRound(bangobj, cfg, pidx);

        _pidx = pidx;
        _bangobj.addListener(_ticker);

        // Make sure our lights are setup properly
        _highNoon = false;
        _wendigoAmbiance = false;
        refreshLights();

        // preload our sounds for this scenario
        for (String clip : bangobj.scenario.getPreLoadClips()) {
            _sounds.preloadClip(clip);
        }

        // start with the camera controls disabled; the controller will
        // reenable them when we are completely ready to play (starting units
        // moved into place and everything)
        _ctx.getInputHandler().setEnabled(false);
    }

    /**
     * Called by the {@link BangView} when the actual round has started. When
     * we're done resolving sprites, we'll report back to the controller that
     * we're ready (in {@link #reallyStartRound}).
     */
    public void startRound ()
    {
        // if we haven't toured the board yet (we showed up in the middle of a
        // game, we need to do it now to properly position the camera)
        if (!_haveDoneTour) {
            _haveDoneTour = true;
            doBoardTour(new Runnable() {
                public void run () {
                    startRound();
                }
            });
            return;
        }

        // drop rendering to 1 FPS while we load the models
        final long start = System.currentTimeMillis();
        // final int oldFPS = _ctx.getApp().setTargetFPS(1);

        // TODO: disable renderer?
        // _ctx.getApp().setEnabled(true, false);

        // when the round starts a bunch of piece creation notifications come
        // in which are thrown onto the event queue, and we need to postpone
        // actually starting the round until those have been processed
        _ctx.getApp().postRunnable(new Runnable() {
            public void run () {
                // create a loading marquee to report unit loading progress
                if (_toLoad > 0) {
                    updateLoadingMarquee();
                }

                // wait for the unit models to resolve, then start up
                addResolutionObserver(new ResolutionObserver() {
                    public void mediaResolved () {
                        reallyStartRound(start);
                    }
                });
            }
        });
    }

    @Override // documentation inherited
    public void endRound ()
    {
        super.endRound();

        clearSelection();
        clearPlacingCard();
        clearHighlights();

        // remove our event listener
        _bangobj.removeListener(_ticker);

        // clear out advance orders
        for (AdvanceOrder order : _orders.values()) {
            order.clear();
        }
        _orders.clear();

        // reset our have done touredness
        _haveDoneTour = false;
    }

    @Override // documentation inherited
    public void clearResolving (Object resolved)
    {
        // note units that are resolved prior to the first tick so that we can
        // run them to their starting position when we're ready
        if (!_bangobj.isInteractivePlay() && (resolved instanceof UnitSprite) &&
            !((UnitSprite)resolved).movingToStart) {
            ((UnitSprite)resolved).movingToStart = true;
            _readyUnits.add((UnitSprite)resolved);
        }
        super.clearResolving(resolved);
    }

    @Override // documentation inherited
    public boolean isHoverable (Sprite sprite)
    {
        if (!super.isHoverable(sprite)) {
            return false;
        }
        if (sprite instanceof PieceSprite) {
            return ((PieceSprite)sprite).isHoverable();
        }
        return false;
    }

    @Override // documentation inherited
    public boolean hasTooltip (Sprite sprite)
    {
        return (sprite instanceof PieceSprite &&
            ((PieceSprite)sprite).hasTooltip());
    }

    /**
     * Fires off a particle effect on top of the camera (for bursts of rain and
     * such).
     *
     * @param duration the duration of the effect in seconds
     */
    public void displayCameraParticles (String effect, final float duration)
    {
        // create a node that tracks the position of the camera and removes
        // itself after the duration is up and the particle system has
        // exhausted itself
        Node cnode = new Node("camera") {
            public void updateWorldData (float time) {
                getLocalTranslation().set(
                    _ctx.getCameraHandler().getCamera().getLocation());
                super.updateWorldData(time);
                if ((_accum += time) >= duration &&
                    getControllers().isEmpty()) {
                    ParticleUtil.stopAndRemove(this);
                }
            }
            protected float _accum;
        };
        _node.attachChild(cnode);

        // attach an instance of the effect to the node
        _ctx.loadParticles(effect, new ResultAttacher<Spatial>(cnode) {
            public void requestCompleted (Spatial result) {
                super.requestCompleted(result);
                addWindInfluence(result);
            }
        });
    }

    /**
     * Called when an advance order is reported as invalid by the server.
     */
    public void orderInvalidated (int unitId, int targetId, boolean alert)
    {
        // clear their advance order icon
        clearAdvanceOrder(unitId);

        // clear any pending shot indicator
        if (targetId != -1) {
            clearPendingShot(targetId);
        }

        // stop here if this is not an alert-worth cancelation
        if (!alert) {
            return;
        }

        // give some auditory feedback
        if (_sounds != null) {
            _sounds.getSound(ORDER_INVALIDATED).play(true);
        }

        // show a question mark over the unit
        Unit unit = (Unit)_bangobj.pieces.get(unitId);
        if (unit != null) {
            PieceSprite sprite = getPieceSprite(unit);
            if (sprite != null) {
                IconViz iviz = new IconViz("textures/effects/invalidated.png");
                iviz.init(_ctx, this, sprite, null);
                iviz.display();
            }
        }
    }

    /**
     * Checks whether the board is in "high noon" mode.
     */
    public boolean isHighNoon ()
    {
        return _highNoon;
    }

    /**
     * Fades into or out of "high noon" mode.
     */
    public void setHighNoon (final boolean enable)
    {
        _highNoon = enable;

        // fade to white before changing lights
        _ctx.getInterface().attachChild(new FadeInOutEffect(
            ColorRGBA.white, 0f, 1f, NOON_FADE_DURATION, false) {
            protected void fadeComplete () {
                super.fadeComplete();
                continueSettingHighNoon();
            }
        });
    }

    /**
     * Fades in a scalar change to the primary diffuse light.
     */
    public void setWendigoAmbiance (final float duration, final boolean enable)
    {
        // store the values before updating
        final DirectionalLight[] olights = copyLights();

        _wendigoAmbiance = enable;
        refreshLights();

        // fade in/out the music and
        if (enable) {
            _ctx.getBangClient().fadeOutMusic(duration);
            _sounds.getSound(WENDIGO_AMBIANCE_START).play(true);
            if (_wendigoLoop == null) {
                _wendigoLoop = _sounds.getSound(WENDIGO_AMBIANCE_LOOP);
            }
            _wendigoLoop.loop(true);

        } else {
            if (_wendigoLoop != null) {
                _wendigoLoop.stop();
            }
            _ctrl.startScenarioMusic(duration);
        }

        // store the new values and transition to them
        final DirectionalLight[] nlights = copyLights();
        _node.addController(new Controller() {
            public void update (float time) {
                _elapsed = Math.min(_elapsed + time, duration);
                float alpha = _elapsed / duration;
                interpolateLights(olights, nlights, alpha);
                if (_elapsed >= duration) {
                    _node.removeController(this);
                }
            }
            protected float _elapsed;
        });
    }

    /**
     * Places bouncing pointer geometry over the piece in question.
     */
    public void activatePointer (Piece piece)
    {
        PieceSprite psp = getPieceSprite(piece);
        if (psp != null) {
            _pointer.setLocalTranslation(new Vector3f(0, 0, psp.getHeight()));
            psp.attachChild(_pointer);
        } else {
            log.info("Missing sprite for pointing", "piece", piece);
        }
    }

    /**
     * Clears any active highlight geometry.
     */
    public void clearPointer ()
    {
        if (_pointer.removeFromParent()) {
            log.info("Cleared pointer.");
        }
    }

    @Override // documentation inherited
    public float getShadowIntensity ()
    {
        return _highNoon ? 0f : super.getShadowIntensity();
    }

    /**
     * Returns a set of pieceIds for pieces with pending move actions.
     */
    public ArrayList<EffectHandler> getPendingMovers ()
    {
        return _pmoves;
    }

    @Override // documentation inherited
    public void addSprite (Sprite sprite)
    {
        if (sprite instanceof UnitSprite) {
            ((UnitSprite)sprite).setPidx(_pidx);
        }
        super.addSprite(sprite);
    }

    @Override // documentation inherited
    public void removeSprite (Sprite sprite)
    {
        if (sprite instanceof MobileSprite) {
            ((MobileSprite)sprite).checkMoveSound("removing sprite");
        }
        super.removeSprite(sprite);
    }

    @Override // documentation inherited
    public void refreshBoard ()
    {
        super.refreshBoard();
        if (!_bangobj.scenario.showDetailedMarquee()) {
            return;
        }

        MessageBundle msgs = _ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);

        // recreate the title marquee so that it is positioned properly
        clearMarquee(0f);
        if (_bangobj.marquee != null) {
            addMarquee(_marquee = createMarqueeLabel(msgs.xlate(_bangobj.marquee)),
                    _ctx.getRenderer().getWidth()/2, _ctx.getRenderer().getHeight()/2 - 90);
        }

        _pmarquees = new BWindow(_ctx.getStyleSheet(), new AbsoluteLayout()) {
            public boolean isOverlay () {
                return true;
            }
            public BComponent getHitComponent (int mx, int my) {
               return null;
            }
        };
        _pmarquees.setPreferredSize(BangUI.MIN_WIDTH, BangUI.MIN_HEIGHT);

        // we want to add more spacing between teams (when there are teams)
        int pcount = _bangobj.players.length, tcount = 1;
        boolean[] added = new boolean[pcount];
        boolean coop = true;
        for (int ii = 1; ii < _bangobj.teams.length; ii++) {
            tcount++;
            if (_bangobj.teams[ii-1] != _bangobj.teams[ii]) {
                coop = false;
            }
            for (int jj = 0; jj < ii; jj++) {
                if (_bangobj.teams[ii] == _bangobj.teams[jj]) {
                    tcount--;
                    break;
                }
            }
        }
        if (coop) {
            tcount = pcount;
        }
        BContainer players = new BContainer(
                GroupLayout.makeHoriz(GroupLayout.CENTER).setGap((pcount - tcount) * 10));
        BContainer gangs = new BContainer(
                GroupLayout.makeHoriz(GroupLayout.CENTER).setGap(2));

        // add the player avatars and gang buckles
        for (int ii = 0; ii < _bangobj.players.length; ii++) {
            if (coop) {
                players.add(createPlayerMarquee(ii));
                if (ii > 0) {
                    gangs.add(new BLabel(msgs.get("m.and"), "marquee_vs"));
                }
                gangs.add(createGangMarquee(ii, colorLookup[ii + 1], false));
            } else if (!added[_bangobj.teams[ii]]) {
                players.add(createTeamMarquee(_bangobj.teams[ii]));
                if (ii > 0) {
                    gangs.add(new BLabel(msgs.get("m.vs"), "marquee_vs"));
                }
                gangs.add(createTeamGangMarquee(_bangobj.teams[ii]));
                added[_bangobj.teams[ii]] = true;
            }
        }
        _pmarquees.add(players, new Rectangle(0, 368, BangUI.MIN_WIDTH, 350));
        _pmarquees.add(gangs, new Rectangle(0, 50, BangUI.MIN_WIDTH, 180));

        // add some additional information in the center colum
        String msg = MessageBundle.compose(
            "m.marquee_header", MessageBundle.taint(String.valueOf(_bangobj.roundId + 1)),
            MessageBundle.taint(String.valueOf(_bangobj.perRoundRanks.length)),
            _bangobj.scenario.getName());
        _pmarquees.add(new BLabel(msgs.xlate(msg), "marquee_subtitle"),
                new Rectangle(0, 340, BangUI.MIN_WIDTH, 30));
        msg = _bconfig.rated ? (_bconfig.grantAces ? "m.gang" : "m.ranked") : "m.unranked";
        _pmarquees.add(new BLabel(msgs.get(msg), "marquee_subtitle"),
                new Rectangle(0, 224, BangUI.MIN_WIDTH, 30));

        // add the marquee window
        int x = (_ctx.getDisplay().getWidth() - BangUI.MIN_WIDTH) / 2,
            y = (_ctx.getDisplay().getHeight() - BangUI.MIN_HEIGHT) / 2;
        _pmarquees.setBounds(x, y, BangUI.MIN_WIDTH, BangUI.MIN_HEIGHT);
        _ctx.getRootNode().addWindow(_pmarquees);
    }

    /**
     * Continues switching into or out of "high noon" mode after the screen has
     * faded to white.
     */
    protected void continueSettingHighNoon ()
    {
        // switch terrain shadows off for high noon
        MaterialState mstate = (MaterialState)_tnode.getRenderState(
            RenderState.RS_MATERIAL);
        mstate.setColorMaterial(_highNoon ?
            MaterialState.CM_NONE : MaterialState.CM_DIFFUSE);

        // switch to high noon lighting or restore the original
        refreshLights();
        for (PieceSprite sprite : _pieces.values()) {
            sprite.updateShadowValue();
        }

        // fade back in
        _ctx.getInterface().attachChild(new FadeInOutEffect(
            ColorRGBA.white, 1f, 0f, NOON_FADE_DURATION, false));
    }

    @Override // documentation inherited
    protected void refreshLights ()
    {
        super.refreshLights();
        if (_highNoon) {
            _lights[0].getDirection().set(-0.501f, 0.213f, -0.839f);
            _lights[0].getDiffuse().set(1f, 1f, 0.8f, 1f);
            _lights[0].getAmbient().set(0.16f, 0.16f, 0.05f, 1f);

            _lights[1].getDirection().set(0.9994f, 0f, 0.035f);
            _lights[1].getDiffuse().set(1f, 0.8f, 0.2f, 1f);
            _lights[1].getAmbient().set(0.06f, 0.15f, 0.18f, 1f);
        }
        if (_wendigoAmbiance) {
            // Calculate the darkened diffuse color by converting to YUV,
            // altering the luminance to 0.3, then converting back, this should
            // preserve the color
            ColorRGBA diffuseYUV = RGBtoYUV(_lights[0].getDiffuse());
            diffuseYUV.r = 0.3f;
            _lights[0].getDiffuse().set(YUVtoRGB(diffuseYUV));
            _lights[1].getDiffuse().set(COLOR_CYAN);
            _lights[1].getDirection().set(0f, 0f, 1f);
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // create our cursor
        _cursor = new Node("cursor");
        _ctx.loadModel("bonuses", "frontier_town/bonus_point",
                       new ResultAttacher<Model>(_cursor));
        _cursor.addController(new Spinner(_cursor, FastMath.PI));
        _cursor.addController(new Bouncer(_cursor, TILE_SIZE, TILE_SIZE/4));
        _cursor.setRenderState(RenderUtil.lequalZBuf);
        _cursor.setIsCollidable(false);
        _cursor.updateRenderState();

        // create our pointing arrow
        _pointer = new Node("pointer");
        _ctx.loadModel("extras", "frontier_town/arrow",
                       new ResultAttacher<Model>(_pointer));
        _pointer.addController(new Spinner(_pointer, FastMath.PI));
        _pointer.addController(new Bouncer(_pointer, TILE_SIZE, TILE_SIZE/4));
        _pointer.setRenderState(RenderUtil.lequalZBuf);
        _pointer.setIsCollidable(false);
        _pointer.updateRenderState();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // stop the board tour if it's still going
        if (_tpath != null) {
            _ctx.getCameraHandler().moveCamera(null);
        }

        // make sure our mouse is appropriate
        resetCursor();
    }

    @Override // documentation inherited
    protected boolean shouldShowSky ()
    {
        return false;
    }

    /**
     * Creates and returns a player view for the opening marquee.
     */
    protected BContainer createPlayerMarquee (int pidx)
    {
        // create the marquee
        BContainer marquee = new BContainer(GroupLayout.makeVert(GroupLayout.CENTER).setGap(-1));
        marquee.setStyleClass("team_marquee_cont");
        if (_bangobj.playerInfo[pidx].avatar != null) {
            AvatarView aview = new AvatarView(_ctx, 0.5f*0.85f);
            aview.setAvatar(_bangobj.playerInfo[pidx].avatar);
            marquee.add(aview);
        }
        marquee.add(new BLabel(_bangobj.players[pidx].toString(),
                    "team_marquee_scroll" + colorLookup[pidx + 1]));
        return marquee;
    }

    /**
     * Creates and resturns a team view for the opening marquee.
     */
    protected BContainer createTeamMarquee (int tidx)
    {
        int color = 0;
        int pcount = _bangobj.players.length;
        for (int ii = 0; ii < pcount; ii++) {
            if (_bangobj.teams[ii] == tidx) {
                color = Math.max(color, colorLookup[ii + 1]);
            }
        }
        BContainer marquee = new BContainer(GroupLayout.makeVert(GroupLayout.CENTER).setGap(-1));
        marquee.setStyleClass("team_marquee_cont");
        BContainer avatars = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER).setGap(0));
        BContainer names = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER).setGap(10));
        names.setStyleClass("team_marquee_scroll" + color);
        for (int ii = 0; ii < pcount; ii++) {
            if (_bangobj.teams[ii] == tidx) {
                AvatarView aview = new AvatarView(_ctx, 0.5f*0.85f);
                aview.setAvatar(_bangobj.playerInfo[ii].avatar);
                avatars.add(aview);
                BLabel name = new BLabel(_bangobj.players[ii].toString(), "team_marquee_label");
                name.setFit(BLabel.Fit.SCALE);
                name.setPreferredSize(226, 27);
                names.add(name);
            }
        }
        marquee.add(avatars);
        marquee.add(names);
        return marquee;
    }

    /**
     * Creates and returns a team gang view for the opening marquee.
     */
    protected BContainer createTeamGangMarquee (int tidx)
    {
        int color = 0;
        int pidx = 0;
        boolean mult = true;
        Handle gang = null;
        // if we're all on the same gang, just show the one buckle
        for (int ii = 0; ii < _bangobj.teams.length; ii++) {
            if (_bangobj.teams[ii] == tidx) {
                // let bounties combine team gangs always
                if (_bangobj.bounty == null) {
                    if (gang == null) {
                        gang = _bangobj.playerInfo[ii].gang;
                    } else if (!gang.equals(_bangobj.playerInfo[ii].gang)) {
                        mult = false;
                        break;
                    }
                }
                if (colorLookup[ii + 1] > color) {
                    color = colorLookup[ii + 1];
                    pidx = ii;
                }
            }
        }
        // in a bounty, the player's buckle is always their team's buckle
        if (_bangobj.bounty != null && _bangobj.teams[0] == _bangobj.teams[pidx]) {
            pidx = 0;
        }
        if (mult) {
            return createGangMarquee(pidx, color, true);
        }
        BContainer cont = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER));
        for (int ii = 0; ii < _bangobj.teams.length; ii++) {
            if (_bangobj.teams[ii] == tidx) {
                cont.add(createGangMarquee(ii, colorLookup[ii + 1], false));
            }
        }
        return cont;
    }

    /**
     * Creates and returns a gang view for the opening marquee.
     */
    protected BContainer createGangMarquee (int pidx, int color, boolean mult)
    {
        BContainer marquee = new BContainer(GroupLayout.makeVert(GroupLayout.CENTER));
        marquee.setStyleClass("team_marquee_cont");
        if (_bangobj.playerInfo[pidx].buckle != null) {
            BuckleView bview = new BuckleView(_ctx, 2);
            bview.setBuckle(_bangobj.playerInfo[pidx].buckle);
            marquee.add(bview);
        }
        if (_bangobj.playerInfo[pidx].gang != null) {
            String style = (color > 4 && mult ? "gang_team_label" : "gang_marquee_label") + color;
            BLabel gangLabel = new BLabel(_bangobj.playerInfo[pidx].gang.toString(), style);
            gangLabel.setFit(BLabel.Fit.SCALE);
            gangLabel.setPreferredSize(145, 19);
            marquee.add(gangLabel);
        }
        return marquee;
    }

    @Override // documentation inherited
    protected void clearMarquee (float fadeTime)
    {
        super.clearMarquee(fadeTime);
        if (_pmarquees != null) {
            WindowFader.remove(_ctx, _pmarquees, fadeTime);
            _pmarquees = null;
        }
    }

    @Override // documentation inherited
    protected void fadeInComplete ()
    {
        if (_tpath == null) {
            super.fadeInComplete();

        } else {
            // we will clear the marquee when the board tour is finished
            _fadein = null;
        }
    }

    @Override // documentation inherited
    protected void hoverSpritesChanged (Sprite hover, Sprite thover)
    {
        // if we were hovering over a unit, clear its hover state
        if (_hover instanceof UnitSprite) {
            ((UnitSprite)_hover).setHovered(false);
        }

        super.hoverSpritesChanged(hover, thover);

        if (_bangobj.isActivePlayer(_pidx)) {

            // if we're hovering over a unit we can click, mark it as such
            if (hover instanceof UnitSprite) {
                UnitSprite usprite = (UnitSprite)hover;
                Piece piece = usprite.getPiece();
                if (piece.isAlive() && (isSelectable(piece) ||
                                _attackSet.contains(piece.x, piece.y))) {
                    usprite.setHovered(true);
                }
            }

            highlightPossibleAttacks();

            // update tile highlights for card placement
            if (_card != null &&
                (_card.getPlacementMode() == Card.PlacementMode.VS_PIECE ||
                 _card.getPlacementMode() == Card.PlacementMode.VS_PLAYER)) {
                if (hover instanceof PieceSprite) {
                    Piece piece = ((PieceSprite)hover).getPiece();
                    clearHighlights();
                    _attackSet.clear();
                    _attackSet.add(piece.x, piece.y);
                    targetTiles(
                            _attackSet, _card.isValidPiece(_bangobj, piece));
                } else {
                    updatePlacingCard(_mouse.x, _mouse.y);
                }
            }
        }

        // display contextual help on units and other special sprites
        _tiptext = null;
        if (thover instanceof PieceSprite) {
            _tiptext = ((PieceSprite)thover).getTooltip(_pidx);
            if (_tiptext != null) {
                _tiptext = _ctx.xlate(GameCodes.GAME_MSGS, _tiptext);
            }
        }

        // force an update to the tooltip window as we're one big window with
        // lots of different tips
        _ctx.getRootNode().tipTextChanged(this);
    }

    @Override // documentation inherited
    protected BComponent createTooltipComponent (String tiptext)
    {
        // only show our tooltip window when we're in "insta-tip" mode
        return (_ctx.getRootNode().getTooltipTimeout() == 0) ?
            super.createTooltipComponent(tiptext) : null;
    }

    @Override // documentation inherited
    protected void createPieceSprite (Piece piece, short tick)
    {
        super.createPieceSprite(piece, tick);

        // pieces added before the game starts will be handled by a call to
        // shadowPieces()
        if (_bangobj.state != BangObject.IN_PLAY) {
            return;
        }

        // update the shadow we use to do path finding and whatnot
        _bangobj.board.shadowPiece(piece);

        // if this piece influenced our selection, refresh it
        checkForSelectionInfluence();

        // if this is not a unit, we can stop here
        if (!(piece instanceof Unit)) {
            // report to our tutorial controller that a piece was added
            _ctrl.postEvent(TutorialCodes.PIECE_ADDED, piece.pieceId);
            return;
        }

        // let the unit status view know that a unit was added
        UnitSprite sprite = getUnitSprite(piece);
        BangView bview = (BangView)getParent();
        if (bview.ustatus != null) {
            bview.ustatus.unitAdded(sprite);
        }

        // if we've not yet started the game, place the unit at the corner of the board nearest to
        // the player's start position because we're going to run them to the player's starting
        // location once their animations are resolved.  if the unit is respawning, queue up the
        // respawn action
        if (_bconfig.type != BangConfig.Type.TUTORIAL && _bangobj.tick < 0) {
            Point corner = getStartCorner(piece);
            sprite.setLocation(_board, corner.x, corner.y);
        }

        // report to our tutorial controller that a unit was added
        _ctrl.postEvent(TutorialCodes.UNIT_ADDED, piece.pieceId);
    }

    /**
     * Called when all of our models (and sounds?) are fully resolved and we're
     * ready to start the round.
     */
    protected void reallyStartRound (long start)
    {
        clearLoadingMarquee();

        // restore normal rendering
        long end = System.currentTimeMillis();
        // TODO: turn renderer back on?
        // _ctx.getApp().setEnabled(true, true);
        // _ctx.getApp().setTargetFPS(oldFPS);

        // report model loading time to admins
        if (_ctx.getUserObject().tokens.isAdmin()) {
            String msg = "Loaded units in " + (end-start) +
                " millis.";
            _ctx.getChatDirector().displayInfo(
                null, MessageBundle.taint(msg));
        }

        // move all of our loaded models into position
        long delay = 1L;
        Camera camera = _ctx.getRenderer().getCamera();
        BoundingBox bbox = new BoundingBox(new Vector3f(),
            TILE_SIZE/2, TILE_SIZE/2, TILE_SIZE/2);
        long pathDelay = 0L, segTime = (long)(1000L / Config.getMovementSpeed());
        for (UnitSprite sprite : _readyUnits) {
            Piece unit = sprite.getPiece();
            Point corner = getStartCorner(unit);
            final List<java.awt.Point> path = AStarPathUtil.getPath(
                _tpred, unit.getStepper(), unit, _board.getWidth() / 2,
                corner.x, corner.y, unit.x, unit.y, false);

            // strip off all but the last location that is not visible given
            // the current camera position
            if (path != null) {
                int startidx = path.size();
                for (int ii = path.size()-1; ii >= 0; ii--) {
                    java.awt.Point point = path.get(ii);
                    Vector3f center = bbox.getCenter();
                    center.set((point.x + 0.5f) * TILE_SIZE,
                        (point.y + 0.5f) * TILE_SIZE, TILE_SIZE/2);
                    center.z += _board.getElevationScale(TILE_SIZE) *
                        unit.computeElevation(_board, point.x, point.y);
                    startidx = ii;
                    int state = camera.getPlaneState();
                    int rv = camera.contains(bbox);
                    camera.setPlaneState(state);
                    if (rv == Camera.OUTSIDE_FRUSTUM) {
                        break;
                    }
                }
                // strip off everything up to the start index; but make sure
                // the path is at least two long
                startidx = Math.min(path.size()-2, startidx);
                for (int ii = 0; ii < startidx; ii++) {
                    path.remove(0);
                }
            }

            // if we have no path left, just blip to our starting spot
            if (path == null || path.size() < 2) {
                sprite.setLocation(_board, unit.x, unit.y);
                sprite.snapToTerrain(false);
                continue;
            }

            // otherwise move there like a civilized unit
            final UnitSprite fsprite = sprite;
            new Interval(_ctx.getApp()) {
                public void expired () {
                    fsprite.move(_board, path, Config.getMovementSpeed());
                }
            }.schedule(delay);

            // calculate how long it should take for all paths to complete
            pathDelay = Math.max(pathDelay, path.size() * segTime);
            delay += 150L;
            pathDelay -= 150L;
        }
        _readyUnits.clear();

        // tell the controller to report back to the server that we're ready;
        // but give the units another half a second to arrive
        delay += 500L + Math.max(0L, pathDelay);
        new Interval(_ctx.getApp()) {
            public void expired () {
                _ctrl.readyForRound();
            }
        }.schedule(delay);
    }

    /**
     * Returns the corner of the board from which the specified piece
     * should emerge.
     */
    protected Point getStartCorner (Piece piece)
    {
        if (piece.owner != -1) {
            StreamablePoint pt = _bangobj.startPositions[piece.owner];
            return getNearestCorner(pt.x, pt.y);
        } else {
            return getNearestCorner(piece.x, piece.y);
        }
    }

    /**
     * Returns the location of the corner of the board nearest to the
     * given coordinates.
     */
    protected Point getNearestCorner (int x, int y)
    {
        return new Point(
            (x < _board.getWidth() / 2) ? 0 : _board.getWidth() - 1,
            (y < _board.getHeight() / 2) ? 0 : _board.getHeight() - 1);
    }

    /** Called by the {@link EffectHandler} when a piece was affected. */
    protected void pieceWasAffected (Piece piece, String effect)
    {
        checkForSelectionInfluence();

        // just report the effect to the controller in case the tutorial cares
        _ctrl.postEvent(TutorialCodes.EFFECT_PREFIX + effect, piece.pieceId);
    }

    /** Called by the {@link EffectHandler} when a piece has moved. */
    protected void pieceDidMove (Piece piece)
    {
        // clear out any advance order for this piece
        clearAdvanceOrder(piece.pieceId);

        // if this was our selection, refresh it
        if (_selection == piece) {
            clearSelection();
            selectUnit((Unit)piece, false);
        } else {
            // otherwise, if this piece was inside our attack set or within
            // range to be inside our move set, recompute the selection as it
            // may have changed
            checkForSelectionInfluence();
        }

        // let the controller know that a unit moved
        _ctrl.postEvent(TutorialCodes.UNIT_MOVED, piece.pieceId);
    }

    /** Called by the {@link EffectHandler} when a piece was killed. */
    protected void pieceWasKilled (int pieceId)
    {
        // Some effects are valid after a unit is killed, others are not,
        // let's make a note when this happens but it's no longer insane
        if (_pendmap.get(pieceId) > 0) {
            log.info("Piece with pending actions killed", "id", pieceId,
                     "pendcount", _pendmap.get(pieceId));
        }

        // clear out any advance order for this piece
        clearAdvanceOrder(pieceId);

        // if this piece was selected, clear it
        if (_selection != null && _selection.pieceId == pieceId) {
            clearSelection();
        } else {
            checkForSelectionInfluence();
        }

        // report that a piece was killed
        _ctrl.postEvent(TutorialCodes.UNIT_KILLED, pieceId);
    }

    /** Handles a left mouse button click. */
    protected void handleLeftPress (int mx, int my)
    {
        // nothing doing if the game is not in play or we're not a player or the tutorials have
        // disabled movement
        if (_bangobj == null || !_bangobj.isActivePlayer(_pidx) || !_bangobj.isInteractivePlay()) {
            return;
        }

        // check for a piece under the mouse
        PieceSprite sprite = null;
        Piece piece = null;
        if (_hover instanceof Targetable) {
            sprite = (PieceSprite)_hover;
            piece = _bangobj.pieces.get(sprite.getPieceId());
            if (piece != null && !piece.isAlive()) {
                sprite = null;
                piece = null;
            }
        }

        // if we are placing a card, activate it
        if (_card != null) {
            Object target = null;
            switch (_card.getPlacementMode()) {
            case VS_AREA:
                if (_card.isValidLocation(_bangobj, _mouse.x, _mouse.y)) {
                    log.info("Activating " + _card);
                    target = new int[] { _mouse.x, _mouse.y };
                }
                break;

            case VS_PIECE:
            case VS_PLAYER:
                if (_hover != null && _hover instanceof PieceSprite) {
                    Piece p = ((PieceSprite)_hover).getPiece();
                    if (_card.isValidPiece(_bangobj, p)) {
                        log.info("Activating " + _card);
                        if (_card.getPlacementMode() ==
                                Card.PlacementMode.VS_PIECE) {
                            target = new Integer(p.pieceId);
                        } else {
                            target = new Integer(p.owner);
                        }
                    }
                }
                break;

            default:
                break; // nada
            }

            if (target != null) {
                _ctrl.activateCard(_card.cardId, target);
            }
            return;
        }

        // select the piece under the mouse if it meets our sundry conditions
        if (piece != null &&  sprite != null && isSelectable(piece)) {
            selectUnit((Unit)piece, false);
            // if shift was held down down when this unit was selected and the
            // unit has an advance order, clear it out
            if (_selection != null && _shiftDown &&
                getUnitSprite(_selection).getAdvanceOrder() !=
                UnitSprite.AdvanceOrder.NONE) {
                _ctrl.cancelOrder(_selection.pieceId);
            }
            return;
        }

        // if we have a selection
        if (_selection != null) {
            // and we have an attack set
            if (_attackSet.size() > 0) {
                // if they clicked on a piece, use its coordinates, otherwise
                // use the coordinates of the highlight tile over which the
                // mouse is hovering
                int ax = _high.x, ay = _high.y;
                if (piece != null) {
                    ax = piece.x;
                    ay = piece.y;
                }
                if (handleClickToAttack(piece, ax, ay)) {
                    return;
                }
            }

            if (handleClickToMove(_high.x, _high.y, true)) {
                return;
            }
        }
    }

    /** Handles a click that should select a potential move location. */
    protected boolean handleClickToMove (int tx, int ty, boolean mouse)
    {
        // or if we're clicking in our move set or on our selected piece
        if (!_moveSet.contains(tx, ty) &&
            (_selection == null || _selection.x != tx || _selection.y != ty)) {
            log.info("Rejecting move +" + tx + "+" + ty + " moves:" + _moveSet);
            return false;
        }

        // store the coordinates toward which we wish to move
        _action = new int[] { _selection.pieceId, tx, ty, -1 };

        // clear any previous attack set
        clearAttackSet();

        // reduce the highlighted move tiles to just the selected tile
        clearHighlights();
        PointSet moves = new PointSet();
        moves.add(tx, ty);
        highlightMovementTiles(moves, _goalSet, getHighlightColor(_selection));

        // potentially display our potential attacks (if shift is held down, we
        // don't generate an attack set which causes us to move immediately to
        // our clicked destination without attacking)
        if (!_shiftDown && canAttack(tx, ty)) {
            pruneAttackSet(moves, _attackSet, true);
        }

        // if there are no valid attacks, assume they're just moving (but
        // do nothing if they're not even moving)
        if (_attackSet.size() == 0 &&
            (_action[1] != _selection.x || _action[2] != _selection.y)) {
            // only execute the action if it was a mouse click that caused it,
            // not because all targets moved out of range
            if (mouse) {
                executeAction();
                _attackSet.clear();
            } else {
                clearSelection();
            }
        } else {
            log.info("Waiting for attack selection (" + tx + ", " + ty + ")");
            PointSet attackRange = _selection.computeShotRange(
                    _bangobj.board, tx, ty);
            highlightTiles(attackRange, ATTACK_RANGE_COLOR, _rngstate, false);
        }
        return true;
    }

    protected boolean canAttack (int x, int y)
    {
        return _attackEnabled == null || (_attackEnabled.x == x && _attackEnabled.y == y);
    }

    /** Handles a click that indicates the coordinates of a piece we'd
     * like to attack. */
    protected boolean handleClickToAttack (Piece piece, int tx, int ty)
    {
        if (piece != null) {
            log.info("Clicking to attack " + piece);
        } else {
            log.info("Clicking to attack +" + tx + "+" + ty);
        }

        // maybe we're clicking on a piece that is in our attack set
        if (_attackSet.contains(tx, ty) && piece != null &&
            piece.owner != _pidx) {
            // if we have not yet selected move coordinates, we'll let the
            // server figure it out for us when it processes our move
            if (_action == null) {
                _action = new int[] { _selection.pieceId,
                                      Short.MAX_VALUE, Short.MAX_VALUE, -1 };
            }
            // note the piece we desire to fire upon
            _action[3] = piece.pieceId;

            getTargetableSprite(piece).setPendingShot(true);
            executeAction();
            return true;
        }

        // maybe we're clicking a second time on our desired move location
        if (_action != null && tx == _action[1] && ty == _action[2]) {
            if (_attackSet.size() > 0 && !_shiftDown) {
                // select a random target and request to shoot it
                int idx = RandomUtil.getInt(_attackSet.size());
                Piece target = _bangobj.getTarget(
                    _attackSet.getX(idx), _attackSet.getY(idx));
                // TODO: prefer targets that are guaranteed to be there
                // when we make our move versus those that may be gone
                if (target != null && _selection.validTarget(
                        _bangobj, target, false)) {
                    log.info("Randomly targeting " + target);
                    _action[3] = target.pieceId;
                    Targetable tsprite = getTargetableSprite(target);
                    if (tsprite != null) {
                        tsprite.setPendingShot(true);
                    }
                }
            }
            executeAction();
            return true;
        }

        return false;
    }

    @Override // documentation inherited
    protected void hoverHighlightChanged (TerrainNode.Highlight hover)
    {
        super.hoverHighlightChanged(hover);
        highlightPossibleAttacks();
    }

    /**
     * Scans all pieces in the attack set and highlights those who are in
     * range from the current hover coordinate.
     */
    protected void highlightPossibleAttacks ()
    {
        if (_selection != null && _attackSet.size() > 0 && _action == null) {
            boolean hoveringAttack = false;
            // highlight the unit we're hovering over if applicable
            if (_hover != null && _hover instanceof Targetable) {
                Piece p = ((PieceSprite)_hover).getPiece();
                if (_attackSet.contains(p.x, p.y)) {
                    ((Targetable)_hover).setPossibleShot(true);
                    hoveringAttack = true;
                }
            }
            // go through all the pieces and change the color of the
            // highlight where applicable
            for (Piece p : _bangobj.pieces) {
                if (!p.isTargetable() || !_attackSet.contains(p.x, p.y)) {
                    continue;
                }
                Targetable target = getTargetableSprite(p);
                if (target == null) {
                    continue;
                }
                boolean possible = false;
                if (_hover == null || hoveringAttack == false) {
                    PointSet moves = new PointSet();
                    if (_highlightHover != null) {
                        moves.add(_highlightHover.getTileX(),
                                _highlightHover.getTileY());
                    } else {
                        moves.add(_selection.x, _selection.y);
                    }
                    possible = _selection.validTarget(_bangobj, p, false) &&
                        _selection.computeShotLocation(
                                _board, p, moves, true) != null &&
                        _bangobj.scenario.validShot(_selection, moves, p);
                } else if (_hover == target) {
                    possible = true;
                }
                target.setPossibleShot(possible);
            }
        }
    }

    /**
     * Scans all pieces to which we can compute a valid firing location, adds
     * their location to the supplied destination set, and marks their sprite
     * as being targeted.
     */
    protected void pruneAttackSet (
            PointSet moves, PointSet dest, boolean possible)
    {
        for (Piece p : _bangobj.pieces) {
            if (!_selection.validTarget(_bangobj, p, false) ||
                _selection.computeShotLocation(
                    _board, p, moves, true) == null ||
                !_bangobj.scenario.validShot(_selection, moves, p)) {
                continue;
            }

            Targetable target = getTargetableSprite(p);
            if (target != null) {
                target.setTargeted(_bangobj,
                        _selection.lastActed >= p.lastActed ?
                             Targetable.TargetMode.MAYBE :
                             (_selection.killShot(_bangobj, p) ?
                                 Targetable.TargetMode.KILL_SHOT :
                                 Targetable.TargetMode.SURE_SHOT),
                        _selection);
                target.setPossibleShot(possible);
                dest.add(p.x, p.y);
            } else {
                log.warning("No sprite for unit!", "unit", p);
            }
        }
    }

    /**
     * Removes from the movement set all tiles which are not reachable
     * after moving to the supplied detination.
     */
    protected void pruneMoveSet (PointSet moves, int dx, int dy)
    {
        int dist = _selection.getDistance(dx, dy);
        int remain = _selection.getMoveDistance() - dist;
        for (Iterator<Integer> iter = moves.iterator(); iter.hasNext(); ) {
            int coord = iter.next();
            int x = PointSet.decodeX(coord), y = PointSet.decodeY(coord);
            if (Piece.getDistance(dx, dy, x, y) > remain) {
                iter.remove();
            }
        }
    }

    /** Handles a right mouse button click. */
    protected void handleRightPress (int mx, int my)
    {
        // nothing doing if the game is not in play
        if (_bangobj == null || !_bangobj.isActivePlayer(_pidx) ||
                !_bangobj.isInteractivePlay()) {
            return;
        }

        // if we are placing a card, clear it out
        if (clearPlacingCard()) {
            return;
        }

        // if we're hovering over the selection, cancel any advance order for
        // that selection
        if (_hover instanceof UnitSprite && _selection != null) {
            UnitSprite uhover = (UnitSprite)_hover;
            if (uhover.getPieceId() == _selection.pieceId &&
                uhover.getAdvanceOrder() != UnitSprite.AdvanceOrder.NONE) {
                _ctrl.cancelOrder(uhover.getPieceId());
            }
        }

        // clear any unit selection
        clearSelection();

//         // if there is a piece under the cursor, show their possible shots
//         if (_hover instanceof PieceSprite) {
//             PieceSprite sprite = (PieceSprite)_hover;
//             Piece piece = (Piece)_bangobj.pieces.get(sprite.getPieceId());
//             if (sprite instanceof UnitSprite && piece.isAlive()) {
//                 clearSelection();
//                 PointSet moveSet = new PointSet();
//                 _attackSet.clear();
//                 _bangobj.board.computeMoves(piece, moveSet, null);
//                 for (int ii = 0; ii < moveSet.size(); ii++) {
//                     _attackSet.add(moveSet.get(ii));
//                 }
//             }
//         }
    }

    /**
     * Convenience method for getting the sprite for a piece we know to be
     * a unit.
     */
    protected UnitSprite getUnitSprite (Piece piece)
    {
        return (UnitSprite)getPieceSprite(piece);
    }

    /**
     * Convenience method for getting the sprite for a piece we know to be
     * targetable.
     */
    protected Targetable getTargetableSprite (Piece piece)
    {
        return (Targetable)getPieceSprite(piece);
    }

    protected void selectUnit (Unit piece, boolean scrollCamera)
    {
        // if we're not interactive, ignore this request
        if (!isInteractive()) {
            return;
        }

        boolean deselect = (piece == _selection);
        clearSelection();
        if (deselect || !piece.isAlive()) {
            return;
        }
        _selection = piece;

//         log.info("Selecting " + _selection);

        // select the sprite and center it in the view
        PieceSprite sprite = getPieceSprite(_selection);
        if (sprite == null) {
            return; // we might still be setting up
        }
        sprite.setSelected(true);
        _cursor.setLocalTranslation(new Vector3f(0, 0, sprite.getHeight()));
        sprite.attachChild(_cursor);
        if (scrollCamera) {
            centerCameraOnPiece(_selection);
        }

        // highlight our potential moves and attackable pieces
        piece.computeMoves(_bangobj.board, _moveSet, null);
        _attackSet.clear();
        if (canAttack(piece.x, piece.y)) {
            pruneAttackSet(_moveSet, _attackSet, false);
        } else if (_attackEnabled != null &&
                _bangobj.board.getPlayableArea().contains(_attackEnabled)) {
            if (!_moveSet.contains(_attackEnabled.x, _attackEnabled.y)) {
                pruneAttackSet(_moveSet, _attackSet, false);
                if (_attackSet.contains(_attackEnabled.x, _attackEnabled.y)) {
                    _attackSet.clear();
                    _attackSet.add(_attackEnabled.x, _attackEnabled.y);
                } else {
                    _attackSet.clear();
                }
                _moveSet.clear();
            } else {
                _moveSet.clear();
                _moveSet.add(_attackEnabled.x, _attackEnabled.y);
            }
        }
        highlightPossibleAttacks();

        // find the moves that lead to goals
        _goalSet.clear();
        _bangobj.scenario.getMovementGoals(
            _bangobj, _selection, _moveSet, _goalSet);

        // clear out our current location as we don't want to highlight that as
        // a potential move (but we needed it earlier when computing attacks)
        _moveSet.remove(piece.x, piece.y);
        highlightMovementTiles(_moveSet, _goalSet, getHighlightColor(piece));

        // report that the user took an action (for tutorials)
        _ctrl.postEvent(TutorialCodes.UNIT_SELECTED, piece.pieceId);
    }

    protected boolean checkForSelectionInfluence ()
    {
        if (_selection == null) {
            return false;
        }

        // refresh our selection
        int[] oaction = _action;
        Unit oselection = _selection;
        clearSelection();
        selectUnit(oselection, false);

        if (oaction != null) {
            // if we had already selected a movement, reconfigure that (it
            // might no longer be valid but handleClickToMove will ignore
            // us in that case
            if (oaction[3] == -1) {
                log.info("Reissuing click to move +" + oaction[1] + "+" + oaction[2]);
                handleClickToMove(oaction[1], oaction[2], false);
            }
        }

        return true;
    }

    protected void centerCameraOnPiece (Piece piece)
    {
        PieceSprite sprite = getPieceSprite(piece);
        if (sprite != null) {
            ((GameInputHandler)_ctx.getInputHandler()).aimCamera(
                sprite.getWorldTranslation());
        }
    }

    protected void executeAction ()
    {
        // send off a request to move and shoot
        _ctrl.moveAndFire(_action[0], _action[1], _action[2], _action[3]);
        // and clear our selection
        clearSelection();
    }

    protected void addAdvanceOrder (int unitId, int tx, int ty, int targetId)
    {
        // clear any old advance order for this unit
        clearAdvanceOrder(unitId);

        // look up the unit we're "acting" with
        Unit actor = (Unit)_bangobj.pieces.get(unitId);
        if (actor == null) {
            log.warning("Missing piece for advance order", "id", unitId);
            return;
        }
        UnitSprite sprite = getUnitSprite(actor);
        if (sprite == null) {
            log.warning("Missing sprite for advance order", "p", actor);
            return;
        }
        _orders.put(unitId, new AdvanceOrder(sprite, tx, ty, targetId));
    }

    protected boolean hasAdvanceOrder (int unitId)
    {
        return _orders.containsKey(unitId);
    }

    protected void clearAdvanceOrder (int unitId)
    {
        AdvanceOrder order = _orders.remove(unitId);
        if (order != null) {
            order.clear();
        }
    }

    protected void clearMoveSet ()
    {
        clearHighlights();
        _moveSet.clear();
    }

    protected void clearAttackSet ()
    {
        if (_attackSet.size() > 0) {
            clearHighlights();
            _attackSet.clear();
        }
        for (PieceSprite s : _pieces.values()) {
            if (s instanceof Targetable) {
                ((Targetable)s).setTargeted(
                        _bangobj, Targetable.TargetMode.NONE, null);
            }
        }
    }

    protected void clearSelection ()
    {
        if (_selection != null) {
            PieceSprite psprite = getPieceSprite(_selection);
            if (psprite != null) {
                psprite.setSelected(false);
                psprite.detachChild(_cursor);
            }
            _selection = null;
            _ctrl.postEvent(TutorialCodes.UNIT_DESELECTED, -1);
        }
        clearMoveSet();

        // clear out any pending action
        _action = null;
        clearAttackSet();
    }

    protected ColorRGBA getHighlightColor (Piece piece)
    {
        if (piece instanceof Unit &&
            ((Unit)piece).getConfig().rank == UnitConfig.Rank.BIGSHOT) {
            return BMOVE_HIGHLIGHT_COLOR;
        } else {
            return MOVEMENT_HIGHLIGHT_COLOR;
        }
    }

    protected void updatePlacingCard (int tx, int ty)
    {
        switch (_card.getPlacementMode()) {
        case VS_AREA:
            clearHighlights();
            _attackSet.clear();
            _bangobj.board.computeAttacks(0, _card.getRadius(), tx, ty, _attackSet);
            if (_bangobj.board.getPlayableArea().contains(tx, ty)) {
                _attackSet.add(tx, ty);
            }
            targetTiles(_attackSet, _card.isValidLocation(_bangobj, tx, ty));
            break;

        case VS_PIECE:
        case VS_PLAYER:
            // if we're hovering over a piece it will get set properly in hoverSpriteChanged()
            if (_hover == null) {
                clearHighlights();
                _attackSet.clear();
                if (_bangobj.board.getPlayableArea().contains(tx, ty)) {
                    _attackSet.add(tx, ty);
                    targetTiles(_attackSet, false);
                }
            }
            break;

        default:
            break; // nada
        }
    }

    @Override // documentation inherited
    protected void hoverTileChanged (int tx, int ty)
    {
        // if we have an active card, update its area of effect
        if (_card != null) {
            updatePlacingCard(tx, ty);
        }
    }

    @Override // documentation inherited
    protected void pieceRemoved (Piece piece, short tick)
    {
        super.pieceRemoved(piece, tick);

        // clear out our pathfinding shadow for this piece
        _bangobj.board.clearShadow(piece);
    }

    @Override // documentation inherited
    protected PieceSprite removePieceSprite (int pieceId, String why)
    {
        PieceSprite sprite = _pieces.get(pieceId);
        if (sprite instanceof BonusSprite) {
            // if this was a bonus, note that it was activated
            _ctrl.postEvent(TutorialCodes.BONUS_ACTIVATED, pieceId);
        }
        if (sprite instanceof ActiveSprite) {
            ActiveSprite asprite = (ActiveSprite)sprite;
            // if this active sprite is animating it will have to wait
            // until it's finished before being removed
            if (asprite.removed()) {
                _pieces.remove(pieceId);
                asprite.addObserver(_deadRemover);
                return sprite;
            } else {
                log.info("Removing dead sprite immediately " + asprite.getPiece() + ".");
            }
        }
        return super.removePieceSprite(pieceId, why);
    }

    /**
     * Called every time the board ticks.
     */
    protected void ticked (short tick)
    {
        // Once we've got the tick, force all our sprites to the starting positions (they should
        // already be there), and show the GO! marquee
        if (tick == 0) {
            for (PieceSprite sprite : _pieces.values()) {
                if (sprite instanceof UnitSprite) {
                    UnitSprite usprite = (UnitSprite)sprite;
                    Piece p = usprite.getPiece();
                    if (sprite.isMoving()) {
                        usprite.cancelMove();
                        log.warning("Jumped sprite to start", "piece", p);
                    }
                    usprite.setLocation(_board, p.x, p.y);
                    usprite.snapToTerrain(false);
                }
            }

            // add and immediately fade in and out a "GO!" marquee
            fadeMarqueeInOut("m.round_start", 1f);
        }
        // update all of our sprites
        for (Piece piece : _bangobj.pieces) {
            if (piece.updateSpriteOnTick()) {
                PieceSprite ps = getPieceSprite(piece);
                if (ps != null) {
                    ps.updated(piece, tick);
                }
            }
        }
    }

    /**
     * Does a camera tour of the board, properly positioning the camera when
     * the tour is complete.
     */
    protected void doBoardTour (final Runnable onComplete)
    {
        // make sure all elements are resolved
        if (_resolving > 0) {
            addResolutionObserver(new ResolutionObserver() {
                public void mediaResolved () {
                    doBoardTour(onComplete);
                }
            });
            return;
        }

        // do a full GC before we start the board tour to tidy up after all the
        // prop model loading
        System.gc();

        // compute the desired starting location and orientation
        GameCameraHandler camhand = (GameCameraHandler)_ctx.getCameraHandler();
        java.awt.Point start = _bangobj.startPositions[
            Math.max(0, _bangobj.getPlayerIndex(_ctx.getUserObject().handle))];
        Vector3f gpoint = camhand.getGroundPoint(),
            oloc = new Vector3f(camhand.getCamera().getLocation());
        float dx = (start.x + 0.5f) * TILE_SIZE - gpoint.x,
            dy = (start.y + 0.5f) * TILE_SIZE - gpoint.y;
        if (dx >= 0f && dy >= 0f) {
            camhand.orbitCamera(FastMath.HALF_PI);
        } else if (dx < 0f && dy >= 0f) {
            camhand.orbitCamera(FastMath.PI);
        } else if (dx < 0f && dy < 0f) {
            camhand.orbitCamera(-FastMath.HALF_PI);
        }
        // first, pan to the appropriate location so we know where it is
        // and the ground height is up-to-date; then, restore the original
        // position so that we start the path from the right place
        camhand.panCameraAbs(dx, dy);
        Vector3f pan = camhand.getGroundPoint().subtractLocal(gpoint);
        camhand.getCamera().setLocation(oloc);

        // pan, orbit, and zoom over the board
        camhand.setLimitsEnabled(false);
        _tpath = new SwingPath(camhand, gpoint, camhand.getGroundNormal(),
            FastMath.TWO_PI, FastMath.TWO_PI / BOARD_TOUR_DURATION,
            camhand.getCamera().getLeft(), -FastMath.PI * 0.25f, pan, -75f) {
            public boolean tick (float secondsSince) {
                // fade the marquee out when there's a second or less remaining
                boolean ret = super.tick(secondsSince);
                float remaining = (_pangle - _protated) / _pangvel;
                if (!_clearing && remaining <= 1f) {
                    clearMarquee(remaining);
                    _clearing = true;
                }
                return ret;
            }
            protected boolean _clearing;
        };
        camhand.moveCamera(_tpath);
        camhand.addCameraObserver(new CameraPath.Observer() {
            public boolean pathCompleted (CameraPath path) {
                // clear the marquee, return the camera to normal, and let the
                // controller start up the next phase (if we're not leaving)
                GameCameraHandler camhand =
                    (GameCameraHandler)_ctx.getCameraHandler();
                camhand.setLimitsEnabled(true);
                if (isAdded()) {
                    _ctrl.preSelectBoardTourComplete();
                }
                _tpath = null;
                if (onComplete != null) {
                    onComplete.run();
                }
                return false;
            }
        });
    }

    /**
     * Called when an effect is applied to the board.
     */
    protected void applyEffect (Effect effect)
    {
        log.info("Applying effect " + effect + ".");
        EffectHandler handler = effect.createHandler(_bangobj);
        if (handler != null) {
            handler.init(_ctx, _bangobj, _pidx, this,
                (BangView)_ctrl.getPlaceView(), _sounds, effect);
            executeAction(handler);
        }
    }

    /**
     * Returns a (deep) copy of the lights to use in blending.
     */
    protected DirectionalLight[] copyLights ()
    {
        DirectionalLight[] nlights = new DirectionalLight[_lights.length];
        for (int ii = 0; ii < _lights.length; ii++) {
            nlights[ii] = new DirectionalLight();
            nlights[ii].setAmbient(new ColorRGBA(_lights[ii].getAmbient()));
            nlights[ii].setDiffuse(new ColorRGBA(_lights[ii].getDiffuse()));
            nlights[ii].setSpecular(new ColorRGBA(_lights[ii].getSpecular()));
            nlights[ii].setDirection(new Vector3f(_lights[ii].getDirection()));
        }
        return nlights;
    }

    /**
     * Interpolates between two sets of lights and stores the result in the
     * board lights.
     */
    protected void interpolateLights (
        DirectionalLight[] l1, DirectionalLight[] l2, float alpha)
    {
        for (int ii = 0; ii < _lights.length; ii++) {
            _lights[ii].getAmbient().interpolate(
                l1[ii].getAmbient(), l2[ii].getAmbient(), alpha);
            _lights[ii].getDiffuse().interpolate(
                l1[ii].getDiffuse(), l2[ii].getDiffuse(), alpha);
            _lights[ii].getSpecular().interpolate(
                l1[ii].getSpecular(), l2[ii].getSpecular(), alpha);
            Vector3f dir1 = l1[ii].getDirection(),
                dir2 = l2[ii].getDirection();
            getDirectionVector(
                FastMath.LERP(alpha, getAzimuth(dir1), getAzimuth(dir2)),
                FastMath.LERP(alpha, getElevation(dir1), getElevation(dir2)),
                _lights[ii].getDirection());
        }
    }

    /**
     * Converts a RGB value to a YUV value.
     */
    protected ColorRGBA RGBtoYUV (ColorRGBA source)
    {
        float y = 0.299f*source.r + 0.587f*source.g + 0.114f*source.b;
        return new ColorRGBA(
                y, (source.b-y)*0.565f, (source.r-y)*0.713f, source.a);
    }

    /**
     * Converts a YUV value to an RGB value.
     */
    protected ColorRGBA YUVtoRGB (ColorRGBA source)
    {
        return new ColorRGBA(
                source.r + 1.403f*source.b,
                source.r - 0.344f*source.g - 0.714f*source.b,
                source.r + 1.770f*source.g,
                source.a);
    }

    @Override // documentation inherited
    protected void processActions ()
    {
        _pmoves.clear();
        super.processActions();
        if (_pendingMarquee != MarqueeMode.NONE) {
            if (noActions()) {
                switch (_pendingMarquee) {
                case GAME:
                    showPostGameMarquee();
                    break;
                case ROUND:
                    showInterRoundMarquee();
                    break;
                default:
                    break; // nada
                }
                _pendingMarquee = MarqueeMode.NONE;
            }
        }
    }

    @Override // documentation inherited
    protected void notePending (BoardAction action)
    {
        super.notePending(action);
        if (action.moveIds.length > 0 && action instanceof EffectHandler) {
            _pmoves.add((EffectHandler)action);
        }
    }

    @Override // documentation inherited
    protected void clearBoardActions ()
    {
        super.clearBoardActions();
        _pmoves.clear();
        _pendmap.clear();
    }

    /**
     * Creates a custom cursor with a card icon.
     */
    protected void createCardCursor (Card card)
    {
        BufferedImage merge = ImageCache.createCompatibleImage(32, 32, true);
        BufferedImage cursor =
            _ctx.getImageCache().getBufferedImage("ui/cursor_default.png");
        BufferedImage icon =
            _ctx.getImageCache().getBufferedImage(card.getIconPath("icon"));
        Graphics2D g = merge.createGraphics();

        // we're going to shove the icon into the bottom right corner removing
        // any transparent pixels
        IndexColorModel cm = (IndexColorModel)icon.getColorModel();
        int transparent = cm.getTransparentPixel();
        Raster raster = icon.getRaster();
        int ww = icon.getWidth(), hh = icon.getHeight();

        int xx = ww - 1;
        int[] pixels = new int[hh];
      CROP_WIDTH:
        for (; xx >= 0; xx--) {
            raster.getPixels(xx, 0, 1, hh, pixels);
            for (int ii = 0; ii < pixels.length; ii++) {
                if (pixels[ii] != transparent) {
                    xx++;
                    break CROP_WIDTH;
                }
            }
        }

        int yy = hh - 1;
        pixels = new int[ww];
      CROP_HEIGHT:
        for (; yy >= 0; yy--) {
            raster.getPixels(0, yy, ww, 1, pixels);
            for (int ii = 0; ii < pixels.length; ii++) {
                if (pixels[ii] != transparent) {
                    yy++;
                    break CROP_HEIGHT;
                }
            }
        }

        int ix = 0, iy = 0;
        if (xx >= 32) {
            ix = xx - 32;
        }
        if (yy >= 32) {
            iy = yy - 32;
        }
        g.drawImage(icon.getSubimage(
                        ix, iy, Math.min(32, xx - ix), Math.min(32, yy - iy)),
                    null, Math.max(0, 31 - xx - ix), Math.max(0, 31 - yy - iy));
        g.drawImage(cursor, null, 0, 0);
        try {
            BCursor defaultCursor = BangUI.loadCursor("default");
            if (_defaultCursor == null) {
                _defaultCursor = defaultCursor.getCursor();
            }
            defaultCursor.setCursor(merge, 0, 0);
            defaultCursor.show();
        } catch (Exception e) {
            // this should never happen
        }
        g.dispose();
    }

    /**
     * Reset the cursor to it's default state.
     */
    protected void resetCursor ()
    {
        if (_defaultCursor != null) {
            try {
                BCursor defaultCursor = BangUI.loadCursor("default");
                defaultCursor.setCursor(_defaultCursor);
                defaultCursor.show();
            } catch (Exception e) {
                // this should never happen
            }
        }
    }

    /** Used to visualize advance orders. */
    protected class AdvanceOrder
    {
        public int unitId, mx, my, targetId;

        public AdvanceOrder (UnitSprite unit, int mx, int my, int targetId)
        {
            unitId = unit.getPieceId();
            this.mx = mx;
            this.my = my;
            this.targetId = targetId;

            int x = mx, y = my;
            if (mx == Short.MAX_VALUE) {
                Piece target = _bangobj.pieces.get(targetId);
                if (target == null) {
                    target = _unit.getPiece();
                }
                x = target.x;
                y = target.y;
            }

            // mark our unit as having an advance order
            _unit = unit;
            _unit.setAdvanceOrder(targetId == -1 ?
                                  UnitSprite.AdvanceOrder.MOVE :
                                  UnitSprite.AdvanceOrder.MOVE_SHOOT);

            // denote our desired move location on the board
            if (mx != Short.MAX_VALUE) {
                _highlight = _tnode.createHighlight(x, y, true, true);
                _unit.setPendingNode(_highlight);
                _highlight.updateRenderState();
                _pnode.attachChild(_highlight);
            }

            // mark our attacker as targeted
            Piece target = _bangobj.pieces.get(targetId);
            if (target != null) {
                _target = getTargetableSprite(target);
                if (_target != null) {
                    _target.configureAttacker(_pidx, 1);
                }
            }
        }

        public void clear ()
        {
            _unit.setAdvanceOrder(UnitSprite.AdvanceOrder.NONE);
            if (_highlight != null) {
                _pnode.detachChild(_highlight);
                _unit.setPendingNode(null);
            }
            if (_target != null) {
                _target.configureAttacker(_pidx, -1);
            }
        }

        protected UnitSprite _unit;
        protected Targetable _target;
        protected TerrainNode.Highlight _highlight;
    }

    /** Listens for ticks and effects and does the right thing. */
    protected AttributeChangeListener _ticker = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(BangObject.TICK)) {
                ticked(_bangobj.tick);
            } else if (event.getName().equals(BangObject.EFFECT)) {
                applyEffect((Effect)event.getValue());
            }
        }
    };

    /** Used to remove unit sprites that have completed their death
     * paths. */
    protected ActiveSprite.ActionObserver _deadRemover =
        new ActiveSprite.ActionObserver() {
        public void actionCompleted (Sprite sprite, String action) {
            ActiveSprite asprite = (ActiveSprite)sprite;
            if (!asprite.isAnimating()) {
                log.info("Removing dead sprite post-fade " + asprite.getPiece() + ".");
                removeSprite(sprite);

                // let our unit status know if a unit just departed
                BangView bview = (BangView)getParent();
                if (sprite instanceof UnitSprite && bview.ustatus != null) {
                    bview.ustatus.unitRemoved((UnitSprite)sprite);
                }
            }
        }
    };

    protected BangContext _ctx;
    protected BangController _ctrl;
    protected BangConfig _bconfig;

    protected Node _cursor, _pointer;
    protected Unit _selection;

    /** The only valid location to attack from or null if you can attack from anywhere. */
    protected java.awt.Point _attackEnabled;

    protected PointSet _moveSet = new PointSet();
    protected PointSet _goalSet = new PointSet();
    protected PointSet _attackSet = new PointSet();

    protected int _pidx;
    protected int _downButton = -1;

    /** If we were around for the pre-select phase, we'll note that we did the
     * board tour then, otherwise we need to do it before we start. */
    protected boolean _haveDoneTour;

    protected int[] _action;
    protected Card _card;

    protected BWindow _pmarquees;
    protected SwingPath _tpath;

    protected ArrayList<UnitSprite> _readyUnits = new ArrayList<UnitSprite>();

    /** The old default mouse cursor. */
    protected Cursor _defaultCursor;

    /** Set to true if shift was down during the mouse press. */
    protected boolean _shiftDown = false;

    /** Texture used to show a unit's shot range. */
    protected TextureState _rngstate;

    /** A traversal predicate for units running to their initial positions. */
    protected AStarPathUtil.TraversalPred _tpred =
        new AStarPathUtil.TraversalPred() {
        public boolean canTraverse (Object traverser, int x, int y) {
            return _board.isGroundOccupiable(x, y, true);
        }
    };

    /** Tracks pieces that will be moving as soon as the board finishes
     * animating previous actions. */
    protected IntIntMap _pendmap = new IntIntMap();

    /** Tracks pieces in order for pending moves. */
    protected ArrayList<EffectHandler> _pmoves = new ArrayList<EffectHandler>();

    /** Tracks all pending advance orders. */
    protected HashMap<Integer,AdvanceOrder> _orders =
        new HashMap<Integer,AdvanceOrder>();

    /** Whether or not "high noon" mode is active. */
    protected boolean _highNoon;

    /** Whether or not wendigo ambiance mode is active. */
    protected boolean _wendigoAmbiance;

    /** The looping wendigo ambiance sound. */
    protected Sound _wendigoLoop;

    /** The marquee to display. */
    protected static enum MarqueeMode { NONE, GAME, ROUND };
    protected MarqueeMode _pendingMarquee = MarqueeMode.NONE;

    /** The color of BigShot movement highlights. */
    protected static final ColorRGBA BMOVE_HIGHLIGHT_COLOR =
        new ColorRGBA(0.5f, 1f, 0f, 0.5f);

    /** The color of the queued movement highlights. */
    protected static final ColorRGBA QMOVE_HIGHLIGHT_COLOR =
        new ColorRGBA(1f, 0.5f, 0.5f, 0.5f);

    /** The color of the attack range highlights. */
    protected static final ColorRGBA ATTACK_RANGE_COLOR =
        new ColorRGBA(1f, 0f, 0f, 0.5f);

    /** The color cyan. */
    protected static final ColorRGBA COLOR_CYAN =
        new ColorRGBA(0f, 1f, 1f, 1f);

    /** The duration of the board tour in seconds. */
    protected static final float BOARD_TOUR_DURATION = 10f;

    /** The time it takes to fade in and out of high noon mode. */
    protected static final float NOON_FADE_DURATION = 1f;

    /** The sound to play when a unit's order is invalidated. */
    protected static final String ORDER_INVALIDATED =
        "rsrc/sounds/effects/order_invalid.ogg";

    /** The sound to play when entering wendigo ambiance mode. */
    protected static final String WENDIGO_AMBIANCE_START =
        "rsrc/extras/indian_post/wendigo/ambiance_start.ogg";

    /** The sound to loop continuously while in wendigo ambiance mode. */
    protected static final String WENDIGO_AMBIANCE_LOOP =
        "rsrc/extras/indian_post/wendigo/ambiance_loop.ogg";
}
