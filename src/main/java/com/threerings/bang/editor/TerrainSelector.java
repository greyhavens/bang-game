//
// $Id$

package com.threerings.bang.editor;

import java.util.Collection;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.HGroupLayout;
import com.samskivert.util.QuickSort;

import com.threerings.bang.data.TerrainConfig;
import com.threerings.bang.util.BasicContext;

/**
 * Displays a drop-down of terrain types.
 */
public class TerrainSelector extends JPanel
{
    public TerrainSelector (BasicContext ctx)
    {
        _ctx = ctx;
        setLayout(new HGroupLayout(HGroupLayout.STRETCH));

        add(new JLabel(_ctx.xlate("editor", "m.terrain_select")),
            HGroupLayout.FIXED);

        Collection<TerrainConfig> configs = TerrainConfig.getConfigs();
        TerrainSelection[] choices =
            new TerrainSelection[configs.size()];
        int ii = 0;
        for (TerrainConfig config : configs) {
            choices[ii++] = new TerrainSelection(config);
        }
        QuickSort.sort(choices);
        add(_selector = new JComboBox<TerrainSelection>(choices));
    }

    public TerrainConfig getSelectedTerrain ()
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
        implements Comparable<TerrainSelection>
    {
        public TerrainConfig terrain;

        public TerrainSelection (TerrainConfig terrain)
        {
            this.terrain = terrain;
        }

        public String toString ()
        {
            String msg = "m.terrain_" + terrain.type;
            return _ctx.getMessageManager().getBundle("editor").exists(msg) ?
                _ctx.xlate("editor", msg) : terrain.type;
        }

        public int compareTo (TerrainSelection other)
        {
            return terrain.type.compareTo(other.terrain.type);
        }
    }

    protected BasicContext _ctx;
    protected JComboBox<TerrainSelection> _selector;
}
