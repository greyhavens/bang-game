//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BToggleButton;

import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.saloon.client.TableGameView;
import com.threerings.bang.saloon.data.TableGameObject;

/**
 * Hideout support for a table game.
 */
public class TableView extends TableGameView
{
    public TableView (BangContext ctx, StatusLabel status, BContainer cont, BToggleButton button)
    {
        super(ctx, status);
        setStyleClass("table_view");

        _cont = cont;
        _button = button;
    }

    @Override // documentation inherited
    public void attributeChanged (AttributeChangedEvent event)
    {
        super.attributeChanged(event);

        if (event.getName().equals(TableGameObject.PLAYER_OIDS)) {
            boolean showButtons = true;
            if (_tobj.playerOids != null) {
                PlayerObject user = _ctx.getUserObject();
                for (int oid : _tobj.playerOids) {
                    if (oid == user.getOid()) {
                        showButtons = false;
                        break;
                    }
                }
            }
            _cont.setEnabled(showButtons);
            _button.setEnabled(showButtons);
        }
    }

    protected BContainer _cont;
    protected BToggleButton _button;
}
