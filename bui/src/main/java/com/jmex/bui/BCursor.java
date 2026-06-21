//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.InputManager;
import com.jme3.util.BufferUtils;

/**
 * Contains a cursor image and its hotspot.
 *
 * <p> <b>jME3 cutover note:</b> the original implementation built and installed an LWJGL2
 * {@code org.lwjgl.input.Cursor} directly via {@code Mouse.setNativeCursor}. Under jME3/LWJGL3
 * the hardware cursor is a {@link JmeCursor} installed through the {@link InputManager}
 * ({@code InputManager.setMouseCursor}, which in turn drives {@code glfwCreateCursor}/
 * {@code glfwSetCursor}). The shipped client constructs cursors from a {@link BufferedImage} +
 * hotspot, so that is the API kept here. The host ({@code JmeApp}) hands us the live
 * {@code InputManager} once input is wired; {@link #show} is a no-op until then.
 */
public class BCursor
{
    /**
     * Records the host's jME3 {@link InputManager} so {@link #show} can install hardware
     * cursors. Called once by the host (see {@code JmeApp}) when BUI input is wired; mirrors the
     * fork's reliance on the static {@code org.lwjgl.input.Mouse.setNativeCursor} entry point.
     */
    public static void setInputManager (InputManager inputManager)
    {
        _inputManager = inputManager;
    }

    public BCursor (BufferedImage image, int hx, int hy)
    {
        setCursor(image, hx, hy);
    }

    /**
     * Configures this cursor's image and hotspot.
     */
    public void setCursor (BufferedImage image, int hx, int hy)
    {
        _image = image;
        _hx = hx;
        _hy = hy;
        // invalidate any cached native cursor; it will be rebuilt lazily in show()
        _jmeCursor = null;
    }

    /**
     * Returns the cursor image.
     */
    public BufferedImage getImage ()
    {
        return _image;
    }

    /**
     * Returns the x coordinate of the cursor hotspot.
     */
    public int getHotspotX ()
    {
        return _hx;
    }

    /**
     * Returns the y coordinate of the cursor hotspot.
     */
    public int getHotspotY ()
    {
        return _hy;
    }

    /**
     * Display this cursor. Installs the hardware cursor through the jME3 {@link InputManager};
     * a no-op until the host hands us the InputManager (see {@link #setInputManager}).
     */
    public void show ()
    {
        if (_inputManager == null || _image == null) {
            return;
        }
        if (_jmeCursor == null) {
            _jmeCursor = createCursor(_image, _hx, _hy);
        }
        if (_jmeCursor != null && _current != _jmeCursor) {
            try {
                _inputManager.setMouseCursor(_jmeCursor);
                _current = _jmeCursor;
            } catch (Throwable t) {
                Log.log.warning("Problem updating mouse cursor: " + t);
            }
        }
    }

    /**
     * Builds a {@link JmeCursor} from a {@link BufferedImage} with the supplied hotspot.
     *
     * <p>jME3's {@code GlfwMouseInput} consumes the {@link JmeCursor}'s image data as an
     * {@link IntBuffer} of <em>bottom-up ARGB</em> ints (row 0 at the bottom): its
     * {@code transformCursorImage} iterates {@code y = height-1 .. 0} when building the
     * top-down RGBA buffer GLFW wants. {@code BufferedImage.getRGB} yields top-down pixels,
     * so we must flip the rows here (read {@code hh-1-yy}) — otherwise the cursor renders
     * upside down. The hotspot is converted for us ({@code glfwHotspotY = height - yHotSpot}),
     * so storing {@code height - hy} lands GLFW's hotspot on our top-down {@code hy}.
     */
    protected static JmeCursor createCursor (BufferedImage image, int hx, int hy)
    {
        int ww = image.getWidth();
        int hh = image.getHeight();
        IntBuffer data = BufferUtils.createIntBuffer(ww * hh);
        for (int yy = 0; yy < hh; yy++) {
            for (int xx = 0; xx < ww; xx++) {
                // store bottom-up: jME3 reads row 0 as the bottom of the cursor
                data.put(image.getRGB(xx, hh - 1 - yy));
            }
        }
        data.flip();

        JmeCursor cursor = new JmeCursor();
        cursor.setWidth(ww);
        cursor.setHeight(hh);
        cursor.setxHotSpot(hx);
        // GLFW wants a top-left hotspot origin, but JmeCursor stores it bottom-left:
        // GlfwMouseInput installs the cursor with glfwHotspotY = height - getYHotSpot().
        // We want that to land on our top-down hy, so store height - hy here.
        cursor.setyHotSpot(hh - hy);
        cursor.setNumImages(1);
        cursor.setImagesData(data);
        cursor.setImagesDelay(BufferUtils.createIntBuffer(1));
        return cursor;
    }

    /** The host's jME3 input manager; the only path to a hardware cursor under LWJGL3. */
    protected static InputManager _inputManager;

    /** The cursor most recently installed via the InputManager (avoids redundant reinstalls). */
    protected static JmeCursor _current;

    protected BufferedImage _image;
    protected int _hx, _hy;

    /** Lazily built, cached native cursor for {@link #_image}. */
    protected JmeCursor _jmeCursor;
}
