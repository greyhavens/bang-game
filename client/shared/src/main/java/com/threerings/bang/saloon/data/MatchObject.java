//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DObject;

import com.threerings.crowd.chat.data.SpeakObject;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.avatar.data.Look;

/**
 * Contains the shared state for a pending matched game.
 */
public class MatchObject extends DObject
    implements SpeakObject
{
    /** Used to keep track of player occupant information even if they're not in the room. */
    public static class PlayerInfo implements Streamable
    {
        /** The player's unique identifier. */
        public int playerId = -1;

        /** The player's handle. */
        public Handle handle;

        /** The player's avatar pose. */
        public AvatarInfo avatar;

        public PlayerInfo ()
        {
        }

        public PlayerInfo (PlayerObject user)
        {
            this.handle = user.handle;
            Look look = user.getLook(Look.Pose.DEFAULT);
            this.avatar  = (look != null ? look.getAvatar(user) : new AvatarInfo());
        }

        public PlayerInfo (Handle handle, AvatarInfo avatar)
        {
            this.handle = handle;
            this.avatar = avatar;
        }
    }

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>playerOids</code> field. */
    public static final String PLAYER_OIDS = "playerOids";

    /** The field name of the <code>criterion</code> field. */
    public static final String CRITERION = "criterion";

    /** The field name of the <code>starting</code> field. */
    public static final String STARTING = "starting";
    // AUTO-GENERATED: FIELDS END

    /** The player object ids of the players in this game. */
    public int[] playerOids;

    /** The current criterion for matching. */
    public Criterion criterion;

    /** Set to true if this match is about to start. */
    public boolean starting;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>playerOids</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPlayerOids (int[] value)
    {
        int[] ovalue = this.playerOids;
        requestAttributeChange(
            PLAYER_OIDS, value, ovalue);
        this.playerOids = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>playerOids</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setPlayerOidsAt (int value, int index)
    {
        int ovalue = this.playerOids[index];
        requestElementUpdate(
            PLAYER_OIDS, index, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.playerOids[index] = value;
    }

    /**
     * Requests that the <code>criterion</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setCriterion (Criterion value)
    {
        Criterion ovalue = this.criterion;
        requestAttributeChange(
            CRITERION, value, ovalue);
        this.criterion = value;
    }

    /**
     * Requests that the <code>starting</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setStarting (boolean value)
    {
        boolean ovalue = this.starting;
        requestAttributeChange(
            STARTING, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.starting = value;
    }
    // AUTO-GENERATED: METHODS END

    // documentation inherited from interface SpeakObject
    public String getChatIdentifier (UserMessage message) {
        return DEFAULT_IDENTIFIER;
    }

    // documentation inherited from interface SpeakObject
    public void applyToListeners (ListenerOp op)
    {
        // nothing doing
    }
}
