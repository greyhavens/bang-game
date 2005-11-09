//
// $Id$

package com.threerings.bang.server.persist;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Properties;
import java.util.logging.Level;

import com.samskivert.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.game.data.piece.Track;

import static com.threerings.bang.Log.log;

/**
 * Contains persistent information associated with a particular game
 * board.
 */
public class BoardRecord
{
    /** The unique identifier for this board. */
    public int boardId;

    /** The human readable name of this board. */
    public String name;

    /** The username of the player that created this board, or null if it
     * is a system created board. */
    public String creator;

    /** A comma separated list of scenarios for which this board is
     * usable. Use {@link #setScenarios} and {@link #getScenarios}. */
    public String scenarios;

    /** The number of players for which this board is appropriate. */
    public int players;

    /** The number of player-games that have been played on this board
     * (ie. if four players play one game on the board, this value is
     * incremented by four). */
    public int plays;

    /** The serialized board data. */
    public byte[] data;
    
    /**
     * Serializes the supplied board and piece information and stuffs it
     * into the {@link #data} member.
     */
    public void setData (BangBoard board, Piece[] pieces)
    {
        try {
            // serialize the board and pieces
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bout);
            oos.writeObject(board);
            oos.writeInt(pieces.length);
            for (int ii = 0; ii < pieces.length; ii++) {
                writePiece(oos, pieces[ii]);
            }
            oos.flush();

            // store the various bits into our record
            data = bout.toByteArray();
            _board = board;
            _pieces = pieces;

        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to encode board " + this, ioe);
        }
    }

    /**
     * Decodes and returns the board in this record.
     */
    public BangBoard getBoard ()
    {
        if (_board == null) {
            decodeData();
        }
        return _board;
    }

    /**
     * Decodes and returns the pieces in this record.
     */
    public Piece[] getPieces ()
    {
        if (_pieces == null) {
            decodeData();
        }
        return _pieces;
    }

    /**
     * Returns an array of all scenarios for which this board is
     * applicable.
     */
    public String[] getScenarios ()
    {
        return (scenarios == null) ? new String[0] :
            StringUtil.split(scenarios, ",");
    }

    /**
     * Configures this board record with a set of valid scenarios.
     */
    public void setScenarios (String[] scenids)
    {
        scenarios = StringUtil.join(scenids, ",");
    }

    /** Returns a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    /** Helper function for {@link #toString}. */
    public String dataToString ()
    {
        return data.length + " bytes";
    }

    /**
     * Saves this board record to the specified target file.
     */
    public void save (File target)
        throws IOException
    {
        Properties props = new Properties();
        props.setProperty("name", name);
        props.setProperty("players", String.valueOf(players));
        props.setProperty("scenarios", scenarios);
        props.setProperty("data", StringUtil.hexlate(data));
        BufferedOutputStream out = new BufferedOutputStream(
            new FileOutputStream(target));
        props.store(out, "");
        out.close();
    }

    /**
     * Loads the specified file containing board record data into this
     * instance.
     */
    public void load (File source)
        throws IOException
    {
        load(new FileInputStream(source));
    }

    /**
     * Loads the supplied input stream containing board record data into
     * this instance.
     */
    public void load (InputStream in)
        throws IOException
    {
        Properties props = new Properties();
        BufferedInputStream bin = new BufferedInputStream(in);
        props.load(bin);
        name = props.getProperty("name");
        String pstr = props.getProperty("players", "0");
        try {
            players = Integer.valueOf(pstr);
        } catch (Exception e) {
            log.warning("Invalid players property [prop=" + pstr + "].");
        }
        scenarios = props.getProperty("scenarios");
        data = StringUtil.unhexlate(props.getProperty("data", ""));
        bin.close();
    }

    /** Helper for the two load board methods. */
    protected void decodeData ()
    {
        try {
            ObjectInputStream oin = new ObjectInputStream(
                new ByteArrayInputStream(data));
            _board = (BangBoard)oin.readObject();
            _pieces = new Piece[oin.readInt()];
            for (int ii = 0; ii < _pieces.length; ii++) {
                _pieces[ii] = readPiece(oin);
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to decode board " + this, e);
        }
    }

    /** Helper method. */
    protected void writePiece (ObjectOutputStream oout, Piece piece)
        throws IOException
    {
        if (piece instanceof Prop) {
            oout.writeUTF(((Prop)piece).getType());
        } else if (piece instanceof Marker) {
            oout.writeUTF("__marker__");
            oout.writeInt(((Marker)piece).getType());
        } else if (piece instanceof Track) {
            oout.writeUTF("__track__");
            oout.writeByte(((Track)piece).type);
        } else {
            throw new IOException("Unknown piece type " +
                                  "[type=" + piece.getClass().getName() +
                                  ", piece=" + piece + "].");
        }
        oout.writeInt(piece.pieceId);
        oout.writeShort(piece.x);
        oout.writeShort(piece.y);
        oout.writeShort(piece.orientation);
    }

    /** Helper method. */
    protected Piece readPiece (ObjectInputStream oin)
        throws IOException
    {
        String type = oin.readUTF();
        Piece piece;
        if (type.equals("__marker__")) {
            piece = new Marker(oin.readInt());
        } else if (type.equals("__track__")) {
            piece = new Track(oin.readByte());
        } else {
            piece = Prop.getProp(type);
        }
        piece.pieceId = oin.readInt();
        short x = oin.readShort(), y = oin.readShort();
        piece.orientation = oin.readShort();
        piece.position(x, y);
        return piece;
    }

    protected transient BangBoard _board;
    protected transient Piece[] _pieces;
}
