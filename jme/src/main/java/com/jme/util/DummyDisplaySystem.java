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

import java.awt.Canvas;
import java.net.URL;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import com.jme.image.Texture;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.RenderContext;
import com.jme.renderer.RenderQueue;
import com.jme.renderer.Renderer;
import com.jme.renderer.TextureRenderer;
import com.jme.scene.SceneElement;
import com.jme.scene.Spatial;
import com.jme.scene.batch.GeomBatch;
import com.jme.scene.batch.LineBatch;
import com.jme.scene.batch.PointBatch;
import com.jme.scene.batch.QuadBatch;
import com.jme.scene.batch.TriangleBatch;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.AttributeState;
import com.jme.scene.state.ClipState;
import com.jme.scene.state.ColorMaskState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.DitherState;
import com.jme.scene.state.FogState;
import com.jme.scene.state.FragmentProgramState;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.ShadeState;
import com.jme.scene.state.StateRecord;
import com.jme.scene.state.StencilState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.VertexProgramState;
import com.jme.scene.state.WireframeState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;
import com.jme.system.SystemProvider;
import com.jmex.awt.JMECanvas;

/**
 * Started Date: Jul 2, 2004 <br>
 * <br>
 * <p/>
 * This class is a dummy class that is not suppost to be rendered. It is here to
 * allow the easy creation of dummy jME objects (like various RenderState and
 * Spatial) so that they can be used by conversion utilities to read/write jME.
 * It is <b>NOT </b> to be used for rendering as it won't do anything at all.
 *
 * @author Jack Lindamood
 */
public class DummyDisplaySystem extends DisplaySystem {

    public DummyDisplaySystem() {
        system = new SystemProvider() {
            @Override
            public String getProviderIdentifier() {
                return "dummy";
            }

            @Override
            public DisplaySystem getDisplaySystem() {
                return DummyDisplaySystem.this;
            }

            @Override
            public Timer getTimer() {
                return Timer.getTimer();
            }
        };
    }

    @Override
    public boolean isValidDisplayMode( int width, int height, int bpp, int freq ) {
        return false;
    }

    @Override
    public void setIcon(com.jme.image.Image[] iconImages) {
    }

    @Override
    public void setVSyncEnabled( boolean enabled ) {
    }

    @Override
    public void setTitle( String title ) {
    }

    @Override
    public void createWindow( int w, int h, int bpp, int frq, boolean fs ) {
    }

    @Override
    public void createHeadlessWindow( int w, int h, int bpp ) {
    }

    @Override
    public void recreateWindow( int w, int h, int bpp, int frq, boolean fs ) {
    }

    @Override
    public Renderer getRenderer() {
        return new Renderer() {

            @Override
            public void setCamera( Camera camera ) {
            }

            @Override
            public Camera createCamera( int width, int height ) {
                return null;
            }

            @Override
            public AlphaState createAlphaState() {
                return new AlphaState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void apply() {
                    }

                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public void flush() {
            }

            @Override
            public AttributeState createAttributeState() {
                return new AttributeState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void apply() {
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public CullState createCullState() {
                return new CullState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void apply() {
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public DitherState createDitherState() {
                return new DitherState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void apply() {
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public FogState createFogState() {
                return new FogState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void apply() {
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public LightState createLightState() {
                return new LightState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void apply() {
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public MaterialState createMaterialState() {
                return new MaterialState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void apply() {
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public ShadeState createShadeState() {
                return new ShadeState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void apply() {
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            class TextureStateN extends TextureState {

                private static final long serialVersionUID = 1L;

                TextureStateN() {
                    numTotalTexUnits = 1;
                    texture = new ArrayList<Texture>(1);
                }

                @Override
                public void load( int unit ) {
                }

                @Override
                public void delete( int unit ) {
                }

                @Override
                public void deleteAll() {
                }

                @Override
                public void deleteAll(boolean removeFromCache) {
                }

                @Override
                public void apply() {
                }
                @Override
                public StateRecord createStateRecord() { return null; }
            }

            @Override
            public TextureState createTextureState() {
                return new TextureStateN();
            }

            @Override
            public WireframeState createWireframeState() {
                return new WireframeState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void apply() {
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public ZBufferState createZBufferState() {
                return new ZBufferState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void apply() {
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public VertexProgramState createVertexProgramState() {
                return new VertexProgramState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isSupported() {
                        return false;
                    }

                    @Override
                    public void load( URL file ) {
                    }

                    @Override
                    public void load( String contents ) {
                    }

                    @Override
                    public void apply() {
                    }

                    @Override
                    public String getProgram() {
                        return null;
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public FragmentProgramState createFragmentProgramState() {
                return new FragmentProgramState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isSupported() {
                        return false;
                    }

                    @Override
                    public void load( URL file ) {
                    }

                    @Override
                    public void load( String contents ) {
                    }

                    @Override
                    public void apply() {
                    }

                    @Override
                    public String getProgram() {
                        return null;
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public GLSLShaderObjectsState createGLSLShaderObjectsState() {
                return new GLSLShaderObjectsState() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isSupported() {
                        return false;
                    }

                    @Override
                    public void load( URL vert, URL frag ) {
                    }

                    @Override
                    public void load(String vert, String frag) {

                    }

                    @Override
                    public void apply() {
                    }

                    @Override
                    public void relinkProgram() {
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public StencilState createStencilState() {
                return new StencilState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void apply() {
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public ClipState createClipState() {
                return new ClipState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void apply() {
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public ColorMaskState createColorMaskState() {
                return new ColorMaskState() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void apply() {
                    }
                    @Override
                    public StateRecord createStateRecord() { return null; }
                };
            }

            @Override
            public void enableStatistics( boolean value ) {
            }

            @Override
            public void clearStatistics() {
            }

            @Override
            public void setBackgroundColor( ColorRGBA c ) {
            }

            @Override
            public ColorRGBA getBackgroundColor() {
                return null;
            }

            @Override
            public void clearZBuffer() {
            }

            @Override
            public void clearColorBuffer() {
            }

            @Override
            public void clearStencilBuffer() {
            }

            @Override
            public void clearBuffers() {
            }

            @Override
            public void clearStrictBuffers() {
            }

            @Override
            public void displayBackBuffer() {
            }

            @Override
            public void setOrtho() {
            }

            @Override
            public void setOrthoCenter() {
            }

            @Override
            public void unsetOrtho() {
            }

            @Override
            public boolean takeScreenShot( String filename ) {
                return false;
            }

            @Override
            public void grabScreenContents( IntBuffer buff, int x, int y, int w,
                                            int h ) {
            }

            @Override
            public void draw( Spatial s ) {
            }

            @Override
            public void draw( PointBatch batch ) {
            }

            @Override
            public void draw( LineBatch batch ) {
            }

            @Override
            public RenderQueue getQueue() {
                return null;
            }

            @Override
            public boolean isProcessingQueue() {
                return false;
            }

            @Override
            public boolean checkAndAdd( SceneElement s ) {
                return false;
            }

            @Override
            public boolean supportsVBO() {
                return false;
            }

            @Override
            public boolean isHeadless() {
                return false;
            }

            @Override
            public void setHeadless( boolean headless ) {
            }

            @Override
            public int getWidth() {
                return -1;
            }

            @Override
            public int getHeight() {
                return -1;
            }

            @Override
            public void reinit( int width, int height ) {
            }

            @Override
            public int createDisplayList( GeomBatch g ) {
                return -1;
            }

            @Override
            public void releaseDisplayList( int listId ) {
            }

            @Override
            public void setPolygonOffset( float factor, float offset ) {
            }

            @Override
            public void clearPolygonOffset() {
            }

            @Override
            public void deleteVBO( Buffer buffer ) {

            }

            @Override
            public void deleteVBO( int vboid ) {

            }

            @Override
            public void clearVBOCache() {

            }

            @Override
            public Integer removeFromVBOCache( Buffer buffer ) {
                return null;
            }

            @Override
            public void draw(TriangleBatch batch) {
            }

            @Override
            public void draw(QuadBatch batch) {
            }

            @Override
            public StateRecord createLineRecord() {
                return null;
            }
        };
    }

    @Override
    public boolean isClosing() {
        return false;
    }

    @Override
    public boolean isActive()
    {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void close() {
    }

    @Override
    public Vector3f getScreenCoordinates( Vector3f worldPosition, Vector3f store ) {
        return null;
    }

    @Override
    public Vector3f getWorldCoordinates( Vector2f screenPosition, float zPos,
                                         Vector3f store ) {
        return null;
    }

    @Override
    public void setRenderer( Renderer r ) {
    }

    @Override
    public Canvas createCanvas( int w, int h ) {
        return null;
    }

    @Override
    public TextureRenderer createTextureRenderer( int width, int height,
                                                  boolean useRGB, boolean useRGBA, boolean useDepth,
                                                  boolean isRectangle, int target, int mipmaps ) {
        return null;
    }

    @Override
    public TextureRenderer createTextureRenderer( int width, int height,
                                                  boolean useRGB, boolean useRGBA, boolean useDepth,
                                                  boolean isRectangle, int target, int mipmaps, int bpp, int alpha,
                                                  int depth, int stencil, int samples ) {
        return null;
    }

    @Override
    protected void updateDisplayBGC() { }

    @Override
    public String getAdapter() {
        return null;
    }

    @Override
    public String getDriverVersion() {
        return null;
    }

    @Override
    public void setCurrentCanvas(JMECanvas canvas) { }

    @Override
    public RenderContext getCurrentContext() {
        return null;
    }
}
