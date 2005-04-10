//
// $Id$

package com.samskivert.bang.client;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.samskivert.swing.Label;
import com.samskivert.util.StringUtil;

import com.threerings.media.animation.SpriteAnimation;
import com.threerings.media.sprite.LabelSprite;
import com.threerings.media.sprite.PathObserver;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.util.LinePath;
import com.threerings.media.util.MathUtil;
import com.threerings.media.util.Path;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.toybox.data.ToyBoxGameConfig;
import com.threerings.toybox.util.ToyBoxContext;

import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.client.sprite.ShotSprite;
import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.effect.Effect;
import com.samskivert.bang.data.effect.ShotEffect;
import com.samskivert.bang.data.piece.BigPiece;
import com.samskivert.bang.data.piece.Chopper;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.data.surprise.Surprise;
import com.samskivert.bang.util.PieceSet;
import com.samskivert.bang.util.PointSet;
import com.samskivert.bang.util.VisibilityState;

import static com.samskivert.bang.Log.log;
import static com.samskivert.bang.client.BangMetrics.*;

/**
 * Displays the main game board.
 */
public class BangBoardView extends BoardView
    implements MouseListener, MouseMotionListener
{
    public BangBoardView (ToyBoxContext ctx)
    {
        super(ctx);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    /**
     * Requests that the specified surprise be enabled for placement. The
     * area of effect of the surprise will be rendered around the cursor
     * and a left click will activate the surprise at the specified
     * coordinates while a right click will cancel the placement.
     */
    public void placeSurprise (Surprise s)
    {
        // clear any current selection
        clearSelection();

        // set up the display of our surprise attack set
        _surprise = s;
        _attackSet = new PointSet();
        _bangobj.board.computeAttacks(
            _surprise.getRadius(), _mouse.x, _mouse.y, _attackSet);
        dirtySet(_attackSet);
        log.info("Placing " + _surprise);
    }

    // documentation inherited from interface MouseListener
    public void mouseClicked (MouseEvent e)
    {
        // nothing doing, we handle this ourselves
    }

    // documentation inherited from interface MouseListener
    public void mousePressed (MouseEvent e)
    {
        switch (_downButton = e.getButton()) {
        case MouseEvent.BUTTON3:
            // button 3 (right button) creates or extends a path
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

    // documentation inherited from interface MouseMotionListener
    public void mouseMoved (MouseEvent e)
    {
        int mx = e.getX() / SQUARE, my = e.getY() / SQUARE;
        updateMouseTile(mx, my);
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseDragged (MouseEvent e)
    {
        // first update our mousely business
        mouseMoved(e);
    }

    @Override // documentation inherited
    public void keyPressed (KeyEvent e)
    {
        super.keyPressed(e);
    }

    @Override // documentation inherited
    public void startGame (BangObject bangobj, ToyBoxGameConfig cfg, int pidx)
    {
        super.startGame(bangobj, cfg, pidx);

        _pidx = pidx;
        _bangobj.addListener(_ticker);

        // set up the starting visibility if we're using it
        if ((Boolean)cfg.params.get("fog")) {
            _vstate = new VisibilityState(_bbounds.width, _bbounds.height);
            adjustBoardVisibility();
            adjustEnemyVisibility();
        }
    }

    @Override // documentation inherited
    public void endGame ()
    {
        super.endGame();
        clearSelection();

        // remove our event listener
        _bangobj.removeListener(_ticker);

        // allow everything to be visible
        if (_vstate != null) {
            _vstate.reveal();
            adjustEnemyVisibility();
            dirtyScreenRect(new Rectangle(0, 0, getWidth(), getHeight()));
        }
    }

    @Override // documentation inherited
    protected void paintMouseTile (Graphics2D gfx, int mx, int my)
    {
        // only highlight the mouse coordinates while we're in play
        if (_bangobj != null && _bangobj.isInPlay()) {
            super.paintMouseTile(gfx, mx, my);
        }
    }

    @Override // documentation inherited
    protected void paintInFront (Graphics2D gfx, Rectangle dirtyRect)
    {
        super.paintInFront(gfx, dirtyRect);

        // render our possible moves
        if (_moveSet.size() > 0) {
            renderSet(gfx, dirtyRect, _moveSet, _whiteRenderer);
        }

        // render the necessary tiles as dimmed if it is not "visible"
        if (_board != null && _vstate != null) {
            Composite ocomp = gfx.getComposite();
            gfx.setComposite(SET_ALPHA);
            gfx.setColor(Color.black);
            _pr.setLocation(0, 0);
            for (int yy = 0, hh = _board.getHeight(); yy < hh; yy++) {
                _pr.x = 0;
                int xoff = yy * _board.getWidth();
                for (int xx = 0, ww = _board.getWidth(); xx < ww; xx++) {
                    if (!_vstate.getVisible(xoff+xx) &&
                        dirtyRect.intersects(_pr)) {
                        gfx.fill(_pr);
                    }
                    _pr.x += SQUARE;
                }
                _pr.y += SQUARE;
            }
            gfx.setComposite(ocomp);
        }
    }

    /** Handles a left mouse button click. */
    protected void handleLeftPress (int mx, int my)
    {
        int tx = mx / SQUARE, ty = my / SQUARE;

        // nothing doing if the game is not in play or we're not a player
        if (_bangobj == null || !_bangobj.isInPlay() || _pidx == -1) {
            return;
        }

        // check for a piece under the mouse
        PieceSprite sprite = null;
        Piece piece = null;
        Sprite s = _spritemgr.getHighestHitSprite(mx, my);
        if (s instanceof PieceSprite) {
            sprite = (PieceSprite)s;
            piece = (Piece)_bangobj.pieces.get(sprite.getPieceId());
            // we currently don't do anything with non-player pieces
            if (piece != null && piece.owner == -1) {
                sprite = null;
                piece = null;
            }
        }

        // if we are placing a surprise, activate it
        if (_surprise != null) {
            log.info("activating " + _surprise);
            BangController.postAction(
                this, BangController.ACTIVATE_SURPRISE,
                new int[] { _surprise.surpriseId, tx, ty });
            _surprise = null;
            clearAttackSet();
            return;
        }

        // if we have a selection
        if (_selection != null) {
            // and we have an attack set
            if (_attackSet != null) {
                // and we are clicking a piece to be attacked
                if (_attackSet.contains(tx, ty) &&
                    piece != null && piece.owner != _pidx) {
                    // note the piece we desire to fire upon
                    _action[3] = piece.pieceId;
                    executeAction();
                    return;

                } else if (tx == _action[1] && ty == _action[2]) {
                    // or if we're clicking a second time on our desired
                    // move location, just move there and don't attack
                    executeAction();
                    return;
                }
            }

            // or if we're clicking in our move set or on our selected piece
            if ((_moveSet.size() > 0 && _moveSet.contains(tx, ty)) ||
                _selection.x == tx && _selection.y == ty) {
                // store the coordinates toward which we wish to move
                _action = new int[] { _selection.pieceId, tx, ty, -1 };

                // clear any previous attack set
                clearAttackSet();

                // display our potential attacks
                _attackSet = new PointSet();
                PointSet attacks = new PointSet();
                _bangobj.board.computeAttacks(
                    _selection.getFireDistance(), tx, ty, attacks);
                for (Iterator iter = _bangobj.pieces.iterator();
                     iter.hasNext(); ) {
                    Piece p = (Piece)iter.next();
                    if (_selection.validTarget(p) &&
                        attacks.contains(p.x, p.y)) {
                        _attackSet.add(p.x, p.y);
                    }
                }

                // if there are no valid attacks, assume they're just moving
                if (_attackSet.size() == 0) {
                    executeAction();
                    _attackSet = null;
                } else {
                    dirtySet(_attackSet);
                }
                return;
            }
        }

        // select the piece under the mouse if it meets our various and
        // sundry conditions
        if (piece != null &&  sprite != null && piece.owner == _pidx &&
            sprite.isSelectable()) {
            selectPiece(piece);
        }
    }

    /** Handles a right mouse button click. */
    protected void handleRightPress (int mx, int my)
    {
        int tx = mx / SQUARE, ty = my / SQUARE;

        // nothing doing if the game is not in play
        if (_bangobj == null || !_bangobj.isInPlay()) {
            return;
        }

        // if we are placing a surprise, clear it out
        if (_surprise != null) {
            log.info("Clearing " + _surprise);
            _surprise = null;
            clearAttackSet();
            return;
        }

        // if there is a piece under the cursor, show their possible shots
        PieceSprite sprite = null;
        Sprite s = _spritemgr.getHighestHitSprite(mx, my);
        if (s instanceof PieceSprite) {
            sprite = (PieceSprite)s;
            Piece piece = (Piece)_bangobj.pieces.get(sprite.getPieceId());
            if (sprite.isSelectable() && piece.isAlive()) {
//                 _attackSet = piece.getAttacks(_bangobj.board);
                _remgr.invalidateRegion(_vbounds);
            }
        }
    }

    protected void selectPiece (Piece piece)
    {
        boolean deselect = (piece == _selection);
        clearSelection();
        if (!deselect && piece.isAlive()) {
            _selection = piece;
            getPieceSprite(_selection).setSelected(true);
            if (_moveSet.size() > 0) {
                dirtySet(_moveSet);
                _moveSet.clear();
            }
            _bangobj.board.computeMoves(piece, _moveSet);
            dirtySet(_moveSet);
        }
    }

    protected void executeAction ()
    {
        // enact the move/fire combination
        BangController.postAction(this, BangController.MOVE_AND_FIRE, _action);
        // and clear everything out
        clearSelection();
    }

    protected void clearSelection ()
    {
        if (_selection != null) {
            getPieceSprite(_selection).setSelected(false);
            _selection = null;
            dirtySet(_moveSet);
            _moveSet.clear();
        }

        // clear out any pending action
        _action = null;
        clearAttackSet();
    }

    @Override // documentation inherited
    protected boolean updateMouseTile (int mx, int my)
    {
        if (super.updateMouseTile(mx, my)) {
            // if we have an active surprise, update its area of effect
            if (_surprise != null) {
                dirtySet(_attackSet);
                _attackSet.clear();
                _bangobj.board.computeAttacks(
                    _surprise.getRadius(), mx, my, _attackSet);
                dirtySet(_attackSet);
            }
            return true;
        }
        return false;
    }

    @Override // documentation inherited
    protected void pieceUpdated (Piece opiece, Piece npiece)
    {
        super.pieceUpdated(opiece, npiece);

        // clear and reselect if this piece was the selection and it moved
        if (npiece != null && _selection != null &&
            _selection.pieceId == npiece.pieceId &&
            (_selection.x != npiece.x || _selection.y != npiece.y)) {
            clearSelection();
            selectPiece(npiece);
        }

        // update board and enemy visibility
        if (_vstate != null) {
            adjustBoardVisibility();
            adjustEnemyVisibility();
        }
    }

    /**
     * Called every time the board ticks.
     */
    protected void ticked (short tick)
    {
        // update all of our piece sprites
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            PieceSprite sprite = _pieces.get(p.pieceId);
            if (sprite == null) {
                continue;
            }
            sprite.updated(p, tick);
        }
    }

    /**
     * Called when an effect is applied to the board.
     */
    protected void applyEffect (Effect effect)
    {
        if (effect instanceof ShotEffect) {
            new ShotHandler((ShotEffect)effect);
        } else {
            effect.apply(_bangobj, _effector);
        }
    }

    /** Adjusts the visibility settings for the tiles of the board. */
    protected void adjustBoardVisibility ()
    {
        // if we're out of the game, just reveal everything
        if (!_bangobj.hasLivePieces(_pidx)) {
            _vstate.reveal();
            dirtyScreenRect(new Rectangle(0, 0, getWidth(), getHeight()));
            return;
        }

        // swap our visibility state to the fresh set
        _vstate.swap();

        // update the board visibility based on our piece's new position
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece piece = (Piece)iter.next();
            if (piece.owner == -1 || (_pidx != -1 && _pidx != piece.owner)) {
                continue; // skip non-player pieces in this pass
            }

            int dist = piece.getSightDistance(), dist2 = dist * dist;
            Rectangle rect = new Rectangle(
                piece.x - dist, piece.y - dist, 2*dist+1, 2*dist+1);
            rect = rect.intersection(
                new Rectangle(0, 0, _board.getWidth(), _board.getHeight()));
            for (int yy = rect.y, ly = yy + rect.height; yy < ly; yy++) {
                for (int xx = rect.x, lx = xx + rect.width; xx < lx; xx++) {
                    int tdist = MathUtil.distanceSq(xx, yy, piece.x, piece.y);
                    if (tdist < dist2) {
                        _vstate.setVisible(xx, yy);
                    }
                }
            }
        }

        // now dirty any tiles whose visibility changed
        for (int yy = 0, ly = _board.getHeight(); yy < ly; yy++) {
            for (int xx = 0, lx = _board.getHeight(); xx < lx; xx++) {
                if (_vstate.visibilityChanged(xx, yy)) {
                    dirtyTile(xx, yy);
                }
            }
        }
    }

    /** Makes enemy pieces visible or invisible based on _vstate. */
    protected void adjustEnemyVisibility ()
    {
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece piece = (Piece)iter.next();
            if (piece.owner == -1 || (_pidx == -1 || _pidx == piece.owner)) {
                continue; // skip unowned and player pieces in this pass
            }

            PieceSprite sprite = _pieces.get(piece.pieceId);
            if (sprite != null) {
                boolean viz = _vstate.getVisible(piece.x, piece.y);
                if (viz && !isManaged(sprite)) {
                    sprite.updated(piece, _bangobj.tick);
                    addSprite(sprite);
                } else if (!viz && isManaged(sprite)) {
                    removeSprite(sprite);
                }
            }
        }
    }

    /** Called to display something useful when an effect is applied. */
    protected void createEffectAnimation (Piece piece, String effect)
    {
        // currently just update the piece in question immediately
        Piece opiece = (Piece)_bangobj.pieces.get(piece.pieceId);
        pieceUpdated(opiece, piece);

        // and create a simple label animation naming the effect
        Label label = new Label(effect);
        label.setTextColor(Color.white);
        label.setAlternateColor(Color.black);
        label.setStyle(Label.OUTLINE);
        LabelSprite lsprite = new LabelSprite(label);
        lsprite.setRenderOrder(100);
        int px = piece.x * SQUARE, py = piece.y * SQUARE;
        LinePath path = new LinePath(px, py, px, py - 25, 1000L);
        addAnimation(new SpriteAnimation(_spritemgr, lsprite, path));
    }

    /** Waits for all sprites involved in a shot to stop moving and then
     * animates the fired shot. */
    protected class ShotHandler
        implements PathObserver
    {
        public ShotHandler (ShotEffect shot) {
            _shot = shot;
            _shooter = (Piece)_bangobj.pieces.get(shot.shooterId);
            if (_shooter == null) {
                log.warning("Missing shooter? [shot=" + shot + "].");
                // abandon ship, we're screwed
                return;
            }
            _target = (Piece)_bangobj.pieces.get(shot.targetId);
            if (_target == null) {
                log.warning("Missing target? [shot=" + shot + "].");
                // abandon ship, we're screwed
                return;
            }

            // figure out which sprites we need to wait for
            considerPiece(_shooter);
            considerPiece(_target);

            // if no one was managed, it's a shot fired from an invisible
            // piece at invisible pieces, ignore it
            if (_managed == 0) {
                log.info("Tree feel in the woods, no one was around.");

            } else if (_sprites == 0) {
                // if we're not waiting for any sprites to finish moving,
                // fire the shot immediately
                fireShot();
            }
        }

        public void pathCompleted (Sprite sprite, Path path, long when) {
            sprite.removeSpriteObserver(this);
            if (sprite == _ssprite) {
                applyShot();
                removeSprite(sprite);
            } else if (--_sprites == 0) {
                fireShot();
            }
        }

        public void pathCancelled (Sprite sprite, Path path) {
            sprite.removeSpriteObserver(this);
            if (sprite == _ssprite) {
                applyShot();
                removeSprite(sprite);
            } else if (--_sprites == 0) {
                fireShot();
            }
        }

        protected void considerPiece (Piece piece) {
            PieceSprite sprite = null;
            if (piece != null) {
                sprite = _pieces.get(piece.pieceId);
            }
            if (sprite == null) {
                return;
            }
            if (isManaged(sprite)) {
                _managed++;
                if (sprite.isMoving()) {
                    sprite.addSpriteObserver(this);
                    _sprites++;
                }
            }
        }

        protected void fireShot ()
        {
            _ssprite = new ShotSprite();
            int sx = _shooter.x * SQUARE + SQUARE/2;
            int sy = _shooter.y * SQUARE + SQUARE/2;
            int tx = _target.x * SQUARE + SQUARE/2;
            int ty = _target.y * SQUARE + SQUARE/2;
            int duration = (int)MathUtil.distance(sx, sy, tx, ty) * 2;
            _ssprite.setLocation(sx, sy);
            _ssprite.addSpriteObserver(this);
            addSprite(_ssprite);
            _ssprite.move(new LinePath(sx, sy, tx, ty, duration));
        }

        protected void applyShot ()
        {
            // apply the shot
            _shot.apply(_bangobj, _effector);
        }

        protected ShotEffect _shot;
        protected ShotSprite _ssprite;
        protected Piece _shooter, _target;
        protected int _sprites, _managed;
    }

    /** Used to remove shot sprites when they reach their target. */
    protected PathObserver _remover = new PathObserver() {
        public void pathCompleted (Sprite sprite, Path path, long when) {
            removeSprite(sprite);
        }
        public void pathCancelled (Sprite sprite, Path path) {
            removeSprite(sprite);
        }
    };

    /** Handles the results of effects. */
    protected Effect.Observer _effector = new Effect.Observer() {
        public void pieceAdded (Piece piece) {
            // this will create and add the sprite to the board
            getPieceSprite(piece);
        }

        public void pieceAffected (Piece piece, String effect) {
            createEffectAnimation(piece, effect);
        }

        public void pieceRemoved (Piece piece) {
            removePieceSprite(piece.pieceId);
        }
    };

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

    protected Piece _selection;
    protected PointSet _moveSet = new PointSet();
    protected int _pidx;
    protected int _downButton = -1;

    protected int[] _action;
    protected Surprise _surprise;

    /** Tracks coordinate visibility. */
    protected VisibilityState _vstate;
}
