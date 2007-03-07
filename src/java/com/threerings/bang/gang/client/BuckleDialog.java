//
// $Id$

package com.threerings.bang.gang.client;

import java.util.ArrayList;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntListUtil;

import com.threerings.presents.dobj.DSet;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.RequestButton;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Item;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.client.BuckleView;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.util.GangUtil;

/**
 * Allows gang leaders to configure their belt buckles.
 */
public class BuckleDialog extends BDecoratedWindow
    implements ActionListener, IconPalette.Inspector, HideoutCodes
{
    public BuckleDialog (BangContext ctx, HideoutObject hideoutobj, GangObject gangobj)
    {
        super(ctx.getStyleSheet(), ctx.xlate(HIDEOUT_MSGS, "t.buckle_dialog"));
        setStyleClass("buckle_dialog");
        setModal(true);
        _ctx = ctx;
        _hideoutobj = hideoutobj;
        _gangobj = gangobj;
        _msgs = ctx.getMessageManager().getBundle(HIDEOUT_MSGS);

        BContainer mcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)mcont.getLayoutManager()).setGap(30);
        add(mcont, GroupLayout.FIXED);

        add(_status = new StatusLabel(_ctx), GroupLayout.FIXED);
        _status.setStatus(" ", false); // for layout purposes

        BContainer ccont = new BContainer(GroupLayout.makeVert(
            GroupLayout.STRETCH, GroupLayout.TOP, GroupLayout.NONE));
        mcont.add(ccont);

        ccont.add(new Spacer(1, 40), GroupLayout.FIXED);
        ccont.add(new BLabel(_msgs.get("m.buckle_preview"), "buckle_heading"), GroupLayout.FIXED);
        ccont.add(_preview = new BucklePreview(), GroupLayout.FIXED);

        ccont.add(_controls = new BContainer(GroupLayout.makeVStretch()));
        _controls.setPreferredSize(new Dimension(240, 230));

        _icontrols = GroupLayout.makeVBox(GroupLayout.CENTER);
        _icontrols.add(new Spacer(1, 10));
        _icontrols.add(new BLabel(_msgs.get("m.icon_editor"), "buckle_heading"));
        _icontrols.add(new Spacer(1, 4));

        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)bcont.getLayoutManager()).setGap(20);
        _icontrols.add(bcont);

        BContainer lcont = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)lcont.getLayoutManager()).setOffAxisPolicy(GroupLayout.EQUALIZE);
        lcont.add(createAltButton("raise"));
        lcont.add(createAltButton("lower"));
        lcont.add(createAltButton("remove"));
        bcont.add(lcont);

        BContainer rcont = new BContainer(new BorderLayout());
        rcont.add(createArrowButton("up"), BorderLayout.NORTH);
        rcont.add(createArrowButton("down"), BorderLayout.SOUTH);
        rcont.add(createArrowButton("right"), BorderLayout.EAST);
        rcont.add(createArrowButton("left"), BorderLayout.WEST);
        rcont.add(new BLabel(_msgs.get("m.move"), "buckle_label"), BorderLayout.CENTER);
        bcont.add(rcont);

        _icontrols.add(new BLabel(_msgs.get("m.icon_tip"), "buckle_tip"));

        _controls.add(_icontrols);

        ccont.add(new RequestButton(ctx, HIDEOUT_MSGS, "m.commit", _status) {
            public void fireRequest () {
                BucklePart[] parts = new BucklePart[_buckle.length];
                for (int ii = 0; ii < parts.length; ii++) {
                    parts[ii] = _parts.get(_buckle[ii]);
                }
                _hideoutobj.service.setBuckle(_ctx.getClient(), parts, this);
            }
            public void requestProcessed () {
                super.requestProcessed();
                _ctx.getBangClient().clearPopup(BuckleDialog.this, true);
            }
        }, GroupLayout.FIXED);
        ccont.add(new Spacer(1, 5), GroupLayout.FIXED);
        ccont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"), GroupLayout.FIXED);

        BContainer pcont = new BContainer(new BorderLayout(0, -8));
        mcont.add(pcont);

        pcont.add(new Spacer(10, 1), BorderLayout.WEST);
        pcont.add(_palette = new IconPalette(this, 4, 4, BUCKLE_ICON_SIZE, Integer.MAX_VALUE) {
            public void init (int columns, int rows, Dimension isize) {
                super.init(columns, rows, isize);
                _icont.setStyleClass("buckle_palette");
            }
        }, BorderLayout.CENTER);
        _palette.setPaintBorder(true);

        HackyTabs htabs = new HackyTabs(ctx, false, "ui/hideout/buckle_tab_", TABS, 157, 18) {
            protected void tabSelected (int index) {
                displayPartClass(TABS[index]);
            }
        };
        htabs.setStyleClass("buckle_tabs");
        pcont.add(htabs, BorderLayout.NORTH);

        // extract the buckle parts from the gang's inventory
        ArrayList<BucklePart> parts = new ArrayList<BucklePart>();
        for (Item item : _gangobj.inventory) {
            if (item instanceof BucklePart) {
                parts.add((BucklePart)item);
            }
        }
        _parts = new DSet<BucklePart>(parts);
        _buckle = _gangobj.buckle;
        _preview.update();

        displayPartClass(TABS[0]);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("up")) {
        } else if (action.equals("down")) {
        } else if (action.equals("left")) {
        } else if (action.equals("right")) {
        } else if (action.equals("raise")) {
        } else if (action.equals("lower")) {
        } else if (action.equals("remove")) {
        } else if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }

    // documentation inherited from interface IconPalette.Inspector
    public void iconUpdated (SelectableIcon icon, boolean selected)
    {
        int partId = ((ItemIcon)icon).getItem().getItemId(),
            pidx = IntListUtil.indexOf(_buckle, partId);
        if (selected && pidx == -1) {
            if (_pcrec.isMultiple()) {
                _buckle = ArrayUtil.append(_buckle, partId);
            } else {
                _buckle[_pcrec.idx] = partId;
            }
        } else if (!selected && _pcrec.isMultiple() && pidx != -1) {
            _buckle = ArrayUtil.splice(_buckle, pidx, 1);
        } else {
            return;
        }
        _preview.update();
    }

    /**
     * Creates a button to move in one of the four cardinal directions, wrapped in a container to
     * keep it from being stretched out.
     */
    protected BContainer createArrowButton (String action)
    {
        BButton arrow = new BButton(new ImageIcon(
            _ctx.loadImage("ui/icons/small_" + action + "_arrow.png")), this, action);
        arrow.setStyleClass("small_arrow_button");
        BContainer cont = GroupLayout.makeHBox(GroupLayout.CENTER);
        cont.add(arrow);
        return cont;
    }

    /**
     * Creates an alt-style button with the supplied action.
     */
    protected BButton createAltButton (String action)
    {
        BButton button = new BButton(_msgs.get("m." + action), this, action);
        button.setStyleClass("alt_button");
        return button;
    }

    /**
     * Populates the palette with all parts of the specified class.
     */
    protected void displayPartClass (String pclass)
    {
        // find the part class descriptor
        for (AvatarLogic.PartClass pcrec : AvatarLogic.BUCKLE_PARTS) {
            if (pcrec.name.equals(pclass)) {
                _pcrec = pcrec;
                break;
            }
        }

        // add icons for the parts of that class
        _palette.clear();
        for (BucklePart part : _parts) {
            if (!part.getPartClass().equals(pclass)) {
                continue;
            }
            ItemIcon iicon = new ItemIcon(_ctx, part) {
                public Dimension getPreferredSize (int whint, int hhint) {
                    return BUCKLE_ICON_SIZE;
                }
                protected void fireAction (long when, int modifiers) {
                    if (!_selected || _pcrec.isOptional()) {
                        super.fireAction(when, modifiers);
                    }
                }
            };
            iicon.setStyleClass("buckle_palette_icon");
            _palette.addIcon(iicon);
            if (IntListUtil.contains(_buckle, part.getItemId())) {
                iicon.setSelected(true);
            }
        }

        // update the number of icons selectable
        _palette.setSelectable(_pcrec.isMultiple() ? _gangobj.getMaxBuckleIcons() : 1);
        _icontrols.setVisible(_pcrec.isMultiple());
    }

    /** Displays the buckle being configured. */
    protected class BucklePreview extends BContainer
    {
        public BucklePreview ()
        {
            super(new AbsoluteLayout(true));
            setStyleClass("buckle_preview");

            Point bloc = new Point(32, 20);
            add(_bottom = new BuckleView(_ctx, 2), bloc);
            add(_top = new BuckleView(_ctx, 2), bloc);
            update();

            String msg = MessageBundle.compose("m.buckle_type",
                "m.buckle_icons." + _gangobj.getMaxBuckleIcons());
            add(new BLabel(_msgs.xlate(msg), "buckle_preview_label"),
                new Rectangle(22, 149, 176, 14));
        }

        public void update ()
        {
            _bottom.setBuckle(GangUtil.getBuckleInfo(_buckle, _parts));
        }

        BuckleView _bottom, _top;
    }

    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;
    protected MessageBundle _msgs;

    protected DSet<BucklePart> _parts;
    protected int[] _buckle;

    protected BucklePreview _preview;
    protected BContainer _controls, _icontrols;
    protected IconPalette _palette;
    protected StatusLabel _status;

    protected AvatarLogic.PartClass _pcrec;

    protected static final String[] TABS = { "icon", "border", "background" };
    protected static final Dimension BUCKLE_ICON_SIZE =
        new Dimension(AvatarLogic.BUCKLE_WIDTH / 2, AvatarLogic.BUCKLE_HEIGHT / 2);
}
