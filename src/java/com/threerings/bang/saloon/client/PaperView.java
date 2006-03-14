//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.SaloonObject;

/**
 * Displays the daily paper.
 */
public class PaperView extends BContainer
{
    public PaperView (BangContext ctx)
    {
        super(GroupLayout.makeVStretch());
        ((GroupLayout)getLayoutManager()).setGap(20);
        setStyleClass("news_view");
        _ctx = ctx;
        String townId = ctx.getUserObject().townId;

        BLabel lbl;
        add(lbl = new BLabel("", "nameplate"), GroupLayout.FIXED);
        String npath = "ui/saloon/" + townId + "/nameplate.png";
        lbl.setIcon(new ImageIcon(ctx.loadImage(npath)));

        BContainer cols = new BContainer(GroupLayout.makeHStretch());
        ((GroupLayout)cols.getLayoutManager()).setGap(20);
        String left = "I must not fear.\n" +
            "Fear is the mind-killer.\n" +
            "Fear is the little-death that brings total obliteration.\n" +
            "I will face my fear.\n" +
            "I will permit it to pass over me and through me.\n" +
            "And when it has gone past I will turn the inner eye to see " +
            "its path.\n" +
            "Where the fear has gone there will be nothing.\n" +
            "Only I will remain.";
        cols.add(new BLabel(left, "news_column"));

        String right = "Lorem ipsum dolor sit amet, consectetur adipisicing " +
            "elit, sed do eiusmod tempor incididunt ut labore et dolore " +
            "magna aliqua. Ut enim ad minim veniam, quis nostrud " +
            "exercitation ullamco laboris nisi ut aliquip ex ea commodo " +
            "consequat. Duis aute irure dolor in reprehenderit in voluptate " +
            "velit esse cillum dolore eu fugiat nulla pariatur. Excepteur " +
            "sint occaecat cupidatat non proident, sunt in culpa qui officia " +
            "deserunt mollit anim id est laborum.";
        cols.add(new BLabel(right, "news_column"));
        add(cols);

        GroupLayout hlay = GroupLayout.makeHoriz(GroupLayout.RIGHT);
        hlay.setGap(40);
        BContainer bcont = new BContainer(hlay);
        bcont.setStyleClass("news_buttons");
        bcont.add(_back = new BButton("", _listener, "back"));
        _back.setStyleClass("back_button");
        _back.setEnabled(false);
        bcont.add(_forward = new BButton("", _listener, "forward"));
        _forward.setStyleClass("fwd_button");
        _forward.setEnabled(false);
        add(bcont, GroupLayout.FIXED);
    }

    public void init (SaloonObject salobj)
    {
        _salobj = salobj;
        // TODO: display the news
    }

    protected ActionListener _listener = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
//             if (event.getAction().equals("forward")) {
//                 displayPage(_page+1, false);
//             } else if (event.getAction().equals("back")) {
//                 displayPage(_page-1, false);
//             }
        }
    };

    protected BangContext _ctx;
    protected SaloonObject _salobj;
    protected BButton _forward, _back;
}
