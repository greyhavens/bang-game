//
// $Id$

package com.threerings.bang.game.tools.xml;

import java.io.Serializable;
import org.apache.commons.digester.Digester;

import com.samskivert.xml.SetPropertyFieldsRule;
import com.threerings.tools.xml.CompiledConfigParser;

import com.threerings.bang.game.data.TutorialConfig;

/**
 * Parses a {@link TutorialConfig} from its XML definition.
 */
public class TutorialConfigParser extends CompiledConfigParser
{
    // documentation inherited
    protected Serializable createConfigObject ()
    {
        return new TutorialConfig();
    }

    // documentation inherited
    protected void addRules (Digester digest)
    {
        // create and configure class record instances
        String prefix = "tutorial";
        digest.addRule(prefix, new SetPropertyFieldsRule());

        String aprefix = prefix + "/add_unit";
        digest.addObjectCreate(aprefix, TutorialConfig.AddUnit.class.getName());
        digest.addRule(aprefix, new SetPropertyFieldsRule());
        digest.addSetNext(
            aprefix, "addAction", TutorialConfig.Action.class.getName());

        aprefix = prefix + "/wait";
        digest.addObjectCreate(aprefix, TutorialConfig.Wait.class.getName());
        digest.addRule(aprefix, new SetPropertyFieldsRule());
        digest.addSetNext(
            aprefix, "addAction", TutorialConfig.Action.class.getName());

        aprefix = prefix + "/text";
        digest.addObjectCreate(aprefix, TutorialConfig.Text.class.getName());
        digest.addRule(aprefix, new SetPropertyFieldsRule());
        digest.addSetNext(
            aprefix, "addAction", TutorialConfig.Action.class.getName());
    }
}
