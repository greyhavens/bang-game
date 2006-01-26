//
// $Id$

package com.threerings.bang.ranch.client;

import com.jmex.bui.util.Dimension;

import com.threerings.bang.client.Model;
import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BasicContext;

/**
 * Displays a static view of a unit model for use as an icon in interface
 * displays.
 */
public class UnitIcon extends PaletteIcon
{
    public UnitIcon (BasicContext ctx, int itemId, UnitConfig config)
    {
        _itemId = itemId;
        _config = config;
        setText(ctx.xlate("units", config.getName()));
        setIcon(ctx.loadModel("units", config.type).getIcon());
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
