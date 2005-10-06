//
// $Id: ComponentCatalogParser.java 16390 2004-07-13 16:54:11Z mdb $

package com.threerings.bang.avatar.tools.xml;

import java.io.Serializable;

import org.apache.commons.digester.Digester;
import com.samskivert.xml.SetPropertyFieldsRule;

import com.threerings.tools.xml.CompiledConfigParser;

import com.threerings.bang.avatar.util.ComponentCatalog.Component;
import com.threerings.bang.avatar.util.ComponentCatalog.ComponentClass;
import com.threerings.bang.avatar.util.ComponentCatalog;

/**
 * Parses the component catalog XML file.
 */
public class ComponentCatalogParser extends CompiledConfigParser
{
    // documentation inherited
    protected Serializable createConfigObject ()
    {
        return new ComponentCatalog();
    }

    // documentation inherited
    protected void addRules (Digester digest)
    {
        // configure the top-level catalog object
        String prefix = "components";
        digest.addRule(prefix, new SetPropertyFieldsRule());

        // create and configure class record instances
        prefix += "/class";
        digest.addObjectCreate(prefix, ComponentClass.class.getName());
        digest.addRule(prefix, new SetPropertyFieldsRule());
        digest.addSetNext(prefix, "addClass", ComponentClass.class.getName());

        // create and configure component instances
        prefix += "/component";
        digest.addObjectCreate(prefix, Component.class.getName());
        digest.addRule(prefix, new SetPropertyFieldsRule());
        digest.addSetNext(prefix, "addComponent", Component.class.getName());
    }
}
