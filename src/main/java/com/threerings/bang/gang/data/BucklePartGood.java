//
// $Id$

package com.threerings.bang.gang.data;

import java.awt.image.BufferedImage;

import com.jmex.bui.BImage;
import com.jmex.bui.icon.ImageIcon;

import com.threerings.cast.CharacterComponent;
import com.threerings.cast.NoSuchComponentException;

import com.threerings.media.image.ColorPository.ColorRecord;
import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

import com.threerings.presents.dobj.DObject;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.avatar.client.BuckleView;
import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.avatar.util.BucklePartCatalog;

import static com.threerings.bang.Log.log;

/**
 * A buckle part for sale.
 */
public class BucklePartGood extends GangGood
{
    /**
     * Creates a good representing the specified buckle part.
     */
    public BucklePartGood (String type, int scripCost, int coinCost, int aceCost)
    {
        super(type, scripCost, coinCost, aceCost);
    }

    /** A constructor only used during serialization. */
    public BucklePartGood ()
    {
    }

    /**
     * Returns the class of the part.
     */
    public String getPartClass ()
    {
        return _type.substring(0, _type.lastIndexOf('/'));
    }

    /**
     * Returns the name of the part.
     */
    public String getPartName ()
    {
        return _type.substring(_type.lastIndexOf('/') + 1);
    }

    // documentation inherited
    public boolean isAvailable (GangObject gang)
    {
        // TODO: to which parts should we limit access?
        return true;
    }

    @Override // from Good
    public ImageIcon createIcon (BasicContext ctx, DObject entity, int[] colorIds)
    {
        AvatarLogic al = ctx.getAvatarLogic();
        ColorRecord[] crecs = al.pickRandomColors(getColorizationClasses(ctx), entity);
        Colorization[] zations = new Colorization[crecs.length];
        for (int ii = 0; ii < crecs.length; ii++) {
            ColorRecord crec = crecs[ii];
            if (crec == null) {
                continue;
            }
            int cidx = AvatarLogic.getColorIndex(crec.cclass.name);
            colorIds[cidx] = crec.colorId;
            zations[ii] = crec.getColorization();
        }
        return createIcon(ctx, zations);
    }

    @Override // from Good
    public ImageIcon createIcon (BasicContext ctx, Colorization[] zations)
    {
        // clone the colorizations because the character rendering code keeps a reference
        // for caching
        zations = zations.clone();
        CharacterComponent ccomp = getCharacterComponent(ctx);
        BufferedImage bufimg = (ccomp == null) ?
            ImageUtil.createErrorImage(GOOD_PART_WIDTH, GOOD_PART_HEIGHT) :
            BuckleView.getPartImage(ctx, ccomp, zations, GOOD_PART_WIDTH, GOOD_PART_HEIGHT);
        return new ImageIcon(new BImage(bufimg));
    }

    @Override // from Good
    public String[] getColorizationClasses (BasicContext ctx)
    {
        AvatarLogic alogic = ctx.getAvatarLogic();
        BucklePartCatalog.Part part = alogic.getBucklePartCatalog().getPart(
            getPartClass(), getPartName());
        return (part == null) ? null : alogic.getColorizationClasses(part);
    }

    @Override // from Good
    public String getName ()
    {
        return MessageBundle.qualify(AvatarCodes.BUCKLE_MSGS, "m." + getPartName());
    }

    @Override // from Good
    public String getTip ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.buckle_part_tip");
    }

    protected CharacterComponent getCharacterComponent (BasicContext ctx)
    {
        try {
            return ctx.getCharacterManager().getComponentRepository().getComponent(
                "buckle/" + getPartClass(), getPartName());
        } catch (NoSuchComponentException e) {
            log.warning("Missing component for buckle part", "type", _type);
            return null;
        }
    }

    /** The dimensions of the part icon in the goods display. */
    protected static final int GOOD_PART_WIDTH = 122;
    protected static final int GOOD_PART_HEIGHT = 94;
}
