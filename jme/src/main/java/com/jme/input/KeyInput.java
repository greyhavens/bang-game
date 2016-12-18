/*
 * Copyright (c) 2003-2006 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jme.input;

import java.util.ArrayList;

import com.badlogic.gdx.Input.Keys;

import com.jme.input.gdx.GDXKeyInput;

/**
 * <code>KeyInput</code> provides an interface for dealing with keyboard input.
 * There are public contstants for each key of the keyboard, which correspond
 * to the LWJGL key bindings. This may require conversion by other subclasses
 * for specific APIs. <br>
 * The status of spcific keys can be queried via the {@link #isKeyDown}
 * method. For each key that is pressed or released an event is generated which
 * can be received by a {@link KeyInputListener}, these are subsribed via
 * {@link #addListener(KeyInputListener)}. Handling of events is done inside the
 * {@link #update} method.
 *
 * @author Mark Powell
 * @version $Id$
 */
public abstract class KeyInput extends Input {

    private static KeyInput instance = new GDXKeyInput();

    /**
     * list of event listeners.
     */
    protected ArrayList<KeyInputListener> listeners = new ArrayList<KeyInputListener>();

    /**
     * @return the input instance, implementation is determined by querying {@link #getProvider()}
     */
    public static KeyInput get() {
        return instance;
    }


    /**
     * <code>isKeyDown</code> returns true if the given key is pressed. False
     * otherwise.
     *
     * @param key the keycode to check for.
     * @return true if the key is pressed, false otherwise.
     */
    public abstract boolean isKeyDown( int key );

    /**
     * <code>isInited</code> returns true if the key class is not setup
     * already (ie. .get() was not yet called).
     *
     * @return true if it is initialized and ready for use, false otherwise.
     */
    public static boolean isInited() {
        return instance != null;
    }

    /**
     * Updates the current state of the keyboard, holding
     * information about what keys are pressed.
     * Invokes event listeners synchronously.
     *
     * @see
     */
    @Override
    public abstract void update();

    /**
     * <code>destroy</code> frees the keyboard for use by other applications.
     * Destroy is protected now - please is {@link #destroyIfInitalized()}.
     */
    protected abstract void destroy();

    /**
     * Subscribe a listener to receive mouse events. Enable event generation.
     *
     * @param listener to be subscribed
     */
    public void addListener( KeyInputListener listener ) {
        listeners.add( listener );
    }

    /**
     * Unsubscribe a listener. Disable event generation if no more listeners.
     *
     * @param listener to be unsuscribed
     * @see #addListener(KeyInputListener)
     */
    public void removeListener( KeyInputListener listener ) {
        listeners.remove( listener );
    }

    /**
     * Remove all listeners and disable event generation.
     */
    public void removeListeners() {
        listeners.clear();
    }

    /**
     * Destroy the input if it was initialized.
     */
    public static void destroyIfInitalized() {
        if ( instance != null ) {
            instance.destroy();
            instance = null;
        }
    }

    public boolean isShiftDown() {
        return isKeyDown(Keys.SHIFT_LEFT) || isKeyDown(Keys.SHIFT_RIGHT);
    }

    public boolean isControlDown() {
        return isKeyDown(Keys.CONTROL_LEFT) || isKeyDown(Keys.CONTROL_RIGHT);
    }
}
