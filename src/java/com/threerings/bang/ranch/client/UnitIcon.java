//
// $Id$

package com.threerings.bang.ranch.client;

import com.jme.bui.BIcon;

import com.threerings.bang.client.Model;
import com.threerings.bang.client.ModelCache;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

/**
 * Displays a static view of a unit model for use as an icon in interface
 * displays.
 */
public class UnitIcon extends SelectableIcon
{
    public UnitIcon (BangContext ctx, int itemId, UnitConfig config)
    {
        super(new BIcon(ctx.getModelCache().
                        getModel("units", config.type).getIcon(),
                        Model.ICON_SIZE, Model.ICON_SIZE), config.type);
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

    @Override // documentation inherited
    protected void stateDidChange ()
    {
        super.stateDidChange();

        if (_selected && _parent instanceof UnitPalette) {
            ((UnitPalette)_parent).iconSelected(this);
        }
    }

    protected int _itemId;
    protected UnitConfig _config;
}
