//
// $Id$

package com.threerings.bang.ranch.client;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BasicContext;

/**
 * Displays a static view of a unit model for use as an icon in interface
 * displays.
 */
public class UnitIcon extends SelectableIcon
{
    public UnitIcon (BasicContext ctx, int itemId, UnitConfig config)
    {
        BangUI.configUnitLabel(this, config);
        _itemId = itemId;
        _config = config;
    }

    public int getItemId ()
    {
        return _itemId;
    }

    public UnitConfig getUnit ()
    {
        return _config;
    }

    protected int _itemId;
    protected UnitConfig _config;
}
