//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.ArrayList;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

/**
 * Displays a piece of wreckage.
 */
public class WreckageSprite extends PieceSprite
{
    @Override // documentation inherited
    protected void createGeometry ()
    {
        // use the "dead" model of a steam unit chosen with the piece id
        UnitConfig[] configs = UnitConfig.getTownUnits(
            ((BangContext)_ctx).getUserObject().townId);
        ArrayList<UnitConfig> sunits = new ArrayList<UnitConfig>();
        for (UnitConfig config : configs) {
            if (config.make == UnitConfig.Make.STEAM) {
                sunits.add(config);
            }
        }
        String type = sunits.get(_piece.pieceId % sunits.size()).type;
        System.out.println(type);
        loadModel("units", type + "/dead");
    }
}
