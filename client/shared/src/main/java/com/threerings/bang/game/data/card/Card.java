//
// $Id$

package com.threerings.bang.game.data.card;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.samskivert.util.RandomUtil;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.game.data.scenario.TutorialInfo;
import com.threerings.bang.util.BangUtil;

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
    public static enum PlacementMode {
        VS_PIECE, VS_AREA, VS_CARD, VS_PLAYER, VS_BOARD };

    /** Every card has a unique id which is how we reference them. */
    public int cardId;

    /** The player index of the player that is holding this card. */
    public int owner;

    /** If this card was aquired during the course of a round. */
    public boolean found = true;

    /**
     * Selects a random card from the set of all available cards for the specified town.
     *
     * @param bangobj if this card is being created during a game, this will indicate the game
     * object. Otherwise this will be null (meaning the card is being created for a pack).
     * @param pidx the index of the player for whom the card is being generated, if the card is
     * being created for a game
     */
    public static String selectRandomCard (String townId, BangObject bangobj, int pidx)
    {
        if (bangobj != null && bangobj.scenario instanceof TutorialInfo) {
            // we always return missile cards in the tutorial
            return ((TutorialInfo)bangobj.scenario).cardType;

        } else {
            // if in a game, retrieve the player's point factor
            double pointFactor = (bangobj == null) ? 1f : bangobj.pdata[pidx].pointFactor;

            // select the card based on a weighted random choice
            Card[] wcards = _wcards.get(townId);
            int[] weights = _weights.get(townId).clone();

            // clone the weights and adjust them based on the point factor
            if (pointFactor < 1f || pointFactor > 1.25f) {
                if (pointFactor < 1) {
                    // for players at a disadvantage, add a constant value to all weights which
                    // will reduce the rarity variance
                    int adjust = (int)Math.round(200 * (1 - pointFactor));
                    for (int ii = 0; ii < weights.length; ii++) {
                        weights[ii] += adjust;
                    }

                } else { // (pointFactor > 1.25)
                    // for players above the average points, filter out all cards below a cutoff
                    // frequency
                    int cutoff = (pointFactor > 1.5) ? 50 : 25;
                    for (int ii = 0; ii < weights.length; ii++) {
                        if (weights[ii] >= cutoff) {
                            weights[ii] = 0;
                        }
                    }
                }
            }

            // zero out any cards that can't be used in this round (and whose unaltered frequency
            // falls below the cutoff frequency in use for this game)
            if (bangobj != null) {
                for (int ii = 0; ii < wcards.length; ii++) {
                    if (weights[ii] > 0 && (!wcards[ii].isPlayable(bangobj, townId) ||
                                            weights[ii] < bangobj.minCardBonusWeight)) {
                        weights[ii] = 0;
                    }
                }
            }

            // if we eliminated all possible cards, fall back to half_repair
            int cidx = RandomUtil.getWeightedIndex(weights);
            return (cidx >= 0) ? wcards[cidx].getType() : new HalfRepair().getType();
        }
    }

    /**
     * Creates a card of the specified type. Returns null if no card exists with the specified
     * type.
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

    /**
     * Returns the style class for this card.
     */
    public String getStyle ()
    {
        return (found ? "card_found_button" : "card_button");
    }

    /** Returns a string type identifier for this card. */
    public abstract String getType ();

    /**
     * Returns the badge necessary to enable this card for purchase in three packs at the General
     * Store or null if it has no qualifier.
     */
    public Badge.Type getQualifier ()
    {
        return null;
    }

    /**
     * Returns a fully qualified translatable string for displaying the name of this card.
     */
    public String getName ()
    {
        return MessageBundle.qualify(BangCodes.CARDS_MSGS, "m." + getType());
    }

    /**
     * This will be called to determine if a card is playable during a game. If a card needs to
     * inspect the configuration of the game, it can override this method. If it only cares about
     * the scenario type, it should override {@link #isPlayable(ScenarioInfo,String)}.
     */
    public boolean isPlayable (BangObject bangobj, String townId)
    {
        return isPlayable(bangobj.scenario, townId);
    }

    /**
     * Determines whether this card can be played in the specified scenario.
     */
    public boolean isPlayable (ScenarioInfo scenario, String townId)
    {
        return BangUtil.getTownIndex(townId) >= BangUtil.getTownIndex(getTownId());
    }

    /**
     * Returns the placement mode of this card.
     */
    public PlacementMode getPlacementMode ()
    {
        // default to vs unit
        return PlacementMode.VS_PIECE;
    }

    /** Returns the radius that should be used when displaying this card's area of effect. */
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
     * Returns true if the player is a valid target for this card.
     */
    public boolean isValidPlayer (BangObject bangobj, int pidx)
    {
        return false;
    }

    /**
     * Returns true if this card can be played at the moment (only called for VS_BOARD cards).
     */
    public boolean isValid (BangObject bangobj)
    {
        return false;
    }

    /**
     * Checks whether the specified player's client should show a standard visualization (card
     * dropping on a piece, etc.) when the card is played.  Some cards hide their targets from all
     * but their owners.
     */
    public boolean shouldShowVisualization (int pidx)
    {
        return true;
    }

    /**
     * Activates the specified card at the supplied coordinates. The returned effect will be
     * prepared and effected immediately.
     *
     * @return the effect of the card activation.
     */
    public abstract Effect activate (BangObject bangobj, Object target);

    /**
     * Returns the town in which this card was introduced.
     */
    public abstract String getTownId ();

    /**
     * Returns the weight of this card compared to the others which is used to determine its
     * rarity.
     */
    public abstract int getWeight ();

    /**
     * Returns the script cost for a pack of three of these cards or 0 if the cards are not for
     * sale outside of bundles.
     */
    public abstract int getScripCost ();

    /**
     * This is used to assign the owner and a new unique id to a card when it is created (on the
     * server). Derived classes can also override this method and further configure their card
     * based on the relative strength or weakness of the receiving player.
     */
    public void init (BangObject bangobj, int owner)
    {
        _key = null;
        cardId = ++_nextCardId;
        this.owner = owner;
        getKey();
    }

    /**
     * Returns the path to this card's icon image.
     *
     * @param which either <code>icon</code> for its tiny icon image or <code>card</code> for its
     * inventory item image.
     */
    public String getIconPath (String which)
    {
        return "cards/" + getTownId() + "/" + getType() + "/" + which + ".png";
    }

    // documentation inherited from interface DSet.Entry
    public Comparable<?> getKey ()
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
            throw new RuntimeException(cnse);
        }
    }

    @Override // documentation inherited
    public String toString ()
    {
        return getType() + super.toString();
    }

    /**
     * Registers a card prototype so that it may be looked up and instantiated by "type" (as
     * defined by {@link #getType}).
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

    /** Maps town id to an array of the weights for all cards available in that town. Used in
     * conjunction with {@link #_wcards}. */
    protected static HashMap<String,int[]> _weights = new HashMap<String,int[]>();

    /** Maps town id to an array of the cards available in that town. Used in conjunction with
     * {@link #_weights}. */
    protected static HashMap<String,Card[]> _wcards = new HashMap<String,Card[]>();

    static {
        register(new Repair());
        register(new DustDevil());
        register(new Missile());
        register(new Stampede());
        register(new Staredown());
        register(new GiddyUp());
        register(new Joker());
        register(new Trap());
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
        register(new Barricade());
        register(new Lasso());
        register(new DropNugget());
        register(new FoolsGold());
        register(new Engineer());
        register(new Dud());
        register(new Misfire());
        register(new CatBallou());
        register(new MonkeyWrench());
        register(new SnakeBite());
        register(new BlownGasket());
        register(new BuggyLogic());
        register(new HighNoon());
        register(new Snare());
        register(new EagleEye());
        register(new TumbleweedWind());
        register(new Forgiven());
        register(new Firestarter());
        register(new SpiritWalk());
        register(new PeacePipe());
        register(new Rain());
        register(new Lightning());
//         register(new Rockslide());
        register(new UnderdogSoldier());

        // create arrays of all cards introduced in each town
        HashMap<String,ArrayList<Card>> bytown = new HashMap<String,ArrayList<Card>>();
        for (Card card : _cards.values()) {
            ArrayList<Card> clist = bytown.get(card.getTownId());
            if (clist == null) {
                bytown.put(card.getTownId(), clist = new ArrayList<Card>());
            }
            clist.add(card);
        }

        // now create weights and wcards arrays for each town
        ArrayList<Card> clist = new ArrayList<Card>();
        for (String townId : BangCodes.TOWN_IDS) {
            // each town has all the cards from the previous, plus new ones
            clist.addAll(bytown.get(townId));
            int[] weights = new int[clist.size()];
            Card[] cards = new Card[clist.size()];
            int idx  = 0;
            for (Card card : clist) {
                cards[idx] = card;
                weights[idx++] = card.getWeight();
            }
            _weights.put(townId, weights);
            _wcards.put(townId, cards);
        }
    }
}
