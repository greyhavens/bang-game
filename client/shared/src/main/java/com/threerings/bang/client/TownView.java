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
import java.util.HashMap;
import java.util.Properties;

import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BCursor;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BLayoutManager;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.jme.camera.CameraPath;
import com.threerings.jme.camera.PanPath;
import com.threerings.jme.sprite.Sprite;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.bang.data.Shop;
import com.threerings.bang.data.StatType;
import com.threerings.bang.data.TownObject;
import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.PropSprite;
import com.threerings.bang.game.client.sprite.ViewpointSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.ModifiableDSet;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.game.data.piece.Viewpoint;
import com.threerings.bang.game.util.BoardFile;

import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.PropConfig;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.DeploymentConfig;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.tourney.client.TourneyListView;

import static com.threerings.bang.Log.*;

/**
 * Displays the main "town" menu interface where a player can navigate to the ranch, the saloon,
 * the general store, the bank, the train station and wherever else we might dream up.
 */
public class TownView extends BWindow
    implements MainView, ActionListener
{
    public TownView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), null);
        setLayoutManager(new TownLayout());

        _bctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("town");

        int width = ctx.getDisplay().getWidth();
        int height = ctx.getDisplay().getHeight();
        setBounds(0, 0, width, height);

        // load up our menu props
        PlayerObject user = ctx.getUserObject();
        String townId = user.townId;
        Properties props = new Properties();
        String mpath = "rsrc/menu/" + townId + "/menu.properties";
        try {
            ClassLoader loader = getClass().getClassLoader();
            props.load(loader.getResourceAsStream(mpath));
        } catch (Exception e) {
            log.warning("Failed to load menu properties", "path", mpath, e);
        }

        // register the commands for our various shops
        Enumeration<?> iter = props.propertyNames();
        while (iter.hasMoreElements()) {
            String command = (String)iter.nextElement();
            // disable the bank on non-coin deployments
            if (!DeploymentConfig.usesCoins() && command.equals("bank")) {
                continue;
            }
            _commands.put(props.getProperty(command), command);
        }

        // create the town display
        add(_bview = new TownBoardView(ctx));

        // create our overlay menu buttons
        GroupLayout gl = GroupLayout.makeHoriz(
            GroupLayout.STRETCH, GroupLayout.LEFT, GroupLayout.NONE);
        gl.setGap(0);
        gl.setOffAxisJustification(GroupLayout.BOTTOM);
        add(_menu = new BContainer(gl) {
            public void render (Renderer renderer) {
                if (_active) {
                    super.render(renderer);
                }
            }
            public BComponent getHitComponent (int mx, int my) {
                return _active ? super.getHitComponent(mx, my) : null;
            }
        });
        _menu.setStyleClass("town_menubar");

        BContainer left = new BContainer(GroupLayout.makeHoriz(GroupLayout.LEFT).setGap(0));
        _menu.add(left, GroupLayout.FIXED);
        BButton button = new BButton(new BlankIcon(142, 33), this, "tutorials");
        button.setStyleClass("tutorials_button");
        left.add(button);
        button = new BButton(new BlankIcon(142, 33), this, "saddlebag");
        button.setStyleClass("saddlebag_button");
        left.add(button);
        button = new BButton(new BlankIcon(142, 33), this, "pardners");
        button.setStyleClass("pardners_button");
        left.add(button);
        _menu.add(_tip = new BLabel("", "tip_label"));
        button = new BButton(new BlankIcon(128, 30), this, "exit");
        button.setStyleClass("exit_button");
        _menu.add(button, GroupLayout.FIXED);

        // if we're an admin add some temporary buttons
        if (user.tokens.isSupport()) {
            add(_admin = GroupLayout.makeHBox(GroupLayout.CENTER));
            _admin.add(new BButton("Tournaments", this, "tourney"));
            // allow admins to go to the sheriff's office in ITP, etc. to make bounties
            if (!townId.equals(BangCodes.FRONTIER_TOWN)) {
                ActionListener fire = new ActionListener() {
                    public void actionPerformed (ActionEvent event) {
                        fireCommand(event.getAction());
                    }
                };
                _admin.add(new BButton("Sheriff's Office", fire, "office"));
            }
        }

        // show a tip balloon for tutorials unless we've done all the tutorials or turned it off
        String[] tutIds = TutorialCodes.NEW_TUTORIALS[BangUtil.getTownIndex(townId)];
        if (!user.stats.containsValue(StatType.TUTORIALS_COMPLETED, tutIds[tutIds.length-1]) &&
            BangPrefs.shouldShowTutIntro(user)) {
            add(new TipBalloon(_msgs.get("m.tutorial_tip"), 100, 75));
        }

        // if we're in frontier town, add tip balloons to the saloon and office
        if (townId.equals(BangCodes.FRONTIER_TOWN)) {
            if (user.stats.getSetStatSize(StatType.BOUNTIES_COMPLETED) < OFFICE_TIP) {
                add(new TipBalloon(_msgs.get("m.office_tip"), 150, 630));
            }
            if (user.stats.getIntStat(StatType.GAMES_PLAYED) < SALOON_TIP) {
                add(new TipBalloon(_msgs.get("m.saloon_tip"), 600, 500));
            }
        }
    }

    /**
     * Makes the town view responsive to user input or not. It may start out unresponsive when
     * we're showing the create avatar or first time tutorial dialogs.
     */
    public void setActive (boolean active)
    {
        _active = active;
    }

    /**
     * If we fail to enter a shop, we need to reset our camera viewpoint.
     */
    public void resetViewpoint ()
    {
        _bview.moveToViewpoint("main", 1f);

        // wait until we've finished animating the camera and then check to see if we should
        // display a tutorial or intro
        _bctx.getCameraHandler().addCameraObserver(new CameraPath.Observer() {
            public boolean pathCompleted (CameraPath path) {
                finishedIntroPan();
                return false; // removes our observer
            }
        });
    }

    // from interface MainView
    public boolean allowsPopup (Type type)
    {
        switch (type) {
        case STATUS:
        case NOTIFICATION:
        case FKEY:
            return _active;

        default:
            return true;
        }
    }

    // from interface MainView
    public boolean allowsPardnerInvite ()
    {
        return _active;
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if ("exit".equals(cmd)) {
            _bctx.getApp().stop();

        } else if ("tutorials".equals(cmd)) {
            _bctx.getBangClient().getPopupManager().showPopup(FKeyPopups.Type.TUTORIALS);

        } else if ("saddlebag".equals(cmd)) {
            StatusView.showStatusTab(_bctx, StatusView.ITEMS_TAB);

        } else if ("pardners".equals(cmd)) {
            StatusView.showStatusTab(_bctx, StatusView.PARDNERS_TAB);

        } else if ("tourney".equals(cmd)) {
            _bctx.getBangClient().displayPopup(new TourneyListView(_bctx), true, 500);
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // attempt to load the board
        try {
            _bview.loadBoard(_bctx.getUserObject().townId);
        } catch (IOException ioe) {
            log.warning("Failed to load town board!", "error", ioe);
        }
    }

    protected void finishedIntroPan ()
    {
        setActive(!_bctx.getBangClient().checkNotifications());
    }

    protected void fireCommand (String command)
    {
        if ("logoff".equals(command)) {
            _bctx.getApp().stop();

        } else {
            try {
                _bctx.getBangClient().goTo(Enum.valueOf(Shop.class, command.toUpperCase()));
            } catch (Exception e) {
                log.warning("Got unknown town view command " + command + ": " + e);
                return;
            }
        }

        // become inactive now that we're going somewhere
        setActive(false);
    }

    /** A simple viewer for the town board. */
    protected class TownBoardView extends BoardView
        implements Subscriber<TownObject>, AttributeChangeListener
    {
        public TownBoardView (BangContext ctx)
        {
            super(ctx, false);
            _bctx = ctx;
            addListener(this);
            addListener(new MouseAdapter() {
                public void mousePressed (MouseEvent me) {
                    updateHoverState(me);
                    if (_hsprite != null) {
                        enterBuilding(((Prop)_hsprite.getPiece()).getType());
                        // clear out the hover sprite so that we don't booch it if we double click
                        hoverSpritesChanged(null, null);
                    } else if (_ctx.getCameraHandler().cameraIsMoving()) {
                        _ctx.getCameraHandler().skipPath();
                    }
                }
            });

            _hstate = _ctx.getRenderer().createMaterialState();
            _hstate.getAmbient().set(ColorRGBA.white);
            _hstate.getDiffuse().set(ColorRGBA.white);
            _hstate.getEmissive().set(ColorRGBA.white);

            // load up our hover cursor
            try {
                _hcursor = BangUI.loadCursor("hand");
            } catch (IOException e) {
                // we can just let it drop if something goes wrong
            }

            BangBootstrapData bbd = (BangBootstrapData)_bctx.getClient().getBootstrapData();
            _safesub = new SafeSubscriber<TownObject>(bbd.townOid, this);
        }

        /**
         * Attempts to load the town menu board from the specified resource path.
         */
        public void loadBoard (String townId)
            throws IOException
        {
            BoardFile bfile = BoardFile.loadFrom(
                _ctx.getResourceManager().getResourceFile("menu/" + townId + "/town.board"));
            BangObject bangobj = new BangObject();
            // we only want to configure the board name the first time we're shown as it will
            // trigger a marquee being displayed with the town name
            bangobj.marquee = townId.equals(_presented) ? null : MessageBundle.taint(bfile.name);
            bangobj.board = bfile.board;
            bangobj.pieces = new ModifiableDSet<Piece>(bfile.pieces.iterator());
            prepareForRound(bangobj, null, 0);
        }

        @Override // documentation inherited
        public void refreshBoard ()
        {
            super.refreshBoard();

            // if this is the first time this town is being shown, do our aerial sweep, otherwise
            // just go right to the main view
            String townId = _bctx.getUserObject().townId;
            String view = townId.equals(_presented) ? "main" : "aerial";
            _presented = townId;

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

        // from interface Subscriber
        public void objectAvailable (TownObject object)
        {
            _townobj = object;
            _townobj.addListener(this);
            updatePopulationSign(_townobj.population);
        }

        // from interface Subscriber
        public void requestFailed (int oid, ObjectAccessException cause)
        {
            log.warning("Failed to subscribe to town object!", "oid", oid, "cause", cause);
        }

        // from interface AttributeChangeListener
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
            addResolving(this);
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
        protected boolean shouldShowStarter (Piece piece)
        {
            if (!(piece instanceof Prop)) {
                return super.shouldShowStarter(piece);
            }

            // keep bridges and fences, even if they're outside the board
            Prop prop = (Prop)piece;
            String type = prop.getType();
            PropConfig config = PropConfig.getConfig(type);
            return super.shouldShowStarter(prop) || _commands.containsKey(type) ||
                type.indexOf("pop_sign") != -1 || !StringUtil.isBlank(config.blockDir) ||
                (config.passable && config.passElev > 0f);
        }

        @Override // documentation inherited
        protected boolean shouldShowGrid ()
        {
            return false;
        }

        @Override // documentation inherited
        protected BLabel createMarqueeLabel (String text)
        {
            return new BLabel(new ImageIcon(
                _bctx.loadImage("ui/town/" + _bctx.getUserObject().townId + ".png")));
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

            if (_vpsprite != null && !((Viewpoint)_vpsprite.getPiece()).name.equals("main")) {
                // clear out any hover sprite that was established in the moment before we start
                // our cinematic entrance
                hoverSpritesChanged(null, null);
                // sweep the camera from the aerial viewpoint to the main
                moveToViewpoint("main", 4f);

                // wait until we've finished animating the camera and then check to see if we
                // should display a tutorial or intro
                _ctx.getCameraHandler().addCameraObserver(new CameraPath.Observer() {
                    public boolean pathCompleted (CameraPath path) {
                        finishedIntroPan();
                        hoverSpritesChanged(_hover, _thover);
                        return false; // removes our observer
                    }
                });

            } else {
                finishedIntroPan();
                hoverSpritesChanged(_hover, _thover);
            }
        }

        @Override // documentation inherited
        protected void removePieceSprites ()
        {
            // don't remove the piece sprites, even when the view is removed
        }

        @Override // documentation inherited
        protected void hoverSpritesChanged (Sprite hover, Sprite thover)
        {
            super.hoverSpritesChanged(hover, thover);

            // clear our previous highlight
            if (_hsprite != null) {
                _hsprite.clearRenderState(RenderState.RS_MATERIAL);
                _hsprite.updateRenderState();
            }
            _hsprite = null;

            // if we're not yet enabled or the camera is moving, no hovering
            if (!_active || _ctx.getCameraHandler().cameraIsMoving() || hover == null) {
                _tip.setText(_msgs.get("m.tip_select"));
                updateCursor(_cursor);
                return;
            }

            // update the menu tip
            Piece piece = ((PieceSprite)hover).getPiece();
            String cmd = _commands.get(((Prop)piece).getType());
            _tip.setText(_msgs.get("m.tip_" + cmd));

            // highlight the sprite
            _hsprite = (PieceSprite)hover;
            _hsprite.setRenderState(_hstate);
            _hsprite.updateRenderState();

            updateCursor(_hcursor);
        }

        protected Viewpoint getViewpoint (String name)
        {
            for (Piece piece : _bangobj.pieces) {
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
                log.warning("Missing target viewpoint", "cmd", cmd);
                fireCommand(cmd);
                return;
            }

            // wait until we've finished animating the camera before we fire the associated command
            // otherwise things are jerky as it tries to load up the UI while we're moving
            _ctx.getCameraHandler().addCameraObserver(new CameraPath.Observer() {
                public boolean pathCompleted (CameraPath path) {
                    // TEMP: replace these sounds until we get new ones for these buildings
                    String sound = cmd;
                    if ("hideout".equals(sound)) {
                        sound = "saloon";
                    } else if ("office".equals(sound)) {
                        sound = "ranch";
                    }
                    // END TEMP
                    BangUI.playShopEntry(_bctx.getUserObject().townId, sound);
                    fireCommand(cmd);
                    return false; // removes our observer
                }
            });
            // become unactive to make our interface elements go away
            setActive(false);
        }

        protected boolean moveToViewpoint (String view, float duration)
        {
            Viewpoint piece = getViewpoint(view);
            if (piece == null) {
                return false;
            }
            ViewpointSprite sprite = (ViewpointSprite)getPieceSprite(piece);
            _ctx.getCameraHandler().moveCamera(
                new PanPath(_ctx.getCameraHandler(), sprite.getLocalTranslation(),
                            sprite.getViewRotation(), duration));
            return true;
        }

        protected void updatePopulationSign (final int pop)
        {
            _ctx.getInvoker().postUnit(new Invoker.Unit() {
                public boolean invoke () {
                    updatePopulationSignTexture(pop);
                    return true;
                }
                public void handleResult () {
                    // delete the old texture using a dummy state now that we're in the main thread
                    int oldTextureId = _poptex.getTextureId();
                    if (oldTextureId > 0) {
                        TextureState tstate = _ctx.getRenderer().createTextureState();
                        Texture tex = new Texture();
                        tex.setTextureId(oldTextureId);
                        tstate.setTexture(tex);
                        tstate.deleteAll();
                    }
                    _poptex.setTextureId(0);
                    clearResolving(TownBoardView.this);
                }
            });
        }

        protected void updatePopulationSignTexture (int pop)
        {
            // get a reference to the buffered sign image
            String townId = _bctx.getUserObject().townId;
            String path = "props/" + townId + "/structures/pop_sign_" + townId + "/sign.png";
            BufferedImage bimg = _ctx.getImageCache().getBufferedImage(path);
            if (bimg == null) {
                log.warning("Couldn't find population sign image", "path", path);
                return;
            }

            // write population into image
            int width = bimg.getWidth(), height = bimg.getHeight();
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D gfx = img.createGraphics();
            gfx.drawImage(bimg, 0, 0, null);
            gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            gfx.setColor(TOWN_COLORS[BangUtil.getTownIndex(townId)]);
            gfx.setFont(new Font("Dom Casual", Font.PLAIN, 40));
            String pstr = Integer.toString(pop);
            gfx.drawString(pstr, (width - gfx.getFontMetrics().stringWidth(pstr)) / 2, height - 32);
            gfx.dispose();

            // get a reference to the population sign texture (and keep it around, so it doesn't
            // disappear from the cache) and update
            if (_poptex == null) {
                _poptex = _ctx.getTextureCache().getTexture(path);
            }
            RenderUtil.configureTexture(_poptex, TextureManager.loadImage(img, true));
        }

        protected BangContext _bctx;

        protected MaterialState _hstate;
        protected PieceSprite _hsprite;
        protected ViewpointSprite _vpsprite;
        protected Vector3f _pos = new Vector3f();

        protected SafeSubscriber<TownObject> _safesub;
        protected TownObject _townobj;
        protected Texture _poptex;

        protected BCursor _hcursor;
    }

    /** Used to layout our overlapping town menu components. */
    protected class TownLayout extends BLayoutManager
    {
        public Dimension computePreferredSize (BContainer target, int whint, int hhint) {
            return _bview.getPreferredSize(whint, hhint);
        }
        public void layoutContainer (BContainer target) {
            // the town view takes up the whole window
            Rectangle bounds = target.getBounds();
            _bview.setBounds(0, 0, bounds.width, bounds.height);
            // and the extra buttons in the top-left
            if (_admin != null) {
                Dimension aps = _admin.getPreferredSize(bounds.width, -1);
                _admin.setBounds(5, bounds.height-aps.height-5, aps.width, aps.height);
            }
            // then the menu bar is overlayed at the bottom
            Dimension mps = _menu.getPreferredSize(bounds.width, -1);
            _menu.setBounds(0, 0, bounds.width, mps.height);
        }
    }

    protected class TipBalloon extends BLabel
    {
        public TipBalloon (String text, int x, int y) {
            super(text);
            setStyleClass("town_tip");
            setLocation(x, y);
        }

        @Override public void render (Renderer renderer) {
            if (_active) {
                super.render(renderer);
            }
        }

        @Override public BComponent getHitComponent (int mx, int my) {
            return null;
        }

        @Override protected void wasAdded () {
            super.wasAdded();
            // we won't get layed out normally, so we need to do that ourselves
            Dimension psize = getPreferredSize(-1, -1);
            setSize(psize.width, psize.height);
        }
    }

    protected BangContext _bctx;
    protected MessageBundle _msgs;
    protected boolean _active;

    protected TownBoardView _bview;
    protected BContainer _menu, _admin;
    protected BLabel _tip;

    /** Maps prop types to commands. */
    protected HashMap<String, String> _commands = new HashMap<String, String>();

    /** Used to ensure that we only "present" each town once per session. */
    //protected static HashSet<String> _presented = new HashSet<String>();
    protected static String _presented;

    protected static final Color[] TOWN_COLORS = {
        Color.white, // frontier_town
        Color.black, // indian_post
        Color.black, // boom_town
        Color.black, // ghost_post
        Color.black, // city_of_gold
    };

    // the number of bounties and ranked games needed to cause the tip to go away
    protected static final int OFFICE_TIP = 4;
    protected static final int SALOON_TIP = 4;
}
