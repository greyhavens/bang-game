//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BImage;
import com.jmex.bui.icon.ImageIcon;

import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

import com.threerings.bang.avatar.data.AvatarCodes;

import com.threerings.bang.data.Article;
import com.threerings.bang.util.BasicContext;

/**
 * Displays an avatar article in the inventory display.
 */
public class ArticleIcon extends ItemIcon
{
    public ArticleIcon ()
    {
        setStyleClass("article_icon"); // adjust insets
    }

    @Override // documentation inherited
    protected void configureLabel (BasicContext ctx)
    {
        Article article = (Article)_item;
        String ipath = "goods/articles/"+  article.getName() + ".png";
        Colorization[] zations = ctx.getAvatarLogic().decodeColorizations(
            article.getComponents()[0]);
        if (zations != null) {
            BImage image = new BImage(
                ImageUtil.recolorImage(
                    ctx.getImageCache().getBufferedImage(ipath), zations));
            setIcon(new ImageIcon(image));
        }
        String mkey = "m." + article.getName();
        setText(ctx.xlate(AvatarCodes.ARTICLE_MSGS, mkey));
    }
}
