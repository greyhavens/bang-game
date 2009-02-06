//
// $Id$

package com.threerings.bang.avatar.util;

import java.awt.Point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.media.image.ColorPository;
import com.threerings.media.image.ColorPository.ColorRecord;
import com.threerings.media.image.Colorization;
import com.threerings.presents.dobj.DObject;
import com.threerings.resource.ResourceManager;
import com.threerings.util.CompiledConfig;

import com.threerings.cast.CharacterComponent;
import com.threerings.cast.CharacterDescriptor;
import com.threerings.cast.ComponentClass;
import com.threerings.cast.ComponentRepository;
import com.threerings.cast.NoSuchComponentException;

import com.threerings.bang.data.Item;
import com.threerings.bang.data.Article;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangUtil;

import com.threerings.bang.game.util.PointSet;
import com.threerings.bang.gang.data.GangObject;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.data.LookConfig;

import static com.threerings.bang.Log.log;

/**
 * Used to calculate various things about avatars, decode avatar fingerprints and whatnot.
 */
public class AvatarLogic
{
    /** Defines a particular aspect of an avatar's look. An aspect will configure one or more
     * character components in the avatar's look. */
    public static class Aspect
    {
        /** A string identifier for this aspect. Translated for display on the client. */
        public String name;

        /** The names of the component classes configured by this aspect. */
        public String[] classes;

        /** Indicates whether or not this aspect can be omitted. */
        public boolean optional;

        /** Indicates that this aspect is only for male avatars. */
        public boolean maleOnly;

        public Aspect (String name, String[] classes, boolean optional, boolean maleOnly)
        {
            this.name = name;
            this.classes = classes;
            this.optional = optional;
            this.maleOnly = maleOnly;
        }

        public String toString ()
        {
            return StringUtil.fieldsToString(this);
        }
    }

    /** Defines a class of parts for gang buckles. */
    public static class PartClass
    {
        /** The name of the class. */
        public String name;

        /** For mandatory, singular elements, the index in the item array.  For
         * optional, multiple elements, -1. */
        public int idx;

        public PartClass (String name, int idx)
        {
            this.name = name;
            this.idx = idx;
        }

        /** Checks whether or not this part can be omitted. */
        public boolean isOptional ()
        {
            return (idx == -1);
        }

        /** Checks whether or not buckles can contain multiple instances of this part. */
        public boolean isMultiple ()
        {
            return (idx == -1);
        }
    }

    /** Defines the various aspects of an avatar's look. */
    public static final Aspect[] ASPECTS = {
        new Aspect("head", new String[] { "head" }, false, false),
        new Aspect("hair", new String[] { "hair_front", "hair_middle", "hair_back" }, false, false),
        new Aspect("eyebrows", new String[] { "eyebrows" }, false, false),
        new Aspect("eyes", new String[] { "eyes" }, false, false),
        new Aspect("nose", new String[] { "nose" }, false, false),
        new Aspect("mouth", new String[] { "mouth" }, false, false),
        new Aspect("mustache", new String[] { "mustache" }, true, true),
        new Aspect("beard", new String[] { "beard", "beard_back" }, true, true),
    };

    /** Defines the various article slots available to an avatar. */
    public static final Aspect[] SLOTS = {
        new Aspect("hat", new String[] { "hat", "hat_back", "hat_band" }, true, false),
        new Aspect("clothing", new String[] {
            "clothing_back", "clothing_front", "clothing_props" }, false, false),
        new Aspect("glasses", new String[] { "glasses" }, true, false),
        new Aspect("jewelry", new String[] { "jewelry" }, true, false),
        new Aspect("makeup", new String[] { "makeup" }, true, false),
        new Aspect("familiar", new String[] { "familiar" }, true, false),
    };

    /** Defines the classes of parts that define a buckle. */
    public static final PartClass[] BUCKLE_PARTS = {
        new PartClass("background", 0),
        new PartClass("border", 1),
        new PartClass("icon", -1),
    };

    /** The colorization class for skin colors. */
    public static final String SKIN = "skin";

    /** The colorization class for hair colors. */
    public static final String HAIR = "hair";

    /** The colorization class for eye colors. */
    public static final String EYES = "iris_t";

    /** The width of our avatar source images when shown in a frame. */
    public static final int FRAMED_WIDTH = 468;

    /** The height of our avatar source images when shown in a frame. */
    public static final int FRAMED_HEIGHT = 600;

    /** The width of our avatar source images. */
    public static final int WIDTH = 540;

    /** The height of our avatar source images. */
    public static final int HEIGHT = 640;

    /** The width of our buckle source images. */
    public static final int BUCKLE_WIDTH = 312;

    /** The height of our buckle source images. */
    public static final int BUCKLE_HEIGHT = 240;

    /**
     * Returns the index in the {@link #SLOTS} array of the specified slot.
     */
    public static int getSlotIndex (String slot)
    {
        for (int ii = 0; ii < SLOTS.length; ii++) {
            if (SLOTS[ii].name.equals(slot)) {
                return ii;
            }
        }
        return -1;
    }

    /**
     * Returns the index in the {@link #BUCKLE_PARTS} array of the specified part class.
     */
    public static int getPartIndex (String pclass)
    {
        for (int ii = 0; ii < BUCKLE_PARTS.length; ii++) {
            if (BUCKLE_PARTS[ii].name.equals(pclass)) {
                return ii;
            }
        }
        return -1;
    }

    /**
     * Returns the appropriate index for this color class (which depends on whether it is primary,
     * secondary or tertiary).
     */
    public static int getColorIndex (String cclass)
    {
        if (cclass.endsWith("_p")) {
            return 0;
        } else if (cclass.endsWith("_s")) {
            return 1;
        } else if (cclass.endsWith("_t")) {
            return 2;
        } else  {
            log.warning("Requested color index for non-indexed color", "cclass", cclass);
            return 0;
        }
    }

    /**
     * Creates a colorization mask with the specified colorization id in the appropriate position
     * for the specified colorization class.
     */
    public static int composeZation (String cclass, int colorId)
    {
        switch (getColorIndex(cclass)) {
        default:
        case 0: return composeZations(colorId, 0, 0);
        case 1: return composeZations(0, colorId, 0);
        case 2: return composeZations(0, 0, colorId);
        }
    }

    /**
     * Creates a colorization mask with the specified three colorization ids (which may be zero if
     * the component in question does not require secondary or tertiary colorizations). This value
     * can then be provided to {@link #createArticle}.
     */
    public static int composeZations (int primary, int secondary, int tertiary)
    {
        return (primary << 16) | (secondary << 21) | (tertiary << 26);
    }

    /**
     * Creates a colorization mask containing the specified colors.
     */
    public static int composeZations (ColorRecord[] crecs)
    {
        int zation = 0;
        for (ColorRecord crec : crecs) {
            zation |= composeZation(crec.cclass.name, crec.colorId);
        }
        return zation;
    }

    /** Decodes the global skin colorization. */
    public static int decodeSkin (int value)
    {
        return value & 0x1F;
    }

    /** Decodes the global hair colorization. */
    public static int decodeHair (int value)
    {
        return (value >> 5) & 0x1F;
    }

    /** Decodes the primary colorization. */
    public static int decodePrimary (int value)
    {
        return (value >> 16) & 0x1F;
    }

    /** Decodes the secondary colorization. */
    public static int decodeSecondary (int value)
    {
        return (value >> 21) & 0x1F;
    }

    /** Decodes the tertiary colorization. */
    public static int decodeTertiary (int value)
    {
        return (value >> 26) & 0x1F;
    }

    /**
     * Strips the supplied list of articles from the supplied looks as well as verifying that
     * all the looks have valid articles, returning a list of looks that were modified during
     * the process.
     */
    public static ArrayList<Look> stripLooks (
            ArrayIntSet removals, Iterable<Item> items, Iterable<Look> looks)
    {
        // find the item ids of all gang articles as well as suitable replacements for
        // each slot
        int[] replacements = new int[AvatarLogic.SLOTS.length];
        ArrayIntSet validArts = new ArrayIntSet();
        for (Item item : items) {
            if (!(item instanceof Article)) {
                continue;
            }
            Article article = (Article)item;
            int itemId = article.getItemId();
            validArts.add(itemId);
            if (article.getGangId() > 0 || removals.contains(itemId)) {
                continue;
            }
            int sidx = getSlotIndex(article.getSlot());
            if (!SLOTS[sidx].optional) {
                // we end up with the newest articles for each slot, or 0 if we can't
                // find one (which shouldn't happen).  the selection doesn't really
                // matter, but we need to be consistent between the database and the
                // dobj
                replacements[sidx] = Math.max(replacements[sidx], itemId);
            }
        }

        // modify the looks
        ArrayList<Look> modified = new ArrayList<Look>();
        for (Look look : looks) {
            int[] articles = look.articles;
            boolean replaced = false;
            for (int ii = 0; ii < articles.length; ii++) {
                if (removals.contains(articles[ii]) || !validArts.contains(articles[ii])) {
                    articles[ii] = replacements[ii];
                    replaced = true;
                }
            }
            if (replaced) {
                modified.add(look);
            }
        }
        return modified;
    }

    /**
     * Creates a logic instance which will make use of the supplied sources to obtain avatar
     * related information.
     */
    public AvatarLogic (ResourceManager rsrcmgr, ComponentRepository crepo)
        throws IOException
    {
        _crepo = crepo;
        _pository = ColorPository.loadColorPository(rsrcmgr);
        _aspcat = (AspectCatalog)CompiledConfig.loadConfig(
            rsrcmgr.getResource(AspectCatalog.CONFIG_PATH));
        _artcat = (ArticleCatalog)CompiledConfig.loadConfig(
            rsrcmgr.getResource(ArticleCatalog.CONFIG_PATH));
        _partcat = (BucklePartCatalog)CompiledConfig.loadConfig(
            rsrcmgr.getResource(BucklePartCatalog.CONFIG_PATH));
    }

    /**
     * Returns the repository which defines our various recolorizations.
     */
    public ColorPository getColorPository ()
    {
        return _pository;
    }

    /**
     * Returns the catalog that defines the various avatar aspects.
     */
    public AspectCatalog getAspectCatalog ()
    {
        return _aspcat;
    }

    /**
     * Returns the catalog that defines the various avatar articles.
     */
    public ArticleCatalog getArticleCatalog ()
    {
        return _artcat;
    }

    /**
     * Returns the catalog that defines the various buckle parts.
     */
    public BucklePartCatalog getBucklePartCatalog ()
    {
        return _partcat;
    }

    /**
     * Decodes an avatar fingerprint into a {@link CharacterDescriptor} that can be passed to the
     * character manager.
     */
    public CharacterDescriptor decodeAvatar (int[] avatar)
    {
        // decode the skin and hair colorizations
        _globals[0] = _pository.getColorization(SKIN, decodeSkin(avatar[0]));
        _globals[1] = _pository.getColorization(HAIR, decodeHair(avatar[0]));

        // compact the array to remove unused entries
        avatar = IntListUtil.compact(avatar);

        // the subsequent elements are article colorizations and component ids composed into a
        // single integer
        int clength = avatar.length-1;
        int[] componentIds = new int[clength];
        Colorization[][] zations = new Colorization[clength][];
        for (int ii = 0; ii < clength; ii++) {
            int pvalue = avatar[ii+1];
            componentIds[ii] = (pvalue & 0xFFFF);
            zations[ii] = decodeColorizations(pvalue, null);
        }

        return new CharacterDescriptor(componentIds, zations);
    }

    /**
     * Decodes a buckle fingerprint into a {@link CharacterDescriptor} that can be passed to the
     * character manager.
     */
    public CharacterDescriptor decodeBuckle (int[] buckle)
    {
        // each element consists of two integers: the first containing the component id and
        // colorization, the second containing the encoded coordinates (if any)
        int[] componentIds = new int[buckle.length / 2];
        Colorization[][] zations = new Colorization[componentIds.length][];
        Point[] xlations = new Point[componentIds.length];

        for (int ii = 0; ii < componentIds.length; ii++) {
            int pvalue = buckle[ii*2];
            componentIds[ii] = (pvalue & 0xFFFF);
            zations[ii] = decodeColorizations(pvalue, null);
            try {
                CharacterComponent ccomp = _crepo.getComponent(componentIds[ii]);
                if (!ccomp.componentClass.translate) {
                    continue;
                }
                int cvalue = buckle[ii*2+1];
                xlations[ii] = new Point(PointSet.decodeX(cvalue), PointSet.decodeY(cvalue));

            } catch (NoSuchComponentException e) {
                // a warning will have already been logged
            }
        }

        CharacterDescriptor cdesc = new CharacterDescriptor(componentIds, zations);
        cdesc.setTranslations(xlations);
        return cdesc;
    }

    /**
     * Determines whether the given avatar belongs to a male character by looking for a
     * gender-specific component.  Returns false if no gender-specific components are found.
     */
    public boolean isMale (AvatarInfo avatar)
    {
        if (avatar == null || avatar.print == null) {
            return false;
        }
        for (int ii = 1; ii < avatar.print.length; ii++) {
            CharacterComponent ccomp;
            try {
                ccomp = _crepo.getComponent(avatar.print[ii] & 0xFFFF);
            } catch (NoSuchComponentException e) {
                continue;
            }
            if (ccomp.componentClass.name.startsWith("male")) {
                return true;
            } else if (ccomp.componentClass.name.startsWith("female")) {
                return false;
            }
        }
        return false;
    }

    /**
     * Decodes and returns the colorizations encoded into the supplied encoded component.
     *
     * @param colors usually null, in which case the colorization classes appropriate to the
     * specified component will be used, but if non-null, they are a list of colorization classes
     * to use instead.
     */
    public Colorization[] decodeColorizations (int fqComponentId, String[] colors)
    {
        // look up the component in the repository
        int componentId = (fqComponentId & 0xFFFF);
        CharacterComponent ccomp = null;
        try {
            ccomp = _crepo.getComponent(componentId);
        } catch (NoSuchComponentException nsce) {
            log.warning("Avatar contains non-existent component", "compId", componentId);
            return null;
        }

        // if we weren't provided with classes, use the values from the component class record
        ArrayList<String> cols = new ArrayList<String>();
        if (colors == null) {
            CollectionUtil.addAll(cols, ccomp.componentClass.colors);
            CollectionUtil.addAll(cols, _artcat.getColorOverrides(
                                      ccomp.componentClass.name, ccomp.name));
        } else {
            CollectionUtil.addAll(cols, colors);
        }

        // decode the colorization color id values
        _colors[0] = decodePrimary(fqComponentId);
        _colors[1] = decodeSecondary(fqComponentId);
        _colors[2] = decodeTertiary(fqComponentId);

        // look up the actual colorizations from those
        Colorization[] zations = new Colorization[5];
        for (String color : cols) {
            if (color.equals(SKIN)) {
                zations[3] = _globals[0];
            } else if (color.equals(HAIR)) {
                zations[4] = _globals[1];
            } else {
                int cidx = getColorIndex(color);
                zations[cidx] = _pository.getColorization(color, _colors[cidx]);
            }
        }

        /*
        log.info("Decoded colors for " + ccomp.name + " into " +
                  StringUtil.toString(zations) + " using " +
                  StringUtil.toString(colors) + " and " +
                  StringUtil.toString(_colors));
        */

        return zations;
    }

    /**
     * Creates a new {@link Look} with the specified configuration.
     *
     * @return the newly created look or null if the look configuration was invalid for some reason
     * (in which case an error will have been logged).
     *
     * @param user the user for whom we are creating the look.
     * @param cost a two element array into which the scrip and coin cost of the look will be
     * filled in (in that order).
     */
    public Look createLook (PlayerObject user, LookConfig config, int[] cost)
    {
        String gender = user.isMale ? "male/" : "female/";
        int scrip = AvatarCodes.BASE_LOOK_SCRIP_COST, coins = AvatarCodes.BASE_LOOK_COIN_COST;
        ArrayIntSet compids = new ArrayIntSet();
        for (int ii = 0; ii < config.aspects.length; ii++) {
            AvatarLogic.Aspect aclass = AvatarLogic.ASPECTS[ii];
            String acname = gender + aclass.name;
            if (config.aspects[ii] == null) {
                if (aclass.optional) {
                    continue;
                }
                log.warning("Requested to purchase look that is missing a non-optional aspect",
                            "who", user.who(), "class", acname);
                return null;
            }

            AspectCatalog.Aspect aspect = _aspcat.getAspect(acname, config.aspects[ii]);
            if (aspect == null) {
                log.warning("Requested to purchase look with unknown aspect", "who", user.who(),
                            "class", acname, "choice", config.aspects[ii]);
                return null;
            }

            // make sure the aspect is from the appropriate town
            if (BangUtil.getTownIndex(aspect.townId) >
                BangUtil.getTownIndex(user.townId)) {
                log.warning("Requested to purchase look in invalid town", "who", user.who(),
                            "intown", user.townId, "aspect", aspect);
                return null;
            }

            // add the cost to the total cost
            scrip += aspect.scrip;
            coins += aspect.coins;

            // look up the aspect's components
            for (int cc = 0; cc < aclass.classes.length; cc++) {
                String cclass = gender + aclass.classes[cc];
                try {
                    CharacterComponent ccomp = _crepo.getComponent(
                        cclass, aspect.name);
                    int compmask = ccomp.componentId;
                    if (config.colors[ii] != 0) {
                        // TODO: additional costs for some colors?
                        compmask |= config.colors[ii];
                    }
                    compids.add(compmask);
                } catch (NoSuchComponentException nsce) {
                    // no problem, some of these are optional
                }
            }
        }

        Look look = new Look();
        look.name = config.name;
        look.aspects = new int[compids.size()+1];
        // TODO: additional costs for some hair and skin colorizations?
        look.aspects[0] = (config.hair << 5) | config.skin;
        compids.toIntArray(look.aspects, 1);

        cost[0] = scrip;
        cost[1] = user.holdsGoldPass(user.townId) ? 0 : coins;
        return look;
    }

    /**
     * Picks a set of random aspects from the set available to the given player.
     */
    public int[] pickRandomAspects (boolean male, PlayerObject user)
    {
        String gender = male ? "male/" : "female/";
        ArrayIntSet compids = new ArrayIntSet();
        for (Aspect aspect : ASPECTS) {
            if ((aspect.maleOnly && !male) || aspect.optional) {
                continue;
            }

            ArrayList<AspectCatalog.Aspect> catasps = new ArrayList<AspectCatalog.Aspect>();
            for (AspectCatalog.Aspect catasp : _aspcat.getAspects(gender + aspect.name)) {
                if (catasp.scrip <= AvatarCodes.MAX_STARTER_COST && catasp.coins == 0 &&
                    BangUtil.getTownIndex(catasp.townId) <= BangUtil.getTownIndex(user.townId)) {
                    catasps.add(catasp);
                }
            }

            AspectCatalog.Aspect catasp = RandomUtil.pickRandom(catasps);
            for (String cclass : aspect.classes) {
                try {
                    CharacterComponent ccomp = _crepo.getComponent(gender + cclass, catasp.name);
                    int compmask = ccomp.componentId;
                    for (String color : ccomp.componentClass.colors) {
                        if (color.equals(HAIR) || color.equals(SKIN)) {
                            continue;
                        }
                        compmask |= composeZation(color,
                            ColorConstraints.pickRandomColor(_pository, color, user).colorId);
                    }
                    compids.add(compmask);

                } catch (NoSuchComponentException nsce) {
                    // no problem, some of these are optional
                }
            }
        }

        int[] aspects = new int[compids.size()+1];
        int hair = ColorConstraints.pickRandomColor(_pository, HAIR, user).colorId,
            skin = ColorConstraints.pickRandomColor(_pository, SKIN, user).colorId;
        aspects[0] = (hair << 5) | skin;
        compids.toIntArray(aspects, 1);
        return aspects;
    }

    /**
     * Creates an inventory article from an article catalog entry and a colorization mask.
     */
    public Article createArticle (int playerId, ArticleCatalog.Article article, int zations)
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
            log.warning("Requested to create article for unknown slot", "pid", playerId,
                        "article", article);
            return null;
        }
        String type = article.townId + "/" + article.name;
        int[] compIds = getComponentIds(article, zations);
        if (compIds == null) {
            return null; // an error will have been logged
        }
        return new Article(playerId, article.slot, type, compIds, article.stop);
    }

    /**
     * Creates the default clothing article for the specified gender, choosing random
     * colorizations.
     */
    public Article createDefaultClothing (PlayerObject user, boolean forMale)
    {
        int primary = ColorConstraints.pickRandomColor(_pository, "clothes_p", user).colorId;
        int secondary = ColorConstraints.pickRandomColor(_pository, "clothes_s", user).colorId;
        int tertiary = ColorConstraints.pickRandomColor(_pository, "clothes_t", user).colorId;
        return createDefaultClothing(user, forMale, composeZations(primary, secondary, tertiary));
    }

    /**
     * Creates the default clothing article for the specified gender, with the specified
     * colorizations (which should have come from {@link #composeZation}.
     */
    public Article createDefaultClothing (PlayerObject user, boolean forMale, int zations)
    {
        // look up the starter article
        String prefix = forMale ? "male" : "female";
        ArticleCatalog.Article article = null;
        for (ArticleCatalog.Article art : _artcat.getArticles()) {
            if (art.name.startsWith(prefix) && art.starter) {
                article = art;
            }
        }
        if (article == null) {
            log.warning("Missing starter clothing article", "gender", prefix);
            return null;
        }
        return createArticle(user.playerId, article, zations);
    }

    /**
     * Creates a starter gang buckle.  The returned items will need to have their ownerIds
     * set.
     */
    public BucklePart[] createDefaultBuckle ()
    {
        // create a dummy gangobj
        GangObject gangobj = new GangObject();

        // add instances of all required parts
        ArrayList<BucklePart> parts = new ArrayList<BucklePart>();
        for (PartClass pclass : BUCKLE_PARTS) {
            if (pclass.isOptional()) {
                continue;
            }
            BucklePartCatalog.Part part = _partcat.getStarter(pclass.name);
            if (part == null) {
                log.warning("Couldn't find starter buckle part", "gang", gangobj.name,
                            "class", pclass.name);
                continue;
            }
            ColorRecord[] crecs = pickRandomColors(getColorizationClasses(part), gangobj);
            parts.add(createBucklePart(-1, part, composeZations(crecs)));
        }
        return parts.toArray(new BucklePart[parts.size()]);
    }

    /**
     * Creates a buckle part from a catalog entry and a colorization mask.
     */
    public BucklePart createBucklePart (int gangId, BucklePartCatalog.Part part, int zations)
    {
        int[] compIds = getComponentIds(part, zations);
        if (compIds == null) {
            return null; // an error will have been logged
        }
        return new BucklePart(gangId, part.pclass.name, part.name, compIds);
    }

    /**
     * Picks a set of random colors for the supplied article, limiting them to those accessible
     * by the given entity.
     */
    public ColorRecord[] pickRandomColors (ArticleCatalog.Article article, DObject entity)
    {
        return pickRandomColors(getColorizationClasses(article), entity);
    }

    /**
     * Picks random colors for each of the color classes specified.
     */
    public ColorRecord[] pickRandomColors (String[] cclasses, DObject entity)
    {
        ColorRecord[] crecs = new ColorRecord[cclasses.length];
        for (int ii = 0; ii < cclasses.length; ii++) {
            crecs[ii] = ColorConstraints.pickRandomColor(_pository, cclasses[ii], entity);
        }
        return crecs;
    }

    /**
     * Returns the colorization classes used by the specified article.
     */
    public String[] getColorizationClasses (ArticleCatalog.Article article)
    {
        // if a specific set of colorizations have not been specified for an article, we generate
        // the list by computing the union of the classes used by each of the individual components
        // in the article; then we cache it because we're cool like that
        if (article.colors == null) {
            HashSet<String> classes = new HashSet<String>();
            for (ArticleCatalog.Component comp : article.components) {
                ComponentClass cclass = _crepo.getComponentClass(comp.cclass);
                if (cclass == null) {
                    log.warning("Missing component classs for article", "article", article,
                                "cclass", comp.cclass);
                    continue;
                }
                for (int ii = 0; ii < cclass.colors.length; ii++) {
                    classes.add(cclass.colors[ii]);
                }
            }
            article.colors = classes.toArray(new String[classes.size()]);
        }
        return article.colors;
    }

    /**
     * Returns the colorization classes used by the specified part.
     */
    public String[] getColorizationClasses (BucklePartCatalog.Part part)
    {
        if (part.colors == null) {
            ComponentClass cclass = _crepo.getComponentClass(part.pclass.getComponentClass());
            part.colors = cclass.colors;
        }
        return part.colors;
    }

    /**
     * Looks up the appropriate component ids for the specified article, combines them with the
     * supplied colorizations and returns an array suitable for using in an {@link Article}
     * instance.
     */
    public int[] getComponentIds (ArticleCatalog.Article article, int zations)
    {
        int[] componentIds = new int[article.components.size()];
        int idx = 0;
        for (ArticleCatalog.Component comp : article.components) {
            try {
                CharacterComponent ccomp = _crepo.getComponent(comp.cclass, comp.name);
                // the zations are already shifted 16 bits left
                componentIds[idx++] = ccomp.componentId | zations;
            } catch (NoSuchComponentException nsce) {
                log.warning("Article references unknown component", "article", article.name,
                            "cclass", comp.cclass, "name", comp.name);
                return null; // abandon ship!
            }
        }
        return componentIds;
    }

    /**
     * Looks up the component id for the specified part and combines it with the given colorization
     * mask.
     */
    public int[] getComponentIds (BucklePartCatalog.Part part, int zations)
    {
        int[] componentIds = null;
        String cclass = part.pclass.getComponentClass();
        try {
            CharacterComponent ccomp = _crepo.getComponent(cclass, part.name);
            componentIds = new int[] { ccomp.componentId | zations };
        } catch (NoSuchComponentException nsce) {
            log.warning("Buckle part does not correspond to component", "part", part.name,
                        "cclass", cclass);
            return null; // abandon ship!
        }
        return componentIds;
    }

    protected ComponentRepository _crepo;
    protected ColorPository _pository;
    protected AspectCatalog _aspcat;
    protected ArticleCatalog _artcat;
    protected BucklePartCatalog _partcat;

    /** Used by {@link #decodeAvatar}. */
    protected Colorization[] _globals = new Colorization[2];

    /** Used by {@link #decodeAvatar}. */
    protected int[] _colors = new int[3];
}
