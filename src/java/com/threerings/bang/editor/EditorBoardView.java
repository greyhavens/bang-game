//
// $Id$

package com.threerings.bang.editor;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.image.BufferedImage;

import java.nio.FloatBuffer;

import java.util.Iterator;

import com.jme.math.FastMath;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Line;
import com.jme.scene.state.LightState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.WireframeState;
import com.jme.util.geom.BufferUtils;

import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseListener;
import com.jmex.bui.event.MouseWheelListener;

import com.jmex.terrain.util.AbstractHeightMap;
import com.jmex.terrain.util.FaultFractalHeightMap;
import com.jmex.terrain.util.MidPointHeightMap;
import com.jmex.terrain.util.ParticleDepositionHeightMap;

import com.threerings.jme.sprite.Sprite;

import com.threerings.util.RandomUtil;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.Terrain;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.PieceDSet;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.editor.EditorContext;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the board when in editor mode.
 */
public class EditorBoardView extends BoardView
{
    public EditorBoardView (BasicContext ctx, EditorPanel panel)
    {
        super(ctx);
        _panel = panel;
        addListener(this);

        // put piece sprites in editor mode
        PieceSprite.setEditorMode(true);
    }

    @Override // documentation inherited
    public void refreshBoard ()
    {
        super.refreshBoard();
        
        // recenter the camera
        _panel.tools.cameraDolly.recenter();
        
        // make sure highlights and grid are reset to new size
        _hnode.detachAllChildren();
        _highlights = null;
        _grid = null;
        updateHighlights();
        updateGrid();
    }
    
    /**
     * Activates or deactivates wireframe rendering.
     */
    public void toggleWireframe ()
    {
        WireframeState wstate = (WireframeState)_node.getRenderState(
            RenderState.RS_WIREFRAME);
        if (wstate == null) {
            wstate = _ctx.getRenderer().createWireframeState();
            wstate.setFace(WireframeState.WS_FRONT_AND_BACK);
            _node.setRenderState(wstate);
            
        } else {
            wstate.setEnabled(!wstate.isEnabled());
        }
        _node.updateRenderState();
    }
    
    /**
     * Shows or hides the tile grid.
     */
    public void toggleGrid ()
    {
        _showGrid = !_showGrid;
        updateGrid();
    }
    
    /**
     * Shows or hides the unoccupiable tile highlights.
     */
    public void toggleHighlights ()
    {
        _showHighlights = !_showHighlights;
        updateHighlights();
    }

    /**
     * Sets the heightfield to the contents of the specified image.
     */
    public void setHeightfield (BufferedImage image)
    {
        // scale the image to the size of the heightfield, flip it upside down,
        // and convert it to 8-bit grayscale
        int hfwidth = _board.getHeightfieldWidth(),
            hfheight = _board.getHeightfieldHeight();
        BufferedImage grayimg = new BufferedImage(hfwidth, hfheight,
            BufferedImage.TYPE_BYTE_GRAY);
        grayimg.createGraphics().drawImage(image, 0, hfheight, hfwidth,
            0, 0, 0, image.getWidth(), image.getHeight(), null);
        
        // transfer the pixels to the heightfield array
        int[] vals = grayimg.getData().getPixels(0, 0, hfwidth, hfheight,
            (int[])null);
        byte[] hf = _board.getHeightfield();
        for (int i = 0; i < hf.length; i++) {
            hf[i] = (byte)(vals[i] - 128);
        }
        
        heightfieldChanged();
    }
    
    /**
     * Creates and returns an image representation of the heightfield.
     */
    public BufferedImage getHeightfieldImage ()
    {
        int hfwidth = _board.getHeightfieldWidth(),
            hfheight = _board.getHeightfieldHeight();
        BufferedImage grayimg = new BufferedImage(hfwidth, hfheight,
            BufferedImage.TYPE_BYTE_GRAY);
        
        // transfer the heightfield values to the bitmap one line at a time
        // upside-down
        int[] vals = new int[hfwidth];
        for (int y = 0; y < hfheight; y++) {
            for (int x = 0; x < hfwidth; x++) {
                vals[x] = _board.getHeightfieldValue(x, y) + 128;
            }
            grayimg.getRaster().setPixels(0, hfheight-y-1, hfwidth, 1, vals);
        }

        return grayimg;
    }
    
    /**
     * Paints a the circle specified in node space coordinates with the given
     * terrain.
     */
    public void paintTerrain (float x, float y, float radius, Terrain terrain)
    {
        byte code = (byte)terrain.code;
        
        // find the boundaries of the circle in sub-tile coordinates
        float stscale = TILE_SIZE / BangBoard.HEIGHTFIELD_SUBDIVISIONS,
            rr = radius*radius;
        int x1 = (int)((x-radius)/stscale), y1 = (int)((y-radius)/stscale),
            x2 = (int)((x+radius)/stscale), y2 = (int)((y+radius)/stscale);
        
        x1 = clamp(x1, 0, _board.getHeightfieldWidth() - 1);
        x2 = clamp(x2, 0, _board.getHeightfieldWidth() - 1);
        y1 = clamp(y1, 0, _board.getHeightfieldHeight() - 1);
        y2 = clamp(y2, 0, _board.getHeightfieldHeight() - 1);
        
        // scan over the sub-tile coordinates, setting any that fall in the
        // circle
        Vector2f vec = new Vector2f();
        for (int ty = y1; ty <= y2; ty++) {
            for (int tx = x1; tx <= x2; tx++) {
                vec.set(x - tx*stscale, y - ty*stscale);
                if (vec.lengthSquared() <= rr) {
                    _board.setTerrainValue(tx, ty, code);
                }
            }
        }
        
        // update the terrain splats
        _tnode.refreshTerrain(x1, y1, x2, y2);
    }
    
    /**
     * Paints a circle of values into the heightfield, either raising/lowering
     * the values or setting them directly.
     *
     * @param add if true, add to the existing heightfield values; if false,
     * just set the values
     */
    public void paintHeightfield (float x, float y, float radius, int value,
        boolean add)
    {
        // find the boundaries of the circle in sub-tile coordinates
        float stscale = TILE_SIZE / BangBoard.HEIGHTFIELD_SUBDIVISIONS,
            rr = radius*radius;
        int x1 = (int)((x-radius)/stscale), y1 = (int)((y-radius)/stscale),
            x2 = (int)((x+radius)/stscale), y2 = (int)((y+radius)/stscale);
        
        x1 = clamp(x1, 0, _board.getHeightfieldWidth() - 1);
        x2 = clamp(x2, 0, _board.getHeightfieldWidth() - 1);
        y1 = clamp(y1, 0, _board.getHeightfieldHeight() - 1);
        y2 = clamp(y2, 0, _board.getHeightfieldHeight() - 1);
        
        // scan over the sub-tile coordinates, setting any that fall in the
        // circle
        Vector2f vec = new Vector2f();
        for (int ty = y1; ty <= y2; ty++) {
            for (int tx = x1; tx <= x2; tx++) {
                vec.set(x - tx*stscale, y - ty*stscale);
                if (add) {
                    float w = 1.0f - vec.lengthSquared()/rr;
                    if (w > 0.0f) {
                        _board.addHeightfieldValue(tx, ty,
                            Math.round(w*value));
                    }
                    
                } else if (vec.lengthSquared() <= rr) {
                    _board.setHeightfieldValue(tx, ty, (byte)value);
                }
            }
        }
        
        // update the heightfield bits
        heightfieldChanged(x1 - 1, y1 - 1, x2 + 1, y2 + 1);
    }
    
    /**
     * Adds some random noise to the heightfield (just enough to create some
     * interesting texture.
     */
    public void addHeightfieldNoise ()
    {
        int width = _board.getHeightfieldWidth(),
            height = _board.getHeightfieldHeight();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                _board.addHeightfieldValue(x, y, RandomUtil.getInt(+2, -2));
            }
        }
        
        heightfieldChanged();
    }
    
    /**
     * Smooths the heightfield using a simple blur.
     */
    public void smoothHeightfield ()
    {
        byte[] smoothed = new byte[_board.getHeightfield().length];
        
        int width = _board.getHeightfieldWidth(),
            height = _board.getHeightfieldHeight(), idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // average this pixel and its eight neighbors
                smoothed[idx++] = (byte)
                    (((int)_board.getHeightfieldValue(x-1, y-1) +
                    _board.getHeightfieldValue(x, y-1) +
                    _board.getHeightfieldValue(x+1, y-1) +
                    _board.getHeightfieldValue(x-1, y) +
                    _board.getHeightfieldValue(x, y) +
                    _board.getHeightfieldValue(x+1, y) +
                    _board.getHeightfieldValue(x-1, y+1) +
                    _board.getHeightfieldValue(x, y+1) +
                    _board.getHeightfieldValue(x+1, y+1)) / 9);
            }
        }
        
        System.arraycopy(smoothed, 0, _board.getHeightfield(), 0,
            smoothed.length);
        heightfieldChanged();
    }
    
    /**
     * Generates a heightfield using JME's midpoint displacement class.
     */
    public void generateMidpointDisplacement (float roughness)
    {
        int size = RenderUtil.nextPOT(Math.max(_board.getHeightfieldWidth(),
            _board.getHeightfieldHeight()));
        setHeightfield(new MidPointHeightMap(size, roughness));
    }
    
    /**
     * Generates a heightfield using JME's fault fractal class.
     */
    public void generateFaultFractal (int iterations, int minDelta,
        int maxDelta, float filter)
    {
        int size = RenderUtil.nextPOT(Math.max(_board.getHeightfieldWidth(),
            _board.getHeightfieldHeight()));
        setHeightfield(new FaultFractalHeightMap(size, iterations, minDelta,
            maxDelta, filter));
    }
    
    /**
     * Generates a heightfield using JME's particle deposition class.
     */
    public void generateParticleDeposition (int jumps, int peakWalk,
        int minParticles, int maxParticles, float caldera)
    {
        int size = RenderUtil.nextPOT(Math.max(_board.getHeightfieldWidth(),
            _board.getHeightfieldHeight()));
        setHeightfield(new ParticleDepositionHeightMap(size, jumps, peakWalk,
            minParticles, maxParticles, caldera));
    }
    
    /**
     * Sets the board's water level.
     */
    public void setWaterLevel (int level)
    {
        _board.setWaterLevel((byte)level);
        _wnode.refreshSurface();
    }
    
    /**
     * Sets the parameters of the board's light.
     */
    public void setLightParams (float azimuth, float elevation,
        int diffuseColor, int ambientColor)
    {
        _board.setLightParams(azimuth, elevation, diffuseColor, ambientColor);
        refreshLight();
    }
    
    /**
     * Creates a fresh new board.
     */
    public void createNewBoard (int width, int height)
    {
        _bangobj.setBoard(new BangBoard(width, height));
        _bangobj.board.fillTerrain(Terrain.DIRT);
        _bangobj.setPieces(new PieceDSet());
        refreshBoard();
        _panel.info.clear();
        _panel.info.updatePlayers(0);
    }
    
    /**
     * Changes the board size, preserving as much of its contents as possible.
     */
    public void changeBoardSize (int width, int height)
    {
        // make sure it's not the same size
        if (width == _board.getWidth() && height == _board.getHeight()) {
            return;
        }
        
        // first transfer the board
        BangBoard nboard = new BangBoard(width, height);
        int hfwidth = nboard.getHeightfieldWidth(),
            hfheight = nboard.getHeightfieldHeight(),
            xoff = (_board.getHeightfieldWidth() - hfwidth)/2,
            yoff = (_board.getHeightfieldHeight() - hfheight)/2;
        for (int y = 0; y < hfheight; y++) {
            for (int x = 0; x < hfwidth; x++) {
            
                nboard.setHeightfieldValue(x, y,
                    _board.getHeightfieldValue(x+xoff, y+yoff));
                
                nboard.setTerrainValue(x, y,
                    _board.getTerrainValue(x+xoff, y+yoff));
            }
        }
        nboard.setWaterLevel(_board.getWaterLevel());
        nboard.setLightParams(_board.getLightAzimuth(),
            _board.getLightElevation(), _board.getLightDiffuseColor(),
            _board.getLightAmbientColor());
        _bangobj.setBoard(nboard);
        
        // then move the pieces
        xoff = (width - _board.getWidth())/2;
        yoff = (height - _board.getHeight())/2;
        for (Iterator it = _bangobj.pieces.iterator(); it.hasNext(); ) {
            Piece piece = (Piece)it.next();
            piece.position(piece.x + xoff, piece.y + yoff);
        }
        
        // finally, refresh
        refreshBoard();
    }

    /**
     * Returns the piece associated with the sprite under the mouse, if
     * there is one and if it is a piece sprite. Returns null otherwise.
     */
    public Piece getHoverPiece ()
    {
        int pid = (_hover instanceof PieceSprite) ?
            ((PieceSprite)_hover).getPieceId() : -1;
        return (Piece)_bangobj.pieces.get(pid);
    }

    /**
     * Sets the heightfield to the contents of the given JME height map (whose
     * size must be equal to or greater than that of the heightfield).
     *
     * @param above whether or not to 
     */
    protected void setHeightfield (AbstractHeightMap map)
    {
        int width = _board.getHeightfieldWidth(),
            height = _board.getHeightfieldHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                _board.setHeightfieldValue(x, y,
                    (byte)(map.getTrueHeightAtPoint(x, y) - 128));
            }
        }
        
        heightfieldChanged();
    }
    
    /**
     * Called when the entire heightfield has changed.
     */
    protected void heightfieldChanged ()
    {
        _tnode.refreshHeightfield();
        _wnode.refreshSurface();
        updatePieces();
        updateGrid();
        updateHighlights();
    }
    
    /**
     * Called when part of the heightfield (as specified in sub-tile
     * coordinates) has changed.
     */
    protected void heightfieldChanged (int x1, int y1, int x2, int y2)
    {
        int txmax = _board.getWidth() - 1, tymax = _board.getHeight() - 1,
            tx1 = clamp(x1 / BangBoard.HEIGHTFIELD_SUBDIVISIONS, 0, txmax),
            ty1 = clamp(y1 / BangBoard.HEIGHTFIELD_SUBDIVISIONS, 0, tymax),
            tx2 = clamp(x2 / BangBoard.HEIGHTFIELD_SUBDIVISIONS, 0, txmax),
            ty2 = clamp(y2 / BangBoard.HEIGHTFIELD_SUBDIVISIONS, 0, tymax);
        _tnode.refreshHeightfield(x1, y1, x2, y2);
        _wnode.refreshSurface(tx1, ty1, tx2, ty2);
        updatePieces();
        updateGrid();
        updateHighlights(tx1, ty1, tx2, ty2);
    }
    
    /**
     * Clamps v between a and b (inclusive).
     */
    protected int clamp (int v, int a, int b)
    {
        return Math.min(Math.max(v, a), b);
    }
    
    /**
     * Updates all the pieces in response to a change in terrain.
     */
    protected void updatePieces ()
    {
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece piece = (Piece)iter.next();
            pieceUpdated(piece, piece);
        }
    }
    
    /**
     * Updates the tile grid over the entire board.
     */
    protected void updateGrid ()
    {
        if (_showGrid) {
            if (_grid == null) {
                _grid = new TileGrid();
            }
            _grid.updateVertices();
            if (_grid.getParent() == null) {
                _hnode.attachChild(_grid);
            }

        } else if (_grid != null && _grid.getParent() != null) {
            _hnode.detachChild(_grid);
        }
    }
    
    /**
     * Updates the highlights over the entire board.
     */
    protected void updateHighlights ()
    {
        updateHighlights(0, 0, _board.getWidth() - 1, _board.getHeight() - 1);
    }
    
    /**
     * Updates the highlights over the specified tile coordinate rectangle.
     */
    protected void updateHighlights (int x1, int y1, int x2, int y2)
    {
        if (_highlights == null) {
            _highlights = new TerrainNode.Highlight[_board.getWidth()][
                _board.getHeight()];
        }
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (_showHighlights && _board.exceedsMaxHeightDelta(x, y)) {
                    if (_highlights[x][y] == null) {
                        _highlights[x][y] = _tnode.createHighlight(x, y,
                            false);
                        _highlights[x][y].setDefaultColor(HIGHLIGHT_COLOR);
                    }
                    _highlights[x][y].updateVertices();
                    if (_highlights[x][y].getParent() == null) {
                        _hnode.attachChild(_highlights[x][y]);
                    }
                    
                } else if (_highlights[x][y] != null &&
                    _highlights[x][y].getParent() != null) {
                    _hnode.detachChild(_highlights[x][y]);
                }
            }
        }
    }
    
    @Override // documentation inherited
    protected void hoverTileChanged (int tx, int ty)
    {
        super.hoverTileChanged(tx, ty);
        _panel.tools.getActiveTool().hoverTileChanged(tx, ty);
    }

    @Override // documentation inherited
    protected void hoverSpriteChanged (Sprite hover)
    {
        super.hoverSpriteChanged(hover);
        _panel.tools.getActiveTool().hoverSpriteChanged(hover);
    }

    /** A grid indicating where the tile boundaries lie. */
    protected class TileGrid extends Line
    {
        public TileGrid ()
        {
            super("grid");
            
            setDefaultColor(ColorRGBA.gray);
            setLightCombineMode(LightState.OFF);
            setRenderState(RenderUtil.overlayZBuf);
            updateRenderState();
            
            int vertices = (_board.getHeight() + 1) *
                (_board.getHeightfieldWidth() - 1) * 2 +
                (_board.getWidth() + 1) *
                (_board.getHeightfieldHeight() - 1) * 2;
            setVertexBuffer(BufferUtils.createFloatBuffer(vertices * 3));
            generateIndices();
            
            updateVertices();
        }
        
        /**
         * Updates the vertices of the grid when the heightfield changes.
         */
        public void updateVertices ()
        {
            Vector3f vertex = new Vector3f();
            FloatBuffer vbuf = getVertexBuffer();
            int idx = 0;
            
            // horizontal grid lines
            for (int ty = 0, height = _board.getHeight(); ty <= height; ty++) {
                int y = ty * BangBoard.HEIGHTFIELD_SUBDIVISIONS;
                for (int x = 0, width = _board.getHeightfieldWidth() - 1;
                        x < width; x++) {
                    _tnode.getHeightfieldVertex(x, y, vertex);
                    vertex.z += 0.1f;
                    BufferUtils.setInBuffer(vertex, vbuf, idx++);
                    
                    _tnode.getHeightfieldVertex(x + 1, y, vertex);
                    vertex.z += 0.1f;
                    BufferUtils.setInBuffer(vertex, vbuf, idx++);
                }
            }
            
            // vertical grid lines
            for (int tx = 0, width = _board.getWidth(); tx <= width; tx++) {
                int x = tx * BangBoard.HEIGHTFIELD_SUBDIVISIONS;
                for (int y = 0, height = _board.getHeightfieldHeight() - 1;
                        y < height; y++) {
                    _tnode.getHeightfieldVertex(x, y, vertex);
                    vertex.z += 0.1f;
                    BufferUtils.setInBuffer(vertex, vbuf, idx++);
                    
                    _tnode.getHeightfieldVertex(x, y + 1, vertex);
                    vertex.z += 0.1f;
                    BufferUtils.setInBuffer(vertex, vbuf, idx++);
                }
            }
        }
    }
    
    /** The panel that contains additional interface elements with which
     * we interact. */
    protected EditorPanel _panel;

    /** Highlights indicating which tiles are occupiable. */
    protected TerrainNode.Highlight[][] _highlights;
    
    /** The grid indicating where the tile boundaries lie. */
    protected TileGrid _grid;
    
    /** Whether or not to show the tile grid. */
    protected boolean _showGrid;
    
    /** Whether or not to show the highlights. */
    protected boolean _showHighlights;
    
    /** The color to use for highlights. */
    protected static final ColorRGBA HIGHLIGHT_COLOR =
        new ColorRGBA(1f, 0f, 0f, 0.25f);
}
