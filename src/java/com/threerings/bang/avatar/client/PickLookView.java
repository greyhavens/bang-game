//
// $Id$

package com.threerings.bang.avatar.client;

import java.util.Iterator;

import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.StringUtil;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.Look;

/**
 * Allows a player to select one of their active looks for use.
 */
public class PickLookView extends BContainer
    implements ActionListener// , SetListener
{
    public PickLookView (BangContext ctx)
    {
        super(GroupLayout.makeVert(GroupLayout.CENTER));

        _ctx = ctx;
        add(_avatar = new AvatarView(ctx));
        add(_looks = new BComboBox());
        _looks.addListener(this);
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        // rebuild our available looks
        PlayerObject user = _ctx.getUserObject();
        String[] looks = new String[user.looks.size()];
        int idx = 0;
        for (Iterator iter = user.looks.iterator(); iter.hasNext(); ) {
            Look look = (Look)iter.next();
            looks[idx++] = !StringUtil.blank(look.name) ? look.name :
                _ctx.xlate(AvatarCodes.AVATAR_MSGS, "m.default_look");
        }
        _looks.setItems(looks);

        // select their current look (which will update the display)
        Look current = user.getLook();
        if (current != null) {
            _looks.selectItem(current.name);
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String name = (String)_looks.getSelectedItem();
        Look look = (Look)_ctx.getUserObject().looks.get(name);
        if (look != null) {
            _avatar.setAvatar(look.getAvatar());
        }
    }

    protected BangContext _ctx;
    protected AvatarView _avatar;
    protected BComboBox _looks;
}
