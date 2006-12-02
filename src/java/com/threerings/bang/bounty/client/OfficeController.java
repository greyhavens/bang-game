//
// $Id$

package com.threerings.bang.bounty.client;

import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.bounty.data.OfficeObject;
import com.threerings.bang.util.BangContext;

/**
 * Manages the client side of the Sheriff's Office.
 */
public class OfficeController extends PlaceController
    implements ActionListener
{
    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("bounty_test".equals(event.getAction())) {
            _ctx.getBangClient().displayPopup(
                new BountyGameEditor(_ctx, (OfficeObject)_plobj), true, -1);
        }
    }

    @Override // from PlaceController
    public void init (CrowdContext ctx, PlaceConfig config)
    {
        super.init(ctx, config);
        _ctx = (BangContext)ctx;
    }

    // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return new OfficeView((BangContext)ctx, this);
    }

    protected BangContext _ctx;
}
