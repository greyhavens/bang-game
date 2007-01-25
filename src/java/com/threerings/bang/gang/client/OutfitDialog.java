//
// $Id$

package com.threerings.bang.gang.client;

import java.util.Comparator;
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
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.QuickSort;

import com.threerings.media.image.ColorPository.ColorRecord;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Article;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BangUtil;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.client.ColorSelector;
import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.avatar.util.ArticleCatalog;

import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.data.OutfitArticle;

/**
 * Allows gang leaders to select and purchase outfits for their gangs.
 */
public class OutfitDialog extends BDecoratedWindow
    implements ActionListener, IconPalette.Inspector, HideoutCodes
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
        
        // remember the current gang outfit by article type
        ArticleCatalog acat = _ctx.getAvatarLogic().getArticleCatalog();
        for (OutfitArticle oart : _gangobj.outfit) {
            ArticleCatalog.Article catart = acat.getArticle(oart.article);
            _oarts.put(new OutfitKey(catart), oart);
        }
        
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
            new HackyTabs(ctx, true, "ui/hideout/outfit_tab_left_", TABS, 85, 10) {
                protected void tabSelected (int index) {
                    OutfitDialog.this.tabSelected(index, false);
                }
            });
        _ltabs.setStyleClass("outfit_tabs_left");
        
        pcont.add(_palette = new IconPalette(this, 5, 3, ItemIcon.ICON_SIZE, Integer.MAX_VALUE));
        _palette.setPaintBackground(true);
        _palette.setShowNavigation(false);
        populatePalette();
        
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
            new HackyTabs(ctx, true, "ui/hideout/outfit_tab_right_", TABS, 85, 10) {
                protected void tabSelected (int index) {
                    OutfitDialog.this.tabSelected(index, true);
                }
            });
        _rtabs.setStyleClass("outfit_tabs_right");
        _rtabs.setDefaultTab(-1);
        
        BContainer ncont = GroupLayout.makeHBox(GroupLayout.RIGHT);
        ncont.setPreferredSize(new Dimension(ItemIcon.ICON_SIZE.width * 5, -1));
        ncont.add(_palette.getNavigationContainer());
        add(ncont);
        
        add(_status = new StatusLabel(ctx), GroupLayout.FIXED);
             
        BContainer bcont = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER));
        bcont.add(_quote = new BButton(_msgs.get("m.get_quote"), this, "get_quote"));
        _quote.setEnabled(!_oarts.isEmpty());
        bcont.add(_buy = new BButton(_msgs.get("m.buy_outfits"), this, "buy_outfits"));
        _buy.setEnabled(!_oarts.isEmpty());
        bcont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(bcont, GroupLayout.FIXED);
        
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
        if (action.equals("get_quote")) {
            OutfitArticle[] oarts = _oarts.values().toArray(new OutfitArticle[0]);
            _quote.setEnabled(false);
            _hideoutobj.service.getOutfitQuote(_ctx.getClient(), oarts,
                new HideoutService.ResultListener() {
                    public void requestProcessed (Object result) {
                        int[] cost = (int[])result;
                        System.out.println(cost[0] + " " + cost[1]);
                        _quote.setEnabled(true);
                    }
                    public void requestFailed (String cause) {
                        _status.setStatus(_msgs.xlate(cause), true);
                        _quote.setEnabled(true);
                    }
                });
        } else if (action.equals("buy_outfits")) {
            OutfitArticle[] oarts = _oarts.values().toArray(new OutfitArticle[0]);
            _buy.setEnabled(false);
            _hideoutobj.service.buyOutfits(_ctx.getClient(), oarts,
                new HideoutService.ConfirmListener() {
                    public void requestProcessed () {
                        _buy.setEnabled(true);
                    }
                    public void requestFailed (String cause) {
                        _status.setStatus(_msgs.xlate(cause), true);
                        _buy.setEnabled(true);
                    }
                });
        } else if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }
    
    // documentation inherited from interface IconPalette.Inspector
    public void iconUpdated (SelectableIcon icon, boolean selected)
    {
        Article article = (Article)((ItemIcon)icon).getItem();
        OutfitKey key = new OutfitKey(article);
        if (selected) {
            ItemIcon oicon = _oicons.get(key);
            if (oicon != null) {
                _preventAvatarUpdate = true;
                oicon.setSelected(false);
                _preventAvatarUpdate = false;
            }
            _oicons.put(key, (ItemIcon)icon);
            _oarts.put(key, new OutfitArticle(
                article.getArticleName(), article.getComponents()[0] & 0xFFFF0000));
        } else {
            _oicons.remove(key);
            _oarts.remove(key);
        }
        if (!_preventAvatarUpdate) {
            updateAvatar(key.male);
        }
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
            populatePalette();
        }
    }
    
    /**
     * Populates the article palette with the articles in the currently selected gender/slot
     * combination.
     */
    protected void populatePalette ()
    {
        _palette.clear();
        _oicons.clear();
        
        // sort the articles in the catalog first by slot, then by town, then by name
        AvatarLogic alogic = _ctx.getAvatarLogic();
        ArticleCatalog.Article[] catarts = alogic.getArticleCatalog().getArticles().toArray(
            new ArticleCatalog.Article[0]);
        QuickSort.sort(catarts, new Comparator<ArticleCatalog.Article>() {
            public int compare (ArticleCatalog.Article a1, ArticleCatalog.Article a2) {
                int diff = AvatarLogic.getSlotIndex(a1.slot) - AvatarLogic.getSlotIndex(a2.slot);
                if (diff != 0) {
                    return diff;
                }
                diff = BangUtil.getTownIndex(a1.townId) - BangUtil.getTownIndex(a2.townId);
                return (diff == 0) ? a1.name.compareTo(a2.name) : diff;
            }
        });
        
        boolean support = _ctx.getUserObject().tokens.isSupport();
        for (ArticleCatalog.Article catart : catarts) {
            if (catart.qualifier != null && (!support || catart.qualifier.equals("ai"))) {
                continue;
            }
            if (!(catart.townId.equals(BangCodes.FRONTIER_TOWN) || support ||
                _ctx.getUserObject().holdsTicket(catart.townId))) {
                // TODO: filter the articles according to the gang state, perhaps by
                // making town articles accessible for gang outfits when the gangs
                // achieve a certain amount of experience in the town's scenarios
                continue;
            }
            if (!matchesSelection(catart)) {
                continue;
            }
            int zations = 0;
            boolean select = false;
            OutfitKey key = new OutfitKey(catart);
            OutfitArticle oart = _oarts.get(key);
            if (oart != null && oart.article.endsWith(catart.name)) {
                // use the colors configured for the current outfit
                zations = oart.zations;
                select = true;
            } else {
                // choose random colors
                for (ColorRecord crec : alogic.pickRandomColors(catart, _gangobj)) {
                    if (crec == null || AvatarLogic.SKIN.equals(crec.cclass.name)) {
                        continue;
                    }
                    zations |= AvatarLogic.composeZation(crec.cclass.name, crec.colorId);
                }
            }
            ItemIcon icon = new ItemIcon(_ctx, alogic.createArticle(-1, catart, zations));
            _palette.addIcon(icon);
            if (select) {
                icon.setSelected(true);
            }
        }
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
            case 0:
                return catart.slot.equals("hat");
            case 1:
                return catart.slot.equals("clothing");
            default:
                return !(catart.slot.equals("hat") || catart.slot.equals("clothing"));
        }
    }
    
    /**
     * Updates the specified avatar with the current outfit selection.
     */
    protected void updateAvatar (boolean male)
    {
        int[] avatar = (male ? _maspects : _faspects);
        AvatarLogic alogic = _ctx.getAvatarLogic();
        ArticleCatalog artcat = alogic.getArticleCatalog();
        for (OutfitArticle oart : _oarts.values()) {
            if (isMaleArticle(oart.article) == male) {
                avatar = ArrayUtil.concatenate(avatar,
                    alogic.getComponentIds(artcat.getArticle(oart.article), oart.zations));
            }
        }
        (male ? _mavatar : _favatar).setAvatar(avatar);
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
    
    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;
    protected MessageBundle _msgs;
    
    /** The currently selected outfit. */
    protected HashMap<OutfitKey, OutfitArticle> _oarts = new HashMap<OutfitKey, OutfitArticle>();
    protected HashMap<OutfitKey, ItemIcon> _oicons = new HashMap<OutfitKey, ItemIcon>();
    
    /** Used to prevent unwanted avatar updates when switching between articles. */
    protected boolean _preventAvatarUpdate;
    
    /** The male and female avatar views. */
    protected AvatarView _mavatar, _favatar;
    protected int[] _maspects, _faspects;
    
    /** The left and right article tabs. */
    protected HackyTabs _ltabs, _rtabs;
    protected boolean _selmale;
    protected int _selidx;
    
    protected IconPalette _palette;
    
    protected StatusLabel _status;
    
    protected BButton _quote, _buy;
    
    protected static final String[] TABS = { "hats", "clothes", "gear" };
}
