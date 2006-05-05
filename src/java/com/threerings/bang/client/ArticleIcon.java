//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BImage;
import com.jmex.bui.icon.ImageIcon;

import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.data.Article;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Displays an avatar article in the inventory display.
 */
public class ArticleIcon extends ItemIcon
{
    @Override // documentation inherited
    protected void configureLabel (BasicContext ctx)
    {
        Article article = (Article)_item;
        AvatarLogic al = ctx.getAvatarLogic();
        ArticleCatalog.Article aca =
            al.getArticleCatalog().getArticle(article.getName());
        if (aca == null) {
            log.warning("Article no longer exists? " + this);
            return;
        }

        String ipath = "goods/articles/"+  article.getName() + ".png";
        Colorization[] zations = al.decodeColorizations(
            article.getComponents()[0], al.getColorizationClasses(aca));
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
