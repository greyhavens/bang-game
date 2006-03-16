//
// $Id$

package com.threerings.bang.store.client;

import java.util.ArrayList;

import com.jmex.bui.BImage;
import com.jmex.bui.icon.ImageIcon;

import com.threerings.media.image.ColorPository;
import com.threerings.media.image.Colorization;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.avatar.util.ColorConstraints;

import com.threerings.bang.store.data.ArticleGood;
import com.threerings.bang.store.data.Good;

/**
 * Displays a salable good.
 */
public class GoodsIcon extends PaletteIcon
{
    /** Contains our randomly selected color ids for colorized goods. */
    public int[] colorIds;

    public GoodsIcon (BangContext ctx, Good good)
    {
        _ctx = ctx;
        setGood(good);
    }

    public Good getGood ()
    {
        return _good;
    }

    public void setGood (Good good)
    {
        _good = good;

        BImage image;
        if (_good instanceof ArticleGood) {
            AvatarLogic al = _ctx.getAvatarLogic();
            String[] cclasses = al.getColorizationClasses(
                al.getArticleCatalog().getArticle(_good.getType()));
            colorIds = new int[3];
            Colorization[] zations = new Colorization[cclasses.length];
            for (int ii = 0; ii < zations.length; ii++) {
                ColorPository.ColorRecord crec = 
                    ColorConstraints.pickRandomColor(
                        al.getColorPository(), cclasses[ii],
                        _ctx.getUserObject());
                if (crec == null) {
                    continue;
                }
                int cidx = AvatarLogic.getColorIndex(crec.cclass.name);
                colorIds[cidx] = crec.colorId;
                zations[ii] = crec.getColorization();
            }
            image = _ctx.getImageCache().createColorizedBImage(
                good.getIconPath(), zations, true);
        } else {
            image = _ctx.loadImage(good.getIconPath());
        }

        setIcon(new ImageIcon(image));
        setText(_ctx.xlate(BangCodes.GOODS_MSGS, good.getName()));
    }

    protected BangContext _ctx;
    protected Good _good;
}
