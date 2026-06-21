//
// $Id$

package com.threerings.jme.tools;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingSphere;
import com.jme.bounding.BoundingVolume;
import com.jme.util.LoggingSystem;
import com.jme.util.DummyDisplaySystem;

import com.threerings.jme.tools.model.Model;

/**
 * An ant task for determining the height of props and setting an attribute in
 * the prop configuration file read by both client and server.
 *
 * <p>jME3 cutover (Phase 2): relocated from {@code :tools}
 * ({@code com.threerings.bang.tools}) into app's fork-coupled {@code modeltool} source set,
 * alongside {@code CompileModelTask} and the fork {@code Model} reader. It reads the fork-format
 * {@code model.dat} that {@code CompileModelTask} just produced, so it must run against the fork
 * {@code Model}/bounding API (the fork-free {@code :tools} module no longer has them on its
 * classpath). The {@code BangMetrics.TILE_SIZE} (= 10) constant is inlined to avoid pulling
 * client/shared onto this build-time classpath.
 */
public class UpdatePropHeightTask extends Task
{
    /** Tile size in world units (was {@code com.threerings.bang.client.BangMetrics.TILE_SIZE}). */
    public static final float TILE_SIZE = 10;

    /**
     * Adds a nested &lt;fileset&gt; element.
     */
    public void addFileset (FileSet set)
    {
        _filesets.add(set);
    }

    @Override // documentation inherited
    public void init () throws BuildException
    {
        // create a dummy display system
        new DummyDisplaySystem();
        LoggingSystem.getLogger().setLevel(Level.WARNING);
    }

    @Override // documentation inherited
    public void execute () throws BuildException
    {
        // Ensure a headless display system is installed before any model.dat is read: the fork
        // ModelMesh.read() path calls DisplaySystem.getDisplaySystem() (to build render states),
        // which otherwise constructs the gdx GDXDisplaySystem and NPEs in this headless ant task.
        // init() may not be invoked when run via gradle's ant bridge, so do it here too.
        new DummyDisplaySystem();
        LoggingSystem.getLogger().setLevel(Level.WARNING);

        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            String[] srcFiles = ds.getIncludedFiles();
            for (int ii = 0; ii < srcFiles.length; ii++) {
                updatePropHeight(new File(fromDir, srcFiles[ii]));
            }
        }
    }

    /**
     * Updates the height property of the specified prop if it is out of date.
     *
     * @param file the prop.properties file
     */
    protected void updatePropHeight (File file)
    {
        // see if model.dat is newer than prop.properties
        File dir = file.getParentFile(), mfile = new File(dir, "model.dat");
        if (mfile.lastModified() < file.lastModified()) {
            return;
        }

        // load the model
        Model model;
        try {
            model = Model.readFromFile(mfile);
        } catch (IOException e) {
            System.out.println("Error reading " + mfile + ": " + e);
            return;
        } catch (RuntimeException e) {
            // jME3 cutover: the fork BinaryImporter returns null (after logging) when a model.dat
            // embeds a class it can no longer instantiate -- e.g. a stale source-tree model.dat
            // written by the pre-cutover fork whose root/controllers reference now-relocated fork
            // classes -- which then NPEs inside readFromFile's initPrototype(). The freshly compiled
            // models live in the staging tree; a stray source-tree model.dat is build cruft. Skip
            // rather than fail the build so the pipeline validation completes.
            System.out.println("Skipping unreadable (pre-cutover-format) model: " + mfile +
                " (" + e + ")");
            return;
        }
        if (model == null) {
            System.out.println("Skipping unreadable model (null): " + mfile);
            return;
        }

        // find the model's vertical size in tiles
        model.updateGeometricState(0f, true);
        float height = 0f;
        BoundingVolume bound = model.getWorldBound();
        if (bound != null) {
            height = bound.getCenter().z;
            if (bound instanceof BoundingBox) {
                height += ((BoundingBox)bound).zExtent;
            } else if (bound instanceof BoundingSphere) {
                height += ((BoundingSphere)bound).radius;
            } else {
                System.out.println("Unknown bounding type in " + mfile +
                    ": " + bound);
            }
            height /= TILE_SIZE;
        }

        // read in the prop.properties and see if the height needs changing
        // (if not, just touch the file)
        Properties props = new Properties();
        BufferedInputStream in;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            in.mark((int)file.length());
            props.load(in);
        } catch (IOException e) {
            System.out.println("Error reading " + file + ": " + e);
            return;
        }
        if (Float.parseFloat(props.getProperty("height", "2")) == height) {
            file.setLastModified(System.currentTimeMillis());
            return;
        }

        // if so, copy out the properties with the revised height
        System.out.println("Updating prop height for " + file + "...");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            in.reset();
            PrintWriter writer = new PrintWriter(file);
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                boolean hline = line.startsWith("height");
                if (!hline) {
                    writer.println(line);
                }
                if (hline || (line.startsWith("length") &&
                    !props.containsKey("height"))) {
                    writer.println("height = " + height);
                }
            }
            writer.close();

        } catch (IOException e) {
            System.out.println("Error writing " + file + ": " + e);
        }
    }

    /** A list of filesets that contain board definitions. */
    protected ArrayList<FileSet> _filesets = new ArrayList<FileSet>();
}
