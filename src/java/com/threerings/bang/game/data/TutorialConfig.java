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
    implements Serializable
{
    public static class Action implements Serializable
    {
        public int index;

        public String toString () {
            return getClass().getName() + StringUtil.fieldsToString(this);
        }
    }

    public static class Text extends Action
    {
        public String message;
        public int step;
    }

    public static class Wait extends Action
    {
        public String event;
    }

    public static class AddUnit extends Action
    {
        public String type;
        public int id;
        public int[] location;
        public int owner;
    }

    public static class CenterOnUnit extends Action
    {
        public int id;
    }

    public static class MoveUnit extends Action
    {
        public int id;
        public int[] location = { Short.MAX_VALUE, Short.MAX_VALUE };
        public int target;
    }

    public static class AddBonus extends Action
    {
        public String type;
        public int[] location;
    }

    public static class ShowView extends Action
    {
        public String name;
    }
    
    /** The identifier for this tutorial, which defines its message bundle. */
    public String ident;

    /** The name of the board to use for this tutorial. */
    public String board;

    /** The number of players in this tutorial. The first player will be the
     * human, and all other players will be computer controlled. */
    public int players;

    /** Returns an array containing the actions for this tutorial. */
    public Action getAction (int index)
    {
        return _actions.get(index);
    }

    /**
     * Returns the total count of actions in this tutorial.
     */
    public int getActionCount ()
    {
        return _actions.size();
    }

    /**
     * Returns the total number of "steps" in this tutorial. This is for
     * display to the user.
     */
    public int getSteps ()
    {
        return _steps;
    }

    /** Used when parsing this tutorial from an XML config file. */
    public void addAction (Action action)
    {
        action.index = _actions.size();
        _actions.add(action);
        if (action instanceof Text) {
            _steps = Math.max(_steps, ((Text)action).step);
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

    /** The total number of steps in this tutorial. This is inferred to be the
     * step number of the highest numbered step. */
    protected int _steps;

    /** Contains the list of actions used in this tutorial. */
    protected ArrayList<Action> _actions = new ArrayList<Action>();
}
