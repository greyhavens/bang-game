//
// $Id$

package com.threerings.bang.client;

import java.awt.Dimension;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.swing.util.SwingUtil;
import com.threerings.media.SafeScrollPane;
import com.threerings.util.MessageBundle;

import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.data.piece.SteamGunman;
import com.threerings.bang.data.piece.Artillery;
import com.threerings.bang.data.piece.Dirigible;
import com.threerings.bang.data.piece.Gunslinger;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangConfig;
import com.threerings.bang.data.BangObject;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Displays an interface for purchasing units.
 */
public class PurchasePanel extends JPanel
    implements TableModelListener, ActionListener
{
    public PurchasePanel (BangContext ctx, BangConfig config,
                          BangObject bangobj, int pidx)
    {
        setLayout(new VGroupLayout(VGroupLayout.NONE, VGroupLayout.NONE,
                                   5, VGroupLayout.CENTER));

        VGroupLayout vgl = new VGroupLayout(
            VGroupLayout.STRETCH, VGroupLayout.STRETCH, 5, VGroupLayout.CENTER);
        JPanel box = new JPanel(vgl);
        box.setPreferredSize(new Dimension(400, 300));
        add(box);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        _bangobj = bangobj;
        _pidx = pidx;

        // add the header
        JPanel bbox = GroupLayout.makeHStretchBox(5);
        bbox.add(new JLabel(_msgs.get("m.buying_phase")));
        JLabel round = new JLabel(
            _msgs.get("m.round", ""+_bangobj.roundId, ""+config.rounds));
        round.setHorizontalAlignment(JLabel.RIGHT);
        bbox.add(round);
        box.add(bbox, VGroupLayout.FIXED);

        // add a big table listing all purchasable units
        _model = new UnitTableModel();
        _model.addTableModelListener(this);
        box.add(new SafeScrollPane(new JTable(_model)));

        _total = new JLabel(_msgs.get("m.total_cost", "" + 0));
        bbox = GroupLayout.makeButtonBox(GroupLayout.RIGHT);
        bbox.add(_total);
        bbox.add(_ready = new JButton(_msgs.get("m.ready")));
        _ready.addActionListener(this);
        _ready.setEnabled(false);
        box.add(bbox, VGroupLayout.FIXED);
    }

    // documentation inherited from interface TableModelListener
    public void tableChanged (TableModelEvent e)
    {
        // update the total cost and ready button
        int totalCost = 0;
        for (int ii = 0; ii < UNIT_PROTOS.length; ii++) {
            int count = (Integer)_model.getValueAt(ii, 2);
            totalCost += (count * UNIT_PROTOS[ii].getCost());
        }
        _total.setText(_msgs.get("m.total_cost", "" + totalCost));
        _ready.setEnabled(_pidx != -1 && totalCost > 0 &&
                          totalCost <= _bangobj.reserves[_pidx]);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent e)
    {
        ArrayList<Piece> pieces = new ArrayList<Piece>();
        for (int ii = 0; ii < UNIT_PROTOS.length; ii++) {
            int count = (Integer)_model.getValueAt(ii, 2);
            for (int pp = 0; pp < count; pp++) {
                pieces.add((Piece)UNIT_PROTOS[ii].clone());
            }
        }
        SwingUtil.setEnabled(this, false);
        Piece[] pvec = pieces.toArray(new Piece[pieces.size()]);
        _bangobj.service.purchasePieces(_ctx.getClient(), pvec);
    }

    /** Displays a table of purchasable units. */
    protected class UnitTableModel extends AbstractTableModel
    {
        public String getColumnName (int col) {
            return _msgs.get("m.col_" + COLUMNS[col]);
        }

        public Class getColumnClass (int columnIndex) {
            switch (columnIndex) {
            case 0: return String.class;
            case 1: return Integer.class;
            case 2: return Integer.class;
            default:
            case 3: return Integer.class;
            }
        }

        public int getRowCount () {
            return UNIT_IDENTS.length;
        }

        public int getColumnCount () {
            return COLUMNS.length;
        }

        public Object getValueAt (int row, int col) {
            switch (col) {
            case 0: return _msgs.get("m." + UNIT_IDENTS[row]);
            case 1: return "$" + UNIT_PROTOS[row].getCost();
            case 2: return new Integer(_unitCounts[row]);
            case 3: return "$" + (UNIT_PROTOS[row].getCost() * _unitCounts[row]);
            default:
                log.warning("Unknown column requested [col=" + col +
                            ", row=" + row + "].");
                return "";
            }
        }

        public boolean isCellEditable (int row, int col) {
            return (_pidx != -1 && col == 2);
        }

        public void setValueAt (Object value, int row, int col) {
            if (col == 2) {
                _unitCounts[row] = (value == null) ? 0 : (Integer)value;
                fireTableCellUpdated(row, 3);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BangObject _bangobj;
    protected int _pidx;

    protected UnitTableModel _model;
    protected int[] _unitCounts = new int[UNIT_PROTOS.length];

    protected JLabel _total;
    protected JButton _ready;

    protected static final String[] COLUMNS = {
        "unit", "cost", "count", "total_cost"
    };
    protected static final String[] UNIT_IDENTS = {
        "steamgunman", "artillery", "dirigible", "gunslinger"
    };
    protected static final Piece[] UNIT_PROTOS = {
        new SteamGunman(), new Artillery(), new Dirigible(), new Gunslinger()
    };
}
