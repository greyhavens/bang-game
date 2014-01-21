//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import java.io.InputStream;
import java.io.InputStreamReader;

import com.jme.app.SimpleGame;
import com.jme.input.KeyBindingManager;
import com.jme.input.MouseInput;
import com.jme.renderer.ColorRGBA;

/**
 * A base class for our various visual tests.
 */
public abstract class BaseTest extends SimpleGame
{
    protected void simpleInitGame ()
    {
        _root = new PolledRootNode(timer, input);
        rootNode.attachChild(_root);

        // we don't hide the cursor
        MouseInput.get().setCursorVisible(true);

        // load up the default BUI stylesheet
        BStyleSheet style = null;
        try {
            InputStream stin = getClass().getClassLoader().getResourceAsStream("style.bss");
            style = new BStyleSheet(new InputStreamReader(stin),
                                    new BStyleSheet.DefaultResourceProvider());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }

        createWindows(_root, style);

        // these just get in the way
        KeyBindingManager.getKeyBindingManager().remove("toggle_pause");
        KeyBindingManager.getKeyBindingManager().remove("toggle_wire");
        KeyBindingManager.getKeyBindingManager().remove("toggle_lights");
        KeyBindingManager.getKeyBindingManager().remove("toggle_bounds");
        KeyBindingManager.getKeyBindingManager().remove("camera_out");

        lightState.setEnabled(false);

        display.getRenderer().setBackgroundColor(ColorRGBA.gray);
    }

    protected void simpleUpdate ()
    {
    }

    protected abstract void createWindows (BRootNode root, BStyleSheet style);

    protected PolledRootNode _root;
}
