//
// $Id$

package com.threerings.bang.game.data;

import java.util.HashSet;

import com.threerings.util.RandomUtil;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.data.Handle;
import com.threerings.bang.util.NameFactory;

/**
 * Contains configuration parameters for our AIs.
 */
public class BangAI extends GameAI
{
    /** Used to pick an appropriate avatar and name for this AI. */
    public boolean isMale;

    /** The avatar for this AI. */
    public int[] avatar;

    /** This AI's name. */
    public Handle handle;

    /** A blank constructor for serialization. */
    public BangAI ()
    {
    }

    /**
     * Creates a new AI for a Bang game.
     */
    public static BangAI createAI (int personality, int skill,
                                   HashSet<String> usedNames)
    {
        // create an AI instance and configure it
        BangAI ai = new BangAI();
        ai.personality = personality;
        ai.skill = skill;
        ai.isMale = (RandomUtil.getInt(100) > 49);

        // first pick a name for this AI
        HashSet<String> prefs =
            NameFactory.getCreator().getHandlePrefixes(ai.isMale);
        HashSet<String> roots =
            NameFactory.getCreator().getHandleRoots(ai.isMale);
        HashSet<String> suffs =
            NameFactory.getCreator().getHandleSuffixes(ai.isMale);
        for (int ii = 0; ii < 25; ii++) {
            String name;
            if (RandomUtil.getInt(100) > 49) {
                name = (String)RandomUtil.pickRandom(prefs) + " "
                    + (String)RandomUtil.pickRandom(roots);
            } else {
                name = (String)RandomUtil.pickRandom(roots) + " "
                    + (String)RandomUtil.pickRandom(suffs);
            }
            if (!usedNames.contains(name)) {
                usedNames.add(name);
                ai.handle = new Handle(name);
            }
        }
        if (ai.handle == null) { // last ditch fallback
            ai.handle = new Handle("HAL" + RandomUtil.getInt(1000));
        }

        // pick a random avatar finger print
        ai.avatar = (int[])RandomUtil.pickRandom(
            AVATAR_PRINTS[ai.isMale ? 0 : 1]);

        return ai;
    }

    protected static final int[][][] AVATAR_PRINTS = {
        {{ 231, 105, 123, 142, 152, 161, 248905823, 469762161 }, // male
         { 225, 106, 123, 142, 150, 159, 248905823, 536871021 },
         { 257, 106, 117, 142, 147, 159, 248905823, 536871021 },
         { 133, 105, 123, 142, 150, 159, 248905823, 536871021 },
         { 167, 106, 117, 142, 152, 161, 248905823, 335544429 },
         { 129, 106, 123, 142, 150, 161, 248905823, 335544429 },
         { 197, 107, 123, 142, 150, 159, 248905823, 469762161 },
         { 293, 106, 123, 142, 150, 159, 67108977, 248905823 },
         { 231, 106, 117, 142, 150, 161, 248905823, 335544429 },
         { 165, 105, 123, 142, 152, 159, 67108977, 248905823 },
         { 194, 106, 117, 142, 150, 159, 67108973, 248905823 },
         { 161, 105, 123, 142, 150, 161, 67108973, 248905823 },
         { 198, 107, 123, 142, 147, 160, 248905823, 335544429 },
         { 289, 105, 123, 142, 150, 160, 67108973, 248905823 },
         { 228, 107, 117, 142, 147, 160, 248905823, 469762161 },
         { 261, 105, 123, 142, 152, 160, 248905823, 335544429 },
         { 295, 105, 123, 142, 150, 161, 248905823, 335544429 },
         { 196, 105, 117, 142, 147, 159, 248905823, 335544429 },
         { 230, 106, 117, 142, 150, 160, 248905823, 469762161 },
         { 257, 106, 123, 142, 150, 160, 248905823, 335544433 },
        },
        {{ 198, 19, 36, 42, 71, 78, 82, 469762074, 918749188 }, // female
         { 129, 15, 35, 40, 68, 78, 81, 67108890, 918749188 },
         { 261, 19, 38, 45, 67, 78, 81, 335544347, 918749188 },
         { 225, 18, 35, 40, 68, 78, 82, 67108890, 918749188 },
         { 289, 19, 34, 39, 71, 78, 81, 335544346, 918749188 },
         { 289, 15, 38, 45, 68, 77, 81, 536870937, 918749188 },
         { 194, 15, 35, 40, 70, 78, 82, 536870943, 918749188 },
         { 292, 17, 36, 42, 71, 77, 82, 67108897, 918749188 },
         { 133, 24, 38, 45, 68, 77, 82, 469762073, 918749188 },
         { 166, 17, 36, 42, 66, 78, 82, 536870938, 918749188 },
         { 133, 17, 34, 39, 67, 77, 81, 469762081, 918749188 },
         { 130, 20, 36, 42, 66, 77, 82, 469762073, 918749188 },
         { 225, 14, 38, 45, 65, 77, 81, 67108897, 918749188 },
         { 261, 24, 35, 40, 70, 77, 81, 67108893, 918749188 },
         { 229, 14, 34, 39, 71, 78, 81, 469762074, 918749188 },
         { 165, 15, 34, 39, 70, 77, 82, 67108894, 918749188 },
         { 134, 19, 35, 40, 69, 78, 81, 469762075, 918749188 },
         { 226, 15, 35, 40, 67, 78, 81, 536870939, 918749188 },
         { 229, 15, 36, 42, 66, 77, 82, 335544348, 918749188 },
         { 133, 17, 36, 42, 68, 78, 81, 469762079, 918749188 },
        }
    };
}
