//
// $Id$

package com.threerings.bang.client;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.threerings.bang.client.BangPrefs;

public class BangDesktop
{
    public static void main (String[] args) {
        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
        cfg.title = "Bang! Howdy";
        cfg.width = BangPrefs.getDisplayWidth();
        cfg.height = BangPrefs.getDisplayHeight();
        cfg.depth = BangPrefs.getDisplayBPP();
        cfg.fullscreen = BangPrefs.isFullscreen();
        // cfg.resizble = false;
        new LwjglApplication(new BangApp(), cfg);
    }
}
