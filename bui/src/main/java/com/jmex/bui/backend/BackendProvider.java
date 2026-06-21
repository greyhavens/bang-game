//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.backend;

/**
 * Provides the render backend in use by BUI.
 *
 * <p> As of the jME3 cutover (Phase 1) the only backend is {@link Jme3RenderBackend}. It
 * cannot be created lazily because it needs the host application's
 * {@code com.jme3.asset.AssetManager}, so the host must install it (via {@link #set}) before
 * any BUI component is rendered (and before {@link #get} is first called).
 */
public class BackendProvider
{
    /**
     * Returns the active render backend.
     *
     * @throws IllegalStateException if no backend has been installed via {@link #set}.
     */
    public static BRenderBackend get ()
    {
        if (_backend == null) {
            throw new IllegalStateException(
                "No BUI render backend installed. The jME3 host must call " +
                "BackendProvider.set(new Jme3RenderBackend(assetManager)) before rendering.");
        }
        return _backend;
    }

    /**
     * Installs the render backend. Must be called before any BUI component is rendered.
     */
    public static void set (BRenderBackend backend)
    {
        _backend = backend;
    }

    protected static BRenderBackend _backend;
}
