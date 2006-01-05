//
// $Id$

package com.threerings.bang.store.client;

import com.jme.image.Image;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jmex.bui.BComponent;
import com.jmex.bui.border.LineBorder;
import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.util.RenderUtil;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.util.ArticleCatalog;

import com.threerings.bang.store.data.ArticleGood;
import com.threerings.bang.store.data.Good;

/**
 * Displays the tabs next to the {@link GoodsPalette} and configures it with
 * the proper filters when selected.
 */
public class GoodsTabs extends BComponent
{
    public GoodsTabs (BangContext ctx, GoodsPalette palette)
    {
        _ctx = ctx;
        _palette = palette;

        addListener(_mlistener);

        // load up our tab images
        for (int ii = 0; ii < TABS.length; ii++) {
            _tabs[ii] = _ctx.loadImage("ui/store/tab_" + TABS[ii] + ".png");
        }

        // start with the top tab selected
        selectTab(0);
    }

    public void selectTab (int index)
    {
        if (_selidx != index) {
            _selidx = index;
            _palette.setFilter(_filters[_selidx]);
        }
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        RenderUtil.blendState.apply();
        int iy = getHeight() - TAB_HEIGHT*_selidx - _tabs[_selidx].getHeight();
        RenderUtil.renderImage(_tabs[_selidx], 0, iy);
    }

    protected abstract class ArticleGoodFilter implements GoodsPalette.Filter
    {
        public boolean isValid (Good good) {
            if (good instanceof ArticleGood) {
                ArticleCatalog.Article article =
                    _ctx.getAvatarLogic().getArticleCatalog().getArticle(
                        good.getType());
                return (article != null) && isValid(article);
            }
            return false;
        }
        public abstract boolean isValid (ArticleCatalog.Article article);
    }

    protected MouseAdapter _mlistener = new MouseAdapter() {
        public void mousePressed (MouseEvent event) {
            int mx = event.getX() - getAbsoluteX(),
                my = getHeight() - (event.getY() - getAbsoluteY());
            int tabidx = (my-TAB_BORDER)/TAB_HEIGHT;
            if (tabidx >= 0 && tabidx < TABS.length) {
                selectTab(tabidx);
            }
        }
    };

    protected GoodsPalette.Filter[] _filters = new GoodsPalette.Filter[] {
        new GoodsPalette.Filter() { // items
            public boolean isValid (Good good) {
                return (!(good instanceof ArticleGood));
            }
        },
        new ArticleGoodFilter() { // hats
            public boolean isValid (ArticleCatalog.Article article) {
                return article.slot.equals("hat");
            }
        },
        new ArticleGoodFilter() { // clothing
            public boolean isValid (ArticleCatalog.Article article) {
                return article.slot.equals("clothing");
            }
        },
        new ArticleGoodFilter() { // gear
            public boolean isValid (ArticleCatalog.Article article) {
                return !article.slot.equals("hat") &&
                    !article.slot.equals("clothing");
            }
        },
    };

    protected BangContext _ctx;
    protected GoodsPalette _palette;
    protected Image[] _tabs = new Image[TABS.length];
    protected int _selidx = -1;

    protected static final String[] TABS = {
        "items", "hats", "clothes", "gear" };

    protected static final int TAB_HEIGHT = 76;
    protected static final int TAB_BORDER = 30;
}
