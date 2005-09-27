//
// $Id$

package com.threerings.bang.store.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.BlankIcon;
import com.jmex.bui.ImageIcon;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.StoreObject;

/**
 * Displays detailed information on a particular good.
 */
public class GoodsInspector extends BContainer
    implements IconPalette.Inspector, ActionListener
{
    public GoodsInspector (BangContext ctx, BTextArea status)
    {
        _ctx = ctx;
        _status = status;

        setLayoutManager(GroupLayout.makeHoriz(GroupLayout.LEFT));
        add(_icon = new BLabel(""));

        BContainer vert, horiz;
        add(vert = new BContainer(GroupLayout.makeVStretch()));

        vert.add(_title = new BLabel(""));
        _title.setLookAndFeel(BangUI.dtitleLNF);
        vert.add(_descrip = new BLabel(""));
        vert.add(horiz = GroupLayout.makeHBox(GroupLayout.LEFT));

        horiz.add(_cost = new MoneyLabel(ctx));
        _cost.setMoney(0, 0, false);
        horiz.add(new BLabel(new BlankIcon(25, 10))); // spacer
        BButton buy;
        horiz.add(buy = new BButton(_ctx.xlate("store", "m.buy")));
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
        _good = ((GoodsIcon)icon).getGood();
        _icon.setIcon(new ImageIcon(_ctx.loadImage(_good.getIconPath())));
        _title.setText(_ctx.xlate(BangCodes.GOODS_MSGS, _good.getName()));
        _descrip.setText(_ctx.xlate(BangCodes.GOODS_MSGS, _good.getTip()));
        _cost.setMoney(_good.getScripCost(), _good.getCoinCost(), false);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if (_good == null || _stobj == null) {
            return;
        }

        StoreService.ConfirmListener cl = new StoreService.ConfirmListener() {
            public void requestProcessed () {
                _status.setText(_ctx.xlate("store", "m.purchased"));
            }
            public void requestFailed (String cause) {
                _status.setText(_ctx.xlate("store", cause));
            }
        };
        _stobj.service.buyGood(_ctx.getClient(), _good.getType(), cl);
    }

    protected BangContext _ctx;
    protected StoreObject _stobj;
    protected Good _good;

    protected BLabel _icon, _title, _descrip;
    protected MoneyLabel _cost;

    protected BTextArea _status;
}
