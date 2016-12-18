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
    public HashSet<String> getAIGangs ()
    {
        return AI_GANG_TABLE;
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

    @Override // documentation inherited
    public HashSet<String> getHandleFamily ()
    {
        return FAMILY_TABLE;
    }

    @Override // documentation inherited
    public HashSet<String> getHandleInitials ()
    {
        return INITIALS_TABLE;
    }

    @Override // documentation inherited
    public HashSet<String> getGangSuffixes ()
    {
        return GANG_SUFF_TABLE;
    }

    @Override // documentation inherited
    public HashSet<String> getGangNames ()
    {
        return GANG_TABLE;
    }

    protected static HashSet<String> MALE_PREF_TABLE = new HashSet<String>();
    protected static HashSet<String> MALE_ROOT_TABLE = new HashSet<String>();
    protected static HashSet<String> MALE_SUFF_TABLE = new HashSet<String>();

    protected static HashSet<String> FEMALE_PREF_TABLE = new HashSet<String>();
    protected static HashSet<String> FEMALE_ROOT_TABLE = new HashSet<String>();
    protected static HashSet<String> FEMALE_SUFF_TABLE = new HashSet<String>();

    protected static HashSet<String> FAMILY_TABLE = new HashSet<String>();
    protected static HashSet<String> INITIALS_TABLE = new HashSet<String>();

    protected static HashSet<String> AI_PREF_TABLE = new HashSet<String>();
    protected static HashSet<String> BSHOT_PREF_TABLE = new HashSet<String>();

    protected static HashSet<String> GANG_SUFF_TABLE = new HashSet<String>();
    protected static HashSet<String> GANG_TABLE = new HashSet<String>();
    protected static HashSet<String> AI_GANG_TABLE = new HashSet<String>();

    protected static final String[] SHARED_PREFIXES = {
        "Anasazi",
        "Apache",
        "Arapaho",
        "Aztec",
        "Badwater",
        "Black Jack",
        "Boxcar",
        "Bronco",
        "Buckaroo",
        "Buffalo",
        "Cajun",
        "Cattle",
        "Cherokee",
        "Chugwater",
        "Crazy",
        "Curly",
        "Deacon",
        "Delaware",
        "Frog Lip",
        "Hohokam",
        "Honest",
        "Midnight",
        "Navaho",
        "Oklahoma",
        "Panhandle",
        "Peacemaker",
        "Pecos",
        "Prairie",
        "Rowdy",
        "Saddlebags",
        "Shotgun",
        "Stagecoach",
        "Tascosa",
        "Tensleep",
        "Texas",
        "Three Fingered",
        "Tombstone",
        "Tumbleweed",
        "Whistlestop",
        "Whoopup",
        "Wild",
    };

    protected static final String[] MALE_PREFIXES = {
        "Cockeyed",
        "Colonel",
        "Cowboy",
        "Deadwood",
        "Dirty",
        "Doc",
        "Foghorn",
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
        "Clankin",
        "Lugbolt",
        "Robo",
        "Steam",
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
        "Pat",
    };

    protected static final String[] MALE_ROOTS = {
        "Abe",
        "Adam",
        "Al",
        "Alfred",
        "Angus",
        "Bart",
        "Bass",
        "Bat",
        "Baxter",
        "Bennie",
        "Bill",
        "Bob",
        "Brigham",
        "Bubba",
        "Butch",
        "Casey",
        "Chance",
        "Clancey",
        "Clay",
        "Dave",
        "Davey",
        "Deuce",
        "Doyle",
        "Ed",
        "Frank",
        "George",
        "Gilbert",
        "Granville",
        "Guy",
        "Hank",
        "Harry",
        "Henry",
        "Holliday",
        "Hugh",
        "Isaac",
        "Jack",
        "James",
        "Jesse",
        "Joaquin",
        "Joe",
        "John",
        "Joseph",
        "Karl",
        "Kit",
        "Lee",
        "Lloyd",
        "Mac",
        "Melville",
        "Pancho",
        "Paul",
        "Pete",
        "Roy",
        "Sam",
        "Samuel",
        "Shug",
        "Temple",
        "Thomas",
        "Tim",
        "Tom",
        "Tomas",
        "Tommy",
        "Tuff",
        "Ty",
        "William",
        "Wyatt",
    };

    protected static final String[] FEMALE_ROOTS = {
        "Alice",
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
        "Ella",
        "Fox",
        "Gabby",
        "Glendoline",
        "Jackie",
        "Jane",
        "Janet",
        "Jennie",
        "Jody",
        "Josey",
        "Judy",
        "Kate",
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
        "Pearl",
        "Rosa",
        "Rose",
        "Rube",
        "Sadie",
        "Sarah",
        "Tana",
        "Tanya",
        "Wanda",
    };

    protected static final String[] SHARED_FAMILY = {
        "Armstrong",
        "Avery",
        "Barker",
        "Barnett",
        "Bean",
        "Black",
        "Bodmer",
        "Boone",
        "Brisbin",
        "Brown",
        "Burrows",
        "Burr",
        "Champion",
        "Clanton",
        "Conway",
        "Curry",
        "Dixon",
        "Doolin",
        "Dunn",
        "Evans",
        "Fargo",
        "Frost",
        "Garcia",
        "Hamilton",
        "Hastings",
        "Henderson",
        "Herring",
        "Hollister",
        "Horn",
        "Houston",
        "James",
        "Ketchum",
        "Kimmel",
        "Krebs",
        "Lambert",
        "LeDoux",
        "LeFors",
        "Lippincott",
        "Logan",
        "Mackenzie",
        "Maddox",
        "Maxwell",
        "McDougal",
        "Metcalf",
        "Montana",
        "Moses",
        "Mulhall",
        "Murieta",
        "Murray",
        "Newcombe",
        "Nolan",
        "O'Dell",
        "Parker",
        "Pierce",
        "Putney",
        "Regret",
        "Ringo",
        "Rogers",
        "Sharp",
        "Sieber",
        "Sinclair",
        "Siringo",
        "Skelton",
        "Starr",
        "Stuart",
        "Thorn",
        "Trafton",
        "Villa",
        "Wagner",
        "Whitfield",
        "Williams",
        "Woodward",
    };

    protected static final String[] SHARED_SUFFIXES = {
        "the Kid",
    };

    protected static final String[] MALE_SUFFIXES = {
        "Crockett",
        "Earp",
        "Hickock",
        "de Wolde",
    };

    protected static final String[] FEMALE_SUFFIXES = {
        "Canary",
        "Cimarron",
        "Oakley",
    };

    protected static final String[] GANG_SUFFIXES = {
        "Bandits",
        "Boys",
        "Bunch",
        "Clan",
        "Congregation",
        "Crew",
        "Dudes",
        "Gals",
        "Gang",
        "Girls",
        "Guys",
        "Outlaws",
        "Posse",
        "Renegades",
        "Riders",
        "Rustlers",
        "Tribe",
    };

    protected static final String[] GANG_NAMES = {
        "Drifter",
    };

    protected static final String[] AI_GANG_NAMES = {
        "Bucket Heads",
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

        CollectionUtil.addAll(GANG_SUFF_TABLE, GANG_SUFFIXES);
        CollectionUtil.addAll(AI_GANG_TABLE, AI_GANG_NAMES);
        CollectionUtil.addAll(GANG_TABLE, GANG_NAMES);

        CollectionUtil.addAll(FAMILY_TABLE, SHARED_FAMILY);
        for (char cc = 'A'; cc <= 'Z'; cc++) {
            INITIALS_TABLE.add(Character.toString(cc));
        }
    }
}
