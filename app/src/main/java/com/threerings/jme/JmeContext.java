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

package com.threerings.jme;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;

import com.jmex.bui.BRootNode;

import com.threerings.jme.camera.CameraHandler;
import com.threerings.jme.camera.GodViewHandler;

/**
 * Provides access to the various bits needed by things that operate in jME land.
 *
 * <p>jME3 cutover (Phase 1): retyped off the fork. jME3 has no global {@code DisplaySystem}
 * service locator (§2.5 / risk #6 of the migration map) — the application owns the
 * {@link AssetManager}, {@link RenderManager} and {@link Camera}, so they are surfaced here as
 * the threaded-through accessors that replace {@code getDisplaySystem()...}: {@code getDisplay()}
 * → {@link #getRenderManager()} + {@link #getCamera()} + {@link #getAssetManager()}, and the
 * fork {@code Renderer} → jME3 {@link RenderManager}. Input ({@code getInputHandler}) is dropped
 * here; the jME3 {@code InputManager}/{@code RawInputListener} path is wired at the Phase-3 host
 * flip. The implementers (BangApp/EditorApp/BasicContext in client/shared) re-fit to this seam
 * in Phase 2.
 */
public interface JmeContext
{
    /** Returns the asset manager used to load textures, models and materials. */
    public AssetManager getAssetManager ();

    /** Returns the render manager driving the viewports. */
    public RenderManager getRenderManager ();

    /** Returns the camera being used to view the scene. */
    public Camera getCamera ();

    /** Returns the handler for the camera being used to view the scene. */
    public CameraHandler getCameraHandler ();

    /**
     * Returns the input handler (a {@link GodViewHandler}, replacing the fork's polled
     * {@code InputHandler}). The object exists in Phase 2 so camera-control logic
     * (enable/disable, round setup) keeps working; its jME3 {@code InputManager} mappings
     * are wired at the Phase-3 host flip via {@link GodViewHandler#registerWith}.
     */
    public GodViewHandler getInputHandler ();

    /** Returns the main geometry of our scene graph. */
    public Node getGeometry ();

    /** Returns the main interface node of our scene graph. */
    public Node getInterface ();

    /** Returns our UI root node. */
    public BRootNode getRootNode ();
}
