//
// $Id$

package com.threerings.bang.avatar.client;

import com.jme.image.Image;
import com.jme.renderer.Renderer;
import com.jmex.bui.BContainer;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;
import com.jmex.bui.util.RenderUtil;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Article;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.BarberObject;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.util.AvatarLogic;

/**
 * Allows the customization of looks with clothing and accessories.
 */
public class WearClothingView extends BContainer
    implements ArticlePalette.Inspector
{
    public WearClothingView (BangContext ctx, StatusLabel status)
    {
        super(new AbsoluteLayout());

        add(_pick = new PickLookView(ctx), new Point(707, 135));
        add(_articles = new ArticlePalette(ctx, this),
            new Rectangle(139, 5, ItemIcon.ICON_SIZE.width*4,
                          ItemIcon.ICON_SIZE.height*3+27));

        // create our tab display which will trigger the avatar display
        String[] tabs = new String[AvatarLogic.SLOTS.length];
        for (int ii = 0; ii < tabs.length; ii++) {
            tabs[ii] = AvatarLogic.SLOTS[ii].name;
        }
        final Image tabbg = ctx.loadImage("ui/barber/side_change_clothes.png");
        add(new HackyTabs(ctx, true, "ui/barber/tab_", tabs, 54, 30) {
            protected void renderBackground (Renderer renderer) {
                super.renderBackground(renderer);
                RenderUtil.blendState.apply();
                RenderUtil.renderImage(
                    tabbg, 0, _height - tabbg.getHeight() - 42);
            }
            protected void tabSelected (int index) {
                setSlot(index);
            }
        }, new Rectangle(10, 35, 140, 470));
    }

    /**
     * Called by the {@link BarberView} to give us a reference to our barber
     * object when needed.
     */
    public void setBarberObject (BarberObject barbobj)
    {
        _pick.setBarberObject(barbobj);
    }

    // documentation inherited from interface ArticlePalette.Inspector
    public void iconUpdated (SelectableIcon icon, boolean selected)
    {
        if (selected) {
            Article article = (Article)((ItemIcon)icon).getItem();
            _pick.getSelection().setArticle(article);
        } else {
            _pick.getSelection().articles[_slotidx] = 0;
        }
        _pick.refreshDisplay();
    }

    protected void setSlot (int slotidx)
    {
        _slotidx = slotidx;
        _articles.setSlot(AvatarLogic.SLOTS[slotidx].name);
    }

    protected int _slotidx;
    protected PickLookView _pick;
    protected ArticlePalette _articles;
}
