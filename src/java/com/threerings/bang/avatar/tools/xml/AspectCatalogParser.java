//
// $Id: AspectCatalogParser.java 16390 2004-07-13 16:54:11Z mdb $

package com.threerings.bang.avatar.tools.xml;

import java.io.Serializable;

import org.apache.commons.digester.Digester;
import com.samskivert.xml.SetPropertyFieldsRule;

import com.threerings.tools.xml.CompiledConfigParser;

import com.threerings.bang.avatar.util.AspectCatalog.Aspect;
import com.threerings.bang.avatar.util.AspectCatalog.AspectClass;
import com.threerings.bang.avatar.util.AspectCatalog;

/**
 * Parses the aspect catalog XML file.
 */
public class AspectCatalogParser extends CompiledConfigParser
{
    // documentation inherited
    protected Serializable createConfigObject ()
    {
        return new AspectCatalog();
    }

    // documentation inherited
    protected void addRules (Digester digest)
    {
        // configure the top-level catalog object
        String prefix = "aspects";
        digest.addRule(prefix, new SetPropertyFieldsRule());

        // create and configure class record instances
        prefix += "/class";
        digest.addObjectCreate(prefix, AspectClass.class.getName());
        digest.addRule(prefix, new SetPropertyFieldsRule());
        digest.addSetNext(prefix, "addClass", AspectClass.class.getName());

        // create and configure aspect instances
        prefix += "/aspect";
        digest.addObjectCreate(prefix, Aspect.class.getName());
        digest.addRule(prefix, new SetPropertyFieldsRule());
        digest.addSetNext(prefix, "addAspect", Aspect.class.getName());
    }
}
