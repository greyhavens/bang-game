//
// $Id$

package com.threerings.bang.jme3.mxml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Epic 4 Phase 2a verification harness: runs the ported fork-free {@link ModelParser} and
 * {@link AnimationParser} over the whole committed <code>.mxml</code> corpus. For every
 * <code>model.properties</code> under the given root it parses the sibling
 * <code>model.mxml</code> and every sibling animation <code>.mxml</code>, printing per-model
 * mesh/vertex/index/bone stats and per-animation frame counts, then a final summary line:
 * <code>PARSED &lt;n&gt; models, &lt;m&gt; animations, &lt;k&gt; failures</code>.
 * Exits non-zero if anything failed to parse.
 */
public class MxmlProbe
{
    public static void main (String[] args)
        throws IOException
    {
        File root = new File(args.length > 0 ? args[0] : "assets/rsrc");
        if (!root.isDirectory()) {
            System.err.println("Not a directory: " + root);
            System.exit(2);
        }

        List<Path> propFiles;
        try (Stream<Path> walk = Files.walk(root.toPath())) {
            propFiles = walk.filter(p -> p.getFileName().toString().equals("model.properties"))
                .sorted().collect(Collectors.toList());
        }

        ModelParser mparser = new ModelParser();
        AnimationParser aparser = new AnimationParser();
        int models = 0, anims = 0, failures = 0;

        for (Path props : propFiles) {
            Path dir = props.getParent();
            String rel = root.toPath().relativize(dir).toString();

            // parse the model mesh definition
            Path mxml = dir.resolve("model.mxml");
            if (!Files.exists(mxml)) {
                System.out.println(rel + ": FAILED: missing model.mxml");
                failures++;
            } else {
                try {
                    ModelDef model = mparser.parseModel(mxml.toString());
                    int meshes = 0, skinned = 0;
                    long verts = 0, inds = 0;
                    for (ModelDef.SpatialDef spatial : model.spatials) {
                        if (spatial instanceof ModelDef.TriMeshDef) {
                            ModelDef.TriMeshDef mesh = (ModelDef.TriMeshDef)spatial;
                            meshes++;
                            if (mesh instanceof ModelDef.SkinMeshDef) {
                                skinned++;
                            }
                            verts += mesh.vertices.size();
                            inds += mesh.indices.size();
                        }
                    }
                    HashSet<String> bones = model.getBoneNames();
                    models++;
                    System.out.println(rel + ": " + meshes + " meshes (" + skinned +
                        " skinned), " + verts + " vertices, " + inds + " indices, " +
                        bones.size() + " bones");
                } catch (Exception e) {
                    System.out.println(rel + ": FAILED: " + e);
                    failures++;
                }
            }

            // parse every sibling animation .mxml
            List<Path> animFiles = new ArrayList<Path>();
            try (Stream<Path> list = Files.list(dir)) {
                list.filter(p -> p.getFileName().toString().endsWith(".mxml") &&
                        !p.getFileName().toString().equals("model.mxml"))
                    .sorted().forEach(animFiles::add);
            }
            for (Path animFile : animFiles) {
                String aname = animFile.getFileName().toString();
                aname = aname.substring(0, aname.length() - ".mxml".length());
                try {
                    AnimationDef anim = aparser.parseAnimation(animFile.toString());
                    anims++;
                    System.out.println("    " + aname + ": " + anim.frames.size() +
                        " frames @ " + anim.frameRate + " fps");
                } catch (Exception e) {
                    System.out.println("    " + aname + ": FAILED: " + e);
                    failures++;
                }
            }
        }

        System.out.println("PARSED " + models + " models, " + anims + " animations, " +
            failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
