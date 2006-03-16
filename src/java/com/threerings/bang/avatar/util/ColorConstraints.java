//
// $Id$

package com.threerings.bang.avatar.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.threerings.media.image.ColorPository.ClassRecord;
import com.threerings.media.image.ColorPository.ColorRecord;
import com.threerings.media.image.ColorPository;
import com.threerings.util.RandomUtil;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.PlayerObject;

import static com.threerings.bang.Log.log;

/**
 * Defines the various constraints on access to our various color classes
 * (hair, skin, clothing, etc.).
 */
public class ColorConstraints
{
    public interface Predicate
    {
        /** Returns true if the color in question is available to the specified
         * player. */
        public boolean isAvailable (PlayerObject user);
    }

    /**
     * Selects a random color from the records available to the supplied player
     * in the specified class. Returns null if no colors are available to the
     * player (which should never happen unless something is booched).
     */
    public static ColorRecord pickRandomColor (
        ColorPository pository, String colorClass, PlayerObject user)
    {
        ArrayList<ColorRecord> records =
            getAvailableColors(pository, colorClass, user);
        return records.size() == 0 ? null :
            (ColorRecord)RandomUtil.pickRandom(records);
    }

    /**
     * Returns a list of color records available to the supplied player in the
     * specified color class.
     */
    public static ArrayList<ColorRecord> getAvailableColors (
        ColorPository pository, String colorClass, PlayerObject user)
    {
        ArrayList<ColorRecord> colors = new ArrayList<ColorRecord>();
        ClassRecord clrec = pository.getClassRecord(colorClass);
        if (clrec == null) {
            log.warning("Requested non-existent color class " +
                        "[class=" + colorClass + ", who=" + user.who() + "].");
            return colors;
        }

        HashMap<String,Predicate> preds = _preds.get(colorClass);
        Iterator iter = clrec.colors.values().iterator();
        while (iter.hasNext()) {
            ColorRecord crec = (ColorRecord)iter.next();
            Predicate pred = preds.get(crec.name);
            if (pred == null) {
                log.warning("Missing predicate for color [class=" + colorClass +
                            ", color=" + crec.name + "].");
                continue;
            }
            if (pred.isAvailable(user)) {
                colors.add(crec);
            }
        }
        return colors;
    }

    /** Starter colors are available to every player any time. */
    protected static class Starter implements Predicate {
        public boolean isAvailable (PlayerObject user) {
            return true;
        }
    }

    /** Normal colors are available to every player after having created their
     * first avatar look. */
    protected static class Normal implements Predicate {
        public boolean isAvailable (PlayerObject user) {
            // available to anyone that has created their initial avatar
            return (user.handle != null);
        }
    }

    /** Some colors are only available to players that hold a particular
     * badge. */
    protected static class HoldsBadge implements Predicate {
        public HoldsBadge (Badge.Type badge) {
            _badge = badge;
        }
        public boolean isAvailable (PlayerObject user) {
            // available to anyone that has created their initial avatar
            return user.holdsBadge(_badge);
        }
        protected Badge.Type _badge;
    }

    protected static HashMap<String,HashMap<String,Predicate>> _preds =
        new HashMap<String,HashMap<String,Predicate>>();

    static {
        HashMap<String,Predicate> preds;

        _preds.put("hair", preds = new HashMap<String,Predicate>());
        preds.put("red", new Starter());
        preds.put("grey", new Normal());
        preds.put("white", new Normal());
        preds.put("black", new Starter());
        preds.put("brown", new Starter());
        preds.put("lightBrown", new Starter());
        preds.put("sandyBlonde", new Starter());
        preds.put("blonde", new Starter());
        preds.put("toehead", new Starter());
        preds.put("violet", new HoldsBadge(Badge.Type.GAMES_PLAYED_2));
        preds.put("purple", new HoldsBadge(Badge.Type.CONSEC_WINS_1));
        preds.put("navyBlue", new HoldsBadge(Badge.Type.GAMES_PLAYED_3));
        preds.put("blue", new HoldsBadge(Badge.Type.UNITS_KILLED_2));
        preds.put("aqua", new Normal());
        preds.put("lime", new Normal());
        preds.put("green", new HoldsBadge(Badge.Type.SHOTS_FIRED_1));
        preds.put("orange", new HoldsBadge(Badge.Type.UNITS_KILLED_3));
        preds.put("maroon", new Normal());

        _preds.put("skin", preds = new HashMap<String,Predicate>());
        preds.put("darkest", new Starter());
        preds.put("dark", new Starter());
        preds.put("native", new Starter());
        preds.put("medium", new Starter());
        preds.put("tan", new Starter());
        preds.put("white", new Starter());
        preds.put("pale", new Starter());
        preds.put("pasty", new Starter());

        _preds.put("iris_t", preds = new HashMap<String,Predicate>());
        preds.put("blue", new Starter());
        preds.put("purple", new HoldsBadge(Badge.Type.BONUSES_COLLECTED_2));
        preds.put("violet", new HoldsBadge(Badge.Type.GAMES_PLAYED_1));
        preds.put("red", new HoldsBadge(Badge.Type.UNITS_KILLED_3));
        preds.put("brown", new Starter());
        preds.put("orange", new HoldsBadge(Badge.Type.CASH_EARNED_1));
        preds.put("beige", new Starter());
        preds.put("hazel", new Starter());
        preds.put("green", new Normal());
        preds.put("lime", new Normal());
        preds.put("sky", new Starter());

        _preds.put("makeup_p", preds = new HashMap<String,Predicate>());
        _preds.put("makeup_s", preds);
        preds.put("red", new Normal());
        preds.put("brown", new Normal());
        preds.put("white", new Normal());
        preds.put("black", new Normal());
        preds.put("grey", new Normal());
        preds.put("yellow", new Normal());
        preds.put("pink", new Normal());
        preds.put("violet", new Normal());
        preds.put("purple", new Normal());
        preds.put("navyBlue", new Normal());
        preds.put("blue", new Normal());
        preds.put("aqua", new Normal());
        preds.put("lime", new Normal());
        preds.put("green", new Normal());
        preds.put("orange", new Normal());
        preds.put("maroon", new Normal());
        preds.put("darkBrown", new Normal());
        preds.put("gold", new Normal());

        _preds.put("clothes_p", preds = new HashMap<String,Predicate>());
        _preds.put("clothes_s", preds);
        _preds.put("clothes_t", preds);
        preds.put("dkbrn", new Starter());
        preds.put("leather", new Starter());
        preds.put("brown", new Starter());
        preds.put("beige", new Starter());
        preds.put("pink", new Normal());
        preds.put("red", new Normal());
        preds.put("maroon", new Starter());
        preds.put("orange", new Normal());
        preds.put("gold", new Normal());
        preds.put("yellow", new Normal());
        preds.put("moss", new Starter());
        preds.put("lime", new Normal());
        preds.put("green", new Starter());
        preds.put("olive", new Starter());
        preds.put("aqua", new Normal());
        preds.put("blue", new Starter());
        preds.put("navyBlue", new Normal());
        preds.put("slate", new Starter());
        preds.put("violet", new Normal());
        preds.put("purple", new Normal());
        preds.put("white", new Normal());
        preds.put("grey", new Starter());
        preds.put("black", new Normal());

        _preds.put("familiar_p", preds = new HashMap<String,Predicate>());
        _preds.put("familiar_s", preds);
        _preds.put("familiar_t", preds);
    }
}
