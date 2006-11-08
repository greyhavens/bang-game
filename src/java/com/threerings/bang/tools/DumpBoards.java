//
// $Id$

package com.threerings.bang.tools;

import java.io.File;
import java.io.IOException;

import com.samskivert.util.StringUtil;

import com.threerings.bang.game.util.BoardFile;

/**
 * Dumps out metadata for a board or boards.
 */
public class DumpBoards
{
    public static void main (String[] args)
        throws IOException
    {
        if (args.length == 0) {
            System.err.println("Usage DumpBoards file.board [file.board ...]");
            System.exit(255);
        }

        for (String file : args) {
            BoardFile bfile = BoardFile.loadFrom(new File(file));
            System.out.println(bfile.name + "\t" +
                               bfile.players + "\t" + 
                               StringUtil.join(bfile.scenarios, ","));
        }
    }
}
