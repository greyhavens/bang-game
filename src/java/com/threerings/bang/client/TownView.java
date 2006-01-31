//
// $Id$

package com.threerings.bang.client;

import java.io.IOException;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.HashMap;
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

import com.threerings.jme.camera.SplinePath;
import com.threerings.jme.sprite.Sprite;
import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.PieceDSet;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;
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
            String command = (String)iter.nextElement(),
                ptype = props.getProperty(command);
            _commands.put(ptype, command);
        }

        // create the town display
        add(_bview = new TownBoardView(_ctx), BorderLayout.CENTER);
       
        // attempt to load the board
        try {
            _bview.loadBoard("menu/" + townId + "/town.board");
        
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

        } else if ("to_ranch".equals(command)) {
            _ctx.getLocationDirector().moveTo(bbd.ranchOid);

        } else if ("to_bank".equals(command)) {
            _ctx.getLocationDirector().moveTo(bbd.bankOid);

        } else if ("to_store".equals(command)) {
            _ctx.getLocationDirector().moveTo(bbd.storeOid);

        } else if ("to_saloon".equals(command)) {
            _ctx.getLocationDirector().moveTo(bbd.saloonOid);

        } else if ("to_barber".equals(command)) {
            _ctx.getLocationDirector().moveTo(bbd.barberOid);
        }
    }

    /**
     * A simple viewer for the town board.
     */
    protected class TownBoardView extends BoardView
    {
        public TownBoardView (BangContext ctx)
        {
            super(ctx);
            addListener(this);
            addListener(new MouseAdapter() {
                public void mousePressed (MouseEvent me) {
                    if (_hsprite != null) {
                        // move the camera into the sprite
                        _pos.set(_hsprite.getLocalTranslation());
                        _pos.z += TILE_SIZE / 2;
                        _hsprite.getLocalRotation().mult(Vector3f.UNIT_Y,
                            _dir);
                        _ctx.getCameraHandler().moveCamera(
                            new SplinePath(_ctx.getCameraHandler(),
                                _pos, _dir, 0.75f, 0.5f));
                                
                        // fire the associated command
                        String type = ((Prop)_hsprite.getPiece()).getType();
                        fireCommand(_commands.get(type));
                    }
                }
            });
            
            MaterialState mstate = ctx.getRenderer().createMaterialState();
            mstate.setEmissive(ColorRGBA.white);
            _hstate = RenderUtil.createColorMaterialState(mstate, true);
        }
        
        /**
         * Attempts to load the town menu board from the specified resource
         * path.
         */
        public void loadBoard (String path)
            throws IOException
        {
            BoardRecord brec = new BoardRecord();
            brec.load(_ctx.getResourceManager().getResource(path));
            BangObject bangobj = new BangObject();
            bangobj.board = brec.getBoard();
            bangobj.pieces = new PieceDSet(brec.getPieces());
            prepareForRound(bangobj, null, 0);
        }
        
        @Override // documentation inherited
        protected void wasAdded ()
        {
            super.wasAdded();
            
            // find the camera marker, hide it, and set the camera position
            // based on its location
            for (Iterator it = _bangobj.pieces.iterator(); it.hasNext(); ) {
                Piece piece = (Piece)it.next();
                if (piece instanceof Marker &&
                    ((Marker)piece).getType() == Marker.CAMERA) {
                    PieceSprite sprite = getPieceSprite(piece);
                    sprite.setCullMode(PieceSprite.CULL_ALWAYS);
                    sprite.setIsCollidable(false);
                    _loc.set(sprite.getLocalTranslation());
                    _loc.z += TILE_SIZE / 2;
                    _rot.fromAngleNormalAxis(FastMath.HALF_PI,
                        Vector3f.UNIT_X);
                    sprite.getLocalRotation().mult(_rot, _rot);
                    Camera camera = _ctx.getCameraHandler().getCamera();
                    camera.setFrame(_loc, _rot);
                    return;
                }
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
            if (_hsprite != null) {
                // clear the highlight material
                _hsprite.clearRenderState(RenderState.RS_MATERIAL);
                _hsprite.updateRenderState();
            }
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
        
        protected MaterialState _hstate;
        protected PieceSprite _hsprite;
    }
    
    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected TownBoardView _bview;
    
    /** Maps prop types to commands. */
    protected HashMap<String, String> _commands =
        new HashMap<String, String>();
    
    protected Vector3f _loc = new Vector3f(), _pos = new Vector3f(),
        _dir = new Vector3f();
    protected Quaternion _rot = new Quaternion();
    
    /** The resource path of the town board. */
    public static final String TOWN_BOARD = "boards/0/frontier_town.board";
    
}
