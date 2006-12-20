//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;

/**
 * Allows the user to browse the list of gang members.
 */
public class RosterView extends BContainer
    implements SetListener
{
    public RosterView (BangContext ctx, GangObject gangobj)
    {
        super(GroupLayout.makeVStretch());
        _ctx = ctx;
        _gangobj = gangobj;
        
        setStyleClass("roster_view");
        
        BContainer rcont = new BContainer(GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        BContainer tcont = new BContainer();
        
        BScrollPane rpane = new BScrollPane(rcont);
        rpane.setStyleClass("roster_pane");
        add(rpane);
    }
    
    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        GangMemberEntry entry = (GangMemberEntry)event.getEntry();
    }
    
    // documentation inherited from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        GangMemberEntry entry = (GangMemberEntry)event.getOldEntry();
    }
    
    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        GangMemberEntry entry = (GangMemberEntry)event.getEntry();
    }
    
    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _gangobj.addListener(this);
    }
    
    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _gangobj.removeListener(this);
    }
    
    protected BangContext _ctx;
    protected GangObject _gangobj;
    
    protected BContainer _lcont, _mcont;
}
