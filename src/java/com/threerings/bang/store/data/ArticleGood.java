//
// $Id$

package com.threerings.bang.store.data;

import com.jmex.bui.icon.ImageIcon;

import com.threerings.media.image.ColorPository.ColorRecord;
import com.threerings.media.image.Colorization;

import com.threerings.presents.dobj.DObject;

import com.threerings.coin.server.persist.CoinTransaction;
import com.threerings.util.MessageBundle;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

/**
 * Represents an article of clothing or an accessory for sale.
 */
public class ArticleGood extends Good
{
    /**
     * Creates a good representing the specified article.
     */
    public ArticleGood (String type, int scripCost, int coinCost, String qualifier)
    {
        super(type, scripCost, coinCost);
        _qualifier = qualifier;
    }

    /** A constructor only used during serialization. */
    public ArticleGood ()
    {
    }

    @Override // from Good
    public ImageIcon createIcon (BangContext ctx, DObject entity, int[] colorIds)
    {
        AvatarLogic al = ctx.getAvatarLogic();
        ColorRecord[] crecs = al.pickRandomColors(getColorizationClasses(ctx), entity);
        Colorization[] zations = new Colorization[crecs.length];
        for (int ii = 0; ii < crecs.length; ii++) {
            ColorRecord crec = crecs[ii];
            if (crec == null) {
                continue;
            }
            // skip skin, which some article goods use
            if (AvatarLogic.SKIN.equals(crec.cclass.name)) {
                continue;
            }
            int cidx = AvatarLogic.getColorIndex(crec.cclass.name);
            colorIds[cidx] = crec.colorId;
            zations[ii] = crec.getColorization();
        }
        return createIcon(ctx, zations);
    }

    @Override // from Good
    public String getIconPath ()
    {
        return Article.getIconPath(_type);
    }

    @Override // from Good
    public String[] getColorizationClasses (BangContext ctx)
    {
        ArticleCatalog.Article article =
            ctx.getAvatarLogic().getArticleCatalog().getArticle(_type);
        return (article == null) ? null : ctx.getAvatarLogic().getColorizationClasses(article);
    }

    @Override // from Good
    public boolean isAvailable (PlayerObject user)
    {
        // make sure the gender matches
        boolean isMale = (_type.indexOf("female") == -1);
        return user.isMale == isMale &&
            (_qualifier == null || (user.tokens.isSupport() && !"ai".equals(_qualifier)));
    }

    @Override // from Good
    public String getName ()
    {
        return Article.getName(_type);
    }

    @Override // from Good
    public String getTip ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.article_tip");
    }

    @Override // from Good
    public int getCoinType ()
    {
        return CoinTransaction.DUDS_PURCHASE;
    }

    protected String _qualifier;
}
