//
// $Id$

package com.threerings.bang.client;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.io.IOException;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Level;

import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.math.Quaternion;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;

import com.jmex.bui.BWindow;
import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.jme.camera.CameraPath;
import com.threerings.jme.camera.PanPath;
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
    implements MainView
{
    public TownView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), new BorderLayout());
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("town");

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

    // documentation inherited from interface MainView
    public boolean allowsPopup (Type type)
    {
        switch (type) {
        case STATUS:
        case PARDNER_INVITE:
            return _active;

        default:
        case CHAT:
            return true;
        }
    }

    // documentation inherited from interface MainView
    public boolean allowsPardnerInvite ()
    {
        return _active;
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

    protected void finishedIntroPan ()
    {
        _active = !_ctx.getBangClient().checkShowIntro();
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
                        // clear out the hover sprite so that we don't booch it
                        // if we double click
                        hoverSpriteChanged(null);
                    } else if (_ctx.getCameraHandler().cameraIsMoving()) {
                        _ctx.getCameraHandler().skipPath();
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
        protected void wasAdded ()
        {
            super.wasAdded();

            // disable camera input handler
            _ctx.getInputHandler().setEnabled(false);
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

            // make sure we complete any active camera path
            _ctx.getCameraHandler().skipPath();

            // reenable the input handler
            _ctx.getInputHandler().setEnabled(true);
        }

        @Override // documentation inherited
        protected boolean shouldShowGrid ()
        {
            return false;
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

            // make sure the population sign is up-to-date
            updatePopulationSign(51234);
            
            if (_vpsprite != null &&
                !((Viewpoint)_vpsprite.getPiece()).name.equals("main")) {
                // clear out any hover sprite that was established in the
                // moment before we start our cinematic entrance
                hoverSpriteChanged(null);
                // sweep the camera from the aerial viewpoint to the main
                moveToViewpoint("main", 4f);

                // wait until we've finished animating the camera and then
                // check to see if we should display a tutorial or intro
                _ctx.getCameraHandler().addCameraObserver(
                    new CameraPath.Observer() {
                        public boolean pathCompleted (CameraPath path) {
                            finishedIntroPan();
                            hoverSpriteChanged(_hover);
                            return false; // removes our observer
                        }
                    });

            } else {
                finishedIntroPan();
                hoverSpriteChanged(_hover);
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
            _hsprite = null;

            // if we're not yet enabled or the camera is moving, no hovering
            if (!_active || _ctx.getCameraHandler().cameraIsMoving()) {
                return;
            }

            // make sure the sprite we're over is a building
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
            if (!moveToViewpoint(cmd, 0.75f)) {
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
            String view, float duration)
        {
            Viewpoint piece = getViewpoint(view);
            if (piece == null) {
                return false;
            }
            ViewpointSprite sprite = (ViewpointSprite)getPieceSprite(piece);
            _ctx.getCameraHandler().moveCamera(
                new PanPath(_ctx.getCameraHandler(),
                            sprite.getLocalTranslation(),
                            sprite.getViewRotation(),
                            duration));
            return true;
        }

        protected void updatePopulationSign (int pop)
        {
            // get a reference to the buffered sign image
            String path = "props/structures/pop_sign_" +
                TownView.this._ctx.getUserObject().townId + "/sign.png";
            BufferedImage bimg = _ctx.getImageCache().getBufferedImage(path);
            if (bimg == null) {
                log.warning("Couldn't find population sign image [path=" +
                    path + "].");
                return;
            }
            
            // write population into image
            BufferedImage img = new BufferedImage(bimg.getWidth(),
                bimg.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D gfx = img.createGraphics();
            gfx.drawImage(bimg, 0, 0, null);
            gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            gfx.setColor(Color.white);
            gfx.setFont(new Font("Dom Casual", Font.PLAIN, 40));
            String pstr = Integer.toString(pop);
            gfx.drawString(pstr,
                (img.getWidth() - gfx.getFontMetrics().stringWidth(pstr)) / 2,
                img.getHeight() - 32);
            gfx.dispose();
            
            // get a reference to the population sign texture and update
            Texture ptex = _ctx.getTextureCache().getTexture(path);
            int tid = ptex.getTextureId();
            if (tid != 0) {
                // to delete the texture, we need an OpenGL texture state
                TextureState tstate = _ctx.getRenderer().createTextureState();
                tstate.setTexture(ptex);
                tstate.deleteAll();
            }
            ptex.setImage(TextureManager.loadImage(img, true));
            ptex.setCorrection(Texture.CM_PERSPECTIVE);
            ptex.setFilter(Texture.FM_LINEAR);
            ptex.setMipmapState(Texture.MM_LINEAR_LINEAR);
        }
        
        protected MaterialState _hstate;
        protected PieceSprite _hsprite;
        protected ViewpointSprite _vpsprite;
        protected Vector3f _pos = new Vector3f();
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected TownBoardView _bview;
    protected boolean _active;

    /** Maps prop types to commands. */
    protected HashMap<String, String> _commands =
        new HashMap<String, String>();

    /** Used to ensure that we only "present" each town once per session. */
    protected static HashSet<String> _presented = new HashSet<String>();
}
