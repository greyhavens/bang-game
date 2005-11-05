//
// $Id$

package com.threerings.bang.util;

import java.util.HashSet;

import com.samskivert.util.CollectionUtil;

/**
 * Handles the creation of names for the English language.
 */
public class EnglishNameCreator extends NameCreator
{
    @Override // documentation inherited
    public HashSet<String> getHandlePrefixes (boolean isMale)
    {
        return isMale ? MALE_PREFIXES : FEM_PREFIXES;
    }

    @Override // documentation inherited
    public HashSet<String> getHandleRoots (boolean isMale)
    {
        return isMale ? MALE_ROOTS : FEM_ROOTS;
    }

    @Override // documentation inherited
    public HashSet<String> getHandleSuffixes (boolean isMale)
    {
        return isMale ? MALE_SUFFIXES : FEM_SUFFIXES;
    }

    protected static final HashSet<String> MALE_PREFIXES = new HashSet<String>();
    protected static final HashSet<String> MALE_ROOTS = new HashSet<String>();
    protected static final HashSet<String> MALE_SUFFIXES = new HashSet<String>();

    protected static final HashSet<String> FEM_PREFIXES = new HashSet<String>();
    protected static final HashSet<String> FEM_ROOTS = new HashSet<String>();
    protected static final HashSet<String> FEM_SUFFIXES = new HashSet<String>();

    static {
        CollectionUtil.addAll(MALE_PREFIXES, new String[] {
            "Wild",
            "Dirty",
            "Honest",
            "Cockeyed",
            "Buffalo",
            "Oklahoma",
            "Judge",
            "Doc",
        });
        CollectionUtil.addAll(MALE_ROOTS, new String[] {
            "Abe",
            "Bill",
            "William",
            "Henry",
            "Hank",
            "Paul",
            "Pete",
            "Wyatt",
        });
        CollectionUtil.addAll(MALE_SUFFIXES, new String[] {
            "the Kid",
            "Regret",
            "Thorn",
        });

        CollectionUtil.addAll(FEM_PREFIXES, new String[] {
            "Calamity",
            "Wild",
            "Madame",
            "Oklahoma",
        });
        CollectionUtil.addAll(FEM_ROOTS, new String[] {
            "Annie",
            "Mattie",
            "Jane",
            "Mary",
        });
        CollectionUtil.addAll(FEM_SUFFIXES, new String[] {
            "Oakley",
            "Moses",
            "Canary",
        });
    }
}
