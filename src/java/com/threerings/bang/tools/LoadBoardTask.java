//
// $Id$

package com.threerings.bang.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.jdbc.StaticConnectionProvider;

import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.BoardRecord;
import com.threerings.bang.server.persist.BoardRepository;

/**
 * An ant task for loading pre-made boards into the repository.
 */
public class LoadBoardTask extends Task
{
    /**
     * Configures our <code>bang.home</code> directory.
     */
    public void setHome (File home)
    {
        _home = home;
    }

    /**
     * Adds a nested &lt;fileset&gt; element.
     */
    public void addFileset (FileSet set)
    {
        _filesets.add(set);
    }

    @Override // documentation inherited
    public void execute () throws BuildException
    {
        if (_home == null) {
            throw new BuildException("Missing 'home' task property.");
        }
        System.setProperty("bang.home", _home.getPath());

        if (_brepo == null) {
            try {
                StaticConnectionProvider conprov =
                    new StaticConnectionProvider(ServerConfig.getJDBCConfig());
                _brepo = new BoardRepository(conprov);
            } catch (PersistenceException pe) {
                throw new BuildException(
                    "Failed to create board repository", pe);
            }
        }

        // reload the stock boards
        ArrayIntSet loaded = new ArrayIntSet();
        int attempted = 0;
        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            String[] srcFiles = ds.getIncludedFiles();
            for (int ii = 0; ii < srcFiles.length; ii++) {
                attempted++;
                loadBoard(new File(fromDir, srcFiles[ii]), loaded);
            }
        }
        System.out.println("Loaded " + loaded.size() + " boards.");

        // then wipe any stale boards (but only if there were no errors loading
        // the stock boards)
        if (loaded.size() == attempted) {
            try {
                int pruned = _brepo.clearStaleBoards(loaded);
                if (pruned > 0) {
                    System.out.println("Pruned " + pruned + " stale boards.");
                }
            } catch (Exception e) {
                System.err.println("Failure clearing stale boards: " + e);
            }
        }
    }

    /**
     * Parses a single board file and loads its data into the board
     * repository.
     */
    protected void loadBoard (File source, ArrayIntSet loaded)
    {
        try {
            BoardRecord brec = new BoardRecord();
            brec.load(source);
            brec.dataHash = brec.getDataHash();
            _brepo.storeBoard(brec);
            loaded.add(brec.boardId);

        } catch (Exception e) {
            System.err.println("Failed to load board [source=" + source + "].");
            e.printStackTrace(System.err);
        }
    }

    /** Contains our configuration. */
    protected File _home;

    /** Provides access to the board database. */
    protected BoardRepository _brepo;

    /** A list of filesets that contain board definitions. */
    protected ArrayList<FileSet> _filesets = new ArrayList<FileSet>();
}
