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
        String ipath = "goods/articles/"+  article.getName() + ".png";
        setIcon(new ImageIcon(
                    ctx.getImageCache().createImage(
                        ctx.getImageCache().getBufferedImage(ipath),
                        ctx.getAvatarLogic().decodeColorizations(
                            article.getComponents()[0]), true)));
        String mkey = "m." + article.getName();
        setText(ctx.xlate(AvatarCodes.ARTICLE_MSGS, mkey));
    }
}
