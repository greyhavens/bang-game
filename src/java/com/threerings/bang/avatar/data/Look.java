//
// $Id$

package com.threerings.bang.avatar.data;

import java.util.Arrays;

import com.samskivert.util.ArrayIntSet;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;

import com.threerings.bang.avatar.util.AvatarLogic;

import static com.threerings.bang.Log.log;

/**
 * Defines a particular "look" for a player's avatar.
 */
public class Look extends SimpleStreamableObject
    implements DSet.Entry, Cloneable
{
    /** Used to assign different looks for use in different circumstances. */
    public static enum Pose {
        /** Shown in player status view, when match making, during game, etc. */
        DEFAULT("LOOK"),

        /** Shown on a player's wanted poster and on top rank lists. Note that
         * a snapshot of this look is maintained in a special table so that it
         * can be easily shown when the player is offline. */
        WANTED_POSTER("WANTED_LOOK"),

        /** Shown in the end of game view if player is in first place. */
        VICTORY("VICTORY_LOOK");

        public String getColumnName () {
            return _columnName;
        }

        Pose (String columnName) {
            _columnName = columnName;
        }

        protected String _columnName;
    };

    /** The maximum length of a look's name. */
    public static final int MAX_NAME_LENGTH = 24;

    /** The number of different poses. */
    public static int POSE_COUNT = Pose.values().length;

    /** The name of this look (provided by the player). */
    public String name;

    /** The immutable avatar aspects associated with this look (character
     * component ids). These are not in any particular order. */
    public int[] aspects;

    /** An array of item ids of the various articles used in this look (the
     * order of the array matches {@link AvatarLogic#SLOTS} with unused slots
     * containing zero). */
    public int[] articles;

    /** Used to note and later flush modified looks on the server. */
    public transient boolean modified;

    /**
     * Combines the aspect and article information into a full avatar fingerprint.
     */
    public AvatarInfo getAvatar (PlayerObject player)
    {
        return getAvatar(player.who(), player.inventory);
    }

    /**
     * Combines the aspect and article information into a full avatar fingerprint.
     */
    public AvatarInfo getAvatar (String who, DSet<Item> inventory)
    {
        ArrayIntSet compids = new ArrayIntSet();

        // add the various aspects (don't add the global colorizations)
        for (int ii = 1; ii < aspects.length; ii++) {
            compids.add(aspects[ii]);
        }

        // decode and add the various articles
        int acount = (articles == null) ? 0 : articles.length;
        for (int ii = 0; ii < acount; ii++) {
            if (articles[ii] == 0) {
                continue;
            }
            Object item = inventory.get(articles[ii]);
            if (!(item instanceof Article)) {
                log.warning("Invalid article referenced in look", "who", who,
                            "look", this, "idx", ii, "item", item);
                continue;
            }
            compids.add(((Article)item).getComponents());
        }

        int[] avatar = new int[compids.size()+1];
        avatar[0] = aspects[0];
        compids.toIntArray(avatar, 1);
        return new AvatarInfo(avatar);
    }

    /**
     * Configures the supplied article in the appropriate slot for this look.
     */
    public void setArticle (Article article)
    {
        String slot = article.getSlot();
        int idx = -1;
        for (int ii = 0; ii < AvatarLogic.SLOTS.length; ii++) {
            if (AvatarLogic.SLOTS[ii].name.equals(slot)) {
                idx = ii;
                break;
            }
        }
        if (idx == -1) {
            log.warning("Requested to configure invalid article in look", "look", this,
                        "article", article);
            return;
        }

        // gracefully deal with old article arrays in case we add new slots
        if (articles.length <= idx) {
            int[] narticles = new int[AvatarLogic.SLOTS.length];
            System.arraycopy(articles, 0, narticles, 0, articles.length);
            articles = narticles;
        }

        articles[idx] = article.getItemId();
    }

    /**
     * Returns true if the specified aspect component is used in this look.
     */
    public boolean containsAspect (int componentId)
    {
        for (int ii = 0; ii < aspects.length; ii++) {
            // mask off the colorizations when comparing
            if ((aspects[ii] & 0xFFFF) == componentId) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        if (other == null || !other.getClass().equals(getClass())) {
            return false;
        }
        Look olook = (Look)other;
        return name.equals(olook.name) &&
            Arrays.equals(aspects, olook.aspects) &&
            Arrays.equals(articles, olook.articles);
    }

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            Look look = (Look)super.clone();
            look.aspects = look.aspects.clone();
            look.articles = look.articles.clone();
            return look;
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }

    // documentation inherited from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return name;
    }
}
