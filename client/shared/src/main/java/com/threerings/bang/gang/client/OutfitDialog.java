//
// $Id$

package com.threerings.bang.gang.client;

import java.util.ArrayList;
import java.util.HashMap;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ArrayUtil;

import com.threerings.util.MessageBundle;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Article;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.client.ColorSelector;
import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.avatar.util.ArticleCatalog;

import com.threerings.bang.store.client.GoodsPalette;
import com.threerings.bang.store.data.ArticleGood;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.GoodsObject;

import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.bang.gang.data.RentalGood;
import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.store.client.GoodsIcon;
import com.threerings.media.image.Colorization;

/**
 * Allows gang leaders to select and purchase outfits for their gangs.
 */
public class OutfitDialog extends BDecoratedWindow
    implements ActionListener, IconPalette.Inspector, HideoutCodes, GangCodes
{
    public OutfitDialog (BangContext ctx, HideoutObject hideoutobj, GangObject gangobj)
    {
        super(ctx.getStyleSheet(), ctx.xlate(HIDEOUT_MSGS, "t.outfit_dialog"));
        setStyleClass("outfit_dialog");
        setModal(true);
        _ctx = ctx;
        _hideoutobj = hideoutobj;
        _gangobj = gangobj;
        _msgs = ctx.getMessageManager().getBundle(HIDEOUT_MSGS);

        ((GroupLayout)getLayoutManager()).setGap(0);

        BContainer pcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        pcont.setStyleClass("outfit_articles");
        ((GroupLayout)pcont.getLayoutManager()).setOffAxisJustification(GroupLayout.TOP);
        ((GroupLayout)pcont.getLayoutManager()).setGap(0);
        add(pcont, GroupLayout.FIXED);

        BContainer lcont = new BContainer(new BorderLayout(-5, 0));
        pcont.add(lcont);

        ImageIcon divicon = new ImageIcon(_ctx.loadImage("ui/hideout/vertical_divider.png"));
        lcont.add(new BLabel(divicon), BorderLayout.EAST);

        BContainer ltcont = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)ltcont.getLayoutManager()).setOffAxisJustification(GroupLayout.RIGHT);
        lcont.add(ltcont, BorderLayout.CENTER);

        BContainer acont = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)acont.getLayoutManager()).setGap(-2);
        acont.setStyleClass("outfit_avatar_left");
        acont.add(_favatar = new AvatarView(ctx, 4, true, false));
        acont.add(new BLabel(_msgs.get("m.cowgirls"), "outfit_scroll"));
        ltcont.add(acont);

        ltcont.add(_ltabs =
            new HackyTabs(ctx, true, "ui/hideout/outfit_tab_left_", TABS, 68, 10) {
                protected void tabSelected (int index) {
                    OutfitDialog.this.tabSelected(index, false);
                }
            });
        _ltabs.setStyleClass("outfit_tabs_left");
        _ltabs.setDefaultTab(-1);

        pcont.add(_palette = new GoodsPalette(_ctx, 5, 3) {
            public boolean autoSelectFirstItem () {
                return false;
            }
            protected boolean isAvailable (Good good) {
                return ((GangGood)good).isAvailable(_gangobj);
            }
            protected DObject getColorEntity () {
                return _gangobj;
            }
        });
        _goodsobj = new GoodsObject() {
            public DSet<Good> getGoods() {
                return _hideoutobj.rentalGoods;
            }
            public void buyGood (String type, Object[] args, InvocationService.ConfirmListener cl) {
                _hideoutobj.service.rentGangGood(type, args, cl);
            }
        };
        _palette.init(_goodsobj);
        _palette.setPaintBackground(true);
        _palette.setShowNavigation(false);

        BContainer rcont = new BContainer(new BorderLayout(-5, 0));
        pcont.add(rcont);

        rcont.add(new BLabel(divicon), BorderLayout.WEST);

        BContainer rtcont = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)rtcont.getLayoutManager()).setOffAxisJustification(GroupLayout.LEFT);
        rcont.add(rtcont, BorderLayout.CENTER);

        acont = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)acont.getLayoutManager()).setGap(-2);
        acont.setStyleClass("outfit_avatar_right");
        acont.add(_mavatar = new AvatarView(ctx, 4, true, false));
        acont.add(new BLabel(_msgs.get("m.cowboys"), "outfit_scroll"));
        rtcont.add(acont);

        rtcont.add(_rtabs =
            new HackyTabs(ctx, true, "ui/hideout/outfit_tab_right_", TABS, 68, 10) {
                protected void tabSelected (int index) {
                    OutfitDialog.this.tabSelected(index, true);
                }
            });
        _rtabs.setStyleClass("outfit_tabs_right");
        _rtabs.setDefaultTab(-1);

        add(new Spacer(1, -4), GroupLayout.FIXED);
        BContainer ncont = GroupLayout.makeHBox(GroupLayout.RIGHT);
        ncont.setPreferredSize(new Dimension(715, -1));
        ncont.add(_palette.getNavigationContainer());
        add(ncont, GroupLayout.FIXED);

        add(new Spacer(1, -10), GroupLayout.FIXED);
        BContainer ccont = new BContainer(GroupLayout.makeHStretch());
        ccont.setPreferredSize(900, 160);
        add(ccont, GroupLayout.FIXED);

        ccont.add(_icont = GroupLayout.makeHBox(GroupLayout.LEFT));
        ((GroupLayout)_icont.getLayoutManager()).setGap(10);
        _palette.setInspector(this);

        BContainer bcont = GroupLayout.makeVBox(GroupLayout.CENTER);
        bcont.setStyleClass("outfit_controls");
        ((GroupLayout)bcont.getLayoutManager()).setPolicy(GroupLayout.STRETCH);
        bcont.add(new BLabel(_msgs.get("m.total_price"), "outfit_total_price"), GroupLayout.FIXED);
        bcont.add(_qlabel = new MoneyLabel(ctx));
        bcont.add(_buy = new BButton(_msgs.get("m.rent_item"), this, "rent_item"),
            GroupLayout.FIXED);
        ccont.add(bcont, GroupLayout.FIXED);

        BContainer dcont = GroupLayout.makeVBox(GroupLayout.CENTER);
        dcont.setStyleClass("outfit_controls");
        ((GroupLayout)dcont.getLayoutManager()).setPolicy(GroupLayout.STRETCH);
        BContainer cofcont = GroupLayout.makeVBox(GroupLayout.CENTER);
        cofcont.add(new BLabel(_msgs.get("m.coffers"), "coffer_label"));
        cofcont.add(_coffers = new CofferLabel(ctx, gangobj));
        dcont.add(cofcont);
        dcont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"), GroupLayout.FIXED);
        ccont.add(dcont, GroupLayout.FIXED);

        _buy.setEnabled(false);

        add(_status = new StatusLabel(ctx), GroupLayout.FIXED);
        _status.setStatus(" ", false); // make sure it takes up space

        // pick random aspects for the avatars and update them
        _faspects = _ctx.getAvatarLogic().pickRandomAspects(false, _ctx.getUserObject());
        updateAvatar(false);
        _maspects = _ctx.getAvatarLogic().pickRandomAspects(true, _ctx.getUserObject());
        updateAvatar(true);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("rent_item")) {
            if (_good == null) {
                return;
            }
            _buy.setEnabled(false);
            HideoutService.ConfirmListener cl = new HideoutService.ConfirmListener() {
                public void requestProcessed () {
                }
                public void requestFailed (String cause) {
                    _buy.setEnabled(true);
                    _status.setStatus(_msgs.xlate(cause), true);
                }
            };
            _goodsobj.buyGood(_good.getType(), _args, cl);

        } else if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }

    // documentation inherited from interface IconPalette.Inspector
    public void iconUpdated (SelectableIcon icon, boolean selected)
    {
        _status.setStatus("", false);
        if (!selected) {
            if (icon == _gicon && _key != null) {
                _oarts.remove(_key);
                updateAvatar(_key.male);
                _key = null;
                populateInspector(null);
            }
        } else {
            populateInspector((GoodsIcon)icon);
        }
        _buy.setEnabled(selected);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _ltabs.selectTab(0);
    }

    /**
     * Called when the user selects an article tab on either the male or female side.
     */
    protected void tabSelected (int index, boolean male)
    {
        if (index != -1) {
            (male ? _ltabs : _rtabs).selectTab(-1, false);
            _selmale = male;
            _selidx = index;
            _palette.setFilter(_filters[(male ? 0 : 1)][index]);
            populateInspector(null);
        }
    }

    /**
     * Configures the item inspector to manipulate the specified icon (or <code>null</code>
     * to clear it out).
     */
    protected void populateInspector (GoodsIcon icon)
    {
        // add the item icon label
        _icont.removeAll();
        if ((_gicon = icon) == null) {
            // insert the tip text
            BContainer tcont = GroupLayout.makeVBox(GroupLayout.TOP);
            ((GroupLayout)tcont.getLayoutManager()).setOffAxisJustification(GroupLayout.LEFT);
            ((GroupLayout)tcont.getLayoutManager()).setGap(10);
            tcont.add(new BLabel(_msgs.get("m.outfit_tip"), "outfit_tip"));
            BContainer scont = new BContainer(new TableLayout(2, 8, 5));
            scont.setStyleClass("outfit_tip_steps");
            for (int ii = 0; ii < 3; ii++) {
                scont.add(new BLabel((ii + 1) + ".", "outfit_tip"));
                scont.add(new BLabel(_msgs.get("m.outfit_tip_step." + ii), "outfit_tip"));
            }
            tcont.add(scont);
            _icont.add(tcont);
            _qlabel.setMoney(0, 0, false);
            return;
        }
        _icont.add(_ilabel = new BLabel(icon.getIcon(), "outfit_item"));
        _good = (RentalGood)_gicon.getGood();
        String[] cclasses = _good.getColorizationClasses(_ctx);
        _isels.clear();
        if (cclasses != null && cclasses.length > 0) {
            _args[0] = _args[1] = _args[2] = Integer.valueOf(0);

            // add the color selectors with the article's current colors
            BContainer scont = GroupLayout.makeVBox(GroupLayout.CENTER);
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
                ColorSelector colorsel = new ColorSelector(_ctx, cclass, _gangobj, _colorpal);
                colorsel.setSelectedColorId(colorIds[index]);
                colorsel.setProperty("index", Integer.valueOf(index));
                scont.add(colorsel);
                _isels.add(colorsel);
                _args[index] = Integer.valueOf(colorsel.getSelectedColor());
                _zations[index] = colorsel.getSelectedColorization();
            }
            _icont.add(scont);
            _icont.add(new Spacer(10, 1));
        } else {
            _zations = null;
        }
        updateAvatar();

        // add the description and price labels
        BContainer dcont = new BContainer(GroupLayout.makeVStretch());
        dcont.setPreferredSize(-1, 135);
        dcont.add(new BLabel(_msgs.xlate(_good.getName()), "medium_title"), GroupLayout.FIXED);
        BLabel tlabel = new BLabel(_msgs.get("m.article_tip"), "goods_descrip");
        tlabel.setPreferredSize(250, -1);
        dcont.add(tlabel);
        BContainer pcont = GroupLayout.makeHBox(GroupLayout.LEFT);
        pcont.add(new BLabel(_msgs.get("m.item_price"), "table_data"));
        MoneyLabel plabel = new MoneyLabel(_ctx);
        plabel.setMoney(_good.getScripCost(), _good.getCoinCost(), false);
        _qlabel.setMoney(
                _good.getRentalScripCost(_gangobj), _good.getRentalCoinCost(_gangobj), false);
        pcont.add(plabel);
        dcont.add(pcont, GroupLayout.FIXED);
        _icont.add(dcont);
    }

    /**
     * Checks whether the described article matches the user's current tab selection.
     */
    protected boolean matchesSelection (ArticleCatalog.Article catart)
    {
        if (isMaleArticle(catart.name) != _selmale) {
            return false;
        }
        switch (_selidx) {
            case 1:
                return catart.slot.equals("hat");
            case 2:
                return catart.slot.equals("clothing");
            case 3:
                return !(catart.slot.equals("hat") || catart.slot.equals("clothing"));
            default:
                return false;
        }
    }

    protected void updateAvatar ()
    {
        if (!(_good.getGood() instanceof ArticleGood)) {
            return;
        }
        int zations = 0;
        for (ColorSelector sel : _isels) {
            zations |= AvatarLogic.composeZation(sel.getColorClass(), sel.getSelectedColor());
        }
        String name = _good.getType();
        AvatarLogic alogic = _ctx.getAvatarLogic();
        Article article = alogic.createArticle(
            -1, alogic.getArticleCatalog().getArticle(name), zations);
        _key = new OutfitKey(article);
        _oarts.put(_key, new OutfitArticle(name, zations));
        updateAvatar(_key.male);
    }

    /**
     * Updates the specified avatar with the current outfit selection.
     */
    protected void updateAvatar (boolean male)
    {
        int[] avatar = male ? _maspects : _faspects;
        AvatarLogic alogic = _ctx.getAvatarLogic();
        ArticleCatalog artcat = alogic.getArticleCatalog();
        for (OutfitArticle oart : _oarts.values()) {
            if (isMaleArticle(oart.article) == male) {
                int[] compIds = alogic.getComponentIds(
                    artcat.getArticle(oart.article), oart.zations);
                if (compIds != null) {
                    avatar = ArrayUtil.concatenate(avatar, compIds);
                }
            }
        }
        (male ? _mavatar : _favatar).setAvatar(new AvatarInfo(avatar));
    }

    /**
     * Checks whether the identified article is for men.
     */
    protected static boolean isMaleArticle (String name)
    {
        return (name.indexOf("female") == -1);
    }

    /**
     * Identifies a slot/gender combination for which there may be one article selection.
     */
    protected static class OutfitKey
    {
        /** The slot into which the article fits. */
        public String slot;

        /** Whether or not the article is for men. */
        public boolean male;

        public OutfitKey (Article article)
        {
            slot = article.getSlot();
            male = isMaleArticle(article.getArticleName());
        }

        public OutfitKey (ArticleCatalog.Article catart)
        {
            slot = catart.slot;
            male = isMaleArticle(catart.name);
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return 2 * slot.hashCode() + (male ? 1 : 0);
        }

        @Override // documentation inherited
        public boolean equals (Object obj)
        {
            OutfitKey okey = (OutfitKey)obj;
            return slot.equals(okey.slot) && male == okey.male;
        }
    }

    protected ActionListener _colorpal = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            ColorSelector sel = (ColorSelector)event.getSource();
            int index = (Integer)sel.getProperty("index");
            _zations[index] = sel.getSelectedColorization();
            _gicon.colorIds[index] = sel.getSelectedColor();
            _args[index] = Integer.valueOf(sel.getSelectedColor());
            _ilabel.setIcon(_good.createIcon(_ctx, _zations));
            _gicon.setIcon(_ilabel.getIcon());
            updateAvatar();
        }
    };

    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;
    protected MessageBundle _msgs;

    /** The currently selected outfit. */
    protected HashMap<OutfitKey, OutfitArticle> _oarts = new HashMap<OutfitKey, OutfitArticle>();
    protected HashMap<OutfitKey, ItemIcon> _oicons = new HashMap<OutfitKey, ItemIcon>();

    /** Used to prevent unwanted article updates when switching between articles. */
    protected boolean _preventArticleUpdates;

    /** The male and female avatar views. */
    protected AvatarView _mavatar, _favatar;
    protected int[] _maspects, _faspects;

    /** The left and right article tabs. */
    protected HackyTabs _ltabs, _rtabs;
    protected boolean _selmale;
    protected int _selidx;

    /** Displays the articles in the current tab. */
    protected GoodsPalette _palette;

    /** Article inspector controls. */
    protected BContainer _icont;
    protected GoodsIcon _gicon;
    protected BLabel _ilabel;
    protected ArrayList<ColorSelector> _isels = new ArrayList<ColorSelector>();
    protected RentalGood _good;
    protected Colorization[] _zations;
    protected OutfitKey _key;
    protected GoodsObject _goodsobj;
    protected Object[] _args = new Object[3];

    /** Price quote/buy controls. */
    protected BButton _quote, _buy;
    protected MoneyLabel _qlabel;

    /** Coffer controls. */
    protected CofferLabel _coffers;

    protected StatusLabel _status;

    protected abstract class RentalFilter
        implements GoodsPalette.Filter
    {
        public boolean isValid (Good good) {
            return _ctx.getUserObject().hasAccess(good.getTownId()) && isValid((RentalGood)good);
        }

        public abstract boolean isValid (RentalGood good);
    }

    protected abstract class ArticleGoodFilter extends RentalFilter
    {
        public boolean isValid (RentalGood good) {
            if (good.getGood() instanceof ArticleGood) {
                ArticleCatalog.Article article =
                    _ctx.getAvatarLogic().getArticleCatalog().getArticle(
                        good.getType());
                return (article != null) && isValid(article);
            }
            return false;
        }
        public abstract boolean isValid (ArticleCatalog.Article article);
    }

    protected RentalFilter[][] _filters = new RentalFilter[][] {
        {
            new RentalFilter() { // items
                public boolean isValid (RentalGood good) {
                    return (!(good.getGood() instanceof ArticleGood));
                }
            },
            new ArticleGoodFilter() { // hats
                public boolean isValid (ArticleCatalog.Article article) {
                    return article.slot.equals("hat") && isMaleArticle(article.name);
                }
            },
            new ArticleGoodFilter() { // clothing
                public boolean isValid (ArticleCatalog.Article article) {
                    return article.slot.equals("clothing") && isMaleArticle(article.name);
                }
            },
            new ArticleGoodFilter() { // gear
                public boolean isValid (ArticleCatalog.Article article) {
                    return !article.slot.equals("hat") &&
                        !article.slot.equals("clothing") && isMaleArticle(article.name);
                }
            },
        }, {
            new RentalFilter() { // items
                public boolean isValid (RentalGood good) {
                    return (!(good.getGood() instanceof ArticleGood));
                }
            },
            new ArticleGoodFilter() { // hats
                public boolean isValid (ArticleCatalog.Article article) {
                    return article.slot.equals("hat") && !isMaleArticle(article.name);
                }
            },
            new ArticleGoodFilter() { // clothing
                public boolean isValid (ArticleCatalog.Article article) {
                    return article.slot.equals("clothing") && !isMaleArticle(article.name);
                }
            },
            new ArticleGoodFilter() { // gear
                public boolean isValid (ArticleCatalog.Article article) {
                    return !article.slot.equals("hat") &&
                        !article.slot.equals("clothing") && !isMaleArticle(article.name);
                }
            },
        }
    };

    protected static final String[] TABS = { "items", "hats", "clothes", "gear" };
}
