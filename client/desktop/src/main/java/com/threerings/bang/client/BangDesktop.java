//
// $Id$

package com.threerings.bang.client;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class BangDesktop
{
    public static void main (String[] args) {
        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
        cfg.title = "Bang! Howdy";
        cfg.useGL20 = true;
        cfg.width = 1024;
        cfg.height = 768;
        // cfg.resizble = false;
        // TODO: cfg.setFromDisplayMode when in fullscreen mode
        new LwjglApplication(new TestolaGame(), cfg);
    }
}
