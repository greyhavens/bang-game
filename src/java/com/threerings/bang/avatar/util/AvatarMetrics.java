//
// $Id$

package com.threerings.bang.avatar.util;

import com.samskivert.util.IntListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.media.image.ColorPository;
import com.threerings.media.image.Colorization;

import com.threerings.cast.CharacterComponent;
import com.threerings.cast.CharacterDescriptor;
import com.threerings.cast.ComponentRepository;
import com.threerings.cast.NoSuchComponentException;

import static com.threerings.bang.Log.log;

/**
 * Used to calculate various things about avatars, decode avatar fingerprints
 * and whatnot.
 */
public class AvatarMetrics
{
    /** The different avatar slots available for customization. */
    public static final String[] SLOTS = {
        "BODY_ZATIONS", // hair and skin colorizations
        "familiar",
        "hat",
        "jewelry",
        "glasses",
        "clothing_front",
        "hair_front",
        "facial_hair",
        "eyes",
        "nose",
        "eyebrows",
        "mouth",
        "head",
        "hair_back",
        "clothing_back",
        "background",
    };

    /** The colorization class for skin colors. */
    public static final String SKIN = "skin";

    /** The colorization class for hair colors. */
    public static final String HAIR = "skin";

    /** The colorization class for primary clothing colors. */
    public static final String CLOTHES_P = "clothes_p";

    /** The colorization class for secondary clothing colors. */
    public static final String CLOTHES_S = "clothes_s";

    /** The colorization class for tertiary clothing colors. */
    public static final String CLOTHES_T = "clothes_t";

    /**
     * Creates a metrics instance which will make use of the supplied
     * repositories to obtain avatar related information.
     */
    public AvatarMetrics (ColorPository pository, ComponentRepository crepo)
    {
        _pository = pository;
        _crepo = crepo;
    }

    /**
     * Decodes an avatar fingerprint into a {@link CharacterDescriptor} that
     * can be passed to the character manager.
     */
    public CharacterDescriptor decodeAvatar (int[] avatar)
    {
        // decode the skin and hair colorizations
        _globals[0] = _pository.getColorization(SKIN, avatar[0] & 0x1F);
        _globals[1] = _pository.getColorization(HAIR, (avatar[0] >> 5) & 0x1F);

        // compact the array to remove unused entries
        avatar = IntListUtil.compact(avatar);

        // the subsequent elements are article colorizations and component ids
        // composed into a single integer
        int clength = avatar.length-1;
        int[] componentIds = new int[clength];
        Colorization[][] zations = new Colorization[clength][];
        for (int ii = 0; ii < clength; ii++) {
            int pvalue = avatar[ii+1];

            // decode the fingerprint values
            _colors[0] = (pvalue >> 16) & 0x1F;
            _colors[1] = (pvalue >> 21) & 0x1F;
            _colors[2] = (pvalue >> 26) & 0x1F;
            componentIds[ii] = (pvalue & 0xFFFF);

            // look up the component in the repository
            CharacterComponent ccomp = null;
            try {
                ccomp = _crepo.getComponent(componentIds[ii]);
            } catch (NoSuchComponentException nsce) {
                log.warning("Avatar contains non-existent component " +
                            "[avatar=" + StringUtil.toString(avatar) +
                            ", idx=" + ii + ", cid=" + componentIds[ii] + "].");
                continue;
            }

            // determine which colors are appropriate for this component
            String[] colors = ccomp.componentClass.colors;
            zations[ii] = new Colorization[colors.length];
            for (int cc = 0, ccount = 0; cc < zations.length; cc++) {
                if (colors[cc].equals(SKIN)) {
                    zations[ii][cc] = _globals[0];
                } else if (colors[cc].equals(HAIR)) {
                    zations[ii][cc] = _globals[1];
                } else {
                    zations[ii][cc] = _pository.getColorization(
                        colors[cc], _colors[ccount++]);
                }
            }

            log.info("Decoded colors for " + ccomp.name + " into " +
                     StringUtil.toString(zations[ii]) + " using " +
                     StringUtil.toString(colors) + " and " +
                     StringUtil.toString(_colors));
        }

        return new CharacterDescriptor(componentIds, zations);
    }

    protected ColorPository _pository;
    protected ComponentRepository _crepo;

    /** Used by {@link #decodeAvatar}. */
    protected Colorization[] _globals = new Colorization[2];

    /** Used by {@link #decodeAvatar}. */
    protected int[] _colors = new int[3];
}
