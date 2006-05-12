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
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;
import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.TownObject;
import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.PropSprite;
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
        _bctx = ctx;
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
        add(_bview = new TownBoardView(ctx), BorderLayout.CENTER);
    }

    /**
     * Makes the town view responsive to user input or not. It may start out
     * unresponsive when we're showing the create avatar or first time tutorial
     * dialogs.
     */
    public void setActive (boolean active)
    {
        _active = active;
    }

    // documentation inherited from interface MainView
    public boolean allowsPopup (Type type)
    {
        switch (type) {
        case STATUS:
        case PARDNER_INVITE:
        case FKEY:
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
            _bview.loadBoard(_bctx.getUserObject().townId);
        } catch (IOException ioe) {
            log.warning("Failed to load town board! [error=" + ioe + "].");
        }
    }

    protected void finishedIntroPan ()
    {
        _active = !_bctx.getBangClient().checkShowIntro();
    }

    protected void fireCommand (String command)
    {
        BangBootstrapData bbd = (BangBootstrapData)
            _bctx.getClient().getBootstrapData();
        if ("logoff".equals(command)) {
            _bctx.getApp().stop();
        } else if ("ranch".equals(command)) {
            _bctx.getLocationDirector().moveTo(bbd.ranchOid);
        } else if ("bank".equals(command)) {
            _bctx.getLocationDirector().moveTo(bbd.bankOid);
        } else if ("store".equals(command)) {
            _bctx.getLocationDirector().moveTo(bbd.storeOid);
        } else if ("saloon".equals(command)) {
            _bctx.getLocationDirector().moveTo(bbd.saloonOid);
        } else if ("barber".equals(command)) {
            _bctx.getLocationDirector().moveTo(bbd.barberOid);
        }
    }

    /** A simple viewer for the town board. */
    protected class TownBoardView extends BoardView
        implements Subscriber, AttributeChangeListener
    {
        public TownBoardView (BangContext ctx)
        {
            super(ctx, false);
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

            _hstate = _ctx.getRenderer().createMaterialState();
            _hstate.getAmbient().set(ColorRGBA.white);
            _hstate.getDiffuse().set(ColorRGBA.white);
            _hstate.getEmissive().set(ColorRGBA.white);
            
            BangBootstrapData bbd =
                (BangBootstrapData)_bctx.getClient().getBootstrapData();
            _safesub = new SafeSubscriber(bbd.townOid, this);
        }

        /**
         * Attempts to load the town menu board from the specified resource
         * path.
         */
        public void loadBoard (String townId)
            throws IOException
        {
            BoardRecord brec = new BoardRecord();
            brec.load(_ctx.getResourceManager().getResourceFile(
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
            String townId = _bctx.getUserObject().townId;
            String view = _presented.contains(townId) ? "main" : "aerial";
            _presented.add(townId);

            Viewpoint vp = getViewpoint(view);
            if (vp != null) {
                _vpsprite = (ViewpointSprite)getPieceSprite(vp);
                _vpsprite.bindCamera(_ctx.getCameraHandler().getCamera());
            }
        }

        @Override // documentation inherited
        public boolean isHoverable (Sprite sprite)
        {
            if (!super.isHoverable(sprite)) {
                return false;
            }
            if (sprite instanceof PropSprite) {
                Prop prop = (Prop)((PropSprite)sprite).getPiece();
                return _commands.containsKey(prop.getType());
            }
            return false;
        }
 
        // documentation inherited from interface Subscriber
        public void objectAvailable (DObject object)
        {
            _townobj = (TownObject)object;
            _townobj.addListener(this);
            updatePopulationSign(_townobj.population);
        }
        
        // documentation inherited from interface Subscriber
        public void requestFailed (int oid, ObjectAccessException cause)
        {
            log.warning("Failed to subscribe to town object! [oid=" + oid +
                ", cause=" + cause + "].");
        }
        
        // documentation inherited from interface AttributeChangeListener
        public void attributeChanged (AttributeChangedEvent ace)
        {
            if (ace.getName().equals(TownObject.POPULATION)) {
                updatePopulationSign(_townobj.population);
            }
        }
        
        @Override // documentation inherited
        protected void wasAdded ()
        {
            super.wasAdded();

            // disable camera input handler
            _ctx.getInputHandler().setEnabled(false);
            
            // subscribe to town object
            _safesub.subscribe(_bctx.getDObjectManager());
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            super.wasRemoved();

            // unsubscribe from town object
            _safesub.unsubscribe(_bctx.getDObjectManager());
            if (_townobj != null) {
                _townobj.removeListener(this);
                _townobj = null;
            }
            
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
            return 1f;
        }

        @Override // documentation inherited
        protected void fadeInComplete ()
        {
            super.fadeInComplete();

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
                    BangUI.playShopEntry(_bctx.getUserObject().townId, cmd);
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
                _bctx.getUserObject().townId + "/sign.png";
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
            
            // get a reference to the population sign texture (and keep it
            // around, so it doesn't disappear from the cache) and update
            if (_poptex == null) {
                _poptex = _ctx.getTextureCache().getTexture(path);
            }
            int tid = _poptex.getTextureId();
            if (tid != 0) {
                // to delete the texture, we need an OpenGL texture state
                TextureState tstate = _ctx.getRenderer().createTextureState();
                tstate.setTexture(_poptex);
                tstate.deleteAll();
            }
            _poptex.setImage(TextureManager.loadImage(img, true));
            _poptex.setCorrection(Texture.CM_PERSPECTIVE);
            _poptex.setFilter(Texture.FM_LINEAR);
            _poptex.setMipmapState(Texture.MM_LINEAR_LINEAR);
        }
        
        protected MaterialState _hstate;
        protected PieceSprite _hsprite;
        protected ViewpointSprite _vpsprite;
        protected Vector3f _pos = new Vector3f();
        
        protected SafeSubscriber _safesub;
        protected TownObject _townobj;
        protected Texture _poptex;
    }

    protected BangContext _bctx;
    protected MessageBundle _msgs;
    protected TownBoardView _bview;
    protected boolean _active;

    /** Maps prop types to commands. */
    protected HashMap<String, String> _commands =
        new HashMap<String, String>();

    /** Used to ensure that we only "present" each town once per session. */
    protected static HashSet<String> _presented = new HashSet<String>();
}
