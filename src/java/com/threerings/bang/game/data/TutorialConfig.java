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
    /** an impossible location to attack from. */
    public static final int[] NO_ATTACK = new int[] {0, 0};

    public interface WaitAction
    {
        public String getEvent ();

        public int getCount ();

        public int[] allowAttack ();

        public int getId ();
    }

    public static class Action implements Serializable
    {
        public int index;

        public int getCount () {
            return -1;
        }

        public int[] allowAttack () {
            return NO_ATTACK;
        }

        public String toString () {
            return StringUtil.shortClassName(this) + StringUtil.fieldsToString(this);
        }

        private static final long serialVersionUID = 1;
    }

    public static class Text extends Action
    {
        public String message;
        public String avatar;
        public int step;

        private static final long serialVersionUID = 1;
    }

    public static class Wait extends Action
        implements WaitAction
    {
        public String event;
        public int id = -1;
        public int count;
        public int[] allowAttack = NO_ATTACK;

        public String getEvent () {
            return event;
        }

        public int getCount () {
            return count;
        }

        public int[] allowAttack () {
            return allowAttack;
        }

        public int getId () {
            return id;
        }

        private static final long serialVersionUID = 1;
    }

    public static class WaitHolding extends Wait
    {
        public String holding;
        public int holderId;

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

        public int getId () {
            return id;
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
        public int[] targetLoc = { Short.MAX_VALUE, Short.MAX_VALUE };
        public boolean noWarning;

        private static final long serialVersionUID = 1;
    }

    public static class MoveUnitAndWait extends MoveUnit
        implements WaitAction
    {
        public String event;
        public int[] allowAttack = NO_ATTACK;

        public String getEvent () {
            return event;
        }

        public int[] allowAttack () {
            return allowAttack;
        }

        public int getId () {
            return -1;
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

    public static class SetCard extends Action
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

    /** The card reward at the completion of the tutorial. */
    public String card;

    /** The scrip reward at the completion of the tutorial. */
    public int scrip = 130;

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
