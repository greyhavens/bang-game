//
// $Id$

package com.threerings.bang.avatar.client;

import com.jme.renderer.Renderer;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.StringUtil;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Article;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.data.BarberObject;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.util.AvatarLogic;

/**
 * Allows the customization of looks with clothing and accessories.
 */
public class WearClothingView extends BContainer
    implements ArticlePalette.Inspector
{
    public WearClothingView (final BangContext ctx, StatusLabel status)
    {
        super(new AbsoluteLayout());

        add(_pick = new PickLookView(ctx, true), new Point(707, 135));
        add(_articles = new ArticlePalette(ctx, this, _pick),
            CONTENT_RECT);

        // create our tab display which will trigger the avatar display
        String[] tabs = new String[AvatarLogic.SLOTS.length];
        for (int ii = 0; ii < tabs.length; ii++) {
            tabs[ii] = AvatarLogic.SLOTS[ii].name;
        }
        final BImage blankbg = ctx.loadImage("ui/barber/tabs_back.png");
        final BImage tabbg = ctx.loadImage("ui/barber/side_change_clothes.png");
        HackyTabs htabs = new HackyTabs(ctx, true, "ui/barber/tab_", tabs, 54, 30) {
            protected void wasAdded () {
                super.wasAdded();
                blankbg.reference();
                tabbg.reference();
            }
            protected void wasRemoved () {
                super.wasRemoved();
                blankbg.release();
                tabbg.release();
            }
            protected void renderBackground (Renderer renderer) {
                super.renderBackground(renderer);
                blankbg.render(
                    renderer, -5, _height - blankbg.getHeight() - 5, _alpha);
                tabbg.render(
                    renderer, 0, _height - tabbg.getHeight() - 42, _alpha);
            }
            protected void tabSelected (int index) {
                setSlot(index);
            }
        };
        add(htabs, new Rectangle(10, 35, 140, 470));
        htabs.selectTab(1); // start with clothing as newbies have clothing

        // if we're an admin show a button that copies our avatar fingerprint to the clipboard
        if (ctx.getUserObject().tokens.isAdmin()) {
            String label = ctx.xlate(BarberCodes.BARBER_MSGS, "m.copy_print");
            add(new BButton(label, new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    Look look = _pick.getSelection();
                    if (look == null) {
                        return;
                    }
                    int[] print = look.getAvatar(ctx.getUserObject()).print;
                    if (BangUI.copyToClipboard(StringUtil.toString(print, "", ""))) {
                        ctx.getChatDirector().displayFeedback(
                            BarberCodes.BARBER_MSGS, "m.print_copied");
                    }
                }
            }, ""), new Point(740, 45));
        }
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

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // refresh our article palette to cause the active article to be
        // reselected when we switch to this tab
        if (_slotidx != -1) {
            _articles.setSlot(AvatarLogic.SLOTS[_slotidx].name);
        }
    }

    protected void setSlot (int slotidx)
    {
        if (_slotidx != slotidx) {
            _slotidx = slotidx;
            _articles.setSlot(AvatarLogic.SLOTS[slotidx].name);
        }
    }

    protected int _slotidx = -1;
    protected PickLookView _pick;
    protected ArticlePalette _articles;

    protected static final Rectangle CONTENT_RECT =
        new Rectangle(139, 5, ItemIcon.ICON_SIZE.width*4,
                      ItemIcon.ICON_SIZE.height*3+27);
}
