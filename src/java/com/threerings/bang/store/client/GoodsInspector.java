//
// $Id$

package com.threerings.bang.store.client;

import java.awt.image.BufferedImage;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.client.ColorSelector;
import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.store.data.ArticleGood;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.StoreObject;
import com.threerings.bang.store.data.CardTripletGood;

/**
 * Displays detailed information on a particular good.
 */
public class GoodsInspector extends BContainer
    implements IconPalette.Inspector, ActionListener
{
    public GoodsInspector (BangContext ctx, StoreView parent)
    {
        super(new AbsoluteLayout());

        _ctx = ctx;
        _parent = parent;

        add(_icon = new BLabel(""), new Rectangle(0, 0, 136, 156));

        add(_title = new BLabel("", "medium_title"),
            new Rectangle(190, 115, 300, 40));
        _title.setWrap(false);
        _title.setFit(true);
        add(_descrip = new BLabel("", "goods_descrip"),
            new Rectangle(190, 55, 400, 60));
    }

    /**
     * Gives us access to our store object when it is available.
     */
    public void init (StoreObject stobj)
    {
        _stobj = stobj;
    }

    // documentation inherited from interface IconPalette.Inspector
    public void iconUpdated (SelectableIcon icon, boolean selected)
    {
        // nothing to do on deselection
        if (!selected) {
            return;
        }

        // make sure we're showing the buy interface
        showBuy();

        // remove our color selectors
        removeColors();

        // configure our main interface with the good info
        _good = ((GoodsIcon)icon).getGood();
        _title.setText(_ctx.xlate(BangCodes.GOODS_MSGS, _good.getName()));
        _descrip.setText(_ctx.xlate(BangCodes.GOODS_MSGS, _good.getTip()));
        _cost.setMoney(_good.getScripCost(), _good.getCoinCost(), false);
        _srcimg = _ctx.getImageCache().getBufferedImage(_good.getIconPath());

        // do some special jockeying to handle colorizations for articles
        if (_good instanceof ArticleGood) {
            ArticleCatalog.Article article =
                _ctx.getAvatarLogic().getArticleCatalog().getArticle(
                    _good.getType());
            String[] cclasses =
                _ctx.getAvatarLogic().getColorizationClasses(article);
            _args[0] = _args[1] = _args[2] = Integer.valueOf(0);

            // grab whatever random colorizations we were using for the icon
            // and start with those in the inspector
            int[] colorIds = ((GoodsIcon)icon).colorIds;
            _zations = new Colorization[3];
            for (int ii = 0; ii < cclasses.length; ii++) {
                String cclass = cclasses[ii];
                if (cclass.equals(AvatarLogic.SKIN) ||
                    cclass.equals(AvatarLogic.HAIR)) {
                    continue;
                }

                // primary, secondary and tertiary colors have to go into the
                // appropriate index
                int index = AvatarLogic.getColorIndex(cclass);
                ColorSelector colorsel =
                    new ColorSelector(_ctx, cclass, _colorpal);
                colorsel.setSelectedColorId(colorIds[index]);
                colorsel.setProperty("index", Integer.valueOf(index));
                add(_colorsel[index] = colorsel, CS_SPOTS[index]);
                _args[index] = Integer.valueOf(colorsel.getSelectedColor());
                _zations[index] = colorsel.getSelectedColorization();
            }

        } else {
            _zations = null;
        }

        updateImage();
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if (_good == null || _stobj == null) {
            return;
        }

        String action = event.getAction();

        if ("buy".equals(action)) {
            StoreService.ConfirmListener cl = 
                    new StoreService.ConfirmListener() {
                public void requestProcessed () {
                    if (_good instanceof CardTripletGood) {
                        CardTripletGood ctg = (CardTripletGood)_good;
                        PlayerObject pobj = _ctx.getUserObject();
                        for (Item item : pobj.inventory) {
                            if (item instanceof CardItem) {
                                CardItem ci = (CardItem)item;
                                if (ci.getType().equals(ctg.getCardType())) {
                                    ctg.setQuantity(ci.getQuantity());
                                }
                            }
                        }
                    }
                    String msg = "m.purchased";
                    if (_good instanceof ArticleGood) {
                        showTry();
                        msg += "_article";
                    } else {
                        _parent.goodPurchased();
                    }
                    _descrip.setText(_ctx.xlate("store", msg));
                    BangUI.play(BangUI.FeedbackSound.ITEM_PURCHASE);
                }
                public void requestFailed (String cause) {
                    _descrip.setText(_ctx.xlate("store", cause));
                }
            };
            _stobj.service.buyGood(
                    _ctx.getClient(), _good.getType(), _args, cl);

        } else if ("try".equals(action)) {
            _try.setEnabled(false);
            BangBootstrapData bbd = (BangBootstrapData)
                _ctx.getClient().getBootstrapData();
            _ctx.getLocationDirector().moveTo(bbd.barberOid);
        }
    }

    protected void showBuy ()
    {
        if (_display == Mode.BUY) {
            return;
        }
        _display = Mode.BUY;
        if (_try != null) {
            remove(_try);
        }

        if (_ccont == null) {
            _ccont = GroupLayout.makeHBox(GroupLayout.LEFT);
            _ccont.add(new BLabel(
                        _ctx.xlate("store", "m.price"), "table_data"));
            _ccont.add(_cost = new MoneyLabel(_ctx));
            _cost.setMoney(0, 0, false);
        }
        add(_ccont, new Rectangle(200, 15, 200, 25));

        if (_buy == null) {
            _buy = new BButton(_ctx.xlate("store", "m.buy"), this, "buy");
            _buy.setStyleClass("big_button");
        }
        add(_buy, new Point(400, 10));
    }

    protected void showTry ()
    {
        if (_display == Mode.TRY) {
            return;
        }
        _display = Mode.TRY;
        if (_ccont != null) {
            remove(_ccont);
        }
        if (_buy != null) {
            remove(_buy);
        }
        removeColors();

        if (_try == null) {
            _try = new BButton(_ctx.xlate("store", "m.try"), this, "try");
            _try.setStyleClass("big_button");
        }
        add(_try, new Point(300, 10));
    }

    protected void removeColors ()
    {
        // remove our color selectors
        for (int ii = 0; ii < _colorsel.length; ii++) {
            if (_colorsel[ii] != null) {
                remove(_colorsel[ii]);
                _colorsel[ii] = null;
            }
        }
    }

    protected void updateImage ()
    {
        BImage image;
        if (_zations != null) {
            image = new BImage(ImageUtil.recolorImage(_srcimg, _zations));
        } else {
            image = new BImage(_srcimg);
        }
        _icon.setIcon(new ImageIcon(image));
    }

    protected ActionListener _colorpal = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            ColorSelector colorsel = (ColorSelector)event.getSource();
            int index = (Integer)colorsel.getProperty("index");
            _args[index] = Integer.valueOf(colorsel.getSelectedColor());
            _zations[index] = colorsel.getSelectedColorization();
            updateImage();
        }
    };

    protected BangContext _ctx;
    protected StoreObject _stobj;
    protected Good _good;

    protected BLabel _icon, _title, _descrip;
    protected BButton _buy, _try;
    protected BContainer _ccont;
    protected MoneyLabel _cost;

    protected BufferedImage _srcimg;
    protected StoreView _parent;
    protected ColorSelector[] _colorsel = new ColorSelector[3];

    protected Object[] _args = new Object[3];
    protected Colorization[] _zations;

    protected static enum Mode { NEW, BUY, TRY };
    protected Mode _display = Mode.NEW;

    protected static final Point[] CS_SPOTS = {
        new Point(150, 105),
        new Point(150, 61),
        new Point(150, 17),
    };
}
