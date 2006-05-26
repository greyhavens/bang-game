//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.Spacer;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;
import com.threerings.bang.saloon.data.TopRankedList;

/**
 * Displays top scorers in the Saloon.
 */
public class TopScoreView extends BContainer
{
    public TopScoreView (BangContext ctx, SaloonObject salobj)
    {
        super(new BorderLayout());
        setStyleClass("top_score_view");

        add(new BLabel(ctx.xlate(SaloonCodes.SALOON_MSGS, "m.top_scores"),
                       "top_score_header"), BorderLayout.NORTH);

        BContainer cont = new BContainer(
            GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                 GroupLayout.STRETCH));;
        add(new BScrollPane(cont), BorderLayout.CENTER);

        for (TopRankedList list : salobj.topRanked) {
            cont.add(new Spacer(10, 5));

            BContainer row = new BContainer(
                GroupLayout.makeHoriz(GroupLayout.CENTER));
            ((GroupLayout)row.getLayoutManager()).setGap(25);
            ((GroupLayout)row.getLayoutManager()).setOffAxisJustification(
                GroupLayout.BOTTOM);
            cont.add(row);

            BContainer col = new BContainer(
                GroupLayout.makeVert(GroupLayout.CENTER));
            ((GroupLayout)col.getLayoutManager()).setGap(0);
            row.add(col);

            String cat = ctx.xlate(SaloonCodes.SALOON_MSGS, list.criterion);
            col.add(new BLabel(cat, "top_score_category"));

            AvatarView aview = new AvatarView(ctx, 4, false, true);
            col.add(aview, GroupLayout.FIXED);
            if (list.players.length > 0) {
                aview.setText("1. " + list.players[0]);
            }
            if (list.topDogSnapshot != null) {
                aview.setAvatar(list.topDogSnapshot);
            }

            col = new BContainer(GroupLayout.makeVStretch());
            ((GroupLayout)col.getLayoutManager()).setGap(0);
            row.add(col);
            for (int ii = 1; ii < list.players.length; ii++) {
                String name = (ii + 1) + ". " + list.players[ii];
                col.add(new BLabel(name, "top_score_list"));
            }
        }
    }
}
