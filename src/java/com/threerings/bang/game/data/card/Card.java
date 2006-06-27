//
// $Id$

package com.threerings.bang.game.data.card;

import java.util.Collection;
import java.util.HashMap;

import com.samskivert.util.RandomUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Provides the player with a one-shot crazy thing that they can do during
 * the course of a game. Players can hold some fixed number of cards and
 * use them on any turn during the game (modulo special restrictions).
 */
public abstract class Card extends SimpleStreamableObject
    implements DSet.Entry, Cloneable
{
    /** The different card placement targets. */
    public static enum PlacementMode { VS_PIECE, VS_AREA, VS_CARD };

    /** Every card has a unique id which is how we reference them. */
    public int cardId;

    /** The player index of the player that is holding this card. */
    public int owner;

    /**
     * Selects a random card from the set of all available cards for the
     * specified town.
     *
     * @param inGame whether this card will immediately be deployed in a game
     * or not (not generally means the card is being sold in a pack).
     */
    public static String selectRandomCard (String townId, boolean inGame)
    {
        // select the card based on a weighted random choice
        return _wcards[RandomUtil.getWeightedIndex(_weights)].getType();
    }

    /**
     * Creates a card of the specified type. Returns null if no card
     * exists with the specified type.
     */
    public static Card newCard (String type)
    {
        Card proto = _cards.get(type);
        if (proto == null) {
            log.warning("Requested to create unknown card '" + type + "'.");
            Thread.dumpStack();
            return null;
        }
        return (Card)proto.clone();
    }

    /**
     * Returns a list of all registered card types.
     */
    public static Collection<Card> getCards ()
    {
        return _cards.values();
    }

    /**
     * Returns the prototype card of the specified type.
     */
    public static Card getCard (String type)
    {
        return _cards.get(type);
    }
    
    /** Returns a string type identifier for this card. */
    public abstract String getType ();

    /**
     * Determines whether this card can be played in the identified game.
     */
    public boolean isPlayable (BangObject bangobj)
    {
        return true;
    }
    
    /**
     * Returns the placement mode of this card.
     */
    public PlacementMode getPlacementMode ()
    {
        // default to vs unit
        return PlacementMode.VS_PIECE;
    }

    /** Returns the radius that should be used when displaying this
     * card's area of effect. */
    public int getRadius ()
    {
        return 0;
    }

    /**
     * Returns true if the piece is a valid target for the card.
     */
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return false;
    }

    /**
     * Returns true if ths location is a valid target for the card.
     */
    public boolean isValidLocation (BangObject bangobj, int tx, int ty)
    {
        return false;
    }

    /**
     * Activates the specified card at the supplied coordinates. The
     * returned effect will be prepared and effected immediately.
     *
     * @return the effect of the card activation.
     */
    public abstract Effect activate (BangObject bangobj, Object target);

    /**
     * Returns the weight of this card compared to the others which is used to
     * determine its rarity.
     */
    public abstract int getWeight ();

    /**
     * Returns the script cost for a pack of three of these cards or 0 if the
     * cards are not for sale outside of bundles.
     */
    public abstract int getScripCost ();

    /**
     * Returns the coin cost for a pack of three of these cards.
     */
    public abstract int getCoinCost ();

    /**
     * This is used to assign the owner and a new unique id to a card when
     * it is created (on the server). Derived classes can also override
     * this method and further configure their card based on the relative
     * strength or weakness of the receiving player.
     */
    public void init (BangObject bangobj, int owner)
    {
        _key = null;
        cardId = ++_nextCardId;
        this.owner = owner;
        getKey();
    }

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        if (_key == null) {
            _key = new Integer(cardId);
        }
        return _key;
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return cardId;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return cardId == ((Card)other).cardId;
    }

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException("Pigs flying! " + cnse);
        }
    }

    @Override // documentation inherited
    public String toString ()
    {
        return getType() + super.toString();
    }

    /**
     * Registers a card prototype so that it may be looked up and
     * instantiated by "type" (as defined by {@link #getType}).
     */
    protected static void register (Card card)
    {
        _cards.put(card.getType(), card);
    }

    /** Used as our DSet.Entry key. */
    protected transient Integer _key;

    /** Used to assign unique ids to card instances. */
    protected static int _nextCardId;

    /** A mapping from card identifier to card prototype. */
    protected static HashMap<String,Card> _cards = new HashMap<String,Card>();

    /** Contains a weight value for every registered card. */
    protected static int[] _weights;

    /** Contains the card associated with the weight value of the same index in
     * {@link #_weights}. */
    protected static Card[] _wcards;

    static {
        register(new Repair());
        register(new DustDevil());
        register(new Missile());
        register(new Stampede());
        register(new Staredown());
        register(new GiddyUp());
        register(new Joker());
        register(new Mine());
        register(new HalfRepair());
        register(new JackRabbit());
        register(new Spring());
        register(new HalfGiddyUp());
        register(new Ramblin());
        register(new Hustle());
        register(new HollowPoint());
        register(new LuckyHorseshoe());
        register(new IronPlate());
        register(new Reinforcements());
        register(new Roadblock());
        register(new Lasso());
        register(new DropNugget());
        register(new FoolsGold());
        register(new Engineer());
        register(new Dud());
        register(new Misfire());
        
        // collect the weights of each card into an array used to select
        // randomly based on said weights
        _weights = new int[_cards.size()];
        _wcards = new Card[_cards.size()];
        int idx = 0;
        for (Card card : _cards.values()) {
            _wcards[idx] = card;
            _weights[idx++] = card.getWeight();
        }
    }
}
