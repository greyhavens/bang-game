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
    public HashSet<String> getAIPrefixes (boolean isMale)
    {
        return AI_PREF_TABLE;
    }

    @Override // documentation inherited
    public HashSet<String> getBigShotPrefixes (boolean isMale)
    {
        return BSHOT_PREF_TABLE;
    }
    
    @Override // documentation inherited
    public HashSet<String> getHandlePrefixes (boolean isMale)
    {
        return isMale ? MALE_PREF_TABLE : FEMALE_PREF_TABLE;
    }

    @Override // documentation inherited
    public HashSet<String> getHandleRoots (boolean isMale)
    {
        return isMale ? MALE_ROOT_TABLE : FEMALE_ROOT_TABLE;
    }

    @Override // documentation inherited
    public HashSet<String> getHandleSuffixes (boolean isMale)
    {
        return isMale ? MALE_SUFF_TABLE : FEMALE_SUFF_TABLE;
    }

    protected static HashSet<String> MALE_PREF_TABLE = new HashSet<String>();
    protected static HashSet<String> MALE_ROOT_TABLE = new HashSet<String>();
    protected static HashSet<String> MALE_SUFF_TABLE = new HashSet<String>();

    protected static HashSet<String> FEMALE_PREF_TABLE = new HashSet<String>();
    protected static HashSet<String> FEMALE_ROOT_TABLE = new HashSet<String>();
    protected static HashSet<String> FEMALE_SUFF_TABLE = new HashSet<String>();

    protected static HashSet<String> AI_PREF_TABLE = new HashSet<String>();
    protected static HashSet<String> BSHOT_PREF_TABLE = new HashSet<String>();
    
    protected static final String[] SHARED_PREFIXES = {
        "Anasazi",
        "Apache",
        "Aztec",
        "Bronco",
        "Buckaroo",
        "Buffalo",
        "Cajun",
        "Crazy",
        "Hohokam",
        "Honest",
        "Navaho",
        "Oklahoma",
        "Peacemaker",
        "Shotgun",
        "Texas",
        "Tombstone",
        "Tumbleweed",
        "Whistlestop",
        "Wild",
    };

    protected static final String[] MALE_PREFIXES = {
        "Cockeyed",
        "Colonel",
        "Cowboy",
        "Deadwood",
        "Dirty",
        "Doc",
        "Honkytonk",
        "Judge",
        "Little",
        "Reverend",
        "Tarantula",
        "Ten Gallon",
    };

    protected static final String[] FEMALE_PREFIXES = {
        "Bloomin",
        "Calamity",
        "Calico",
        "Contrary",
        "Little Miss",
        "Madame",
        "Sure Shot",
    };

    protected static final String[] AI_PREFIXES = {
//         "Clankin",
//         "Lugbolt",
//         "Robo",
//         "Steam",
        "Tin Can",
    };

    protected static final String[] BIG_SHOT_PREFIXES = {
        "Big",
        "Old",
        "Solid",
        "Trusty",
        "Handy",
        "Loyal",
        "Faithful",
        "Useful",
        "Timely",
    };
    
    protected static final String[] SHARED_ROOTS = {
        "Charlie",
        "Charley",
    };

    protected static final String[] MALE_ROOTS = {
        "Abe",
        "Adam",
        "Alfred",
        "Baxter",
        "Bennie",
        "Bill",
        "Bob",
        "Bubba",
        "Casey",
        "Chance",
        "Deuce",
        "Doyle",
        "George",
        "Gilbert",
        "Guy",
        "Hank",
        "Henry",
        "Holliday",
        "Hugh",
        "Jack",
        "Jesse",
        "Karl",
        "Kit",
        "Lee",
        "Lloyd",
        "Melville",
        "Paul",
        "Pete",
        "Roy",
        "Shug",
        "Tim",
        "Tommy",
        "Tuff",
        "Ty",
        "William",
        "Wyatt",
    };

    protected static final String[] FEMALE_ROOTS = {
        "Ann",
        "Anna",
        "Annie",
        "Belle",
        "Bonnie",
        "Cathay",
        "Dale",
        "Dixie",
        "Edith",
        "Elaine",
        "Fox",
        "Gabby",
        "Jackie",
        "Jane",
        "Janet",
        "Jody",
        "Josey",
        "Judy",
        "Kathleen",
        "Kay",
        "Lucille",
        "Lulu Bell",
        "Mae",
        "Manda",
        "Marie",
        "Martha",
        "Mary",
        "Mattie",
        "Melanee",
        "Molly",
        "Myra",
        "Patsy",
        "Rose",
        "Sarah",
        "Tana",
        "Tanya",
        "Wanda",
    };

    protected static final String[] SHARED_SUFFIXES = {
        "Armstrong",
        "Avery",
        "Bean",
        "Black",
        "Conway",
        "Dixon",
        "Dunn",
        "Frost",
        "Hastings",
        "James",
        "Krebs",
        "Lambert",
        "LeDoux",
        "Lippincott",
        "Maddox",
        "Montana",
        "Mulhall",
        "Murray",
        "Nolan",
        "O' Dell",
        "Parker",
        "Pierce",
        "Putney",
        "Regret",
        "Rogers",
        "Sharp",
        "Sinclair",
        "Starr",
        "Thorn",
        "Whitfield",
        "Williams",
        "the Kid",
    };

    protected static final String[] MALE_SUFFIXES = {
        "Boone",
        "Crocket",
        "Earp",
        "Hickock",
        "Skelton",
        "de Wolde",
    };

    protected static final String[] FEMALE_SUFFIXES = {
        "Canary",
        "Evans",
        "Moses",
        "Oakley",
    };

    static {
        CollectionUtil.addAll(MALE_PREF_TABLE, SHARED_PREFIXES);
        CollectionUtil.addAll(MALE_PREF_TABLE, MALE_PREFIXES);
        CollectionUtil.addAll(MALE_ROOT_TABLE, SHARED_ROOTS);
        CollectionUtil.addAll(MALE_ROOT_TABLE, MALE_ROOTS);
        CollectionUtil.addAll(MALE_SUFF_TABLE, SHARED_SUFFIXES);
        CollectionUtil.addAll(MALE_SUFF_TABLE, MALE_SUFFIXES);

        CollectionUtil.addAll(FEMALE_PREF_TABLE, SHARED_PREFIXES);
        CollectionUtil.addAll(FEMALE_PREF_TABLE, FEMALE_PREFIXES);
        CollectionUtil.addAll(FEMALE_ROOT_TABLE, SHARED_ROOTS);
        CollectionUtil.addAll(FEMALE_ROOT_TABLE, FEMALE_ROOTS);
        CollectionUtil.addAll(FEMALE_SUFF_TABLE, SHARED_SUFFIXES);
        CollectionUtil.addAll(FEMALE_SUFF_TABLE, FEMALE_SUFFIXES);

        CollectionUtil.addAll(AI_PREF_TABLE, AI_PREFIXES);
        CollectionUtil.addAll(BSHOT_PREF_TABLE, BIG_SHOT_PREFIXES);
    }
}
