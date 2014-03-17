//
// $Id$

package com.threerings.bang.gang.client;

import java.awt.Color;
import java.awt.image.BufferedImage;

import java.util.ArrayList;

import com.jme.renderer.Renderer;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.Interval;
import com.samskivert.util.StringUtil;

import com.threerings.media.image.ImageUtil;
import com.threerings.presents.dobj.DSet;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.MultiIconButton;
import com.threerings.bang.client.bui.RequestButton;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BuckleInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.BuckleUpgrade;
import com.threerings.bang.data.Item;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.client.BuckleView;
import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.util.GangUtil;

import static com.threerings.bang.Log.*;

/**
 * Allows gang leaders to configure their belt buckles.
 */
public class BuckleDialog extends BDecoratedWindow
    implements ActionListener, IconPalette.Inspector, HideoutCodes, GangCodes
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

        ccont.add(_icontrols = GroupLayout.makeVBox(GroupLayout.CENTER));
        _icontrols.setPreferredSize(new Dimension(240, 230));
        _icontrols.add(new Spacer(1, 10));
        _icontrols.add(new BLabel(_msgs.get("m.icon_editor"), "buckle_heading"));
        _icontrols.add(new Spacer(1, 4));

        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)bcont.getLayoutManager()).setGap(20);
        _icontrols.add(bcont);

        BContainer lcont = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)lcont.getLayoutManager()).setOffAxisPolicy(GroupLayout.EQUALIZE);
        lcont.add(_raise = createAltButton("raise"));
        lcont.add(_lower = createAltButton("lower"));
        lcont.add(createAltButton("remove"));
        bcont.add(lcont);

        BContainer rcont = new BContainer(new BorderLayout());
        rcont.add(createArrowButton("up", 0, -4), BorderLayout.NORTH);
        rcont.add(createArrowButton("down", 0, +4), BorderLayout.SOUTH);
        rcont.add(createArrowButton("right", +4, 0), BorderLayout.EAST);
        rcont.add(createArrowButton("left", -4, 0), BorderLayout.WEST);
        rcont.add(new BLabel(_msgs.get("m.move"), "buckle_label"), BorderLayout.CENTER);
        bcont.add(rcont);

        _icontrols.add(new BLabel(_msgs.get("m.icon_tip"), "buckle_tip"));
        _icontrols.setEnabled(false);

        if (_ctx.getUserObject().gangRank == LEADER_RANK) {
            ccont.add(new RequestButton(ctx, HIDEOUT_MSGS, "m.commit", _status) {
                public void fireRequest () {
                    BucklePart[] parts = new BucklePart[_buckle.length];
                    for (int ii = 0; ii < parts.length; ii++) {
                        parts[ii] = _parts.get(_buckle[ii]);
                    }
                    _hideoutobj.service.setBuckle(parts, this);
                }
                public void requestProcessed () {
                    super.requestProcessed();
                    _ctx.getBangClient().clearPopup(BuckleDialog.this, true);
                }
            }, GroupLayout.FIXED);
            ccont.add(new Spacer(1, 5), GroupLayout.FIXED);
        }
        ccont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"), GroupLayout.FIXED);

        if (ctx.getUserObject().tokens.isSupport()) {
            ccont.add(new BButton(_msgs.get("m.buckle_print"), this, "buckle_print"),
                    GroupLayout.FIXED);
        }

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
            moveSelectedIcon(0, -2);
        } else if (action.equals("down")) {
            moveSelectedIcon(0, +2);
        } else if (action.equals("left")) {
            moveSelectedIcon(-2, 0);
        } else if (action.equals("right")) {
            moveSelectedIcon(+2, 0);
        } else if (action.equals("raise")) {
            swapSelectedIcon(_selidx + 1);
        } else if (action.equals("lower")) {
            swapSelectedIcon(_selidx - 1);
        } else if (action.equals("remove")) {
            _iicons.get(_buckle[_selidx]).setSelected(false);
        } else if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);
        } else if (action.equals("buckle_print")) {
            BucklePart[] parts = new BucklePart[_buckle.length];
            for (int ii = 0; ii < parts.length; ii++) {
                parts[ii] = _parts.get(_buckle[ii]);
            }
            BuckleInfo binfo = GangUtil.getBuckleInfo(parts);
            if (BangUI.copyToClipboard(StringUtil.toString(binfo.print, "", ""))) {
                _ctx.getChatDirector().displayFeedback(
                        BarberCodes.BARBER_MSGS, "m.print_copied");
            }
        }
    }

    // documentation inherited from interface IconPalette.Inspector
    public void iconUpdated (SelectableIcon icon, boolean selected)
    {
        int partId = ((ItemIcon)icon).getItem().getItemId(),
            pidx = IntListUtil.indexOf(_buckle, partId);
        if (selected && pidx == -1) {
            // append or set and update preview
            if (_pcrec.isMultiple()) {
                _buckle = ArrayUtil.append(_buckle, partId);
                setSelectedIcon(_buckle.length - 1);
            } else {
                _buckle[_pcrec.idx] = partId;
                _preview.update();
            }

        } else if (!selected && _pcrec.isMultiple() && pidx != -1) {
            // remove and deselect or adjust selection index
            _buckle = ArrayUtil.splice(_buckle, pidx, 1);
            if (_selidx == pidx) {
                setSelectedIcon(-1);
            } else {
                if (pidx < _selidx) {
                    _selidx--;
                }
                _preview.update();
            }
        }
    }

    /**
     * Creates a button to move in one of the four cardinal directions, wrapped in a container to
     * keep it from being stretched out.
     */
    protected BContainer createArrowButton (String action, final int dx, final int dy)
    {
        String pref = "ui/icons/small_" + action + "_arrow";

        MultiIconButton arrow = new MultiIconButton(
            new ImageIcon(_ctx.loadImage(pref + ".png")), this, action) {
            protected void stateDidChange () {
                super.stateDidChange();
                // schedule an interval to move it repeatedly on extended press
                if (_armed && _pressed) {
                    _interval.schedule(INITIAL_ARROW_DELAY, REPEAT_ARROW_DELAY);
                } else {
                    _interval.cancel();
                }
            }
            protected void wasRemoved () {
                super.wasRemoved();
                _interval.cancel(); // make sure the interval isn't running anymore
            }
            protected Interval _interval = new Interval(_ctx.getClient().getRunQueue()) {
                public void expired () {
                    moveSelectedIcon(dx, dy);
                }
            };
        };
        arrow.setIcon(new ImageIcon(_ctx.loadImage(pref + "_disable.png")), BComponent.DISABLED);
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

        // update the number of icons selectable
        _palette.setSelectable(_pcrec.isMultiple() ? _gangobj.getMaxBuckleIcons() : 1);

        // add icons for the parts of that class
        _palette.clear();
        _iicons.clear();
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
            _palette.addIcon(0, iicon);
            _iicons.put(part.getItemId(), iicon);
            if (IntListUtil.contains(_buckle, part.getItemId())) {
                iicon.setSelected(true);
            }
        }
    }

    /**
     * Moves the selected icon by the given amount.
     */
    protected void moveSelectedIcon (int dx, int dy)
    {
        BucklePart part = _parts.get(_buckle[_selidx]);
        part.setPosition(
            (short)Math.min(Math.max(part.getX() + dx, _sxmin), _sxmax),
            (short)Math.min(Math.max(part.getY() + dy, _symin), _symax));
    }

    /**
     * Swaps the selected icon with the one at the specified index.
     */
    protected void swapSelectedIcon (int oidx)
    {
        int tmp = _buckle[_selidx];
        _buckle[_selidx] = _buckle[oidx];
        _buckle[oidx] = tmp;
        _selidx = oidx;
        _preview.update();
        updateLayerButtons();
    }

    /**
     * Selects one of the icons in the buckle by its index in the part list.
     */
    protected void setSelectedIcon (int idx)
    {
        if (_selidx == idx) {
            return;
        }
        _selidx = idx;
        _preview.update();
        _icontrols.setEnabled(_selidx != -1);
        updateLayerButtons();
    }

    /**
     * Updates the enabled states of the buttons that raise or lower the selected icon.
     */
    protected void updateLayerButtons ()
    {
        _raise.setEnabled(_selidx != -1 && _selidx < _buckle.length - 1);
        _lower.setEnabled(_selidx > 2);
    }

    /** Displays the buckle being configured. */
    protected class BucklePreview extends BContainer
    {
        public BucklePreview ()
        {
            super(new AbsoluteLayout(true));
            setStyleClass("buckle_preview");

            Rectangle brect = new Rectangle(
                32, 20, BUCKLE_ICON_SIZE.width, BUCKLE_ICON_SIZE.height);
            add(_base = new BuckleView(_ctx, 2), brect);
            add(new BComponent() {
                public boolean dispatchEvent (BEvent event) {
                    if (!(event instanceof MouseEvent) ||
                        ((MouseEvent)event).getType() != MouseEvent.MOUSE_PRESSED) {
                        return super.dispatchEvent(event);
                    }
                    // (de)select the first icon that passes the hit test
                    MouseEvent mevent = (MouseEvent)event;
                    int rx = mevent.getX() - getAbsoluteX(),
                        ry = BUCKLE_ICON_SIZE.height - (mevent.getY() - getAbsoluteY());
                    for (int ii = _buckle.length - 1; ii >= 2; ii--) {
                        if (_icons.get(_buckle[ii]).hitTest(rx, ry)) {
                            setSelectedIcon(_selidx == ii ? -1 : ii);
                            return true;
                        }
                    }
                    setSelectedIcon(-1);
                    return true;
                }
                protected void renderComponent (Renderer renderer) {
                    // render the icons in order
                    for (int ii = 2; ii < _buckle.length; ii++) {
                        _icons.get(_buckle[ii]).render(renderer);
                    }
                }
            }, brect);

            String msg = BuckleUpgrade.getName(_gangobj.getMaxBuckleIcons());
            add(new BLabel(_msgs.xlate(msg), "buckle_preview_label"),
                new Rectangle(22, 149, 176, 14));
        }

        public void update ()
        {
            // the base displays the background and border
            _base.setBuckle(GangUtil.getBuckleInfo(ArrayUtil.splice(_buckle, 2), _parts));

            // the rest are icons
            for (int ii = 2; ii < _buckle.length; ii++) {
                int partId = _buckle[ii];
                PreviewIcon icon = _icons.get(partId);
                if (icon == null) {
                    BucklePart part = _parts.get(partId);
                    if (part == null) {
                        log.warning("Buckle icon missing from inventory", "partId", partId);
                        continue;
                    }
                    _icons.put(partId, icon = new PreviewIcon(part));
                }
                icon.setSelected(_selidx == ii);
            }
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            super.wasRemoved();
            for (PreviewIcon icon : _icons.values()) {
                icon.release();
            }
            _icons.clear();
        }

        protected BuckleView _base;
        protected HashIntMap<PreviewIcon> _icons = new HashIntMap<PreviewIcon>();
    }

    /** Displays a highlighted icon. */
    protected class PreviewIcon
    {
        public PreviewIcon (BucklePart part)
        {
            _part = part;
            _bimg = BuckleView.getPartIcon(_ctx, _part, _tbounds);
            _uimg = new BImage(_bimg);
            _uimg.reference();
        }

        public void setSelected (boolean selected)
        {
            if (selected == _selected) {
                return;
            }
            getImage().release();
            _selected = selected;
            if (_selected) {
                // create the traced image if necessary and compute the extents to which we can move
                // the icon
                if (_simg == null) {
                    _simg = new BImage(ImageUtil.createTracedImage(
                        _ctx.getImageManager(), _bimg, Color.WHITE, 2, 1f, 0.5f));
                }
                _sxmin = -_tbounds.x;
                _sxmax = AvatarLogic.BUCKLE_WIDTH - (_tbounds.x + _tbounds.width);
                _symin = -_tbounds.y;
                _symax = AvatarLogic.BUCKLE_HEIGHT - (_tbounds.y + _tbounds.height);
            }
            getImage().reference();
        }

        public void render (Renderer renderer)
        {
            getImage().render(renderer, _part.getX()/2, -_part.getY()/2, 1f);
        }

        public boolean hitTest (int x, int y)
        {
            int ix = x - _part.getX()/2, iy = y - _part.getY()/2;
            return (ix >= 0 && iy >= 0 && ix < BUCKLE_ICON_SIZE.width &&
                iy < BUCKLE_ICON_SIZE.height) ? ImageUtil.hitTest(_bimg, ix, iy) : false;
        }

        public void release ()
        {
            getImage().release();
        }

        protected BImage getImage ()
        {
            return (_selected ? _simg : _uimg);
        }

        protected BucklePart _part;
        protected boolean _selected;
        protected BufferedImage _bimg;
        protected java.awt.Rectangle _tbounds = new java.awt.Rectangle();
        protected BImage _uimg, _simg;
    }

    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;
    protected MessageBundle _msgs;

    protected DSet<BucklePart> _parts;
    protected int[] _buckle;
    protected int _selidx = -1;
    protected int _sxmin, _sxmax, _symin, _symax;

    protected BucklePreview _preview;
    protected BContainer _icontrols;
    protected BButton _raise, _lower;
    protected IconPalette _palette;
    protected StatusLabel _status;

    protected HashIntMap<ItemIcon> _iicons = new HashIntMap<ItemIcon>();
    protected AvatarLogic.PartClass _pcrec;

    /** The initial delay in milliseconds before arrow movements start repeating. */
    protected static final long INITIAL_ARROW_DELAY = 500L;

    /** The delay between repeated arrow movements. */
    protected static final long REPEAT_ARROW_DELAY = 25L;

    protected static final String[] TABS = { "icon", "border", "background" };
    protected static final Dimension BUCKLE_ICON_SIZE =
        new Dimension(AvatarLogic.BUCKLE_WIDTH / 2, AvatarLogic.BUCKLE_HEIGHT / 2);
}
