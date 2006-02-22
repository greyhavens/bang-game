//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;

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
        super(ctx.getStyleSheet(), null);
        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);

        setLayoutManager(new BorderLayout());
        add(_title = new BLabel("", "context_help_title"), BorderLayout.NORTH);
        add(_text = new BLabel("", "context_help_text"), BorderLayout.CENTER);
        setHelpItem(null);
    }

    /**
     * Displays the help text associated with the specified item.
     */
    public void setHelpItem (String item)
    {
        if (item == null) {
            _title.setText(_msgs.get("m.help_title"));
            _text.setText(_msgs.get("m.help_default"));

        } else if (item.startsWith("unit_")) {
            String type = item.substring(5);
            _title.setText(_ctx.xlate(BangCodes.UNITS_MSGS, "m." + type));
            _text.setText(_ctx.xlate(BangCodes.UNITS_MSGS,
                                     "m." + type + "_descrip"));

        } else {
            _title.setText(_msgs.get("m.help_" + item + "_title"));
            _text.setText(_msgs.get("m.help_" + item));
        }
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BLabel _title, _text;
}
