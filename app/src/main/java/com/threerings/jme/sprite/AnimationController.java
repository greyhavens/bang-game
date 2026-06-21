//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.jme.sprite;

/**
 * jME3 cutover (Phase 1): the fork's <code>com.jme.scene.Controller</code> exposed
 * <code>setSpeed</code>/<code>setActive</code>/<code>setRepeatType</code>, which {@link
 * Sprite#setAnimationSpeed} and friends used to drive every controller in a model hierarchy.
 * jME3 <code>Control</code>s carry no such flags, so the app-side controls that want to honour
 * those whole-tree commands ({@link Path}, and the model facade's procedural controls) implement
 * this small interface; {@code Sprite}'s walkers apply to any control that does.
 */
public interface AnimationController
{
    /** Sets the speed multiplier applied to the control's time delta. */
    void setSpeed (float speed);

    /** Sets whether the control is active. */
    void setActive (boolean active);

    /** Sets the control's repeat type (see {@link com.threerings.jme.util.JmeUtil}). */
    void setRepeatType (int repeatType);
}
