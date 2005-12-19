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

    /** The name of the board to use for this tutorial. */
    public String board;

    /** The number of players in this tutorial. The first player will be the
     * human, and all other players will be computer controlled. */
    public int players;

    /** Used when parsing this tutorial from an XML config file. */
    public void addAction (Action action)
    {
        _actions.add(action);
    }

    /** Returns an array containing the actions for this tutorial. */
    public Action[] getActions ()
    {
        return _actions.toArray(new Action[_actions.size()]);
    }

    /** Contains the list of actions used in this tutorial. */
    protected ArrayList<Action> _actions = new ArrayList<Action>();
}
