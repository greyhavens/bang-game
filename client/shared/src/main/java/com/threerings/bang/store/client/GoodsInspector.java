//
// $Id$

package com.threerings.bang.store.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.media.image.Colorization;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.ServiceButton;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Shop;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.client.ColorSelector;
import com.threerings.bang.avatar.client.CreateAvatarView;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.store.data.ArticleGood;
import com.threerings.bang.store.data.CardTripletGood;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.GoodsObject;
import com.threerings.bang.store.data.SongGood;
import com.threerings.bang.store.data.StoreCodes;

/**
 * Displays detailed information on a particular good.
 */
public class GoodsInspector extends BContainer
    implements IconPalette.Inspector, ActionListener
{
    public GoodsInspector (BangContext ctx, GoodsPalette palette)
    {
        super(new AbsoluteLayout());
        _ctx = ctx;
        _palette = palette;
        _msgs = _ctx.getMessageManager().getBundle(StoreCodes.STORE_MSGS);

        add(_icon = new BLabel(""), new Rectangle(0, 0, 136, 156));
        int offset = getControlGapOffset();
        add(_title = new BLabel("", "medium_title"), new Rectangle(190 + offset, 115, 300, 40));
        _title.setFit(BLabel.Fit.SCALE);
        add(_descrip = new BLabel("", "goods_descrip"), new Rectangle(190 + offset, 55, 400, 60));

        // we'll add these later
        _ccont = GroupLayout.makeHBox(GroupLayout.LEFT);
        _ccont.add(new BLabel(_msgs.get("m.price"), "table_data"));
        _ccont.add(_cost = createCostLabel());

        // create our all singing, all dancing buy button
        _buy = new ServiceButton(_ctx, _msgs.get("m.buy"), StoreCodes.STORE_MSGS, _descrip) {
            protected boolean callService () {
                if (_good == null || _goodsobj == null) {
                    return false;
                }
                // if we haven't configured a handle (and our gender), we can't buy things yet, so
                // let's encourage the user to set themselves up and then we can let them buy
                if (!_ctx.getUserObject().hasCharacter()) {
                    CreateAvatarView.show(_ctx, _reinit);
                    return false;
                }
                _goodsobj.buyGood(_good.getType(), _args, createConfirmListener());
                return true;
            }
            protected boolean onSuccess (Object result) {
                boughtGood();
                return true;
            }
        };
        _buy.setStyleClass("big_button");
    }

    /**
     * Gives us access to our store object when it is available.
     */
    public void init (GoodsObject goodsobj)
    {
        _goodsobj = goodsobj;
    }

    // documentation inherited from interface IconPalette.Inspector
    public void iconUpdated (SelectableIcon icon, boolean selected)
    {
        // nothing to do on deselection
        if (!selected) {
            return;
        }

        // make sure we're showing the buy interface
        showMode(Mode.BUY);

        // remove our color selectors
        removeColors();

        // configure our main interface with the good info
        _gicon = (GoodsIcon)icon;
        _good = _gicon.getGood();
        _title.setText(_ctx.xlate(BangCodes.GOODS_MSGS, _good.getName()));
        _descrip.setText(_ctx.xlate(BangCodes.GOODS_MSGS, _good.getTip()));
        updateCostLabel();

        // do some special jockeying to handle colorizations
        String[] cclasses = _good.getColorizationClasses(_ctx);
        if (cclasses != null && cclasses.length > 0) {
            _args[0] = _args[1] = _args[2] = Integer.valueOf(0);

            // grab whatever random colorizations we were using for the icon and start with those
            // in the inspector
            int[] colorIds = _gicon.colorIds;
            _zations = new Colorization[3];
            for (int ii = 0; ii < cclasses.length; ii++) {
                String cclass = cclasses[ii];
                if (cclass.equals(AvatarLogic.SKIN) ||
                    cclass.equals(AvatarLogic.HAIR)) {
                    continue;
                }

                // primary, secondary and tertiary colors have to go into the appropriate index
                int index = AvatarLogic.getColorIndex(cclass);
                ColorSelector colorsel = new ColorSelector(
                    _ctx, cclass, _palette.getColorEntity(), _colorpal);
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
        if (_good == null || _goodsobj == null) {
            return;
        }

        String action = event.getAction();
        if ("download".equals(action)) {
            String song = ((SongGood)_good).getSong();
            _ctx.getBangClient().displayPopup(new SongDownloadView(_ctx, song), true,
                                              SongDownloadView.PREF_WIDTH);
        }
    }

    protected MoneyLabel createCostLabel ()
    {
        return new MoneyLabel(_ctx);
    }

    protected void updateCostLabel ()
    {
        _cost.setMoney(_good.getScripCost(), _good.getCoinCost(_ctx.getUserObject()), false);
    }

    protected void boughtGood ()
    {
        // if they just bought cards, update the "in your pocket" count
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

        // reinit our goods in case this was a one shot object
        _palette.reinitGoods(true);

        // if they bought an article, give them a quick button to go try it on
        String msg = purchasedKey();
        if (_good instanceof ArticleGood) {
            showMode(Mode.TRY);
            msg += "_article";
        } else if (_good instanceof SongGood) {
            showMode(Mode.DOWNLOAD);
            msg += "_song";
        }

        _descrip.setText(_msgs.get(msg));
        BangUI.play(BangUI.FeedbackSound.ITEM_PURCHASE);
    }

    protected String purchasedKey ()
    {
        return "m.purchased";
    }

    protected void showMode (Mode mode)
    {
        if (_mode == mode) {
            return;
        }

        safeRemove(_try);
        safeRemove(_ccont);
        safeRemove(_buy);
        safeRemove(_download);
        removeColors();

        int offset = getControlGapOffset();
        switch (_mode = mode) {
        case BUY:
            add(_ccont, new Rectangle(200 + offset, 15, 200, 25));
            add(_buy, new Point(400 + offset, 10));
            break;

        case TRY:
            if (_try == null) {
                _try = new BButton(_msgs.get("m.try"), new ActionListener() {
                    public void actionPerformed (ActionEvent event) {
                        _try.setEnabled(false);
                        _ctx.getBangClient().goTo(Shop.BARBER);
                    }
                }, null);
                _try.setStyleClass("big_button");
            }
            add(_try, new Point(300 + offset, 10));
            break;

        case DOWNLOAD:
            if (_download == null) {
                _download = new BButton(
                    _msgs.get("m.download"), this, "download");
            }
            add(_download, new Point(300 + offset, 10));
            break;

        default:
            break; // nada
        }
    }

    protected int getControlGapOffset ()
    {
        return 0;
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

    protected void safeRemove (BComponent comp)
    {
        if (comp != null && comp.isAdded()) {
            remove(comp);
        }
    }

    protected void updateImage ()
    {
        _icon.setIcon(_good.createIcon(_ctx, _zations));
    }

    protected ActionListener _colorpal = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            ColorSelector colorsel = (ColorSelector)event.getSource();
            int index = (Integer)colorsel.getProperty("index");
            _args[index] = Integer.valueOf(colorsel.getSelectedColor());
            _zations[index] = colorsel.getSelectedColorization();
            updateImage();
            _gicon.setIcon(_icon.getIcon());
            _gicon.colorIds[index] = colorsel.getSelectedColor();
        }
    };

    /** Used to reinitialize our display when the user creates their character. */
    protected Runnable _reinit = new Runnable() {
        public void run () {
            _palette.reinitGoods(true);
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected GoodsObject _goodsobj;
    protected Good _good;
    protected GoodsIcon _gicon;

    protected BLabel _icon, _title, _descrip;
    protected BButton _buy, _try, _download;
    protected BContainer _ccont, _dcont;
    protected MoneyLabel _cost;

    protected GoodsPalette _palette;
    protected ColorSelector[] _colorsel = new ColorSelector[3];

    protected Object[] _args = new Object[3];
    protected Colorization[] _zations;

    protected static enum Mode { NEW, BUY, TRY, DOWNLOAD };
    protected Mode _mode = Mode.NEW;

    protected static final Point[] CS_SPOTS = {
        new Point(150, 105),
        new Point(150, 61),
        new Point(150, 17),
    };
}
