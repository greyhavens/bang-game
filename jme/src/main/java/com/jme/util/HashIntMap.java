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

package com.jme.util;

/**
 * A simple hash map with integer keys.  Using this class instead of a
 * {@link java.util.HashMap} with {@link Integer} keys eliminates the garbage
 * created by auto-boxing.  Uses a linear search, grows by powers of two, never
 * shrinks.
 */
public class HashIntMap<T>
{
    public HashIntMap ()
    {
        resize(INITIAL_CAPACITY);
    }

    /**
     * Gets an entry from the map, returning <code>null</code> if no such entry
     * exists.
     */
    public T get (int key)
    {
        int idx = getIndex(key);
        return (idx == -1) ? null : _entries[idx].value;
    }

    /**
     * Inserts an entry into the map, returning the previous mapping (if any).
     */
    public T put (int key, T value)
    {
        int hash = (key & _mask), ii = hash;
        Entry<T> entry;
        do {
            if ((entry = _entries[ii]) == null) {
                break;
            } else if (entry.key == key) {
                T ovalue = entry.value;
                entry.value = value;
                return ovalue;
            }
        } while ((ii = (ii + 1) & _mask) != hash);

        if (_size < _threshold) {
            _entries[ii] = new Entry<T>(key, value);
            _size++;
        } else {
            resize(_capacity + 1);
            put(key, value);
        }
        return null;
    }

    /**
     * Removes an entry from the map, returning its mapping (or <code>null</code> if
     * it wasn't in the map).
     */
    public T remove (int key)
    {
        int idx = getIndex(key);
        if (idx == -1) {
            return null;
        }
        T value = _entries[idx].value;
        _entries[idx] = null;
        _size--;
        return value;
    }

    protected int getIndex (int key)
    {
        int hash = (key & _mask), ii = hash;
        Entry<T> entry;
        do {
            if ((entry = _entries[ii]) == null) {
                return -1;
            } else if (entry.key == key) {
                return ii;
            }
        } while ((ii = (ii + 1) & _mask) != hash);
        return -1;
    }

    @SuppressWarnings("unchecked")
    protected void resize (int ncapacity)
    {
        Entry<T>[] oentries = _entries;

        _capacity = ncapacity;
        _entries = (Entry<T>[])new Entry<?>[1 << _capacity];
        _threshold = (_entries.length * 3) / 4;
        _mask = _entries.length - 1;

        if (oentries == null) {
            return;
        }
        for (Entry<T> entry : oentries) {
            if (entry == null) {
                continue;
            }
            int hash = (entry.key & _mask), ii = hash;
            do {
                if (_entries[ii] == null) {
                    _entries[ii] = entry;
                    break;
                }
            } while ((ii = (ii + 1) & _mask) != hash);
        }
    }

    protected static class Entry<T>
    {
        public int key;
        public T value;

        Entry (int key, T value)
        {
            this.key = key;
            this.value = value;
        }
    }

    protected int _capacity, _size, _threshold, _mask;
    protected Entry<T>[] _entries;

    /** The initial capacity of the map (as a power of two). */
    protected static final int INITIAL_CAPACITY = 4;
}
