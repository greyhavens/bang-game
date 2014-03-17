//
// $Id$

package com.threerings.bang.avatar.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.samskivert.util.Predicate;
import com.samskivert.util.RandomUtil;

import com.threerings.media.image.ColorPository.ClassRecord;
import com.threerings.media.image.ColorPository.ColorRecord;
import com.threerings.media.image.ColorPository;

import com.threerings.presents.dobj.DObject;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.PlayerObject;

import com.threerings.bang.gang.data.GangObject;

import static com.threerings.bang.Log.log;

/**
 * Defines the various constraints on access to our various color classes
 * (hair, skin, clothing, etc.).
 */
public class ColorConstraints
{
    /**
     * Selects a random color from the records available to the supplied player or gang in the
     * specified class. Returns null if no colors are available to the player or gang (which should
     * never happen unless something is booched).
     */
    public static ColorRecord pickRandomColor (
        ColorPository pository, String colorClass, DObject entity)
    {
        ArrayList<ColorRecord> records =
            getAvailableColors(pository, colorClass, entity);
        return records.size() == 0 ? null : RandomUtil.pickRandom(records);
    }

    /**
     * Returns a list of color records available to the supplied player or gang in the
     * specified color class.
     */
    public static ArrayList<ColorRecord> getAvailableColors (
        ColorPository pository, String colorClass, DObject entity)
    {
        ArrayList<ColorRecord> colors = new ArrayList<ColorRecord>();
        ClassRecord clrec = pository.getClassRecord(colorClass);
        if (clrec == null) {
            log.warning("Requested non-existent color class", "class", colorClass,
                        "which", entity.which());
            return colors;
        }

        HashMap<String,Predicate<DObject>> preds = _preds.get(colorClass);
        Iterator<ColorRecord> iter = clrec.colors.values().iterator();
        while (iter.hasNext()) {
            ColorRecord crec = iter.next();
            Predicate<DObject> pred = preds.get(crec.name);
            if (pred == null) {
                log.warning("Missing predicate for color", "class", colorClass, "color", crec.name);
                continue;
            }
            if (pred.isMatch(entity)) {
                colors.add(crec);
            }
        }
        return colors;
    }

    /**
     * Returns true if the specified color is valid for the specified player or gang.
     */
    public static boolean isValidColor (
        ColorPository pository, String colorClass, int colorId,
        DObject entity)
    {
        ClassRecord clrec = pository.getClassRecord(colorClass);
        HashMap<String,Predicate<DObject>> preds = _preds.get(colorClass);
        if (clrec == null || preds == null) {
            return false;
        }
        ColorRecord crec = clrec.colors.get(colorId);
        if (crec == null) {
            return false;
        }
        Predicate<DObject> pred = preds.get(crec.name);
        return (pred == null) ? false : pred.isMatch(entity);
    }

    /** We use this to disable colors until we know what we want to do. */
    protected static class Disabled extends Predicate<DObject> {
        public boolean isMatch (DObject entity) {
            return false;
        }
    }

    /** Starter colors are available to every player and every gang any time. */
    protected static class Starter extends Predicate<DObject> {
        public boolean isMatch (DObject entity) {
            return true;
        }
    }

    /** Superclass of predicates that act differently on users and gangs. */
    protected static abstract class DoublePredicate extends Predicate<DObject> {
        public boolean isMatch (DObject entity) {
            if (entity instanceof PlayerObject) {
                return isMatch((PlayerObject)entity);
            } else { // entity instanceof GangObject
                return isMatch((GangObject)entity);
            }
        }
        public abstract boolean isMatch (PlayerObject user);
        public abstract boolean isMatch (GangObject gang);
    }

    /** Normal colors are available to every player after having created their
     * first avatar look and to gangs after they've been given their first buckle. */
    protected static class Normal extends DoublePredicate {
        public boolean isMatch (PlayerObject user) {
            // available to anyone that has created their initial avatar
            return user.hasCharacter();
        }
        public boolean isMatch (GangObject gang) {
            return (gang.buckle != null);
        }
    }

    /** Some colors are only available to players that hold a particular
     * badge (never available to gangs, at the moment). */
    protected static class HoldsBadge extends DoublePredicate {
        public HoldsBadge (Badge.Type badge) {
            _badge = badge;
        }
        public boolean isMatch (PlayerObject user) {
            // available to anyone that has created their initial avatar
            return user.holdsBadge(_badge);
        }
        public boolean isMatch (GangObject gang) {
            return false;
        }
        protected Badge.Type _badge;
    }

    protected static HashMap<String,HashMap<String,Predicate<DObject>>>
        _preds = new HashMap<String,HashMap<String,Predicate<DObject>>>();

    static {
        HashMap<String,Predicate<DObject>> preds;

        _preds.put("hair", preds =
            new HashMap<String,Predicate<DObject>>());
        preds.put("black", new Starter());
        preds.put("blonde", new Starter());
        preds.put("brown", new Starter());
        preds.put("lightBrown", new Starter());
        preds.put("sandyBlonde", new Starter());
        preds.put("toehead", new Starter());

        preds.put("aqua", new Normal());
        preds.put("grey", new Normal());
        preds.put("lime", new Normal());
        preds.put("maroon", new Normal());
        preds.put("red", new Normal());
        preds.put("white", new Normal());

        // note: additional badge rewards must be noted in Badge.java
        preds.put("blue", new HoldsBadge(Badge.Type.UNITS_KILLED_2));
        preds.put("green", new HoldsBadge(Badge.Type.SHOTS_FIRED_1));
        preds.put("navyBlue", new HoldsBadge(Badge.Type.GAMES_PLAYED_3));
        preds.put("orange", new HoldsBadge(Badge.Type.UNITS_KILLED_3));
        preds.put("purple", new HoldsBadge(Badge.Type.CONSEC_WINS_2));
        preds.put("violet", new HoldsBadge(Badge.Type.GAMES_PLAYED_2));

        _preds.put("skin", preds =
            new HashMap<String,Predicate<DObject>>());
        preds.put("darkest", new Starter());
        preds.put("warm_dark", new Starter());
        preds.put("dark", new Starter());
        preds.put("native", new Starter());
        preds.put("mixed", new Starter());
        preds.put("medium", new Starter());
        preds.put("mild", new Starter());
        preds.put("tan", new Starter());
        preds.put("white", new Starter());
        preds.put("pasty", new Starter());

        _preds.put("iris_t",
            preds = new HashMap<String,Predicate<DObject>>());
        preds.put("beige", new Starter());
        preds.put("blue", new Starter());
        preds.put("brown", new Starter());
        preds.put("hazel", new Starter());

        preds.put("green", new Normal());
        preds.put("lime", new Normal());
        preds.put("sky", new Normal());

        // note: additional badge rewards must be noted in Badge.java
        preds.put("violet", new HoldsBadge(Badge.Type.GAMES_PLAYED_1));
        preds.put("orange", new HoldsBadge(Badge.Type.CASH_EARNED_1));
        preds.put("purple", new HoldsBadge(Badge.Type.BONUSES_COLLECTED_2));
        preds.put("red", new HoldsBadge(Badge.Type.UNITS_KILLED_3));

        _preds.put("makeup_p",
            preds = new HashMap<String,Predicate<DObject>>());
        _preds.put("makeup_s", preds);
        preds.put("aqua", new Normal());
        preds.put("black", new Normal());
        preds.put("blue", new Normal());
        preds.put("brown", new Normal());
        preds.put("darkBrown", new Normal());
        preds.put("gold", new Normal());
        preds.put("green", new Normal());
        preds.put("grey", new Normal());
        preds.put("lime", new Normal());
        preds.put("maroon", new Normal());
        preds.put("navyBlue", new Normal());
        preds.put("orange", new Normal());
        preds.put("pink", new Normal());
        preds.put("purple", new Normal());
        preds.put("red", new Normal());
        preds.put("violet", new Normal());
        preds.put("white", new Normal());
        preds.put("yellow", new Normal());

        _preds.put("clothes_p",
            preds = new HashMap<String,Predicate<DObject>>());
        _preds.put("clothes_s", preds);
        _preds.put("clothes_t", preds);

        preds.put("beige", new Starter());
        preds.put("blue", new Starter());
        preds.put("brown", new Starter());
        preds.put("green", new Starter());
        preds.put("grey", new Starter());

        preds.put("aqua", new Normal());
        preds.put("lime", new Normal());
        preds.put("red", new Normal());
        preds.put("slate", new Normal());
        preds.put("white", new Normal());
        preds.put("yellow", new Normal());

        // come up with badge requirements for these
        preds.put("dkbrn", new Normal());
        preds.put("gold", new Normal());
        preds.put("maroon", new Normal());
        preds.put("moss", new Normal());
        preds.put("navyBlue", new Normal());
        preds.put("olive", new Normal());
        preds.put("purple", new Normal());

        // note: additional badge rewards must be noted in Badge.java
        preds.put("black", new HoldsBadge(Badge.Type.CONSEC_WINS_3));
        preds.put("leather", new HoldsBadge(Badge.Type.CARDS_PLAYED_1));
        preds.put("orange", new HoldsBadge(Badge.Type.CARDS_PLAYED_2));
        preds.put("pink", new HoldsBadge(Badge.Type.LOOKS_BOUGHT_1));
        preds.put("violet", new HoldsBadge(Badge.Type.DUDS_BOUGHT_2));

        _preds.put("familiar_p",
            preds = new HashMap<String,Predicate<DObject>>());
        _preds.put("familiar_s", preds);
        _preds.put("familiar_t", preds);

        // TODO: figure out requirements for these
        _preds.put("buckle_p",
            preds = new HashMap<String,Predicate<DObject>>());
        _preds.put("buckle_back_p", preds);
        _preds.put("buckle_back_s", preds);

        preds.put("gold", new Normal());
        preds.put("old_gold", new Normal());
        preds.put("silver", new Normal());
        preds.put("bronze", new Normal());
        preds.put("rust", new Starter());
        preds.put("dark", new Starter());
        preds.put("green", new Starter());
        preds.put("steel", new Starter());
        preds.put("copper", new Normal());
        preds.put("old", new Starter());
        preds.put("blue", new Starter());
        preds.put("greencopper", new Starter());
        preds.put("bluesteel", new Starter());
        preds.put("white", new Normal());
        preds.put("tan", new Disabled());
        preds.put("dkbrn", new Normal());
        preds.put("leather", new Starter());
        preds.put("brown", new Starter());
        preds.put("pink", new Disabled());
        preds.put("red", new Normal());
        preds.put("maroon", new Normal());
        preds.put("orange", new Disabled());
        preds.put("yellow", new Normal());
        preds.put("moss", new Normal());
        preds.put("lime", new Normal());
        preds.put("aqua", new Normal());
        preds.put("violet", new Normal());
        preds.put("purple", new Normal());
        preds.put("black", new Disabled());
        preds.put("grey", new Normal());

        _preds.put("armadillo_t",
                preds = new HashMap<String,Predicate<DObject>>());
        preds.put("old_gold", new Normal());
        preds.put("silver", new Normal());
        preds.put("bronze", new Normal());
        preds.put("greencopper", new Normal());
        preds.put("old", new Normal());
        preds.put("bluesteel", new Normal());
        preds.put("white", new Normal());
        preds.put("dkbrn", new Normal());
        preds.put("brown", new Normal());
        preds.put("grey", new Normal());
        preds.put("black", new Normal());
        preds.put("purple", new Normal());

        _preds.put("raccoon_p",
                preds = new HashMap<String,Predicate<DObject>>());
        preds.put("dkbrn", new Normal());
        preds.put("leather", new Normal());
        preds.put("brown", new Normal());
        preds.put("beige", new Normal());
        preds.put("orange", new Normal());
        preds.put("gold", new Normal());
        preds.put("yellow", new Normal());
        preds.put("moss", new Normal());
        preds.put("olive", new Normal());
        preds.put("slate", new Normal());
        preds.put("white", new Normal());
        preds.put("grey", new Normal());
        preds.put("black", new Normal());

        _preds.put("raccoon_s",
                preds = new HashMap<String,Predicate<DObject>>());
        preds.put("redbrown", new Normal());
        preds.put("greybrown", new Normal());
        preds.put("beige", new Normal());
        preds.put("brown", new Normal());
        preds.put("orange", new Normal());
        preds.put("tan", new Normal());
        preds.put("moss", new Normal());
        preds.put("slate", new Normal());
        preds.put("grey", new Normal());
        preds.put("black", new Normal());

        _preds.put("buzzard_p",
                preds = new HashMap<String,Predicate<DObject>>());
        preds.put("leather", new Normal());
        preds.put("brown", new Normal());
        preds.put("beige", new Normal());
        preds.put("pink", new Normal());
        preds.put("red", new Normal());
        preds.put("maroon", new Normal());
        preds.put("orange", new Normal());
        preds.put("gold", new Normal());
        preds.put("olive", new Normal());
        preds.put("blue", new Normal());
        preds.put("slate", new Normal());
        preds.put("violet", new Normal());
        preds.put("grey", new Normal());
    }
}
