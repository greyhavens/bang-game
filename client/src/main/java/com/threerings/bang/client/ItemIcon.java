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

import com.threerings.util.MessageBundle;

import com.threerings.bang.store.client.SongDownloadView;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.client.util.ReportingListener;
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
        _small = small;
        setItem(item);
        String text = _item.getName(small);
        if (text != null) {
            setText(ctx.xlate(BangCodes.BANG_MSGS, text));
        }
        if (!(ctx instanceof BangContext)) {
            return;
        }
        String tt = _item.getTooltipText(((BangContext)ctx).getUserObject());
        if (tt != null) {
            setTooltipText(ctx.xlate(BangCodes.BANG_MSGS, tt));
        }
    }

    /**
     * Sets the item for this icon and updates the contents.
     */
    public void setItem (Item item)
    {
        _item = item;
        setIcon(_item.createIcon(_ctx, _item.getIconPath(_small)));
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

    /**
     * Indicates whether or not the item popup menu is enabled for this item.
     */
    public boolean isMenuEnabled ()
    {
        if (!_menuEnabled) {
            return false;
        }
        if (!(_ctx instanceof BangContext)) {
            return (_item instanceof Song);
        }
        BangContext ctx = (BangContext)_ctx;
        return ctx.getUserObject().tokens.isAdmin() || (_item instanceof Song);
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        BangContext ctx = (BangContext)_ctx;
        String cmd = event.getAction();
        if ("destroy".equals(cmd)) {
            OptionDialog.showConfirmDialog(ctx, BangCodes.BANG_MSGS,
                MessageBundle.compose("m.confirm_destroy", _item.getName()),
                new OptionDialog.ResponseReceiver() {
                    public void resultPosted (int button, Object result) {
                        if (button == OptionDialog.OK_BUTTON) {
                            destroyItem();
                        }
                    }
                });

        } else if ("article_print".equals(cmd)) {
            BangUI.copyToClipboard(((Article)_item).getPrint());
            ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, "m.article_print_copied");

        } else if ("play_song".equals(cmd)) {
            ctx.getBangClient().queueMusic(((Song)_item).getSong(), true, 1f);

        } else if ("download_song".equals(cmd) || "copy_song".equals(cmd)) {
            ctx.getBangClient().displayPopup(
                new SongDownloadView(
                    ctx, ((Song)_item).getSong(), "download_song".equals(cmd)),
                    true, SongDownloadView.PREF_WIDTH);
        }
    }

    @Override // from BToggleButton
    public boolean dispatchEvent (BEvent event)
    {
        if (isEnabled() && isMenuEnabled() && event instanceof MouseEvent) {
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
        BangContext ctx = (BangContext)_ctx;

        BPopupMenu menu = new BPopupMenu(getWindow());
        menu.addListener(this);

        // add their name as a non-menu item
        menu.add(new BLabel("@=u(" + getText() + ")", "popupmenu_title"));

        // add item specific stuff (this is sort of a hack but there's not really anywhere better
        // to put this code)
        if (_item instanceof Article) {
            if (ctx.getUserObject().tokens.isAdmin()) {
                menu.addMenuItem(createItem("article_print"));
            }

        } else if (_item instanceof Song) {
            if (SongDownloadView.songDownloaded(((Song)_item).getSong())) {
                menu.addMenuItem(createItem("copy_song"));
                menu.addMenuItem(createItem("play_song"));
                menu.addMenuItem(createItem("download_song"));
            } else {
                menu.addMenuItem(createItem("download_song"));
            }
        }

        // all destroyable items have a "destroy" menu item
        if (_item.isDestroyable(ctx.getUserObject())) {
            if (ctx.getUserObject().tokens.isAdmin()) {
                menu.addMenuItem(createItem("destroy"));
            }
        }
        menu.popup(mx, my, false);
    }

    protected BMenuItem createItem (String action)
    {
        return new BMenuItem(_ctx.xlate(BangCodes.BANG_MSGS, "m.item_" + action), action);
    }

    protected void destroyItem ()
    {
        BangContext ctx = (BangContext)_ctx;
        ctx.getClient().requireService(PlayerService.class).destroyItem(
            _item.getItemId(), new ReportingListener(ctx, BangCodes.BANG_MSGS, "m.destroy_failed"));
    }

    protected BasicContext _ctx;
    protected Item _item;
    protected boolean _menuEnabled;
}
