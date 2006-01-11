//
// $Id$

package com.threerings.bang.store.client;

import java.awt.image.BufferedImage;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.media.image.Colorization;

import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.client.ColorSelector;
import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.store.data.ArticleGood;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.StoreObject;

/**
 * Displays detailed information on a particular good.
 */
public class GoodsInspector extends BContainer
    implements IconPalette.Inspector, ActionListener
{
    public GoodsInspector (BangContext ctx, StoreView parent)
    {
        super(new AbsoluteLayout());

        _ctx = ctx;
        _parent = parent;

        add(_icon = new BLabel(""), new Rectangle(0, 0, 136, 156));

        add(_title = new BLabel("", "medium_title"),
            new Rectangle(200, 110, 280, 40));
        add(_descrip = new BTextArea(""), new Rectangle(200, 45, 300, 65));
        _descrip.setStyleClass("goods_descrip");

        add(_cost = new MoneyLabel(ctx), new Rectangle(200, 15, 150, 25));
        _cost.setMoney(0, 0, false);

        BButton buy;
        add(buy = new BButton(_ctx.xlate("store", "m.buy")), new Point(400, 10));
        buy.setStyleClass("big_button");
        buy.addListener(this);
    }

    /**
     * Gives us access to our store object when it is available.
     */
    public void init (StoreObject stobj)
    {
        _stobj = stobj;
    }

    // documentation inherited from interface IconPalette.Inspector
    public void iconSelected (SelectableIcon icon)
    {
        // remove our color selectors
        for (int ii = 0; ii < _colorsel.length; ii++) {
            if (_colorsel[ii] != null) {
                remove(_colorsel[ii]);
                _colorsel[ii] = null;
            }
        }

        // configure our main interface with the good info
        _good = ((GoodsIcon)icon).getGood();
        _title.setText(_ctx.xlate(BangCodes.GOODS_MSGS, _good.getName()));
        _descrip.setText(_ctx.xlate(BangCodes.GOODS_MSGS, _good.getTip()));
        _cost.setMoney(_good.getScripCost(), _good.getCoinCost(), false);
        _srcimg = _ctx.getImageCache().getBufferedImage(_good.getIconPath());

        // do some special jockeying to handle colorizations for articles
        if (_good instanceof ArticleGood) {
            ArticleCatalog.Article article =
                _ctx.getAvatarLogic().getArticleCatalog().getArticle(
                    _good.getType());
            String[] cclasses =
                _ctx.getAvatarLogic().getColorizationClasses(article);
            _args[0] = _args[1] = _args[2] = Integer.valueOf(0);

            for (int ii = 0; ii < cclasses.length; ii++) {
                String cclass = cclasses[ii];
                if (cclass.equals(AvatarLogic.SKIN) ||
                    cclass.equals(AvatarLogic.HAIR)) {
                    continue;
                }

                // primary, secondary and tertiary colors have to go into the
                // appropriate index
                int index = cclass.endsWith("_p") ? 0 :
                    (cclass.endsWith("_s") ? 1 : 2);
                ColorSelector colorsel = new ColorSelector(_ctx, cclass);
                colorsel.addListener(_colorpal);
                colorsel.setProperty("index", Integer.valueOf(index));
                add(_colorsel[index] = colorsel, CS_SPOTS[index]);
                _args[index] = Integer.valueOf(colorsel.getSelectedColor());
                _zations[index] = colorsel.getSelectedColorization();
            }
        }

        updateImage();
    }

    // documentation inherited from interface IconPalette.Inspector
    public void selectionCleared ()
    {
        // nada
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if (_good == null || _stobj == null) {
            return;
        }

        StoreService.ConfirmListener cl = new StoreService.ConfirmListener() {
            public void requestProcessed () {
                _descrip.setText(_ctx.xlate("store", "m.purchased"));
                _parent.goodPurchased();
            }
            public void requestFailed (String cause) {
                _descrip.setText(_ctx.xlate("store", cause));
            }
        };
        _stobj.service.buyGood(_ctx.getClient(), _good.getType(), _args, cl);
    }

    protected void updateImage ()
    {
        _icon.setIcon(new ImageIcon(_ctx.getImageCache().createImage(
                                        _srcimg, _zations, true)));
    }

    protected ActionListener _colorpal = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            ColorSelector colorsel = (ColorSelector)event.getSource();
            int index = (Integer)colorsel.getProperty("index");
            _args[index] = Integer.valueOf(colorsel.getSelectedColor());
            _zations[index] = colorsel.getSelectedColorization();
            updateImage();
        }
    };

    protected BangContext _ctx;
    protected StoreObject _stobj;
    protected Good _good;

    protected BLabel _icon, _title;
    protected BTextArea _descrip;
    protected MoneyLabel _cost;

    protected BufferedImage _srcimg;
    protected StoreView _parent;
    protected ColorSelector[] _colorsel = new ColorSelector[3];

    protected Object[] _args = new Object[3];
    protected Colorization[] _zations = new Colorization[3];

    protected static final Point[] CS_SPOTS = {
        new Point(150, 105),
        new Point(150, 61),
        new Point(150, 17),
    };
}
