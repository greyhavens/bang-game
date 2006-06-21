//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Dimension;
import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.shape.Quad;

import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Point;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;
import com.threerings.util.StreamablePoint;

import com.threerings.media.util.AStarPathUtil;
import com.threerings.media.util.MathUtil;

import com.threerings.jme.camera.CameraHandler;
import com.threerings.jme.camera.CameraPath;
import com.threerings.jme.camera.SwingPath;
import com.threerings.jme.effect.FadeInOutEffect;
import com.threerings.jme.model.Model;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;
import com.threerings.jme.util.LinearTimeFunction;
import com.threerings.jme.util.TimeFunction;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.client.Config;
import com.threerings.bang.client.bui.WindowFader;
import com.threerings.bang.client.util.ModelAttacher;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;
import com.threerings.bang.util.SoundUtil;

import com.threerings.bang.game.client.sprite.BonusSprite;
import com.threerings.bang.game.client.sprite.Bouncer;
import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.PropSprite;
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
import com.threerings.bang.game.util.ScenarioUtil;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

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
        addListener(this);
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
     * Called by the controller when we know one way or another regarding a
     * move and shoot request.
     */
    public void clearPendingShot (int targetId)
    {
        Piece piece = (Piece)_bangobj.pieces.get(targetId);
        if (piece != null) {
            getTargetableSprite(piece).setPendingShot(false);
        }
    }

    /**
     * Swings around the board and calls back to the controller to let it know
     * that everything is ready for the selection phase to start.
     */
    public void doPreSelectBoardTour ()
    {
        // make sure all elements are resolved
        if (_resolving > 0) {
            addResolutionObserver(new ResolutionObserver() {
                public void mediaResolved () {
                    doPreSelectBoardTour();
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
        Vector3f gpoint = camhand.getGroundPoint();
        float dx = (start.x + 0.5f) * TILE_SIZE - gpoint.x,
            dy = (start.y + 0.5f) * TILE_SIZE - gpoint.y;
        if (dx >= 0f && dy >= 0f) {
            camhand.orbitCamera(FastMath.HALF_PI);
        } else if (dx < 0f && dy >= 0f) {
            camhand.orbitCamera(FastMath.PI);
        } else if (dx < 0f && dy < 0f) {
            camhand.orbitCamera(-FastMath.HALF_PI);
        }
        camhand.panCameraAbs(dx, dy);
        Vector3f pan = camhand.getGroundPoint().subtractLocal(gpoint);
        camhand.panCameraAbs(-pan.x, -pan.y);

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
                camhand.resetGroundPointHeight();
                if (isAdded()) {
                    _ctrl.preSelectBoardTourComplete();
                }
                _tpath = null;
                return false;
            }
        });
    }

    /**
     * Fades in a "round over" marquee, fades out the board, sets up the fade
     * in for the new board and calls back to the controller to let it know
     * that everything is ready for the next round to start.
     */
    public void doInterRoundMarqueeFade ()
    {
        // create a marquee, but detach it
        createMarquee(_ctx.xlate(GameCodes.GAME_MSGS, "m.round_over"));
        _ctx.getInterface().detachChild(_marquee);

        // then reattach it with a fade in function
        TimeFunction tf = new LinearTimeFunction(0f, 1f, 2f);
        _ctx.getInterface().attachChild(
            new FadeInOutEffect(_marquee, ColorRGBA.white, tf, true) {
                protected void fadeComplete () {
                    super.fadeComplete();
                    // once we've faded in fully, attach it normally...
                    _ctx.getInterface().attachChild(_marquee);
                    // then clear it
                    clearMarquee(0f);
                    // let the controller know to show the stats page
                    _ctrl.interRoundMarqueeFadeComplete();
                }
            });
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
        // create a marquee, but detach it
        createMarquee(_ctx.xlate(GameCodes.GAME_MSGS, "m.game_over"));
        _ctx.getInterface().detachChild(_marquee);

        // then reattach it with a fade in function
        TimeFunction tf = new LinearTimeFunction(0f, 1f, 3f);
        _ctx.getInterface().attachChild(
            new FadeInOutEffect(_marquee, ColorRGBA.white, tf, true) {
                protected void fadeComplete () {
                    super.fadeComplete();
                    _ctrl.postGameFadeComplete();
                }
            });
    }

    /**
     * Fades a marquee in and out with the specified fade in and out time.
     */
    public void fadeMarqueeInOut (String message, final float halftime)
    {
        createMarquee(_ctx.xlate(GameCodes.GAME_MSGS, message));
        _ctx.getInterface().detachChild(_marquee);
        TimeFunction tf = new LinearTimeFunction(0f, 1f, halftime);
        _ctx.getInterface().attachChild(
            new FadeInOutEffect(_marquee, ColorRGBA.white, tf, true) {
                protected void fadeComplete () {
                    super.fadeComplete();
                    clearMarquee(halftime);
                }
            });
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
        return (piece instanceof Unit && piece.owner == _pidx && 
            piece.isAlive() && !(_pendmap.get(piece.pieceId) > 0 ||
            (psprite = getPieceSprite(piece)) == null || psprite.isMoving()));
    }

    /**
     * Notes that the specified piece will be moving when its action is up for
     * exection.
     */
    public void notePendingMove (int pieceId)
    {
//         Piece p = (Piece)_bangobj.pieces.get(pieceId);
//         if (p != null && p.owner == _pidx) {
//             log.info("Noting pending " + p.info());
//         }
        _pendmap.increment(pieceId, 1);
    }

    /**
     * Notes that the specified piece no longer has a pending move action.
     */
    public void clearPendingMove (int pieceId)
    {
//         Piece p = (Piece)_bangobj.pieces.get(pieceId);
//         if (p != null && p.owner == _pidx) {
//             log.info("Clearing pending " + p.info());
//         }
        _pendmap.increment(pieceId, -1);
    }

    // documentation inherited from interface MouseListener
    public void mouseClicked (MouseEvent e)
    {
        // nothing doing, we handle this ourselves
    }

    // documentation inherited from interface MouseListener
    public void mousePressed (MouseEvent e)
    {
        // skip to the end of the board tour path, if active
        if (_tpath != null) {
            _ctx.getCameraHandler().skipPath();
        }

        switch (_downButton = e.getButton()) {
        case MouseEvent.BUTTON2:
            handleRightPress(e.getX(), e.getY());
            break;

        case MouseEvent.BUTTON1:
            handleLeftPress(e.getX(), e.getY());
            break;
        }
    }

    // documentation inherited from interface MouseListener
    public void mouseReleased (MouseEvent e)
    {
        if (_downButton == MouseEvent.BUTTON3) {
            clearAttackSet();
        }
        _downButton = -1;
    }

    // documentation inherited from interface MouseListener
    public void mouseEntered (MouseEvent e)
    {
        // nada
    }

    // documentation inherited from interface MouseListener
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

        // preload our sounds for this scenario
        ScenarioUtil.preloadSounds(bangobj.scenarioId, _sounds);

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
        // drop rendering to 1 FPS while we load the models
        final long start = System.currentTimeMillis();
        // final int oldFPS = _ctx.getApp().setTargetFPS(1);
        _ctx.getApp().setEnabled(true, false);

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
    protected void wasAdded ()
    {
        super.wasAdded();

        // create our cursor
        _cursor = new Node("cursor");
        _cursor.setLocalTranslation(new Vector3f(0, 0, TILE_SIZE));
        // _cursor.setLocalScale(0.75f);
        _ctx.loadModel("bonuses", "frontier_town/bonus_point",
                       new ModelAttacher(_cursor));
        _cursor.addController(new Spinner(_cursor, FastMath.PI));
        _cursor.addController(new Bouncer(_cursor, TILE_SIZE, TILE_SIZE/4));
        _cursor.setRenderState(RenderUtil.lequalZBuf);
        _cursor.updateRenderState();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // stop the board tour if it's still going
        if (_tpath != null) {
            _ctx.getCameraHandler().moveCamera(null);
        }
    }

    @Override // documentation inherited
    protected boolean shouldShowSky ()
    {
        return false;
    }
    
    @Override // documentation inherited
    protected void createMarquee (String text)
    {
        super.createMarquee(text);
        if (_bangobj.state != BangObject.PRE_GAME &&
            _bangobj.state != BangObject.SELECT_PHASE) {
            return;
        }

        // create marquees with the avatars of all the participants
        _pmarquees = new BWindow(
            _ctx.getStyleSheet(), GroupLayout.makeHStretch()) {
            public boolean isOverlay () {
                return true;
            }
            public BComponent getHitComponent (int mx, int my) {
               return null;
            }
        };

        // set up two rows with two slots each that will hold our marquee bits
        GroupLayout layout = GroupLayout.makeVert(GroupLayout.CENTER);
        layout.setGap(30);
        BContainer info = new BContainer(layout);
        BContainer[] cols = new BContainer[2];
        _pmarquees.add(cols[0] = new BContainer(layout));
        _pmarquees.add(info);
        _pmarquees.add(cols[1] = new BContainer(layout));

        // create avatar displays for each player in the game
        for (int ii = 0; ii < _bangobj.players.length; ii++) {
            cols[ii%2].add(createPlayerMarquee(ii));
        }

        // add some additional information in the center colum
        String msg = MessageBundle.compose(
            "m.marquee_header",
            MessageBundle.taint(String.valueOf((_bangobj.roundId + 1))),
            "m.scenario_" + _bangobj.scenarioId);
        info.add(new BLabel(_ctx.xlate(GameCodes.GAME_MSGS, msg),
                            "marquee_subtitle"));
        info.add(new Spacer(25, 100));
        if (_bconfig.rated) {
            info.add(new BLabel(_ctx.xlate(GameCodes.GAME_MSGS, "m.ranked"),
                                "marquee_subtitle"));
        }

        // create and add the marquees and the whole window
        _pmarquees.setBounds(0, 0, _ctx.getDisplay().getWidth(),
                             _ctx.getDisplay().getHeight());
        _ctx.getRootNode().addWindow(_pmarquees);
    }

    /**
     * Creates and returns a player view for the opening marquee.
     */
    protected BContainer createPlayerMarquee (int pidx)
    {
        // create the marquee
        BContainer marquee = new BContainer(
            GroupLayout.makeVert(GroupLayout.CENTER));
        ((GroupLayout)marquee.getLayoutManager()).setGap(-1);
        marquee.setStyleClass("player_marquee_cont");
        if (_bangobj.avatars[pidx] != null) {
            AvatarView aview = new AvatarView(_ctx, 2, false, false);
            aview.setAvatar(_bangobj.avatars[pidx]);
            marquee.add(aview);
        }
        marquee.add(new BLabel(_bangobj.players[pidx].toString(),
                               "player_marquee_label"));
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
    protected void hoverSpriteChanged (Sprite hover)
    {
        // if we were hovering over a unit, clear its hover state
        if (_hover instanceof UnitSprite) {
            ((UnitSprite)_hover).setHovered(false);
        }

        super.hoverSpriteChanged(hover);

        // if we're hovering over a unit we can click, mark it as such
        if (hover instanceof UnitSprite) {
            UnitSprite usprite = (UnitSprite)hover;
            Piece piece = usprite.getPiece();
            if (piece.isAlive() && (isSelectable(piece) ||
                                    _attackSet.contains(piece.x, piece.y))) {
                usprite.setHovered(true);
            }
        }

        // display contextual help on units and other special sprites
        _tiptext = null;
        if (hover instanceof PieceSprite) {
            String item = ((PieceSprite)hover).getHelpIdent(_pidx);
            if (item != null) {
                if (item.startsWith("unit_")) {
                    String type = item.substring(5);
                    String msg = MessageBundle.compose(
                        "m.unit_icon", UnitConfig.getName(type),
                        UnitConfig.getTip(type));
                    _tiptext = _ctx.xlate(BangCodes.UNITS_MSGS, msg);
                } else {
                    item = "m.help_" + item;
                    String msg = MessageBundle.compose(
                        "m.help_tip", item + "_title", item);
                    _tiptext = _ctx.xlate(GameCodes.GAME_MSGS, msg);
                }
            }
            Piece piece;
            if (_card != null && 
                    _card.getPlacementMode() == Card.PlacementMode.VS_PIECE) {
                piece = ((PieceSprite)hover).getPiece();
                clearHighlights();
                _attackSet.clear();
                _attackSet.add(piece.x, piece.y);
                targetTiles(_attackSet, _card.isValidPiece(_bangobj, piece));
            }

        // If we just stopped hovering over a sprite, possibly update the tile
        // location for a card placement
        } else if (_card != null &&
                _card.getPlacementMode() == Card.PlacementMode.VS_PIECE) {
            updatePlacingCard(_mouse.x, _mouse.y);
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
        checkForSelectionInfluence(piece);

        // if this is a unit, we need to tell the unit status view and,
        // if it's before the first tick, reposition the unit so it can
        // run in from off the board
        if (!(piece instanceof Unit)) {
            return;
        }

        // let the unit status view know that a unit was added
        UnitSprite sprite = getUnitSprite(piece);
        BangView bview = (BangView)getParent();
        if (bview.ustatus != null) {
            bview.ustatus.unitAdded(sprite);
        }

        // if we've not yet started the game, place the unit at the corner of
        // the board nearest to the player's start position because we're going
        // to run them to the player's starting location once their animations
        // are resolved
        if (!_bconfig.tutorial && _bangobj.tick < 0) {
            Point corner = getStartCorner(piece.owner);
            sprite.setLocation(_board, corner.x, corner.y);
        }
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
        _ctx.getApp().setEnabled(true, true);
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
        for (UnitSprite sprite : _readyUnits) {
            Piece unit = sprite.getPiece();
            Point corner = getStartCorner(unit.owner);
            final List path = AStarPathUtil.getPath(
                _tpred, unit.getStepper(), unit, _board.getWidth() / 2,
                corner.x, corner.y, unit.x, unit.y, false);

            // strip off all but the last location that is not visible given
            // the current camera position
            if (path != null) {
                int startidx = path.size();
                for (int ii = path.size()-1; ii >= 0; ii--) {
                    java.awt.Point point = (java.awt.Point)path.get(ii);
                    sprite.setLocation(_board, point.x, point.y);
                    sprite.updateGeometricState(0, true);
                    startidx = ii;
                    int state = camera.getPlaneState();
                    int rv = camera.contains(sprite.getWorldBound());
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
            if (path == null || path.size() == 0) {
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
            delay += 150L;
        }
        _readyUnits.clear();

        // tell the controller to report back to the server that we're ready;
        // but give the units another half a second to arrive
        delay += 500L;
        new Interval(_ctx.getApp()) {
            public void expired () {
                _ctrl.readyForRound();
            }
        }.schedule(delay);
    }

    /**
     * Returns the corner of the board nearest to the specified player's start
     * position.
     */
    protected Point getStartCorner (int pidx)
    {
        StreamablePoint pt = _bangobj.startPositions[pidx];
        return new Point(
            (pt.x < _board.getWidth() / 2) ? 0 : _board.getWidth() - 1,
            (pt.y < _board.getHeight() / 2) ? 0 : _board.getHeight() - 1);
    }

    /** Called by the {@link EffectHandler} when a piece was affected. */
    protected void pieceWasAffected (Piece piece, String effect)
    {
        // just report the effect to the controller in case the tutorial cares
        _ctrl.postEvent(TutorialCodes.EFFECT_PREFIX + effect);
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
            checkForSelectionInfluence(piece);
        }

        // let the controller know that a unit moved
        _ctrl.postEvent(TutorialCodes.UNIT_MOVED);
    }

    /** Called by the {@link EffectHandler} when a piece was killed. */
    protected void pieceWasKilled (int pieceId)
    {
        // sanity check
        if (_pendmap.get(pieceId) > 0) {
            log.warning("Piece with pending actions killed [id=" + pieceId +
                        ", pendcount=" + _pendmap.get(pieceId) + "].");
            Thread.dumpStack();
            _pendmap.put(pieceId, 0);
        }

        // clear out any advance order for this piece
        clearAdvanceOrder(pieceId);

        // if this piece was selected, clear it
        if (_selection != null && _selection.pieceId == pieceId) {
            clearSelection();
        }

        // report that a piece was killed
        _ctrl.postEvent(TutorialCodes.UNIT_KILLED);
    }

    /** Handles a left mouse button click. */
    protected void handleLeftPress (int mx, int my)
    {
        // nothing doing if the game is not in play or we're not a player
        if (_pidx == -1 || _bangobj == null || !_bangobj.isInteractivePlay()) {
            return;
        }

        // check for a piece under the mouse
        PieceSprite sprite = null;
        Piece piece = null;
        if (_hover instanceof Targetable) {
            sprite = (PieceSprite)_hover;
            piece = (Piece)_bangobj.pieces.get(sprite.getPieceId());
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
                if (_hover != null && _hover instanceof PieceSprite) {
                    Piece p = ((PieceSprite)_hover).getPiece();
                    if (_card.isValidPiece(_bangobj, p)) {
                        log.info("Activating " + _card);
                        target = new Integer(piece.pieceId);
                    }
                }
                break;
            }

            if (target != null) {
                _ctrl.activateCard(_card.cardId, target);
                _card = null;
                clearAttackSet();
            }
            return;
        }

        // select the piece under the mouse if it meets our sundry conditions
        if (piece != null &&  sprite != null && isSelectable(piece)) {
            selectUnit((Unit)piece, false);
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

            if (handleClickToMove(_high.x, _high.y)) {
                return;
            }
        }
    }

    /** Handles a click that should select a potential move location. */
    protected boolean handleClickToMove (int tx, int ty)
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
        PointSet moves = new PointSet();
        moves.add(tx, ty);
        highlightTiles(moves, getHighlightColor(_selection));

        if (_attackEnabled) {
            // display our potential attacks
            _attackSet.clear();
            pruneAttackSet(moves, _attackSet);
        }

        // if there are no valid attacks, assume they're just moving (but
        // do nothing if they're not even moving)
        if (_attackSet.size() == 0 &&
            (_action[1] != _selection.x || _action[2] != _selection.y)) {
            executeAction();
            _attackSet.clear();
        } else {
            log.info("Waiting for attack selection (" + tx + ", " + ty + ")");
        }
        return true;
    }

    /** Handles a click that indicates the coordinates of a piece we'd
     * like to attack. */
    protected boolean handleClickToAttack (Piece piece, int tx, int ty)
    {
        if (piece != null) {
            log.info("Clicking to attack " + piece.info());
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
            if (_attackSet.size() > 0) {
                // select a random target and request to shoot it
                int idx = RandomUtil.getInt(_attackSet.size());
                Piece target = _bangobj.getPlayerPiece(
                    _attackSet.getX(idx), _attackSet.getY(idx));
                // TODO: prefer targets that are guaranteed to be there
                // when we make our move versus those that may be gone
                if (target != null && _selection.validTarget(target, false)) {
                    log.info("Randomly targeting " + target.info());
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

    /**
     * Scans all pieces to which we can compute a valid firing location, adds
     * their location to the supplied destination set, and marks their sprite
     * as being targeted.
     */
    protected void pruneAttackSet (PointSet moves, PointSet dest)
    {
        for (Piece p : _bangobj.pieces) {
            if (!_selection.validTarget(p, false) ||
                _selection.computeShotLocation(
                    _board, p, moves, true) == null) {
                continue;
            }

            Targetable target = getTargetableSprite(p);
            if (target != null) {
                target.setTargeted(_selection.lastActed >= p.lastActed ?
                                   Targetable.TargetMode.MAYBE :
                                   Targetable.TargetMode.SURE_SHOT,
                                   (Unit)_selection);
                dest.add(p.x, p.y);
            } else {
                log.warning("No sprite for unit! [unit=" + p + "].");
            }
        }
    }

    /** Handles a right mouse button click. */
    protected void handleRightPress (int mx, int my)
    {
        // nothing doing if the game is not in play
        if (_bangobj == null || !_bangobj.isInteractivePlay()) {
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
        boolean deselect = (piece == _selection);
        clearSelection();
        if (deselect || !piece.isAlive()) {
            return;
        }
        _selection = piece;

//         log.info("Selecting " + _selection.info());

        // select the sprite and center it in the view
        PieceSprite sprite = getPieceSprite(_selection);
        if (sprite == null) {
            return; // we might still be setting up
        }
        sprite.setSelected(true);
        sprite.attachChild(_cursor);
        if (scrollCamera) {
            centerCameraOnPiece(_selection);
        }

        // highlight our potential moves and attackable pieces
        _bangobj.board.computeMoves(piece, _moveSet, null);
        if (_attackEnabled) {
            _attackSet.clear();
            pruneAttackSet(_moveSet, _attackSet);
        }
        highlightTiles(_moveSet, getHighlightColor(piece));

        // report that the user took an action (for tutorials)
        _ctrl.postEvent(TutorialCodes.UNIT_SELECTED);
    }

    protected boolean checkForSelectionInfluence (Piece piece)
    {
        if (_selection == null) {
            return false;
        }

        // refresh our selection
        int[] oaction = _action;
        Unit oselection = (Unit)_selection;
        clearSelection();
        selectUnit(oselection, false);

        // if we had already selected a movement, reconfigure that (it might no
        // longer be valid but handleClickToMove will ignore us in that case
        if (oaction != null) {
            log.info("Reissuing click to move +" + oaction[1] +
                "+" + oaction[2]);
            handleClickToMove(oaction[1], oaction[2]);
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
            log.warning("Missing piece for advance order [id=" + unitId + "].");
            return;
        }
        UnitSprite sprite = getUnitSprite(actor);
        if (sprite == null) {
            log.warning("Missing sprite for advance order [p=" + actor + "].");
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
                        Targetable.TargetMode.NONE, null);
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
            _ctrl.postEvent(TutorialCodes.UNIT_DESELECTED);
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
            _bangobj.board.computeAttacks(
                0, _card.getRadius(), tx, ty, _attackSet);
            if (_bangobj.board.getPlayableArea().contains(tx, ty)) {
                _attackSet.add(tx, ty);
            }
            targetTiles(_attackSet, _card.isValidLocation(_bangobj, tx, ty));
            break;

          case VS_PIECE:
            // if we're hovering over a piece it will get set properly in
            // hoverSpriteChanged()
            if (_hover == null) {
                clearHighlights();
                _attackSet.clear();
                if (_bangobj.board.getPlayableArea().contains(tx, ty)) {
                    _attackSet.add(tx, ty);
                    targetTiles(_attackSet, false);
                }
            }
        }
    }

    protected boolean clearPlacingCard ()
    {
        if (_card == null) {
            return false;
        }

        log.info("Clearing " + _card);
        _card = null;
        clearAttackSet();
        return true;
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
        if (sprite instanceof MobileSprite) {
            MobileSprite msprite = (MobileSprite)sprite;
            // if this mobile sprite is animating it will have to wait
            // until it's finished before being removed
            if (msprite.isAnimating()) {
                _pieces.remove(pieceId);
                msprite.addObserver(_deadRemover);
                msprite.queueAction(MobileSprite.REMOVED);
                return sprite;
            } else {
                log.info("Removing dead unit sprite immediately " +
                         msprite.getPiece() + ".");
            }

        } else if (sprite instanceof BonusSprite) {
            // if this was a bonus, note that it was activated
            _ctrl.postEvent(TutorialCodes.BONUS_ACTIVATED);
        }

        return super.removePieceSprite(pieceId, why);
    }

    /**
     * Called every time the board ticks.
     */
    protected void ticked (short tick)
    {
        // update all of our sprites
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece piece = (Piece)iter.next();
            if (piece instanceof Unit) {
                UnitSprite usprite = getUnitSprite(piece);
                if (usprite != null) {
                    usprite.updated(piece, tick);
                }
            }
        }
    }

    /**
     * Called when an effect is applied to the board.
     */
    protected void applyEffect (Effect effect)
    {
        log.info("Applying effect " + effect + ".");
        EffectHandler handler = effect.createHandler(_bangobj);
        if (handler != null) {
            handler.init(_ctx, _bangobj, this, _sounds, effect);
            executeAction(handler);
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
                Piece target = (Piece)_bangobj.pieces.get(targetId);
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
            Piece target = (Piece)_bangobj.pieces.get(targetId);
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
    protected MobileSprite.ActionObserver _deadRemover =
        new MobileSprite.ActionObserver() {
        public void actionCompleted (Sprite sprite, String action) {
            if (action.equals(MobileSprite.REMOVED)) {
                if (((MobileSprite)sprite).isMoving()) {
                    log.warning("Removing dead sprite, but it's still moving " +
                                ((MobileSprite)sprite).getPiece() + ".");
                } else {
                    log.info("Removing dead unit sprite post-fade " +
                             ((MobileSprite)sprite).getPiece() + ".");
                }
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

    protected Node _cursor;
    protected Piece _selection;

    /** This is disabled by the tutorial controller when the player is not
     * allowed to attack during a tutorial. */
    protected boolean _attackEnabled = true;

    protected PointSet _moveSet = new PointSet();
    protected PointSet _attackSet = new PointSet();

    protected int _pidx;
    protected int _downButton = -1;

    protected int[] _action;
    protected Card _card;

    protected BWindow _pmarquees;
    protected SwingPath _tpath;

    protected ArrayList<UnitSprite> _readyUnits = new ArrayList<UnitSprite>();

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

    /** Tracks all pending advance orders. */
    protected HashMap<Integer,AdvanceOrder> _orders =
        new HashMap<Integer,AdvanceOrder>();

    /** The color of BigShot movement highlights. */
    protected static final ColorRGBA BMOVE_HIGHLIGHT_COLOR =
        new ColorRGBA(0.5f, 1f, 0f, 0.5f);

    /** The color of the queued movement highlights. */
    protected static final ColorRGBA QMOVE_HIGHLIGHT_COLOR =
        new ColorRGBA(1f, 0.5f, 0.5f, 0.5f);

    /** The duration of the board tour in seconds. */
    protected static final float BOARD_TOUR_DURATION = 10f;
}
