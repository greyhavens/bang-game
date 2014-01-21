//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import java.util.logging.Level;

import com.jme.util.LoggingSystem;

import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.icon.ImageIcon;

/**
 * Does something extraordinary.
 */
public class LabelTest extends BaseTest
    implements BConstants
{
    protected void createWindows (BRootNode root, BStyleSheet style)
    {
        BWindow window = new BDecoratedWindow(style, null);
        window.setLayoutManager(GroupLayout.makeVStretch());

        BImage image = null;
        try {
            image = new BImage(getClass().getClassLoader().getResource("textures/scroll_right.png"));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        ImageIcon icon = new ImageIcon(image);
        String[] aligns = { "left", "center", "right" };
        int[] orients = { HORIZONTAL, VERTICAL, OVERLAPPING };

        for (int yy = 0; yy < 3; yy++) {
            BContainer cont = new BContainer(GroupLayout.makeHStretch());
            window.add(cont);
            for (int xx = 0; xx < 3; xx++) {
                BLabel label = new BLabel("This is a lovely label " +
                                          aligns[xx] + "/" + orients[yy] + ".",
                                          aligns[xx]);
                label.setIcon(icon);
                label.setOrientation(orients[yy]);
                cont.add(label);
            }
        }

        root.addWindow(window);
        window.setSize(400, 400);
        window.setLocation(25, 25);
    }

    public static void main (String[] args)
    {
        LoggingSystem.getLogger().setLevel(Level.OFF);
        LabelTest test = new LabelTest();
        test.start();
    }
}
