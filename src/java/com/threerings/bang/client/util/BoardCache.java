//
// $Id$

package com.threerings.bang.client.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.lang.ref.WeakReference;

import java.util.Arrays;
import java.util.HashMap;

import com.samskivert.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BoardData;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.piece.Piece;

import com.threerings.bang.client.BasicClient;

import static com.threerings.bang.Log.*;

/**
 * Manages a set of boards cached on the client's file system.
 */
public class BoardCache
{
    /**
     * Attempts to retrieve a board from the cache.
     *
     * @param name the name of the desired board
     * @param players the number of players for the board
     * @param hash the board's MD5 hash value
     * @return the cached board, or <code>null</code> if no cached
     * board matched the given parameters
     */
    public BoardData loadBoard (String name, int players, byte[] hash)
    {
        // first check the memory cache
        BoardKey bkey = new BoardKey(name, players, hash);
        WeakReference<BoardData> bref = _boards.get(bkey);
        BoardData board;
        if (bref != null && (board = bref.get()) != null) {
            return board;
        }
        
        // then look on disk
        File bfile = getBoardFile(name, players);
        if (!bfile.exists()) {
            return null;
        }
        try {
            board = readBoardData(bfile);
        } catch (Exception e) {
            log.warning("Failed to read board from cache! [bfile=" +
                bfile + ", error=" + e + "].");
            return null;
        }
        if (!Arrays.equals(board.getDataHash(), hash)) {
            return null;
        }
        log.info("Loaded board from cached file [bfile=" + bfile + "].");
        _boards.put(bkey, new WeakReference<BoardData>(board));
        return board;
    }
    
    /**
     * Stores a board obtained from the server in the cache.
     *
     * @param name the name of the board
     * @param players the number of players for the board
     * @param hash the board's MD5 hash value
     * @param bboard the board itself
     * @param pieces the initial pieces on the board
     */
    public void saveBoard (String name, int players, byte[] hash,
        BangBoard bboard, Piece[] pieces)
    {
        // store the board in the local cache
        BoardKey bkey = new BoardKey(name, players, hash);
        BoardData board = new BoardData();
        board.setData(bboard, pieces);
        _boards.put(bkey, new WeakReference<BoardData>(board));
        
        // save the board to the cache directory, creating the directory if
        // necessary
        File bfile = getBoardFile(name, players), dir = bfile.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            FileOutputStream fos = new FileOutputStream(bfile);
            fos.write(board.data);
            fos.close();
            
        } catch (IOException e) {
            log.warning("Failed to store board in cache! [bfile=" + bfile +
                ", error=" + e + "].");
        }
    }
    
    /**
     * Returns the cache file for a board with the given parameters.
     */
    protected File getBoardFile (String name, int players)
    {
        return new File(BasicClient.localDataDir("boards" + File.separator +
            players + File.separator + encode(name)));
    }
    
    /**
     * Encodes the specified filename to remove any characters not allowed in
     * filenames.
     */
    protected String encode (String filename)
    {
        StringBuffer buf = new StringBuffer();
        for (int ii = 0, nn = filename.length(); ii < nn; ii++) {
            char c = filename.charAt(ii);
            if (ENCODE_CHARS.indexOf(c) != -1) {
                buf.append('%').append(StringUtil.hexlate(
                    Character.toString(c).getBytes()));

            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }
    
    /**
     * Reads the board data from the given file.
     */
    protected BoardData readBoardData (File file)
        throws IOException
    {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = fis.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        fis.close();
        BoardData board = new BoardData();
        board.data = baos.toByteArray();
        return board;
    }
    
    /** Boards cached in memory. */
    protected HashMap<BoardKey, WeakReference<BoardData>> _boards =
        new HashMap<BoardKey, WeakReference<BoardData>>();
    
    /** Identifies a single version of a single board. */
    protected static class BoardKey
    {
        public String name;
        public int players;
        public byte[] hash;
        
        public BoardKey (String name, int players, byte[] hash)
        {
            this.name = name;
            this.players = players;
            this.hash = hash;
        }
        
        public int hashCode ()
        {
            return name.hashCode() ^ players ^ Arrays.hashCode(hash);
        }
        
        public boolean equals (Object other)
        {
            BoardKey okey = (BoardKey)other;
            return name.equals(okey.name) && players == okey.players &&
                Arrays.equals(hash, okey.hash);
        }
    }
    
    /** Characters that must be encoded in filenames (plus the percent sign,
     * which we use as an encoded character flag). */
    protected static final String ENCODE_CHARS = "/\\:*?\"<>|%";
}
