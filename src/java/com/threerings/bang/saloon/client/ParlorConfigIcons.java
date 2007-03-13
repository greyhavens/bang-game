//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.StringUtil;

import com.threerings.bang.util.BangContext;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.ParlorObject;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ObjectAddedEvent;
import com.threerings.presents.dobj.ObjectRemovedEvent;
import com.threerings.presents.dobj.OidListListener;

/**
 * Displays the current parlor settings in icon form.
 */
public class ParlorConfigIcons extends BContainer
    implements AttributeChangeListener, OidListListener
{
    public ParlorConfigIcons (BangContext ctx)
    {
        super(GroupLayout.makeHoriz(GroupLayout.CENTER).setGap(5));
        _ctx = ctx;

        add(_icon = new BLabel(""));
        add(_occs = new BLabel(""));
    }

    public void willEnterPlace (ParlorObject parobj)
    {
        _parobj = parobj;
        _parobj.addListener(this);

        updateIcon();
        updateOccupants();
    }

    public void didLeavePlace ()
    {
        if (_parobj != null) {
            _parobj.removeListener(this);
            _parobj = null;
        }
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (ParlorObject.INFO.equals(event.getName())) {
            updateIcon();
        }
    }

    // documentation inherited from interface OidListListener
    public void objectAdded (ObjectAddedEvent event)
    {
        if (ParlorObject.OCCUPANTS.equals(event.getName())) {
            updateOccupants();
        }
    }

    // documentation inherited from interface OidListListener
    public void objectRemoved (ObjectRemovedEvent event)
    {
        if (ParlorObject.OCCUPANTS.equals(event.getName())) {
            updateOccupants();
        }
    }

    protected void updateOccupants ()
    {
        _occs.setText(String.valueOf(_parobj.occupants.size()));
    }

    protected void updateIcon ()
    {
        ParlorInfo info = _parobj.info;
        if (info.type != ParlorInfo.Type.NORMAL) {
            String ipath = "ui/saloon/" + StringUtil.toUSLowerCase(info.type.toString()) + ".png";
            _icon.setIcon(new ImageIcon(_ctx.loadImage(ipath)));
        } else {
            _icon.setIcon(new BlankIcon(16, 16));
        }
    }

    protected BangContext _ctx;
    protected ParlorObject _parobj;
    protected BLabel _occs, _icon;
}
