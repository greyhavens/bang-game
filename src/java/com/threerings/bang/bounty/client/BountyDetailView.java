//
// $Id$

package com.threerings.bang.bounty.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bounty.data.BountyConfig;
import com.threerings.bang.bounty.data.OfficeCodes;
import com.threerings.bang.bounty.data.OfficeObject;

/**
 * Displays the details on a particular bounty and which of the games the user has completed.
 */
public class BountyDetailView extends BContainer
    implements IconPalette.Inspector, ActionListener
{
    public BountyDetailView (BangContext ctx)
    {
        super(new AbsoluteLayout());
        _ctx = ctx;
        _comp = new ImageIcon(ctx.loadImage("ui/tutorials/complete.png"));
        _incomp = new ImageIcon(ctx.loadImage("ui/tutorials/incomplete.png"));

        add(_reward = new BLabel("", "bounty_detail_reward"), new Point(263, 438));
        _reward.setIcon(new ImageIcon(ctx.loadImage("ui/icons/big_scrip.png")));
        _reward.setIconTextGap(10);
        add(_title = new BLabel("", "bounty_detail_title"), new Point(203, 388));
        add(_descrip = new BLabel("", "bounty_detail_descrip"), new Rectangle(203, 273, 184, 112));

        GroupLayout glay = GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.STRETCH).setGap(2);
        add(_games = new BContainer(glay), new Rectangle(55, 116, 332, 152));

        TableLayout tlay = new TableLayout(2, 0, 0).setHorizontalAlignment(TableLayout.STRETCH);
        add(_recent = new BContainer(tlay), new Rectangle(55, 3, 326, 86));
    }

    /**
     * Configures us with our office distributed object when it becomes available.
     */
    public void setOfficeObject (OfficeObject offobj)
    {
        _offobj = offobj;
        _games.setEnabled(true);
    }

    // from interface IconPalette.Inspector
    public void iconUpdated (SelectableIcon icon, boolean selected)
    {
        if (!selected) {
            return;
        }

        _config = ((BountyListEntry)icon).config;
        _reward.setText(String.valueOf(_config.reward.scrip));
        _title.setText(_ctx.xlate(OfficeCodes.BOUNTY_MSGS, "m." + _config.ident + "_title"));
        _descrip.setText(_ctx.xlate(OfficeCodes.BOUNTY_MSGS, "m." + _config.ident + "_descrip"));
        _games.removeAll();
        _recent.removeAll();

        PlayerObject user = _ctx.getUserObject();
        GroupLayout glay = GroupLayout.makeHoriz(
            GroupLayout.STRETCH, GroupLayout.LEFT, GroupLayout.CONSTRAIN);
        boolean noMorePlay = false;
        for (String game : _config.games) {
            BContainer row = new BContainer(glay);
            String key = _config.getStatKey(game);
            boolean completed = user.stats.containsValue(Stat.Type.BOUNTY_GAMES_COMPLETED, key);
            row.add(new BLabel(completed ? _comp : _incomp), GroupLayout.FIXED);
            row.add(new BLabel(_ctx.xlate(OfficeCodes.BOUNTY_MSGS, "m." + key)));
            String pmsg = _ctx.xlate(OfficeCodes.OFFICE_MSGS, completed ? "m.replay" : "m.play");
            if (!noMorePlay) {
                BButton play = new BButton(pmsg, this, game);
                if (completed) {
                    play.setStyleClass("alt_button");
                } else {
                    noMorePlay = _config.inOrder;
                }
                row.add(play, GroupLayout.FIXED);
            }
            _games.add(row);
        }
        _games.setEnabled(_offobj != null);

        // TODO: display recent finishers
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        _games.setEnabled(false);
        _offobj.service.playBountyGame(_ctx.getClient(), _config.ident, event.getAction(),
                                       new OfficeService.InvocationListener() {
            public void requestFailed (String cause) {
                _ctx.getChatDirector().displayFeedback(OfficeCodes.OFFICE_MSGS, cause);
                _games.setEnabled(true);
            }
        });
    }

    protected BangContext _ctx;
    protected OfficeObject _offobj;
    protected BountyConfig _config;

    protected BLabel _reward, _title, _descrip;
    protected BContainer _games, _recent;

    protected ImageIcon _comp, _incomp;
}
