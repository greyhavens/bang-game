//
// $Id$

package com.threerings.bang.store.data;

import java.util.Date;

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
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.GuestHandle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BasicContext;

/**
 * Represents an article of clothing or an accessory for sale.
 */
public class ArticleGood extends Good
{
    /**
     * Creates a good representing the specified article.
     */
    public ArticleGood (String type, String townId, int scripCost, int coinCost,
            String qualifier, Date start, Date stop)
    {
        super(type, townId, scripCost, coinCost, ARTICLE_PRIORITY);
        _qualifier = qualifier;
        _dstart = start;
        _dstop = stop;
        if (_dstop != null) {
            _stop = _dstop.getTime();
        }
    }

    /** A constructor only used during serialization. */
    public ArticleGood ()
    {
    }

    @Override // from Good
    public ImageIcon createIcon (BasicContext ctx, DObject entity, int[] colorIds)
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
    public String[] getColorizationClasses (BasicContext ctx)
    {
        ArticleCatalog.Article article =
            ctx.getAvatarLogic().getArticleCatalog().getArticle(_type);
        return (article == null) ? null : ctx.getAvatarLogic().getColorizationClasses(article);
    }

    @Override // from Good
    public boolean isGoldPassFree ()
    {
        return true;
    }

    @Override // from Good
    public boolean isAvailable (PlayerObject user)
    {
        // make sure the gender matches
        boolean isMale = (user.handle instanceof GuestHandle ? user.isMale :
                (_type.indexOf("female") == -1));
        return user.isMale == isMale &&
            (_qualifier == null || (user.tokens.isSupport() && !"ai".equals(_qualifier)));
    }

    @Override // from Good
    public boolean isPending (long timestamp)
    {
        return _dstart != null && _dstart.after(new Date(timestamp));
    }

    @Override // from Good
    public boolean isExpired (long timestamp)
    {
        return _dstop != null && _dstop.before(new Date(timestamp));
    }

    @Override // from Good
    public String getName ()
    {
        return Article.getName(_type);
    }

    @Override // from Good
    public String getTip ()
    {
        if (_stop != 0 && _dstop == null) {
            _dstop = new Date(_stop);
        }
        return MessageBundle.qualify(BangCodes.GOODS_MSGS,
                (_dstop == null ? "m.article_tip" : MessageBundle.tcompose(
                        "m.article_tip_expires", Article.EXPIRE_FORMAT.format(_dstop))));
    }

    @Override // from Good
    public int getCoinType ()
    {
        return CoinTransaction.DUDS_PURCHASE;
    }

    protected String _qualifier;
    protected long _stop;
    protected transient Date _dstart, _dstop;

    protected static final int ARTICLE_PRIORITY = 0;
}
