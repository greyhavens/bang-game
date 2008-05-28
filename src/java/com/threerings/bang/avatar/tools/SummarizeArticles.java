//
// $Id$

package com.threerings.bang.avatar.tools;

import java.io.IOException;
import java.util.HashMap;

import com.threerings.resource.ResourceManager;
import com.threerings.util.CompiledConfig;

import com.threerings.bang.util.BangUtil;

import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

/**
 * A tool to generate a comparsion chart of the article catalog.
 */
public class SummarizeArticles extends SummarizeMetadata
{
    public static void main (String[] args)
        throws IOException
    {
        ResourceManager rmgr = new ResourceManager("rsrc");
        rmgr.initBundles(null, "config/resource/manager.properties", null);
        ArticleCatalog artcat = (ArticleCatalog)CompiledConfig.loadConfig(
            rmgr.getResource(ArticleCatalog.CONFIG_PATH));

        HashMap<String,Row> allarts = new HashMap<String,Row>();
        for (ArticleCatalog.Article article : artcat.getArticles()) {
            Row row = new Row(article);
            Row orow = allarts.get(row.name);
            if (orow != null) {
                orow.setCost(article);
            } else {
                row.townIdx = BangUtil.getTownIndex(article.townId);
                row.catIdx = AvatarLogic.getSlotIndex(article.slot);
                allarts.put(row.name, row);
            }
        }

        String[] cats = new String[AvatarLogic.SLOTS.length];
        for (int ii = 0; ii < cats.length; ii++) {
            cats[ii] = AvatarLogic.SLOTS[ii].name;
        }

        printSummary("Article Catalog", "Article", allarts, cats);
    }
}
