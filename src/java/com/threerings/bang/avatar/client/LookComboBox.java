//
// $Id$

package com.threerings.bang.avatar.client;

import com.jmex.bui.BComboBox;
import com.samskivert.util.StringUtil;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.Look;

/**
 * Displays a combo box with a list of the user's looks.
 */
public class LookComboBox extends BComboBox
{
    public LookComboBox (BangContext ctx)
    {
        _ctx = ctx;
        _deflook = _ctx.xlate(AvatarCodes.AVATAR_MSGS, "m.default_look");

        // start with a proper set of looks so that selectLook() can be called
        // between construct time and the time that we're first shown
        refreshLooks();
    }

    /**
     * Returns the selected {@link Look}.
     */
    public Look getSelectedLook ()
    {
        String name = (String)getSelectedItem();
        if (name.equals(_deflook)) {
            name = "";
        }
        return _ctx.getUserObject().looks.get(name);
    }

    /**
     * Sets the selection to the specified look.
     */
    public void selectLook (Look look)
    {
        selectItem(getName(look));
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        // refresh our available looks in case new ones showed up
        refreshLooks();
    }

    protected void refreshLooks ()
    {
        PlayerObject user = _ctx.getUserObject();
        String[] looks = new String[user.looks.size()];
        int idx = 0;
        for (Look look : user.looks) {
            looks[idx++] = getName(look);
        }
        setItems(looks);
    }

    protected String getName (Look look)
    {
        return !StringUtil.isBlank(look.name) ? look.name : _deflook;
    }

    protected BangContext _ctx;
    protected String _deflook;
}
