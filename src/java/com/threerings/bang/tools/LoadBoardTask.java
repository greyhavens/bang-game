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
import com.samskivert.jdbc.StaticConnectionProvider;

import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.BoardRecord;
import com.threerings.bang.server.persist.BoardRepository;

import static com.threerings.bang.Log.log;

/**
 * An ant task for loading pre-made boards into the repository.
 */
public class LoadBoardTask extends Task
{
    /**
     * Configures our <code>server.properties</code> file.
     */
    public void setProps (File props)
    {
        _props = props;
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
        if (_props == null) {
            throw new BuildException("Missing 'props' task property.");
        }
        System.setProperty("install_config", _props.getPath());

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

        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            String[] srcFiles = ds.getIncludedFiles();
            for (int ii = 0; ii < srcFiles.length; ii++) {
                loadBoard(new File(fromDir, srcFiles[ii]));
            }
        }
    }

    /**
     * Parses a single board file and loads its data into the board
     * repository.
     */
    protected void loadBoard (File source)
    {
        try {
            BoardRecord brec = new BoardRecord();
            brec.load(source);
            brec.dataHash = brec.getDataHash();
            _brepo.storeBoard(brec);

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to load board " +
                    "[source=" + source + "].", e);
        }
    }

    /** Contains our configuration. */
    protected File _props;

    /** Provides access to the board database. */
    protected BoardRepository _brepo;

    /** A list of filesets that contain board definitions. */
    protected ArrayList<FileSet> _filesets = new ArrayList<FileSet>();
}
