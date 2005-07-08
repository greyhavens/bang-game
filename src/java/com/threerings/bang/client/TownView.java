//
// $Id$

package com.threerings.bang.client;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;

import com.jme.bui.BButton;
import com.jme.bui.BWindow;
import com.jme.bui.background.ScaledBackground;
import com.jme.bui.event.MouseAdapter;
import com.jme.bui.event.MouseEvent;
import com.jme.bui.layout.GroupLayout;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.bang.ranch.client.RanchView;
import com.threerings.bang.store.client.StoreView;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Displays the main "town" menu interface where a player can navigate to
 * the ranch, the saloon, the general store, the bank, the train station
 * and wherever else we might dream up.
 */
public class TownView extends BWindow
{
    public TownView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), GroupLayout.makeVert(GroupLayout.TOP));
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("town");

        // display the status view when the player presses escape
        setModal(true);
        new StatusView(_ctx).bind(this);

        int width = ctx.getDisplay().getWidth();
        int height = ctx.getDisplay().getHeight();
        setBounds(0, 0, width, height);

        // TODO: unhack
        String tpath = "rsrc/menu/frontier";
        ClassLoader loader = getClass().getClassLoader();
        setBackground(new ScaledBackground(
                          loader.getResource(tpath + "/town.png"), 0, 0, 0, 0));

        // load up the polygons
        Properties props = new Properties();
        try {
            props.load(loader.getResourceAsStream(tpath + "/menu.properties"));
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to load menu properties " +
                    "[path=" + tpath + "/menu.properties].", e);
        }
        Enumeration iter = props.propertyNames();
        while (iter.hasMoreElements()) {
            String command = (String)iter.nextElement();
            float[] coords = StringUtil.parseFloatArray(
                props.getProperty(command));
            if (coords == null || coords.length % 2 != 0 || coords.length < 6) {
                log.warning("Rejecting malformed command [command=" + command +
                            ", coords=" + props.getProperty(command) + "].");
                continue;
            }
            Polygon poly = new Polygon();
            for (int ii = 0; ii < coords.length; ii += 2) {
                poly.addPoint(Math.round(width * coords[ii]),
                              Math.round(height * coords[ii+1]));
            }
            _polys.add(poly);
            _commands.add(command);
        }

        addListener(new MouseAdapter() { 
            public void mousePressed (MouseEvent event) {
                String cmd = getCommand(event.getX(), event.getY());
                if (cmd != null) {
                    fireCommand(cmd);
                }
            }
            public void mouseMoved (MouseEvent event) {
                // TODO: display highlights when we mouse over a region
            }
       });
    }

    protected String getCommand (int mx, int my)
    {
        for (int ii = 0, ll = _polys.size(); ii < ll; ii++) {
            if (_polys.get(ii).contains(mx, my)) {
                return _commands.get(ii);
            }
        }
        return null;
    }

    protected void fireCommand (String command)
    {
        if ("logoff".equals(command)) {
            _ctx.getApp().stop();

        } else if ("to_ranch".equals(command)) {
            _ctx.setPlaceView(new RanchView(_ctx));

        } else if ("to_bank".equals(command)) {
            return; // TODO

        } else if ("to_store".equals(command)) {
            _ctx.setPlaceView(new StoreView(_ctx));

        } else if ("to_saloon".equals(command)) {
            _ctx.getLocationDirector().moveTo(2);
        }
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected ArrayList<Polygon> _polys = new ArrayList<Polygon>();
    protected ArrayList<String> _commands = new ArrayList<String>();
}
