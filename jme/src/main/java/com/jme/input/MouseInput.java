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

import com.jme.input.gdx.GDXMouseInput;


/**
 * <code>MouseInput</code> defines an interface to communicate with the mouse
 * input device.
 * The status of spcific buttons can be queried via the {@link #isButtonDown}
 * method. Position data can be queried by various get methods.
 * For each button that is pressed or released as well as for movement of
 * mouse or wheel an event is generated which
 * can be received by a {@link MouseInputListener}, these are subsribed via
 * {@link #addListener(MouseInputListener)}. Handling of events is done inside the
 * {@link #update} method.
 * @author Mark Powell
 * @version $Id$
 */
public abstract class MouseInput extends Input {

    private static MouseInput instance = new GDXMouseInput();

    /**
     * list of event listeners.
     */
    protected ArrayList<MouseInputListener> listeners = new ArrayList<MouseInputListener>();

    /**
     * @return the input instance, implementation is determined by querying {@link #getProvider()}
     */
    public static MouseInput get() {
        return instance;
    }

    /**
     * <code>destroy</code> cleans up the native mouse interface.
     * Destroy is protected now - please is {@link #destroyIfInitalized()}.
     */
    protected abstract void destroy();

    /**
     *
     * <code>isButtonDown</code> returns true if a given button is pressed,
     * false if it is not pressed.
     * @param buttonCode the button code to check.
     * @return true if the button is pressed, false otherwise.
     */
    public abstract boolean isButtonDown(int buttonCode);

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
     *
     * <code>getXDelta</code> gets the change along the x axis.
     * @return the change along the x axis.
     */
    public abstract int getXDelta();

    /**
     *
     * <code>getYDelta</code> gets the change along the y axis.
     * @return the change along the y axis.
     */
    public abstract int getYDelta();

    /**
     *
     * <code>getXAbsolute</code> gets the absolute x axis value.
     * @return the absolute x axis value.
     */
    public abstract int getXAbsolute();

    /**
     *
     * <code>getYAbsolute</code> gets the absolute y axis value.
     * @return the absolute y axis value.
     */
    public abstract int getYAbsolute();

    /**
     * Updates the state of the mouse (position and button states). Invokes event listeners synchronously.
     */
    @Override
    public abstract void update();

    //todo:
    /**
     * <code>setCursorVisible</code> sets the visiblity of the hardware cursor.
     * @param v true turns the cursor on false turns it off
     */
    public abstract void setCursorVisible(boolean v);

    /**
     * <code>isCursorVisible</code>
     * @return the visibility of the hardware cursor
     */
    public abstract boolean isCursorVisible();

    /**
     * Subscribe a listener to receive mouse events. Enable event generation.
     * @param listener to be subscribed
     */
    public void addListener( MouseInputListener listener ) {
        listeners.add( listener );
    }

    /**
     * Unsubscribe a listener. Disable event generation if no more listeners.
     * @see #addListener(com.jme.input.MouseInputListener)
     * @param listener to be unsuscribed
     */
    public void removeListener( MouseInputListener listener ) {
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
        if ( instance != null )
        {
            instance.destroy();
            instance = null;
        }
    }

    /**
     * @return number of mouse buttons
     */
    public abstract int getButtonCount();

    public abstract void setCursorPosition( int x, int y);
}
