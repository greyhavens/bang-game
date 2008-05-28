//
// $Id$

package com.threerings.bang.avatar.tools;

import java.io.IOException;
import java.util.HashMap;

import com.threerings.resource.ResourceManager;
import com.threerings.util.CompiledConfig;

import com.threerings.bang.util.BangUtil;

import com.threerings.bang.avatar.util.AspectCatalog;

/**
 * A tool to generate a comparsion chart of the aspect catalog.
 */
public class SummarizeAspects extends SummarizeMetadata
{
    public static void main (String[] args)
        throws IOException
    {
        ResourceManager rmgr = new ResourceManager("rsrc");
        rmgr.initBundles(null, "config/resource/manager.properties", null);
        AspectCatalog aspcat = (AspectCatalog)CompiledConfig.loadConfig(
            rmgr.getResource(AspectCatalog.CONFIG_PATH));

        String[] classes = aspcat.enumerateClassNames("male/");
        HashMap<String,Row> allasps = new HashMap<String,Row>();
        for (int ii = 0; ii < classes.length; ii++) {
            classes[ii] = classes[ii].substring("male/".length());
            for (boolean isMale : new boolean[] { false, true }) {
                String aclass = (isMale ? "male/" : "female/") + classes[ii];
                for (AspectCatalog.Aspect aspect : aspcat.getAspects(aclass)) {
                    Row orow = allasps.get(aspect.name);
                    if (orow != null) {
                        orow.setCost(aspect, isMale);
                    } else {
                        Row row = new Row(aspect, isMale);
                        row.townIdx = BangUtil.getTownIndex(aspect.townId);
                        row.catIdx = ii;
                        allasps.put(row.name, row);
                    }
                }
            }
        }

        printSummary("Aspect Catalog", "Aspect", allasps, classes);
    }
}
