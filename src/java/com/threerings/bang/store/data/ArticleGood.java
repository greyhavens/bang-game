//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.avatar.data.AvatarCodes;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.BangCodes;

/**
 * Represents an article of clothing or an accessory for sale.
 */
public class ArticleGood extends Good
{
    /**
     * Creates a good representing the specified article.
     */
    public ArticleGood (
            String type, int scripCost, int coinCost, String qualifier)
    {
        super(type, scripCost, coinCost);
        _qualifier = qualifier;
    }

    /** A constructor only used during serialization. */
    public ArticleGood ()
    {
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return Article.getIconPath(_type);
    }

    @Override // documentation inherited
    public boolean isAvailable (PlayerObject user)
    {
        // make sure the gender matches
        boolean isMale = (_type.indexOf("female") == -1);
        return user.isMale == isMale && 
            (_qualifier == null || 
             (user.tokens.isAdmin() && !"ai".equals(_qualifier))
    }

    @Override // documentation inherited
    public String getName ()
    {
        return Article.getName(_type);
    }

    @Override // documentation inherited
    public String getTip ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.article_tip");
    }

    protected String _qualifier;
}
