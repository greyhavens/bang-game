//
// $Id$

package com.threerings.bang.avatar.client;

import java.awt.Image;
import java.awt.image.BufferedImage;

import com.jmex.bui.util.Dimension;

import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

import com.threerings.cast.ActionFrames;
import com.threerings.cast.CharacterComponent;
import com.threerings.cast.NoSuchComponentException;

import com.threerings.bang.data.BuckleInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.avatar.util.BucklePartCatalog;

import static com.threerings.bang.Log.log;

/**
 * Displays a gang buckle.
 */
public class BuckleView extends BaseAvatarView
{
    public static Image getPartIcon (BasicContext ctx, BucklePart part)
    {
        int iwidth = AvatarLogic.BUCKLE_WIDTH / 2, iheight = AvatarLogic.BUCKLE_HEIGHT / 2;
        int fqComponentId = part.getComponents()[0],
            componentId = (fqComponentId & 0xFFFF);
        CharacterComponent ccomp;
        try {
            ccomp = ctx.getCharacterManager().getComponentRepository().getComponent(componentId);
        } catch (NoSuchComponentException nsce) {
            log.warning("Buckle part contains unknown component [part=" + part +
                ", componentId=" + componentId + "].");
            return ImageUtil.createErrorImage(iwidth, iheight);
        }
        ActionFrames af = ccomp.getFrames("static", null);
        if (af == null) {
            log.warning("Buckle part component lacks static action [part=" + part +
                ", component=" + ccomp + "].");
            return ImageUtil.createErrorImage(iwidth, iheight);
        }
        AvatarLogic al = ctx.getAvatarLogic();
        BucklePartCatalog.Part cpart = al.getBucklePartCatalog().getPart(
            part.getPartClass(), part.getPartName());
        String[] colors = null;
        if (cpart != null) {
            colors = al.getColorizationClasses(cpart);
        } else {
            log.warning("Buckle part not listed in catalog [part=" + part + "].");
        }
        Colorization[] zations = ctx.getAvatarLogic().decodeColorizations(fqComponentId, colors);
        if (zations != null) {
            af = af.cloneColorized(zations);
        }
        if (part.getX() != 0 || part.getY() != 0) {
            af = af.cloneTranslated(part.getX(), part.getY());
        }
        return renderFrame(ctx, af, AvatarLogic.BUCKLE_WIDTH,
            AvatarLogic.BUCKLE_HEIGHT).getScaledInstance(iwidth, iheight, Image.SCALE_SMOOTH);
    }

    /**
     * Creates a buckle view.
     *
     * @param scale the image will be one over this value times the "natural" size of the buckle
     * imagery. This should be at least 2.
     */
    public BuckleView (BasicContext ctx, int scale)
    {
        super(ctx, scale);
        setPreferredSize(new Dimension(
            AvatarLogic.BUCKLE_WIDTH / scale, AvatarLogic.BUCKLE_HEIGHT / scale));
    }

    /**
     * Sets the buckle to display.
     */
    public void setBuckle (BuckleInfo buckle)
    {
        setAvatar(buckle);
    }
}
