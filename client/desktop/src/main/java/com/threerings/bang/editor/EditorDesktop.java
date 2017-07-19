//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Guice;

import com.badlogic.gdx.backends.lwjgl.LwjglCanvas;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import com.threerings.bang.client.BangApp;

public class EditorDesktop
{
    public static void main (String[] args) {
        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
        cfg.title = "Bang! Howdy Editor";
        cfg.width = 1024;
        cfg.height = 768;
        // cfg.resizble = false;
        // TODO: cfg.setFromDisplayMode when in fullscreen mode

        // configure our debug log
        BangApp.configureLog("editor.log");

        // save these for later
        EditorApp.appArgs = args;

        // create our editor server which we're going to run in the same JVM with the client
        Injector injector = Guice.createInjector(new EditorServer.Module());
        EditorServer server = injector.getInstance(EditorServer.class);
        try {
            server.init(injector);
        } catch (Exception e) {
            System.err.println("Unable to initialize server.");
            e.printStackTrace(System.err);
        }

        // let the BangClientController know we're in editor mode
        System.setProperty("editor", "true");

        // create a frame
        JFrame frame = new JFrame("Bang Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // this is the entry point for all the "client-side" stuff
        EditorApp app = injector.getInstance(EditorApp.class);
        app.frame = frame;

        System.out.println("Start?");
        LwjglCanvas canvas = new LwjglCanvas(app);
        app.canvas = canvas.getCanvas();

        // display the GL canvas to start so that it initializes everything
        frame.getContentPane().add(canvas.getCanvas(), BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.setSize(new Dimension(1224, 768));
    }
}
