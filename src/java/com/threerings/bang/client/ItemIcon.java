//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.BMenuItem;
import com.jmex.bui.BPopupMenu;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;

import com.threerings.bang.store.client.SongDownloadView;

import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.data.Article;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.Song;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;

/**
 * Displays an icon and descriptive text for a particular inventory item.
 */
public class ItemIcon extends PaletteIcon
    implements ActionListener
{
    public ItemIcon (BasicContext ctx, Item item)
    {
        this(ctx, item, false);
    }

    public ItemIcon (BasicContext ctx, Item item, boolean small)
    {
        _ctx = ctx;
        _item = item;
        _small = small;
        setIcon(_item.createIcon(ctx, _item.getIconPath(small)));
        String text = _item.getName(small);
        if (text != null) {
            setText(ctx.xlate(BangCodes.BANG_MSGS, text));
        }
        String tt = _item.getTooltip();
        if (tt != null) {
            setTooltipText(ctx.xlate(BangCodes.BANG_MSGS, tt));
        }
    }

    /**
     * Returns the item associated with this icon.
     */
    public Item getItem ()
    {
        return _item;
    }

    /**
     * Indicates whether our item popup menu should be enabled.
     */
    public void setMenuEnabled (boolean menuEnabled)
    {
        _menuEnabled = menuEnabled;
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        BangContext ctx = (BangContext)_ctx;
        String cmd = event.getAction();
        if ("destroy".equals(cmd)) {
            // TODO: implement

        } else if ("article_print".equals(cmd)) {
            BangUI.copyToClipboard(((Article)_item).getPrint());
            ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, "m.article_print_copied");

        } else if ("download_song".equals(cmd)) {
            ctx.getBangClient().displayPopup(
                new SongDownloadView(ctx, ((Song)_item).getSong()), true, 500);
        }
    }

    @Override // from BToggleButton
    public boolean dispatchEvent (BEvent event)
    {
        if (isEnabled() && _menuEnabled && event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            if (mev.getType() == MouseEvent.MOUSE_PRESSED) {
                displayItemPopup(mev.getX(), mev.getY());
                return true;
            }
        }
        return super.dispatchEvent(event);
    }

    protected void displayItemPopup (int mx, int my)
    {
        // if we're just in a test harness, no can do
        if (!(_ctx instanceof BangContext)) {
            return;
        }

        BPopupMenu menu = new BPopupMenu(getWindow());
        menu.addListener(this);

        // add their name as a non-menu item
        menu.add(new BLabel("@=u(" + getText() + ")", "popupmenu_title"));

        // add item specific stuff (this is sort of a hack but there's not really anywhere better
        // to put this code)
        if (_item instanceof Article) {
            menu.addMenuItem(createItem("article_print"));
        } else if (_item instanceof Song) {
            menu.addMenuItem(createItem("download_song"));
        }

        // all items have a "destroy" menu item
        menu.addMenuItem(createItem("destroy"));
        menu.popup(mx, my, false);
    }

    protected BMenuItem createItem (String action)
    {
        return new BMenuItem(_ctx.xlate(BangCodes.BANG_MSGS, "m.item_" + action), action);
    }

    protected BasicContext _ctx;
    protected Item _item;
    protected boolean _menuEnabled;
}
