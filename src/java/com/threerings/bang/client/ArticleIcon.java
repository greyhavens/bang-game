//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.icon.ImageIcon;

import com.threerings.bang.avatar.data.AvatarCodes;

import com.threerings.bang.data.Article;
import com.threerings.bang.util.BangContext;

/**
 * Displays an avatar article in the inventory display.
 */
public class ArticleIcon extends ItemIcon
{
    @Override // documentation inherited
    protected void configureLabel (BangContext ctx)
    {
        Article article = (Article)_item;
        setIcon(new ImageIcon(ctx.loadImage("ui/unknown_item.png")));
        String mkey = "m." + article.getName();
        setText(ctx.xlate(AvatarCodes.ARTICLE_MSGS, mkey));
    }
}
