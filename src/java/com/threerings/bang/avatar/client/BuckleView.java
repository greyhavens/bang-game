//
// $Id$

package com.threerings.bang.avatar.client;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
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
    /**
     * Gets the icon image for the given part.
     */
    public static BufferedImage getPartIcon (BasicContext ctx, BucklePart part)
    {
        return getPartIcon(ctx, part, null);
    }

    /**
     * Gets the icon image for the given part, populating the supplied {@link Rectangle} with its
     * trimmed bounds.
     */
    public static BufferedImage getPartIcon (BasicContext ctx, BucklePart part, Rectangle tbounds)
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
        if (tbounds != null) {
            af.getFrames(0).getTrimmedBounds(0, tbounds);
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
        return getPartImage(
            ctx, ccomp, zations, AvatarLogic.BUCKLE_WIDTH / 2, AvatarLogic.BUCKLE_HEIGHT / 2);
    }

    /**
     * Gets an image of the part depicted by the given component, scaled to the specified
     * dimensions, optionally applying a set of colorizations.
     */
    public static BufferedImage getPartImage (
        BasicContext ctx, CharacterComponent ccomp, Colorization[] zations, int width, int height)
    {
        ActionFrames af = ccomp.getFrames("static", null);
        if (zations != null) {
            af = af.cloneColorized(zations);
        }
        AffineTransformOp op = new AffineTransformOp(
            AffineTransform.getScaleInstance(
                (double)width / AvatarLogic.BUCKLE_WIDTH,
                (double)height / AvatarLogic.BUCKLE_HEIGHT),
            AffineTransformOp.TYPE_BILINEAR);
        return op.filter(
            renderFrame(ctx, af, AvatarLogic.BUCKLE_WIDTH, AvatarLogic.BUCKLE_HEIGHT),
            null);
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
