//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.util.Dimension;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.GameCodes;

/**
 * Displays contextual help on whatever the mouse is hovering over.
 */
public class ContextHelpView extends BDecoratedWindow
{
    public ContextHelpView (BangContext ctx)
    {
        super(ctx.getStyleSheet(),
              ctx.xlate(GameCodes.GAME_MSGS, "m.context_help_title"));
        _ctx = ctx;
        add(_text = new BLabel("", "context_help_text"));
    }

    /**
     * Displays the help text associated with the specified item.
     */
    public void setHelpItem (String item)
    {
        if (item == null) {
            _text.setText(_ctx.xlate(GameCodes.GAME_MSGS, "m.help_default"));

        } else if (item.startsWith("unit_")) {
            String type = item.substring(5);
            String name = _ctx.xlate(BangCodes.UNITS_MSGS, "m." + type);
            String descrip = _ctx.xlate(
                BangCodes.UNITS_MSGS, "m." + type + "_descrip");
            _text.setText(name + "\n" + descrip);

        } else {
            _text.setText(_ctx.xlate(GameCodes.GAME_MSGS, "m.help_" + item));
        }
    }

    @Override // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return new Dimension(250, 250);
    }

    protected BangContext _ctx;
    protected BLabel _text;
}
