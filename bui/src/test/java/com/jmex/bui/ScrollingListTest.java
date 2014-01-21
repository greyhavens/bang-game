//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import java.util.logging.Level;

import com.jme.util.LoggingSystem;

import com.jmex.bui.layout.GroupLayout;

public class ScrollingListTest extends BaseTest
    implements BConstants
{
    protected void createWindows (BRootNode root, BStyleSheet style)
    {
        BWindow window = new BDecoratedWindow(style, null);
        window.setLayoutManager(GroupLayout.makeVStretch());

//         BImage image;
//         try {
//             image = new BImage(getClass().getClassLoader().
//                                getResource("textures/scroll_right.png"));
//         } catch (Exception e) {
//             e.printStackTrace(System.err);
//         }

        BScrollingList<String, BButton> list = new BScrollingList<String, BButton>() {
            public BButton createComponent(String str) {
                return new BButton(str);
            }
        };

        window.add(list);

        root.addWindow(window);
        window.setSize(400, 400);
        window.setLocation(25, 25);

        for (int i = 0; i < 100; i ++) {
            list.addValue("Item #" + i, true);
        }
    }

    public static void main (String[] args)
    {
        LoggingSystem.getLogger().setLevel(Level.WARNING);
        ScrollingListTest test = new ScrollingListTest();
        test.start();
    }
}
