//
// $Id$

package com.threerings.bang.game.client;

import java.awt.image.BufferedImage;

import com.jme.renderer.Renderer;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Rectangle;

import com.threerings.util.Name;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.util.BasicContext;

/**
 * Displays a player's avatar and name and a medal indicating their rank at the
 * end of a game.
 */
public class FinalistView extends BContainer
{
    /**
     * Creates a view for the specified player, rank, etc.
     *
     * @param pidx the player's index in the game (so we can display a medal of
     * the correct player color).
     * @param name the player's name.
     * @param avatar the player's avatar information.
     * @param rank the plaer's rank at the end of the game. If their rank is
     * zero (1st place) the view will be in the large format, otherwise it will
     * be in the small format.
     */
    public FinalistView (BasicContext ctx, int pidx, Name name, int[] avatar,
                         int rank)
    {
        super(new AbsoluteLayout());

        // load up our medal image and extract the appropriate colored tile
        BufferedImage medal = ctx.getImageCache().getBufferedImage(
            "ui/postgame/medals" + rank + ".png");
        int mwidth = MEDAL_SIZE[rank].width, mheight = MEDAL_SIZE[rank].height;
        _medal = new ImageIcon(
            ctx.getImageCache().createImage(
                medal.getSubimage(mwidth * pidx, 0, mwidth, mheight), true));

        // create our avatar imagery
        boolean winner = (rank == 0);
        int scale =  winner ? 2 : 4;
        _avatar = new ImageIcon(
            AvatarView.getImage(ctx, avatar).getScaledInstance(
                AvatarLogic.WIDTH / scale, AvatarLogic.HEIGHT / scale,
                BufferedImage.SCALE_SMOOTH));

        // load up our frame and scroll banner
        String prefix = winner ? "ui/frames/big_" : "ui/frames/smaller_";
        _frame = new ImageIcon(ctx.loadImage(prefix + "frame.png"));
        _banner = new ImageIcon(ctx.loadImage(prefix + "scroll.png"));

        // create a label for their name
        String sclass = "endgame_player_" + (winner ? "big" : "small");
        add(new BLabel(name.toString(), sclass), NAME_RECTS[winner ? 0 : 1]);
    }

    @Override // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return new Dimension(
            _banner.getWidth(), _avatar.getHeight() + 5*_banner.getHeight()/3);
    }

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        super.renderBackground(renderer);

        int ax = (_width - _avatar.getWidth())/2, ay = _banner.getHeight()/2;
        _avatar.render(renderer, ax, ay);
        _frame.render(renderer, ax-(_frame.getWidth()-_avatar.getWidth())/2,
                      ay-(_frame.getHeight()-_avatar.getHeight())/2);
        _banner.render(renderer, 0, 0);
        _medal.render(renderer, 0, getHeight()-_medal.getHeight());
    }

    protected ImageIcon _avatar, _frame, _banner, _medal;

    protected static final Dimension[] MEDAL_SIZE = {
        new Dimension(102, 138),
        new Dimension(60, 84),
        new Dimension(58, 76),
        new Dimension(44, 62),
    };

    protected static final Rectangle[] NAME_RECTS = {
        new Rectangle(21, 13, 251, 25),
        new Rectangle(7, 6, 136, 17),
    };
}
