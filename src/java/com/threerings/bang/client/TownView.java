//
// $Id$

package com.threerings.bang.client;

import java.io.IOException;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Level;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.math.Quaternion;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;

import com.jmex.bui.BWindow;
import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.jme.camera.CameraPath;
import com.threerings.jme.camera.SplinePath;
import com.threerings.jme.sprite.Sprite;
import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.ViewpointSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.PieceDSet;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.game.data.piece.Viewpoint;
import com.threerings.bang.server.persist.BoardRecord;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the main "town" menu interface where a player can navigate to
 * the ranch, the saloon, the general store, the bank, the train station
 * and wherever else we might dream up.
 */
public class TownView extends BWindow
{
    public TownView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), new BorderLayout());
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("town");

        // display the status view when the player presses escape
        setModal(true);
        new StatusView(_ctx).bind(this);

        int width = ctx.getDisplay().getWidth();
        int height = ctx.getDisplay().getHeight();
        setBounds(0, 0, width, height);

        // load up our menu props
        String townId = ctx.getUserObject().townId;
        Properties props = new Properties();
        String mpath = "rsrc/menu/" + townId + "/menu.properties";
        try {
            ClassLoader loader = getClass().getClassLoader();
            props.load(loader.getResourceAsStream(mpath));
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to load menu properties " +
                    "[path=" + mpath + "].", e);
        }
        Enumeration iter = props.propertyNames();
        while (iter.hasMoreElements()) {
            String command = (String)iter.nextElement();
            _commands.put(props.getProperty(command), command);
        }

        // create the town display
        add(_bview = new TownBoardView(_ctx), BorderLayout.CENTER);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // attempt to load the board
        try {
            _bview.loadBoard(_ctx.getUserObject().townId);
        } catch (IOException ioe) {
            log.warning("Failed to load town board! [error=" + ioe + "].");
        }
    }

    protected void fireCommand (String command)
    {
        BangBootstrapData bbd = (BangBootstrapData)
            _ctx.getClient().getBootstrapData();
        if ("logoff".equals(command)) {
            _ctx.getApp().stop();
        } else if ("ranch".equals(command)) {
            _ctx.getLocationDirector().moveTo(bbd.ranchOid);
        } else if ("bank".equals(command)) {
            _ctx.getLocationDirector().moveTo(bbd.bankOid);
        } else if ("store".equals(command)) {
            _ctx.getLocationDirector().moveTo(bbd.storeOid);
        } else if ("saloon".equals(command)) {
            _ctx.getLocationDirector().moveTo(bbd.saloonOid);
        } else if ("barber".equals(command)) {
            _ctx.getLocationDirector().moveTo(bbd.barberOid);
        }
    }

    /** A simple viewer for the town board. */
    protected class TownBoardView extends BoardView
    {
        public TownBoardView (BangContext ctx)
        {
            super(ctx);
            addListener(this);
            addListener(new MouseAdapter() {
                public void mousePressed (MouseEvent me) {
                    if (_hsprite != null) {
                        enterBuilding(((Prop)_hsprite.getPiece()).getType());
                    }
                }
            });

            MaterialState mstate = ctx.getRenderer().createMaterialState();
            mstate.setEmissive(ColorRGBA.white);
            _hstate = RenderUtil.createColorMaterialState(mstate, false);
        }

        /**
         * Attempts to load the town menu board from the specified resource
         * path.
         */
        public void loadBoard (String townId)
            throws IOException
        {
            BoardRecord brec = new BoardRecord();
            brec.load(_ctx.getResourceManager().getResource(
                          "menu/" + townId + "/town.board"));
            BangObject bangobj = new BangObject();
            // we only want to configure the board name the first time we're
            // shown as it will trigger a marquee being displayed with the town
            // name
            bangobj.boardName = _presented.contains(townId) ? null : brec.name;
            bangobj.board = brec.getBoard();
            bangobj.pieces = new PieceDSet(brec.getPieces());
            prepareForRound(bangobj, null, 0);
        }

        @Override // documentation inherited
        public void refreshBoard ()
        {
            super.refreshBoard();

            // if this is the first time this town is being shown, do our
            // aerial sweep, otherwise just go right to the main view
            String townId = TownView.this._ctx.getUserObject().townId;
            String view = _presented.contains(townId) ? "main" : "aerial";
            _presented.add(townId);

            Viewpoint vp = getViewpoint(view);
            if (vp != null) {
                _vpsprite = (ViewpointSprite)getPieceSprite(vp);
                _vpsprite.bindCamera(_ctx.getCameraHandler().getCamera());
            }
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            super.wasRemoved();

            // unbind our camera (doesn't really do anything)
            if (_vpsprite != null) {
                _vpsprite.unbindCamera();
                _vpsprite = null;
            }
        }

        @Override // documentation inherited
        protected float getFadeInTime ()
        {
            return _presented.contains(
                TownView.this._ctx.getUserObject().townId) ? 1f : 3f;
        }

        @Override // documentation inherited
        protected void fadeInComplete ()
        {
            super.fadeInComplete();

            if (_vpsprite != null &&
                !((Viewpoint)_vpsprite.getPiece()).name.equals("main")) {
                // sweep the camera from the aerial viewpoint to the main
                moveToViewpoint("main", 4f, 0.5f);
            }
        }

        @Override // documentation inherited
        protected void removePieceSprites ()
        {
            // don't remove the piece sprites, even when the view is removed
        }

        @Override // documentation inherited
        protected void hoverSpriteChanged (Sprite hover)
        {
            super.hoverSpriteChanged(hover);

            // clear our previous highlight
            if (_hsprite != null) {
                _hsprite.clearRenderState(RenderState.RS_MATERIAL);
                _hsprite.updateRenderState();
            }

            // make sure the sprite we're over is a building
            _hsprite = null;
            if (!(hover instanceof PieceSprite)) {
                return;
            }
            Piece piece = ((PieceSprite)hover).getPiece();
            if (!(piece instanceof Prop)) {
                return;
            }
            if (!_commands.containsKey(((Prop)piece).getType())) {
                return;
            }

            // highlight the sprite
            _hsprite = (PieceSprite)hover;
            _hsprite.setRenderState(_hstate);
            _hsprite.updateRenderState();
        }

        protected Viewpoint getViewpoint (String name)
        {
            for (Iterator it = _bangobj.pieces.iterator(); it.hasNext(); ) {
                Piece piece = (Piece)it.next();
                if ((piece instanceof Viewpoint) &&
                    name.equals(((Viewpoint)piece).name)) {
                    return (Viewpoint)piece;
                }
            }
            return null;
        }

        protected void enterBuilding (String type)
        {
            final String cmd = _commands.get(type);
            if (!moveToViewpoint(cmd, 0.75f, 0.5f)) {
                log.warning("Missing target viewpoint [cmd=" + cmd  + "].");
                fireCommand(cmd);
                return;
            }

            // wait until we've finished animating the camera before we fire
            // the associated command otherwise things are jerky as it tries to
            // load up the UI while we're moving
            _ctx.getCameraHandler().addCameraObserver(
                new CameraPath.Observer() {
                public boolean pathCompleted (CameraPath path) {
                    fireCommand(cmd);
                    return false; // removes our observer
                }
            });
        }

        protected boolean moveToViewpoint (
            String view, float duration, float tension)
        {
            Viewpoint piece = getViewpoint(view);
            if (piece == null) {
                return false;
            }
            ViewpointSprite sprite = (ViewpointSprite)getPieceSprite(piece);
            _ctx.getCameraHandler().moveCamera(
                new SplinePath(_ctx.getCameraHandler(),
                               sprite.getLocalTranslation(),
                               sprite.getViewDirection(), Vector3f.UNIT_Z,
                               duration, tension));
            return true;
        }

        protected MaterialState _hstate;
        protected PieceSprite _hsprite;
        protected ViewpointSprite _vpsprite;
        protected Vector3f _pos = new Vector3f();
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected TownBoardView _bview;

    /** Maps prop types to commands. */
    protected HashMap<String, String> _commands =
        new HashMap<String, String>();

    /** Used to ensure that we only "present" each town once per session. */
    protected static HashSet<String> _presented = new HashSet<String>();
}
