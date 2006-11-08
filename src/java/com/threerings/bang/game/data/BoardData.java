//
// $Id$

package com.threerings.bang.game.data;

import java.awt.Rectangle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.logging.Level;

import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.Savable;
import com.jme.util.export.binary.BinaryExporter;
import com.jme.util.export.binary.BinaryImporter;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.StringUtil;

import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.game.data.piece.Track;
import com.threerings.bang.game.data.piece.Viewpoint;

import static com.threerings.bang.Log.*;

/**
 * Contains the data ({@link BangBoard} and props, markers, etc.) associated
 * with a board stored on the client or server.
 */
public class BoardData
    implements Savable
{
    /** The serialized board data. */
    public byte[] data;

    /**
     * Serializes the supplied board and piece information and stuffs it
     * into the {@link #data} member.
     */
    public void setData (BangBoard board, Piece[] pieces)
    {
        try {
            // set the fields that will be saved
            _board = board;
            _pieces = pieces;
            
            // serialize this object
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            BinaryExporter.getInstance().save(this, bout);
            
            // store the resulting data
            data = bout.toByteArray();
            
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to encode board " + this, ioe);
        }
    }

    /**
     * Decodes and returns the board in this record.
     */
    public BangBoard getBoard ()
        throws IOException
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
        throws IOException
    {
        if (_pieces == null) {
            decodeData();
        }
        return _pieces;
    }

    /**
     * Computes and returns the MD5 hash of the board data.
     */
    public byte[] getDataHash ()
    {
        try {
            return MessageDigest.getInstance("MD5").digest(data);

        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("MD5 codec not available");
        }
    }

    // documentation inherited from interface Savable
    public Class getClassTag ()
    {
        // no need to save as subclass
        return BoardData.class;
    }
    
    // documentation inherited from interface Savable
    public void read (JMEImporter im)
        throws IOException
    {
        InputCapsule capsule = im.getCapsule(this);
        _board = (BangBoard)capsule.readSavable("board", null);
        _pieces = ArrayUtil.copy(capsule.readSavableArray("pieces", null),
            new Piece[0]);
    }
    
    // documentation inherited from interface Savable
    public void write (JMEExporter ex)
        throws IOException
    {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_board, "board", null);
        capsule.write(_pieces, "pieces", null);
    }
    
    /** Returns a string representation of this instance. */
    public String toString ()
    {
        return ((data == null) ? "0" : String.valueOf(data.length)) + " bytes";
    }

    /** Helper for the two load board methods. */
    protected void decodeData ()
        throws IOException
    {
        // load up our board and pieces
        BoardData bdata = (BoardData)BinaryImporter.getInstance().load(data);
        _board = bdata._board;
        _pieces = bdata._pieces;
    }

    protected transient BangBoard _board;
    protected transient Piece[] _pieces;
}
