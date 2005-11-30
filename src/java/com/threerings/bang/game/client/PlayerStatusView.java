//
// $Id$

package com.threerings.bang.game.client;

import java.awt.image.BufferedImage;

import com.jme.image.Image;
import com.jme.renderer.Renderer;
import com.jme.util.TextureManager;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BIcon;
import com.jmex.bui.BLabel;
import com.jmex.bui.BLookAndFeel;
import com.jmex.bui.ImageIcon;
import com.jmex.bui.background.ScaledBackground;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the name, score, cash and cards held by a particular player.
 */
public class PlayerStatusView extends BContainer
    implements AttributeChangeListener, ElementUpdateListener, SetListener,
               ActionListener
{
    public PlayerStatusView (BangContext ctx, BangObject bangobj,
                             BangController ctrl, int pidx)
    {
        super(new AbsoluteLayout());

        _ctx = ctx;
        _bangobj = bangobj;
        _bangobj.addListener(this);
        _ctrl = ctrl;
        _pidx = pidx;

        // load up the solid color background for our player
        _color = new ImageIcon(
            _ctx.loadImage("textures/pstatus/background" + pidx + ".png"));

        // load up the main status image
        ClassLoader loader = getClass().getClassLoader();
        Image bg = TextureManager.loadImage(
            loader.getResource("rsrc/textures/pstatus/dashboard.png"), true);
        log.info("Type " + bg.getType());
        setBackground(new ScaledBackground(bg, 0, 0, 0, 0));
        setPreferredSize(new Dimension(bg.getWidth(), bg.getHeight()));

        // load up our avatar image
        Look look = ctx.getUserObject().getLook();
        if (look != null) {
            int[] avatar = look.getAvatar(ctx.getUserObject());
            BufferedImage aimage = AvatarView.createImage(ctx, avatar);
            _avatar = new ImageIcon(
                aimage.getScaledInstance(
                    AvatarLogic.WIDTH/10, AvatarLogic.HEIGHT/10,
                    BufferedImage.SCALE_SMOOTH));
        }

        // create our interface elements
        add(_player = new BLabel(_bangobj.players[_pidx].toString()), NAME_RECT);
        _player.setHorizontalAlignment(BLabel.CENTER);
        _player.setVerticalAlignment(BLabel.CENTER);
        add(_cash = new BLabel(""), CASH_LOC);
        _pieces = new BLabel("");

        updateStatus();
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        BLookAndFeel lnf = getLookAndFeel().deriveLookAndFeel();
        lnf.setForeground(true, JPIECE_COLORS[_pidx]);
        _player.setLookAndFeel(lnf);
        super.wasAdded();
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (event.getName().equals(BangObject.STATE)) {
            updateStatus();
        }
    }

    // documentation inherited from interface ElementUpdateListener
    public void elementUpdated (ElementUpdatedEvent event)
    {
        updateStatus();
    }

    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (event.getName().equals(BangObject.CARDS)) {
            Card card = (Card)event.getEntry();
            if (card.owner == _pidx) {
                if (_pidx == 1 || _pidx == 2) {
//                     add(0, createButton(card));
                } else {
//                     add(createButton(card));
                }
            }
        }
    }

    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        // NOOP
    }

    // documentation inherited from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        if (!event.getName().equals(BangObject.CARDS)) {
            return;
        }
        Card card = (Card)event.getOldEntry();
        if (card.owner != _pidx) {
            return;
        }
        String cid = "" + card.cardId;
        for (int ii = 0; ii < getComponentCount(); ii++) {
            Object comp = getComponent(ii);
            if (comp instanceof BButton) {
                BButton button = (BButton)comp;
                if (cid.equals(button.getAction())) {
//                     remove(button);
                    return;
                }
            }
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        try {
            _ctrl.placeCard(Integer.parseInt(event.getAction()));
        } catch (Exception e) {
            log.warning("Bogus card '" + event.getAction() + "': " + e);
        }
    }

    protected void renderBackground (Renderer renderer)
    {
        // first draw our color
        _color.render(renderer, BACKGROUND_LOC.x, BACKGROUND_LOC.y);

        // then draw our avatar
        if (_avatar != null) {
            _avatar.render(renderer, AVATAR_LOC.x, AVATAR_LOC.y);
        }

        // then draw the normal background
        super.renderBackground(renderer);
    }

    protected void updateStatus ()
    {
        if (_bangobj.isInPlay() || _bangobj.state == BangObject.POST_ROUND) {
            _pieces.setText("P" + _bangobj.countLiveUnits(_pidx));
        } else {
            _pieces.setText("");
        }
        _cash.setText("$" + _bangobj.funds[_pidx]);
    }

    protected BButton createButton (Card card)
    {
        BIcon icon = new ImageIcon(
            _ctx.loadImage("cards/" + card.getType() + "/icon.png"));
        BButton btn = new BButton(icon, "" + card.cardId);
        btn.addListener(this);
        return btn;
    }

    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected BangController _ctrl;
    protected int _pidx;
    protected BLabel _player, _cash, _pieces;

    protected ImageIcon _color, _avatar;

    protected static final Point PLACE_LOC = new Point(10, 40);
    protected static final Point BACKGROUND_LOC = new Point(33, 13);
    protected static final Point AVATAR_LOC = new Point(33, 8);
    protected static final Point FIRST_CARD_LOC = new Point(148, 52);
    protected static final Point CASH_LOC = new Point(97, 34);
    protected static final Rectangle NAME_RECT = new Rectangle(11, 0, 100, 16);
}
