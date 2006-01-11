//
// $Id$

package com.threerings.bang.store.client;

import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.util.Dimension;

import com.threerings.media.image.ColorPository;
import com.threerings.media.image.Colorization;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.store.data.ArticleGood;
import com.threerings.bang.store.data.Good;

/**
 * Displays a salable good.
 */
public class GoodsIcon extends SelectableIcon
{
    public static final Dimension ICON_SIZE = new Dimension(136, 156);

    /** Contains our randomly selected color ids for colorized goods. */
    public int[] colorIds;

    public GoodsIcon (BangContext ctx, Good good)
    {
        _ctx = ctx;
        setOrientation(OVERLAPPING);
        setGood(good);
    }

    public Good getGood ()
    {
        return _good;
    }

    public void setGood (Good good)
    {
        _good = good;

        if (_good instanceof ArticleGood) {
            AvatarLogic al = _ctx.getAvatarLogic();
            String[] cclasses = al.getColorizationClasses(
                al.getArticleCatalog().getArticle(_good.getType()));
            colorIds = new int[3];
            Colorization[] zations = new Colorization[cclasses.length];
            for (int ii = 0; ii < zations.length; ii++) {
                ColorPository.ColorRecord crec =
                    al.getColorPository().getRandomStartingColor(cclasses[ii]);
                int cidx = AvatarLogic.getColorIndex(crec.cclass.name);
                colorIds[cidx] = crec.colorId;
                zations[ii] = crec.getColorization();
            }
            setIcon(new ImageIcon(
                        _ctx.getImageCache().createImage(
                            _ctx.getImageCache().getBufferedImage(
                                good.getIconPath()),
                            zations, true)));
        } else {
            setIcon(new ImageIcon(_ctx.loadImage(good.getIconPath())));
        }

        setText(_ctx.xlate(BangCodes.GOODS_MSGS, good.getName()));
    }

    public Dimension getPreferredSize (int whint, int hhint)
    {
        return ICON_SIZE;
    }

    protected BangContext _ctx;
    protected Good _good;
}
