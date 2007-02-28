//
// $Id: AspectCatalogParser.java 16390 2004-07-13 16:54:11Z mdb $

package com.threerings.bang.avatar.tools.xml;

import java.io.Serializable;

import org.apache.commons.digester.Digester;
import com.samskivert.xml.SetPropertyFieldsRule;

import com.threerings.tools.xml.CompiledConfigParser;

import com.threerings.bang.avatar.util.BucklePartCatalog.Part;
import com.threerings.bang.avatar.util.BucklePartCatalog.PartClass;
import com.threerings.bang.avatar.util.BucklePartCatalog;

/**
 * Parses the buckle part catalog XML file.
 */
public class BucklePartCatalogParser extends CompiledConfigParser
{
    // documentation inherited
    protected Serializable createConfigObject ()
    {
        return new BucklePartCatalog();
    }

    // documentation inherited
    protected void addRules (Digester digest)
    {
        // configure the top-level catalog object
        String prefix = "parts";
        digest.addRule(prefix, new SetPropertyFieldsRule());

        // create and configure class record instances
        prefix += "/class";
        digest.addObjectCreate(prefix, PartClass.class.getName());
        digest.addRule(prefix, new SetPropertyFieldsRule());
        digest.addSetNext(prefix, "addClass", PartClass.class.getName());

        // create and configure part instances
        prefix += "/part";
        digest.addObjectCreate(prefix, Part.class.getName());
        digest.addRule(prefix, new SetPropertyFieldsRule());
        digest.addSetNext(prefix, "addPart", Part.class.getName());
    }
}
