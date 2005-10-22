//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.avatar.data.AvatarCodes;

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
    public ArticleGood (String type, int scripCost, int coinCost)
    {
        super(type, scripCost, coinCost);
    }

    /** A constructor only used during serialization. */
    public ArticleGood ()
    {
    }

    @Override // documentation inherited
    public boolean isAvailable (PlayerObject user)
    {
        // TODO: make sure the gender matches
        return true;
    }

    @Override // documentation inherited
    public String getName ()
    {
        return MessageBundle.qualify(AvatarCodes.ARTICLE_MSGS, "m." + _type);
    }

    @Override // documentation inherited
    public String getTip ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.article_tip");
    }
}
