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
    public interface WaitAction
    {
        public String getEvent ();

        public int getCount ();

        public boolean allowAttack ();
    }

    public static class Action implements Serializable
    {
        public int index;

        public int getCount () {
            return -1;
        }

        public boolean allowAttack () {
            return false;
        }

        public String toString () {
            return StringUtil.shortClassName(this) + StringUtil.fieldsToString(this);
        }

        private static final long serialVersionUID = 1;
    }

    public static class Text extends Action
    {
        public String message;
        public int step;

        private static final long serialVersionUID = 1;
    }

    public static class Wait extends Action
        implements WaitAction
    {
        public String event;
        public int count;
        public boolean allowAttack;

        public String getEvent () {
            return event;
        }

        public int getCount () {
            return count;
        }

        public boolean allowAttack () {
            return allowAttack;
        }

        private static final long serialVersionUID = 1;
    }

    public static class AddPiece extends Action
        implements WaitAction
    {
        public String what;
        public String type;
        public int id;
        public int[] location;
        public int owner;

        public String getEvent () {
            return (what.equals("unit") || what.equals("bigshot")) ?
                TutorialCodes.UNIT_ADDED : TutorialCodes.PIECE_ADDED;
        }

        private static final long serialVersionUID = 1;
    }

    public static class CenterOn extends Action
    {
        public String what;
        public int id;
        public boolean arrow = true;

        private static final long serialVersionUID = 1;
    }

    public static class MoveUnit extends Action
    {
        public int id;
        public int[] location = { Short.MAX_VALUE, Short.MAX_VALUE };
        public int target;

        private static final long serialVersionUID = 1;
    }

    public static class MoveUnitAndWait extends MoveUnit
        implements WaitAction
    {
        public String event;
        public boolean allowAttack;

        public String getEvent () {
            return event;
        }

        public boolean allowAttack () {
            return allowAttack;
        }

        private static final long serialVersionUID = 1;
    }

    public static class ShowView extends Action
    {
        public String name;

        private static final long serialVersionUID = 1;
    }

    public static class ScenarioAction extends Action
    {
        public String type;

        private static final long serialVersionUID = 1;
    }

    /** The identifier for this tutorial, which defines its message bundle. */
    public String ident;

    /** The name of the board to use for this tutorial. */
    public String board;

    /** The number of players in this tutorial. The first player will be the human, and all other
     * players will be computer controlled. */
    public int players;

    /** If this is a respawning tutorial. */
    public boolean respawn = true;

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
     * Returns the total number of "steps" in this tutorial. This is for display to the user.
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
        StringBuilder buf = new StringBuilder("[");
        StringUtil.fieldsToString(buf, this);
        buf.append(", actions=");
        StringUtil.toString(buf, _actions);
        return buf.append("]").toString();
    }

    /** The total number of steps in this tutorial. This is inferred to be the step number of the
     * highest numbered step. */
    protected int _steps;

    /** Contains the list of actions used in this tutorial. */
    protected ArrayList<Action> _actions = new ArrayList<Action>();

    /** Serialization and Proguard will never get along. */
    private static final long serialVersionUID = 1;
}
