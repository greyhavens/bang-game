//
// $Id$

package com.threerings.bang.avatar.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.util.AvatarMetrics;

/**
 * Allows the customization of looks with clothing and accessories.
 */
public class WearClothingView extends BContainer
    implements ActionListener, ArticlePalette.Inspector
{
    public WearClothingView (BangContext ctx, BTextArea status)
    {
        super(new BorderLayout(5, 5));
        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(AvatarCodes.AVATAR_MSGS);

        BContainer cont = new BContainer(new BorderLayout(5, 5));
        cont.add(_look = new BComboBox(), BorderLayout.NORTH);
        cont.add(_avatar = new AvatarView(ctx), BorderLayout.CENTER);
        add(cont, BorderLayout.WEST);

        cont = new BContainer(new BorderLayout(5, 5));

        BContainer slotsel = new BContainer(GroupLayout.makeHStretch());
        BButton left = new BButton(BangUI.leftArrow, "down");
        left.addListener(this);
        slotsel.add(left, GroupLayout.FIXED);
        slotsel.add(_slot = new BLabel(""));
        _slot.setHorizontalAlignment(BLabel.CENTER);
        BButton right = new BButton(BangUI.rightArrow, "up");
        right.addListener(this);
        slotsel.add(right, GroupLayout.FIXED);
        cont.add(slotsel, BorderLayout.NORTH);

        cont.add(_articles = new ArticlePalette(ctx, this), BorderLayout.CENTER);
        add(cont, BorderLayout.CENTER);

        // start out with the first slot
        setSlot(0);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if ("down".equals(action)) {
            setSlot((_slotidx + AvatarMetrics.SLOTS.length - 1) %
                    AvatarMetrics.SLOTS.length);
        } else if ("up".equals(action)) {
            setSlot((_slotidx + 1) % AvatarMetrics.SLOTS.length);
        }
    }

    // documentation inherited from interface ArticlePalette.Inspector
    public void iconSelected (SelectableIcon icon)
    {
    }

    protected void setSlot (int slotidx)
    {
        _slotidx = slotidx;
        String slot = AvatarMetrics.SLOTS[slotidx].name;
        _slot.setText(_msgs.get("m." + slot));
        _articles.setSlot(slot);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected AvatarView _avatar;
    protected BComboBox _look;

    protected ArticlePalette _articles;
    protected BLabel _slot;
    protected int _slotidx;
}
