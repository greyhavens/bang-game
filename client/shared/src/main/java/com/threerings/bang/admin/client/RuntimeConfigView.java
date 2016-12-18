//
// $Id$

package com.threerings.bang.admin.client;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.admin.client.AdminService;

import com.threerings.bang.client.bui.TabbedPane;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.admin.data.ConfigObject;

import static com.threerings.bang.Log.log;

/**
 * Displays the runtime configuration objects and allows them to be modified
 * (by admins).
 */
public class RuntimeConfigView extends BDecoratedWindow
    implements AdminService.ConfigInfoListener, ActionListener
{
    public RuntimeConfigView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), ctx.xlate("admin", "m.config_title"));
        ((GroupLayout)getLayoutManager()).setOffAxisPolicy(GroupLayout.STRETCH);

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("admin");

        add(_tabs = new TabbedPane(true));
        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        bcont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(bcont, GroupLayout.FIXED);

        // ship off a getConfigInfo request to find out what config objects are available
        ctx.getClient().requireService(AdminService.class).getConfigInfo(this);
    }

    // documentation inherited from interface AdminService.ConfigInfoListener
    public void gotConfigInfo (String[] keys, int[] oids)
    {
        // create object editor panels for each of the categories
        for (int ii = 0; ii < keys.length; ii++) {
            _tabs.addTab(_msgs.get("m.tab_" + keys[ii]),
                         new BScrollPane(new ObjectView(keys[ii], oids[ii])));
        }
    }

    // documentation inherited from interface AdminService.ConfigInfoListener
    public void requestFailed (String reason)
    {
        log.warning("Failed to get config info", "reason", reason);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        _ctx.getBangClient().clearPopup(this, true);
    }

    @Override
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return new Dimension(800, 600);
    }

    protected class ObjectView extends BContainer
        implements Subscriber<ConfigObject>
    {
        public ObjectView (String key, int oid) {
            super(new TableLayout(2, 5, 5));
            setStyleClass("config_object_view");
            _key = key;
            _safesub = new SafeSubscriber<ConfigObject>(oid, this);
        }

        public void objectAvailable (ConfigObject confobj) {
            Field[] fields = confobj.getClass().getFields();
            for (int ii = 0; ii < fields.length; ii++) {
                // if the field is anything but a plain old public (non-static)
                // field, we don't want to edit it
                if (fields[ii].getModifiers() != Modifier.PUBLIC) {
                    continue;
                }
                String lkey = "m." + _key + "_" + fields[ii].getName();
                add(new BLabel(_msgs.get(lkey), "config_field_label"));
                add(confobj.createEditor(fields[ii]));
            }
        }

        public void requestFailed (int oid, ObjectAccessException cause) {
            log.warning("Failed to subscribe to " + oid + ": " + cause);
        }

        protected void wasAdded () {
            super.wasAdded();
            _safesub.subscribe(_ctx.getClient().getDObjectManager());
        }

        protected void wasRemoved () {
            super.wasRemoved();
            _safesub.unsubscribe(_ctx.getClient().getDObjectManager());
        }

        protected String _key;
        protected SafeSubscriber<ConfigObject> _safesub;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected TabbedPane _tabs;
}
