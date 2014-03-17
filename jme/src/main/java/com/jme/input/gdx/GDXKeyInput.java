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

package com.jme.input.gdx;

import com.badlogic.gdx.Gdx;

import com.jme.input.KeyInput;
import com.jme.input.KeyInputListener;

public class GDXKeyInput extends KeyInput {

    @Override
    public boolean isKeyDown(int key) {
        return Gdx.input.isKeyPressed(key);
    }

    public boolean keyDown (int keycode) {
        for ( int i = 0; i < listeners.size(); i++ ) {
            KeyInputListener listener = listeners.get( i );
            listener.onPress(keycode);
        }
        return listeners.size() > 0;
    }

    public boolean keyUp (int keycode) {
        for ( int i = 0; i < listeners.size(); i++ ) {
            KeyInputListener listener = listeners.get( i );
            listener.onRelease(keycode);
        }
        return listeners.size() > 0;
    }

    public boolean keyTyped (char character) {
        for ( int i = 0; i < listeners.size(); i++ ) {
            KeyInputListener listener = listeners.get( i );
            listener.onType(character);
        }
        return listeners.size() > 0;
    }

    @Override
    public void update() {
    }

    @Override
    public void destroy() {
    }
}
