//
// $Id$

package com.threerings.bang.store.data;

import java.util.Date;

import com.jmex.bui.icon.ImageIcon;

import com.samskivert.util.StringUtil;

import com.threerings.media.image.ColorPository.ColorRecord;
import com.threerings.media.image.Colorization;

import com.threerings.presents.dobj.DObject;

import com.threerings.util.MessageBundle;

import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.Item;
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
    public boolean honorsGoldPass ()
    {
        return true;
    }

    @Override // from Good
    public boolean isAvailable (PlayerObject user)
    {
        if ("ai".equals(_qualifier)) {
            return false;
        }
        if (!StringUtil.isBlank(_qualifier) && !user.tokens.isSupport()) {
            String[] qualities = _qualifier.toLowerCase().split(":");
            if (qualities.length % 2 == 1) {
                return false;
            }
            for (int ii = 0; ii < qualities.length; ii += 2) {
                if ("badge".equals(qualities[ii])) {
                    if (!user.holdsBadge(Badge.getType((int)Long.parseLong(qualities[ii+1], 16)))) {
                        return false;
                    }
                } else if ("bigshot".equals(qualities[ii])) {
                    if (!user.holdsBigShot(qualities[ii+1])) {
                        return false;
                    }

                // all unknown qualifiers will immediately cause failure
                } else {
                    return false;
                }
            }

        }
        // make sure the gender matches
        return !user.hasCharacter() || user.isMale == (_type.indexOf("female") == -1);
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
                        "m.article_tip_expires", Item.EXPIRE_FORMAT.format(_dstop))));
    }

    @Override // from Good
    public String getQualifier ()
    {
        return _qualifier;
    }

    @Override // from Good
    public boolean wouldCreateItem (Item item)
    {
        if (!(item instanceof Article)) {
            return false;
        }
        return ((Article)item).getArticleName().equals(_type);
    }

    protected String _qualifier;
    protected long _stop;
    protected transient Date _dstart, _dstop;

    protected static final int ARTICLE_PRIORITY = 0;
}
