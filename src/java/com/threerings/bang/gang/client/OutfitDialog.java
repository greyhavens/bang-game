//
// $Id$

package com.threerings.bang.gang.client;

import java.util.HashMap;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.media.image.ColorPository.ColorRecord;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Article;
import com.threerings.bang.util.BangContext;

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
        super(ctx.getStyleSheet(), null);
        setModal(true);
        _ctx = ctx;
        _hideoutobj = hideoutobj;
        _gangobj = gangobj;
        _msgs = ctx.getMessageManager().getBundle(HIDEOUT_MSGS);
        
        // remember the current gang outfit by article type
        ArticleCatalog acat = _ctx.getAvatarLogic().getArticleCatalog();
        for (OutfitArticle oart : _gangobj.outfit) {
            ArticleCatalog.Article catart = acat.getArticle(oart.article);
            _oarts.put(new OutfitKey(catart), oart);
        }
        
        add(_palette = new IconPalette(this, 4, 3, ItemIcon.ICON_SIZE, Integer.MAX_VALUE),
            GroupLayout.FIXED);
        _palette.setPaintBackground(true);
        _palette.setPaintBorder(true);
        populatePalette();
        
        add(_status = new StatusLabel(ctx), GroupLayout.FIXED);
               
        BContainer bcont = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER));
        bcont.add(_quote = new BButton(_msgs.get("m.get_quote"), this, "get_quote"));
        _quote.setEnabled(!_oarts.isEmpty());
        bcont.add(_buy = new BButton(_msgs.get("m.buy_outfits"), this, "buy_outfits"));
        _buy.setEnabled(!_oarts.isEmpty());
        bcont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(bcont, GroupLayout.FIXED);
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
                oicon.setSelected(false);
            }
            _oicons.put(key, (ItemIcon)icon);
            _oarts.put(key, new OutfitArticle(
                article.getArticleName(), article.getComponents()[0]));
        } else {
            _oicons.remove(key);
            _oarts.remove(key);
        }
    }
    
    protected void populatePalette ()
    {
        _palette.clear();
        _oicons.clear();
        AvatarLogic alogic = _ctx.getAvatarLogic();
        for (ArticleCatalog.Article catart : alogic.getArticleCatalog().getArticles()) {
            if (catart.qualifier != null && (!_ctx.getUserObject().tokens.isSupport() ||
                    catart.qualifier.equals("ai"))) {
                continue;
            }
            int zations = 0;
            OutfitKey key = new OutfitKey(catart);
            OutfitArticle oart = _oarts.get(key);
            if (oart != null) {
                zations = oart.zations;
            } else {
                for (ColorRecord crec : alogic.pickRandomColors(catart, _gangobj)) {
                    if (crec == null || AvatarLogic.SKIN.equals(crec.cclass.name)) {
                        continue;
                    }
                    zations |= AvatarLogic.composeZation(crec.cclass.name, crec.colorId);
                }
            }
            ItemIcon icon = new ItemIcon(_ctx, alogic.createArticle(-1, catart, zations));
            _palette.addIcon(icon);
            if (oart != null) {
                icon.setSelected(true);
            }
        }
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
            male = article.getArticleName().indexOf("female") == -1;
        }
        
        public OutfitKey (ArticleCatalog.Article catart)
        {
            slot = catart.slot;
            male = catart.name.indexOf("female") == -1;
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
    
    protected IconPalette _palette;
    
    protected StatusLabel _status;
    
    protected BButton _quote, _buy;
}
