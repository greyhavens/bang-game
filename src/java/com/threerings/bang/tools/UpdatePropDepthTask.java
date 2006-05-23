//
// $Id$

package com.threerings.bang.tools;

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
import com.jmex.model.XMLparser.Converters.DummyDisplaySystem;

import com.threerings.jme.model.Model;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * An ant task for determining the "depth" (vertical size) of props and setting
 * an attribute in the prop configuration file read by both client and server.
 */
public class UpdatePropDepthTask extends Task
{    
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
        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            String[] srcFiles = ds.getIncludedFiles();
            for (int ii = 0; ii < srcFiles.length; ii++) {
                updatePropDepth(new File(fromDir, srcFiles[ii]));
            }
        }
    }
    
    /**
     * Updates the depth property of the specified prop if it is out of date.
     *
     * @param file the prop.properties file
     */
    protected void updatePropDepth (File file)
    {
        // see if model.dat is newer than prop.properties
        File dir = file.getParentFile(), mfile = new File(dir, "model.dat");
        if (mfile.lastModified() < file.lastModified()) {
            return;
        }
        
        // load the model
        Model model;
        try {
            model = Model.readFromFile(mfile, false);
        } catch (IOException e) {
            System.out.println("Error reading " + mfile + ": " + e);
            return;
        }
        
        // find the model's vertical size in tiles
        model.updateGeometricState(0f, true);
        BoundingVolume bound = model.getWorldBound();
        float depth = bound.getCenter().z;
        if (bound instanceof BoundingBox) {
            depth += ((BoundingBox)bound).zExtent;
        } else if (bound instanceof BoundingSphere) {
            depth += ((BoundingSphere)bound).radius;
        } else {
            System.out.println("Unknown bounding type in " + mfile +
                ": " + bound);
        }
        depth /= TILE_SIZE;
        
        // read in the prop.properties and see if the depth needs changing
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
        if (Float.parseFloat(props.getProperty("depth", "2")) == depth) {
            return;
        }
        
        // if so, copy out the properties with the revised depth
        System.out.println("Updating prop depth for " + file + "...");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            in.reset();
            PrintWriter writer = new PrintWriter(file);
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                boolean dline = line.startsWith("depth");
                if (!dline) {
                    writer.println(line);
                }
                if (dline || (line.startsWith("height") &&
                    !props.containsKey("depth"))) {
                    writer.println("depth = " + depth);
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
