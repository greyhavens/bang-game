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

import com.jme.input.MouseInput;
import com.jme.input.MouseInputListener;

public class GDXMouseInput extends MouseInput {

    public boolean touchDown (int screenX, int screenY, int pointer, int button) {
        int invY = Gdx.graphics.getHeight() - screenY;
        for ( int i = 0; i < listeners.size(); i++ ) {
            MouseInputListener listener = listeners.get( i );
            listener.onButton( button,  true, screenX, invY );
        }
        return listeners.size() > 0;
    }

    public boolean touchUp (int screenX, int screenY, int pointer, int button) {
        int invY = Gdx.graphics.getHeight() - screenY;
        for ( int i = 0; i < listeners.size(); i++ ) {
            MouseInputListener listener = listeners.get( i );
            listener.onButton( button,  false, screenX, invY );
        }
        return listeners.size() > 0;
    }

    public boolean touchDragged (int screenX, int screenY, int pointer) {
        int invY = Gdx.graphics.getHeight() - screenY;
        for ( int i = 0; i < listeners.size(); i++ ) {
            MouseInputListener listener = listeners.get( i );
            listener.onMove( screenX, invY );
        }
        return listeners.size() > 0;
    }
    public boolean mouseMoved (int screenX, int screenY) {
        int invY = Gdx.graphics.getHeight() - screenY;
        for ( int i = 0; i < listeners.size(); i++ ) {
            MouseInputListener listener = listeners.get( i );
            listener.onMove( screenX, invY );
        }
        return listeners.size() > 0;
    }

    public boolean scrolled (int amount) {
        for ( int i = 0; i < listeners.size(); i++ ) {
            MouseInputListener listener = listeners.get( i );
            listener.onWheel( amount, getXAbsolute(), getYAbsolute() );
        }
        return listeners.size() > 0;
    }

    @Override
    public boolean isButtonDown(int buttonCode) {
        return Gdx.input.isButtonPressed(buttonCode);
    }

    @Override
    public int getXDelta() {
        return Gdx.input.getDeltaX();
    }

    @Override
    public int getYDelta() {
        return Gdx.input.getDeltaY(); // TODO: invert?
    }

    @Override
    public int getXAbsolute() {
        return Gdx.input.getX();
    }

    @Override
    public int getYAbsolute() {
        return Gdx.graphics.getHeight() - Gdx.input.getY();
    }

    @Override
    public void setCursorVisible(boolean v) {
        Gdx.input.setCursorCatched(!v);
    }

    @Override
    public boolean isCursorVisible() {
        return !Gdx.input.isCursorCatched();
    }

    @Override
    public int getButtonCount() {
        return 1;
    }

    @Override
    public void setCursorPosition( int x, int y) {
        Gdx.input.setCursorPosition(x, y);
    }

    @Override
    public void update() {
    }

    @Override
    public void destroy() {
    }
}
