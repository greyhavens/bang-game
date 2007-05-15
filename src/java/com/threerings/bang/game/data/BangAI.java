//
// $Id$

package com.threerings.bang.game.data;

import java.util.HashSet;

import com.samskivert.util.RandomUtil;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.NameFactory;
import com.threerings.bang.data.BuckleInfo;

/**
 * Contains configuration parameters for our AIs.
 */
public class BangAI extends GameAI
{
    /** Used to pick an appropriate avatar and name for this AI. */
    public boolean isMale;

    /** The avatar for this AI. */
    public AvatarInfo avatar;

    /** This AI's name. */
    public Handle handle;

    /** The buckle for this AI. */
    public BuckleInfo buckle;

    /** The gang name. */
    public Handle gang;

    /** A blank constructor for serialization. */
    public BangAI ()
    {
    }

    /**
     * Creates a new AI for a Bang game.
     */
    public static BangAI createAI (int personality, int skill, HashSet<String> usedNames)
    {
        // create an AI instance and configure it
        BangAI ai = new BangAI();
        ai.personality = personality;
        ai.skill = skill;
        ai.isMale = (RandomUtil.getInt(100) > 49);

        // first pick a name for this AI
        HashSet<String> prefs = NameFactory.getCreator().getAIPrefixes(ai.isMale);
        HashSet<String> roots = NameFactory.getCreator().getHandleRoots(ai.isMale);
        for (int ii = 0; ii < 25; ii++) {
            String name = RandomUtil.pickRandom(prefs) + " " + RandomUtil.pickRandom(roots);
            if (!usedNames.contains(name)) {
                usedNames.add(name);
                ai.handle = new Handle(name);
            }
        }
        if (ai.handle == null) { // last ditch fallback
            ai.handle = new Handle("HAL" + RandomUtil.getInt(1000));
        }

        // pick a random avatar finger print
        ai.avatar = getAvatar(ai.isMale);

        // pick a gang name
        HashSet<String> gangs = NameFactory.getCreator().getAIGangs();
        ai.gang = new Handle(RandomUtil.pickRandom(gangs));
        ai.buckle = new BuckleInfo(AI_BUCKLE);

        return ai;
    }

    /**
     * Returns a random AI avatar fingerprint.
     */
    public static AvatarInfo getAvatar (boolean isMale)
    {
        return new AvatarInfo(RandomUtil.pickRandom(AVATAR_PRINTS[isMale ? 0 : 1]));
    }

    protected static final int[][][] AVATAR_PRINTS = {
        {
         // willy
         { 196, 6947325 },
         { 196, 25297405 },
         { 196, 38207997 },
         { 196, 23724541 },
         { 196, 6685181 },
         { 196, 5112317 },
         { 196, 34079229 },
         { 196, 38994429 },
         { 196, 7602685 },
         { 196, 15925757 },
         { 196, 5571069 },
         // pete
         { 196, 197120 },
         { 196, 131584 },
         { 196, 459264 },
         { 196, 721408 },
         { 196, 786944 },
         { 196, 918016 },
         { 196, 1049088 },
         { 196, 1180160 },
         { 196, 1311232 },
         { 196, 1245696 },
         { 196, 655872 },
        },
        {
         // clementine
         { 196, 19071489 },
         { 196, 33686017 },
         { 196, 29819393 },
         { 196, 38470145 },
         { 196, 23855617 },
         { 196, 42861057 },
         { 196, 17826305 },
         { 196, 45220353 },
         { 196, 20185601 },
         { 196, 5440001 },
         { 196, 25821697 },
        },
        /*
        {{ 196, 14, 28, 72, 79, 231, 276168922, 335544550 },
         { 167, 14, 16, 18, 27, 38, 316014810, 536871142 },
         { 260, 14, 18, 26, 74, 231, 314769626, 469762275 },
         { 293, 14, 16, 27, 79, 231, 303431898, 469762278 },
         { 167, 14, 18, 27, 38, 74, 67109091, 210567386 },
         { 134, 14, 27, 74, 79, 231, 67109094, 919601370 },
         { 194, 14, 16, 26, 38, 247, 247660762, 335544550 },
         { 163, 14, 28, 72, 79, 231, 469762275, 1510998234 },
         { 133, 14, 16, 28, 231, 247, 335544550, 907018458 },
         { 290, 14, 16, 28, 231, 247, 296747226, 536871142 },
         { 294, 14, 27, 38, 72, 247, 235733210, 536871142 },
         { 164, 14, 28, 72, 231, 247, 277020890, 335544550 },
         { 290, 14, 27, 74, 79, 231, 67109091, 247726298 },
         { 129, 14, 18, 27, 38, 72, 67109091, 228851930 },
         { 262, 14, 28, 38, 72, 79, 67109091, 316014810 },
         { 132, 14, 18, 28, 74, 231, 274923738, 335544547 },
         { 259, 14, 18, 28, 38, 74, 536871139, 1503854810 },
         { 196, 14, 16, 26, 38, 79, 315424986, 469762278 },
         { 161, 14, 18, 27, 38, 72, 67109091, 230031578 },
         { 263, 14, 26, 38, 72, 247, 67109091, 297140442 },
        },
        {{ 225, 85, 97, 104, 122, 205, 209, 469762187, 1102053456 },
         { 231, 97, 104, 113, 119, 122, 187, 235143248, 536871051 },
         { 229, 112, 119, 124, 188, 191, 194, 67109004, 1121321040 },
         { 196, 97, 104, 119, 124, 186, 207, 67109054, 297140304 },
         { 193, 97, 104, 119, 124, 188, 204, 276168784, 469762189 },
         { 195, 85, 124, 192, 195, 207, 209, 536871049, 1486225488 },
         { 164, 83, 112, 119, 122, 191, 194, 536871051, 879755344 },
         { 229, 97, 104, 119, 122, 187, 207, 536871053, 900726864 },
         { 291, 85, 99, 107, 119, 124, 205, 536871003, 1108738128 },
         { 198, 119, 124, 187, 192, 195, 206, 67109004, 303431760 },
         { 195, 83, 119, 122, 191, 194, 204, 211157072, 469762190 },
         { 161, 83, 97, 104, 122, 206, 209, 469762190, 900726864 },
         { 129, 87, 99, 107, 124, 205, 209, 229441616, 536871102 },
         { 132, 87, 113, 122, 192, 195, 209, 295895120, 469762185 },
         { 161, 97, 104, 122, 188, 207, 209, 67109005, 275578960 },
         { 263, 87, 119, 124, 192, 195, 207, 67109004, 881655888 },
         { 166, 99, 107, 113, 119, 124, 188, 296747088, 536871049 },
         { 229, 83, 119, 124, 151, 191, 194, 236322896, 536871054 },
         { 134, 99, 107, 112, 119, 122, 187, 275578960, 469762238 },
         { 166, 85, 97, 104, 124, 205, 209, 67108955, 302841936 },
        }*/
    };

    protected static final String AI_BUCKLE = "ui/status/tin_cans_buckle.png";
}
