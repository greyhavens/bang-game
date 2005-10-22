//
// $Id: ArticleCatalogParser.java 16390 2004-07-13 16:54:11Z mdb $

package com.threerings.bang.avatar.tools.xml;

import java.io.Serializable;

import org.apache.commons.digester.Digester;
import com.samskivert.xml.SetPropertyFieldsRule;

import com.threerings.tools.xml.CompiledConfigParser;

import com.threerings.bang.avatar.util.ArticleCatalog.Article;
import com.threerings.bang.avatar.util.ArticleCatalog.Component;
import com.threerings.bang.avatar.util.ArticleCatalog;

/**
 * Parses the article catalog XML file.
 */
public class ArticleCatalogParser extends CompiledConfigParser
{
    // documentation inherited
    protected Serializable createConfigObject ()
    {
        return new ArticleCatalog();
    }

    // documentation inherited
    protected void addRules (Digester digest)
    {
        // configure the top-level catalog object
        String prefix = "articles";
        digest.addRule(prefix, new SetPropertyFieldsRule());

        // create and configure class record instances
        prefix += "/article";
        digest.addObjectCreate(prefix, Article.class.getName());
        digest.addRule(prefix, new SetPropertyFieldsRule());
        digest.addSetNext(prefix, "addArticle", Article.class.getName());

        // create and configure article instances
        prefix += "/component";
        digest.addObjectCreate(prefix, Component.class.getName());
        digest.addRule(prefix, new SetPropertyFieldsRule());
        digest.addSetNext(prefix, "addComponent", Component.class.getName());
    }
}
