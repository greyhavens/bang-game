//
// $Id$

package com.threerings.bang.gang.client;

import java.net.MalformedURLException;
import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.Date;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.Spacer;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.MainView;
import com.threerings.bang.client.PlayerPopupMenu;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.client.BuckleView;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangInfo;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.util.GangUtil;

/**
 * Displays information about a gang to any interested parties, much like the Wanted Poster
 * displays information on a player.
 */
public class GangInfoDialog extends BWindow
    implements ActionListener, GangCodes
{
    /**
     * Pops up an info dialog for the specified gang if possible at the moment.
     */
    public static void display (BangContext ctx, Handle name)
    {
        if (ctx.getBangClient().canDisplayPopup(MainView.Type.POSTER_DISPLAY)) {
            ctx.getBangClient().displayPopup(new GangInfoDialog(ctx, name), true, 500);
        } else {
            BangUI.play(BangUI.FeedbackSound.INVALID_ACTION);
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("home_page")) {
            _ctx.showURL(_url);
        } else if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }

    protected GangInfoDialog (BangContext ctx, Handle name)
    {
        super(ctx.getStyleSheet(), GroupLayout.makeVert(GroupLayout.TOP));
        ((GroupLayout)getLayoutManager()).setPolicy(GroupLayout.STRETCH);
        _ctx = ctx;

        ((GroupLayout)getLayoutManager()).setGap(-2);
        setStyleClass("gang_info_dialog");
        setModal(true);
        setLayer(2);

        _vcont = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)_vcont.getLayoutManager()).setPolicy(GroupLayout.STRETCH);
        ((GroupLayout)_vcont.getLayoutManager()).setGap(0);
        _vcont.setStyleClass("gang_info_view");
        add(_vcont);

        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        bcont.add(new BButton(ctx.xlate(GANG_MSGS, "m.dismiss"), this, "dismiss"));
        add(bcont, GroupLayout.FIXED);

        // fetch the gang info from the server
        GangService gsvc = ctx.getClient().requireService(GangService.class);
        gsvc.getGangInfo(name, new GangService.ResultListener() {
            public void requestProcessed (Object result) {
                populate((GangInfo)result);
            }
            public void requestFailed (String cause) {
                _vcont.add(new BLabel(_ctx.xlate(GANG_MSGS, cause)), GroupLayout.FIXED);
            }
        });
    }

    protected void populate (GangInfo info)
    {
        MessageBundle msgs = _ctx.getMessageManager().getBundle(GANG_MSGS),
            hmsgs = _ctx.getMessageManager().getBundle(HideoutCodes.HIDEOUT_MSGS);

        _vcont.add(createLabel("design_top"), GroupLayout.FIXED);
        _vcont.add(new Spacer(1, 4), GroupLayout.FIXED);

        _vcont.add(new BLabel(info.name.toString(), "gang_info_title"), GroupLayout.FIXED);
        _vcont.add(new Spacer(1, -2), GroupLayout.FIXED);
        String date = DATE_FORMAT.format(new Date(info.founded));
        _vcont.add(new BLabel(msgs.get("m.founded", date), "gang_info_founded"), GroupLayout.FIXED);
        _vcont.add(new Spacer(1, 2), GroupLayout.FIXED);

        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)bcont.getLayoutManager()).setOffAxisJustification(GroupLayout.TOP);
        ((GroupLayout)bcont.getLayoutManager()).setGap(30);
        _vcont.add(bcont, GroupLayout.FIXED);
        BuckleView buckle = new BuckleView(_ctx, 2);
        buckle.setBuckle(info.buckle);
        bcont.add(buckle);

        BContainer scont = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)scont.getLayoutManager()).setOffAxisJustification(GroupLayout.LEFT);
        ((GroupLayout)scont.getLayoutManager()).setGap(-2);
        bcont.add(scont);
        bcont.add(new Spacer(55, 1));

        BContainer ncont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ncont.add(new BLabel(msgs.get("m.weight_class." + info.weightClass),
            "gang_info_notoriety"));
        ncont.add(new BLabel(new ImageIcon(_ctx.loadImage("ui/hideout/diamond.png"))));
        ncont.add(new BLabel("\"" + msgs.get("m.notoriety." + info.notoriety) + "\"",
            "gang_info_notoriety"));
        scont.add(ncont);
        scont.add(createLabel("underline_short"));

        scont.add(new Spacer(1, 8));
        scont.add(new BLabel(GangUtil.quoteStatement(_ctx, info.statement, false),
            "gang_info_statement"));
        scont.add(new Spacer(1, 15));

        try {
            _url = new URL(info.url);
            BContainer ucont = GroupLayout.makeHBox(GroupLayout.CENTER);
            ucont.setPreferredSize(new Dimension(166, -1));
            BButton page = new BButton(hmsgs.get("m.home_page"), this, "home_page");
            page.setTooltipText(info.url);
            page.setStyleClass("gang_page_button");
            ucont.add(page);
            scont.add(ucont);
            scont.add(new Spacer(1, 7));

        } catch (MalformedURLException e) {
            // no problem, just don't include the button
        }

        _vcont.add(createLabel("design_bottom"), GroupLayout.FIXED);
        _vcont.add(new Spacer(1, 5), GroupLayout.FIXED);

        BContainer rcont = new BContainer(GroupLayout.makeVert(
            GroupLayout.STRETCH, GroupLayout.TOP, GroupLayout.STRETCH));
        ((GroupLayout)rcont.getLayoutManager()).setGap(-14);
        _vcont.add(rcont);

        BContainer tcont = new BContainer(GroupLayout.makeHoriz(
            GroupLayout.STRETCH, GroupLayout.LEFT, GroupLayout.NONE));
        ((GroupLayout)tcont.getLayoutManager()).setOffAxisJustification(GroupLayout.TOP);
        rcont.add(tcont, GroupLayout.FIXED);

        BContainer left = new BContainer(GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        ((GroupLayout)left.getLayoutManager()).setGap(0);
        tcont.add(left);

        left.add(new BLabel(hmsgs.get("m.leaders"), "roster_title"));
        left.add(createLabel("underline_medium"));
        BContainer lcont = new BContainer(new TableLayout(3));
        lcont.setStyleClass("roster_table");
        left.add(lcont);
        for (GangInfo.Member leader : info.leaders) {
            lcont.add(createMemberLabel(leader));
        }

        if (info.leaders.length > 0) {
            BLabel llabel = createMemberLabel(info.leaders[0]);
            llabel.setStyleClass("gang_info_leader_view");
            AvatarIcon aicon = new AvatarIcon(_ctx);
            aicon.setAvatar(info.avatar);
            llabel.setIcon(aicon);
            llabel.setIconTextGap(0);
            llabel.setOrientation(BLabel.VERTICAL);
            tcont.add(llabel, GroupLayout.FIXED);
        }

        BContainer bottom = new BContainer(GroupLayout.makeVert(
            GroupLayout.STRETCH, GroupLayout.TOP, GroupLayout.STRETCH));
        ((GroupLayout)bottom.getLayoutManager()).setGap(0);
        rcont.add(bottom);

        bottom.add(new BLabel(hmsgs.get("m.members"), "roster_title"),
            GroupLayout.FIXED);
        bottom.add(createLabel("underline_long"), GroupLayout.FIXED);
        bottom.add(new Spacer(1, 2), GroupLayout.FIXED);
        BContainer mcont = new BContainer(new TableLayout(3));
        mcont.setStyleClass("roster_table");
        BScrollPane pane = new BScrollPane(mcont);
        pane.setShowScrollbarAlways(false);
        bottom.add(pane);
        for (GangInfo.Member member : info.members) {
            mcont.add(createMemberLabel(member));
        }
        bottom.add(new Spacer(1, 15), GroupLayout.FIXED);
    }

    protected BLabel createLabel (String img)
    {
        return new BLabel(new ImageIcon(_ctx.loadImage("ui/ganginfo/" + img + ".png")));
    }

    protected BLabel createMemberLabel (final GangInfo.Member member)
    {
        String style = "roster_entry" + (member.active ? "" : "_inactive");
        return new BLabel(member.handle.toString(), style) {
            public boolean dispatchEvent (BEvent event) {
                return super.dispatchEvent(event) ||
                    PlayerPopupMenu.checkPopup(_ctx, getWindow(), event, member.handle, false);
            }
        };
    }

    protected BangContext _ctx;

    protected BContainer _vcont;
    protected URL _url;

    protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM. d, yyyy");
}
