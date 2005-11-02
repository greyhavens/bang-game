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

import com.threerings.bang.data.Article;

import static com.threerings.bang.Log.log;

/**
 * Used to calculate various things about avatars, decode avatar fingerprints
 * and whatnot.
 */
public class AvatarMetrics
{
    /** Defines a particular aspect of an avatar's look. An aspect will
     * configure one or more character components in the avatar's look. */
    public static class Aspect
    {
        /** A string identifier for this aspect. Translated for display on the
         * client. */
        public String name;

        /** The names of the component classes configured by this aspect. */
        public String[] classes;

        /** Indicates whether or not this aspect can be omitted. */
        public boolean optional;

        /** Indicates that this aspect is only for male avatars. */
        public boolean maleOnly;

        public Aspect (String name, String[] classes,
                       boolean optional, boolean maleOnly)
        {
            this.name = name;
            this.classes = classes;
            this.optional = optional;
            this.maleOnly = maleOnly;
        }
    }

    /** Defines the various aspects of an avatar's look. */
    public static final Aspect[] ASPECTS = {
        new Aspect("head", new String[] { "head" }, false, false),
        new Aspect("hair", new String[] {
            "hair_front", "hair_middle", "hair_back" }, false, false),
        new Aspect("eyebrows", new String[] { "eyebrows" }, false, false),
        new Aspect("eyes", new String[] { "eyes" }, false, false),
        new Aspect("nose", new String[] { "nose" }, false, false),
        new Aspect("mustache", new String[] { "mustache" }, true, true),
        new Aspect("mouth", new String[] { "mouth" }, false, false),
        new Aspect("beard", new String[] { "beard", "beard_back" }, true, true),
    };

    /** Defines the various article slots available to an avatar. */
    public static final Aspect[] SLOTS = {
        new Aspect("hat", new String[] {
            "hat", "hat_back", "hat_band" }, true, false),
        new Aspect("clothing", new String[] {
            "clothing_back", "clothing_front", "clothing_props" }, false, false),
        new Aspect("glasses", new String[] { "glasses" }, true, false),
        new Aspect("jewelry", new String[] { "jewelry" }, true, false),
        new Aspect("makeup", new String[] { "makeup" }, true, false),
        new Aspect("familiar", new String[] { "familiar" }, true, false),
    };

    /** The colorization class for skin colors. */
    public static final String SKIN = "skin";

    /** The colorization class for hair colors. */
    public static final String HAIR = "hair";

    /** The width of our avatar source images. */
    public static final int WIDTH = 468;

    /** The height of our avatar source images. */
    public static final int HEIGHT = 600;

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
            for (int cc = 0, ccount = 0; cc < colors.length; cc++) {
                if (colors[cc].equals(SKIN)) {
                    zations[ii][cc] = _globals[0];
                } else if (colors[cc].equals(HAIR)) {
                    zations[ii][cc] = _globals[1];
                } else {
                    zations[ii][cc] = _pository.getColorization(
                        colors[cc], _colors[ccount++]);
                }
            }

//             log.info("Decoded colors for " + ccomp.name + " into " +
//                      StringUtil.toString(zations[ii]) + " using " +
//                      StringUtil.toString(colors) + " and " +
//                      StringUtil.toString(_colors));
        }

        return new CharacterDescriptor(componentIds, zations);
    }

    /**
     * Creates an inventory article from an article catalog entry and a set of
     * colorizations.
     */
    public Article createArticle (int playerId, ArticleCatalog.Article article
                                  /* TODO: , zations */)
    {
        // sanity check the slot name
        Aspect slot = null;
        for (int ii = 0; ii < SLOTS.length; ii++) {
            if (SLOTS[ii].name.equals(article.slot)) {
                slot = SLOTS[ii];
                break;
            }
        }
        if (slot == null) {
            log.warning("Requested to create article for unknown slot " +
                        "[pid=" + playerId + ", article=" + article + "].");
            return null;
        }

        // look up the component ids of the various components in the article
        int[] componentIds = new int[article.components.size()];
        int idx = 0;
        for (ArticleCatalog.Component comp : article.components) {
            try {
                CharacterComponent ccomp = _crepo.getComponent(
                    comp.cclass, comp.name);
                componentIds[idx++] = ccomp.componentId;
                // TODO: add the colorizations
            } catch (NoSuchComponentException nsce) {
                log.warning("Article references unknown component " +
                            "[article=" + article.name +
                            ", cclass=" + comp.cclass +
                            ", name=" + comp.name + "].");
            }
        }

        return new Article(playerId, article.slot, article.name, componentIds);
    }

    protected ColorPository _pository;
    protected ComponentRepository _crepo;

    /** Used by {@link #decodeAvatar}. */
    protected Colorization[] _globals = new Colorization[2];

    /** Used by {@link #decodeAvatar}. */
    protected int[] _colors = new int[3];
}
