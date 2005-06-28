//
// $Id$

package com.threerings.bang.editor;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.HGroupLayout;

import com.threerings.bang.data.Terrain;
import com.threerings.bang.util.BangContext;

/**
 * Displays a drop-down of terrain types.
 */
public class TerrainSelector extends JPanel
{
    public TerrainSelector (BangContext ctx)
    {
        _ctx = ctx;
        setLayout(new HGroupLayout(HGroupLayout.STRETCH));

        add(new JLabel(_ctx.xlate("editor", "m.terrain_select")),
            HGroupLayout.FIXED);

        TerrainSelection[] choices =
            new TerrainSelection[Terrain.USABLE.size()];
        int ii = 0;
        for (Terrain terrain : Terrain.USABLE) {
            choices[ii++] = new TerrainSelection(terrain);
        }
        add(_selector = new JComboBox(choices));
    }

    public Terrain getSelectedTerrain ()
    {
        return ((TerrainSelection)_selector.getSelectedItem()).terrain;
    }

    public void rollSelection (int amount)
    {
        int count = _selector.getItemCount();
        int newidx = (_selector.getSelectedIndex() + amount + count) % count;
        _selector.setSelectedIndex(newidx);
    }

    protected class TerrainSelection
    {
        public Terrain terrain;

        public TerrainSelection (Terrain terrain)
        {
            this.terrain = terrain;
        }

        public String toString ()
        {
            String msg = "m.terrain_" + terrain.toString().toLowerCase();
            return _ctx.xlate("editor", msg);
        }
    }

    protected BangContext _ctx;
    protected JComboBox _selector;
}
