//
// $Id$

package com.threerings.bang.game.data;

import java.io.Serializable;
import java.util.ArrayList;

import com.samskivert.util.StringUtil;

/**
 * Contains the configuration for an in-game tutorial.
 */
public class TutorialConfig
    implements Serializable, Cloneable
{
    public static class Action implements Serializable
    {
        public int index;

        public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    public static class Text extends Action
    {
        public String message;
    }

    public static class Wait extends Action
    {
        public String event;
    }

    public static class AddUnit extends Action
    {
        public String type;
        public int[] location;
        public int owner;
    }

    /** The identifier for this tutorial, which defines its message bundle. */
    public String ident;

    /** The name of the board to use for this tutorial. */
    public String board;

    /** The number of players in this tutorial. The first player will be the
     * human, and all other players will be computer controlled. */
    public int players;

    /**
     * Returns the next action to be fired in the tutorial, or null if it is
     * over.
     */
    public Action getNextAction ()
    {
        if (_index >= _actions.size()) {
            return null;
        }
        return _actions.get(_index++);
    }

    /** Returns an array containing the actions for this tutorial. */
    public Action getAction (int index)
    {
        return _actions.get(index);
    }

    /** Used when parsing this tutorial from an XML config file. */
    public void addAction (Action action)
    {
        action.index = _actions.size();
        _actions.add(action);
    }

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            TutorialConfig config = (TutorialConfig)super.clone();
            // nothing to deep clone at the moment
            return config;
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException("CloneNotSupportedNotSupportedException");
        }
    }

    /** Generates a string representation of this instance. */
    public String toString ()
    {
        StringBuffer buf = new StringBuffer("[");
        StringUtil.fieldsToString(buf, this);
        buf.append(", actions=");
        StringUtil.toString(buf, _actions);
        return buf.append("]").toString();
    }

    /** Contains the list of actions used in this tutorial. */
    protected ArrayList<Action> _actions = new ArrayList<Action>();

    /** The index of the action we're currently acting on. */
    protected int _index;
}
