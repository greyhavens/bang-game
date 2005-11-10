//
// $Id$

package com.threerings.bang.client;

/**
 * Contains all runtime tweakable parameters. These parameters are wired
 * up to an in-game editor to allow their modification at runtime for
 * development and tuning.
 */
public class Config
{
    /** Parameters relating to the display and user interface. */
    public static class Display
    {
        /** Controls the overall speed of the game animations. */
        public float animationSpeed = 1f;

        /** The speed (in tiles per second) of unit movement. */
        public float movementSpeed = 4f;

        /** Whether or not move highlights float above pieces. */
        public boolean floatHighlights = true;

        /** Returns the unit movement speed modulated by the total
         * animation speed. */
        public float getMovementSpeed ()
        {
            return movementSpeed * animationSpeed;
        }
    }

    /** Contains display configuration parameters. */
    public static Display display = new Display();
}
