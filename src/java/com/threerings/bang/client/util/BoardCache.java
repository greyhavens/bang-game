//
// $Id$

package com.threerings.bang.client.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.lang.ref.WeakReference;

import java.util.Arrays;
import java.util.HashMap;

import com.samskivert.util.StringUtil;

import com.threerings.bang.game.data.BoardData;

import com.threerings.bang.client.BasicClient;

import static com.threerings.bang.Log.*;

/**
 * Manages a set of boards cached on the client's file system.
 */
public class BoardCache
{
    /**
     * Retrieves a board from the cache.
     *
     * @param hash the board's MD5 hash value.
     *
     * @return the cached board, or <code>null</code> if no cached board
     * matched the given parameters
     */
    public BoardData loadBoard (byte[] hash)
    {
        // first check the memory cache
        String bkey = StringUtil.hexlate(hash);
        WeakReference<BoardData> bref = _boards.get(bkey);
        BoardData board;
        if (bref != null && (board = bref.get()) != null) {
            return board;
        }

        // then look on disk
        File bfile = getBoardFile(bkey);
        if (!bfile.exists()) {
            return null;
        }

        byte[] data;
        try {
            // read the raw data
            data = readBoardData(bfile);
            // make sure we can turn it into a board
            board = BoardData.fromBytes(data);
        } catch (Exception e) {
            log.warning("Failed to read board from cache!", "bfile", bfile, "error", e);
            return null;
        }

        // make sure the board is the right version
        if (!Arrays.equals(BoardData.getDataHash(data), hash)) {
            return null;
        }

        _boards.put(bkey, new WeakReference<BoardData>(board));
        return board;
    }

    /**
     * Stores a board obtained from the server in the cache.
     *
     * @param hash the board's MD5 hash value.
     * @param bdata the board data.
     */
    public void saveBoard (byte[] hash, BoardData bdata)
    {
        // store the board in the in-memory cache
        String bkey = StringUtil.hexlate(hash);
        _boards.put(bkey, new WeakReference<BoardData>(bdata));

        // create the cache directory if necesary
        File bfile = getBoardFile(bkey), dir = bfile.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // now save the board
        try {
            FileOutputStream fos = new FileOutputStream(bfile);
            fos.write(bdata.toBytes());
            fos.close();
        } catch (IOException e) {
            log.warning("Failed to store board in cache!", "bfile", bfile, "error", e);
        }
    }

    /**
     * Reads the board data from the given file.
     */
    protected byte[] readBoardData (File file)
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
        return baos.toByteArray();
    }

    /**
     * Returns the cache file for a board with the given parameters.
     */
    protected File getBoardFile (String hash)
    {
        return new File(BasicClient.localDataDir(
                            "boards" + File.separator + hash));
    }

    /** Boards cached in memory. */
    protected HashMap<String, WeakReference<BoardData>> _boards =
        new HashMap<String, WeakReference<BoardData>>();
}
