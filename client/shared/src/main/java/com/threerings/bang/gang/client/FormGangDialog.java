//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.QuickSort;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.RequestDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.NameFactory;

import com.threerings.bang.gang.data.HideoutCodes;

/**
 * Displays a dialog allowing a player to pick a name for their new gang.
 */
public class FormGangDialog extends RequestDialog
    implements HideoutCodes
{
    public static void show (BangContext ctx, StatusLabel status, HideoutService hsvc)
    {
        ctx.getBangClient().displayPopup(new FormGangDialog(ctx, status, hsvc), true, 400);
    }

    public FormGangDialog (BangContext ctx, StatusLabel status, HideoutService hsvc)
    {
        super(ctx, HIDEOUT_MSGS, "m.gang_name_tip", "m.form_gang", "m.cancel", "m.formed_gang",
            status);
        _hsvc = hsvc;

        // add the name entry panel
        BContainer ncont = GroupLayout.makeHBox(GroupLayout.CENTER);
        add(1, ncont);
        ncont.add(new BLabel(_ctx.xlate(HIDEOUT_MSGS, "m.gang_name_prefix")));

        // initialize the root with the player's name
        _root = new BTextField(ctx.getUserObject().handle.toString(),
            NameFactory.getValidator().getMaxHandleLength());
        _root.setPreferredWidth(140);
        ncont.add(_root);

        // sort the gang name suffixes by name and start with a random one
        String[] suffixes = NameFactory.getCreator().getGangSuffixes().toArray(new String[0]);
        QuickSort.sort(suffixes);
        _suffix = new BComboBox(suffixes);
        _suffix.selectItem(RandomUtil.getInt(suffixes.length));
        ncont.add(_suffix);

        // enable the form button for valid roots
        new EnablingValidator(_root, _buttons[0]) {
            protected boolean checkEnabled (String text) {
                return NameFactory.getValidator().isValidHandle(new Handle(text));
            }
        };
    }

    // documentation inherited
    protected void fireRequest (Object result)
    {
        _hsvc.formGang(new Handle(_root.getText()), (String)_suffix.getSelectedItem(), this);
    }

    protected HideoutService _hsvc;

    protected BTextField _root;
    protected BComboBox _suffix;
}
