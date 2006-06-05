//
// $Id$

package com.threerings.bang.avatar.util;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.samskivert.util.StringUtil;

import static com.threerings.bang.Log.log;

/**
 * Contains metadata on the various avatar articles.
 */
public class ArticleCatalog
    implements Serializable
{
    /** The path (relative to the resource directory) at which the catalog
     * should be loaded and stored. */
    public static final String CONFIG_PATH = "avatars/articles.dat";

    /** Contains all {@link Component} records for a particular article. */
    public static class Article implements Serializable
    {
        /** The name of this article. */
        public String name;

        /** The town in which this article is available. */
        public String townId;

        /** The avatar slot into which this article "fits". */
        public String slot;

        /** The cost of this article in scrip. */
        public int scrip;

        /** The cost of this article in coins. */
        public int coins;

        /** Used to override the "natural" set of colorization classes used for
         * a component (which is the union of the classes needed by its
         * subcomponents). */
        public String[] colors;

        /** Whether or not this is a starter clothing article. (There should
         * only be one starter for each gender.) */
        public boolean starter;

        /** The list of components that make up this article. */
        public ArrayList<Component> components = new ArrayList<Component>();

        /** Used when parsing constraints definitions. */
        public void addComponent (Component comp)
        {
            components.add(comp);
        }

        /** Generates a string representation of this instance. */
        public String toString ()
        {
            return StringUtil.fieldsToString(this);
        }

        /** Increase this value when object's serialized state is impacted by a
         * class change (modification of fields, inheritance). */
        private static final long serialVersionUID = 1;
    }

    /** Contains metadata on a particular article component. */
    public static class Component implements Serializable
    {
        /** The class of this component. */
        public String cclass;

        /** The name of this component. */
        public String name;

        /** Generates a string representation of this instance. */
        public String toString ()
        {
            return StringUtil.fieldsToString(this);
        }

        /** Increase this value when object's serialized state is impacted by a
         * class change (modification of fields, inheritance). */
        private static final long serialVersionUID = 1;
    }

    /**
     * Returns a collection containing all registered articles.
     */
    public Collection<Article> getArticles ()
    {
        return _articles.values();
    }

    /**
     * Returns the article with the specified name or null if no such article
     * exists.
     */
    public Article getArticle (String article)
    {
        article = article.substring(article.lastIndexOf("/")+1);
        return _articles.get(article);
    }

    /**
     * Adds a parsed article to this catalog. This is used when parsing from
     * the XML definition.
     */
    public void addArticle (Article article)
    {
        _articles.put(article.name, article);
    }

    /** A mapping of all known articles by name. */
    protected HashMap<String,Article> _articles = new HashMap<String,Article>();

    /** Increase this value when object's serialized state is impacted by a
     * class change (modification of fields, inheritance). */
    private static final long serialVersionUID = 1;
}
