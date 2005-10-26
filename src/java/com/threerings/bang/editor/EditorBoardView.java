//
// $Id$

package com.threerings.bang.editor;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.image.BufferedImage;

import java.util.Iterator;

import com.jme.math.FastMath;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;

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
        
        if (_highlights == null) {
            int width = _board.getWidth(), height = _board.getHeight();
            _highlights = new TerrainNode.Highlight[width][height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    _highlights[x][y] = _tnode.createHighlight(x, y, false);
                    _highlights[x][y].setDefaultColor(HIGHLIGHT_COLOR);
                }
            }
        }
        updateHighlights();
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
        
        // update the terrain geometry and update the pieces
        _tnode.refreshHeightfield();
        updatePieces();
        updateHighlights();
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
        for (int y = 1; y <= hfheight; y++) {
            for (int x = 1; x <= hfwidth; x++) {
                vals[x-1] = _board.getHeightfieldValue(x, y) + 128;
            }
            grayimg.getRaster().setPixels(0, hfheight-y, hfwidth, 1, vals);
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
        
        x1 = clamp(x1, 0, _board.getTerrainWidth() - 1);
        x2 = clamp(x2, 0, _board.getTerrainWidth() - 1);
        y1 = clamp(y1, 0, _board.getTerrainHeight() - 1);
        y2 = clamp(y2, 0, _board.getTerrainHeight() - 1);
        
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
        _tnode.refreshTerrain(x1, y1, (x2 - x1) + 1, (y2 - y1) + 1);
        updateHighlights();
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
        
        x1 = clamp(x1, 1, _board.getHeightfieldWidth());
        x2 = clamp(x2, 1, _board.getHeightfieldWidth());
        y1 = clamp(y1, 1, _board.getHeightfieldHeight());
        y2 = clamp(y2, 1, _board.getHeightfieldHeight());
        
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
        
        // update the heightfield geometry and the pieces
        _tnode.refreshHeightfield(x1 - 1, y1 - 1, (x2 - x1) + 2,
            (y2 - y1) + 2);
        updatePieces();
        updateHighlights();
    }
    
    /**
     * Adds some random noise to the heightfield.
     */
    public void addHeightfieldNoise (int value)
    {
        int width = _board.getHeightfieldWidth(),
            height = _board.getHeightfieldHeight();
        
        for (int y = 1; y <= height; y++) {
            for (int x = 1; x <= width; x++) {
                _board.addHeightfieldValue(x, y,
                    RandomUtil.getInt(+value, -value));
            }
        }
        
        // update the heightfield geometry and the pieces
        _tnode.refreshHeightfield();
        updatePieces();
        updateHighlights();
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
    
    @Override // documentation inherited
    public void mouseWheeled (MouseEvent e)
    {
        // don't try to access the nonexistent input handler
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
     * Clamps v between a and b (inclusive).
     */
    protected int clamp (int v, int a, int b)
    {
        return Math.min(Math.max(v, a), b);
    }

    /**
     * Sets the heightfield to the contents of the given JME height map (whose
     * size must be equal to or greater than that of the heightfield).
     */
    protected void setHeightfield (AbstractHeightMap map)
    {
        int width = _board.getHeightfieldWidth(),
            height = _board.getHeightfieldHeight();
        for (int y = 1; y <= height; y++) {
            for (int x = 1; x <= width; x++) {
                _board.setHeightfieldValue(x, y,
                    (byte)(map.getTrueHeightAtPoint(x, y) - 128));
            }
        }
        
        // update the heightfield geometry and the pieces
        _tnode.refreshHeightfield();
        updatePieces();
        updateHighlights();
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
     * Updates the highlights over the entire board.
     */
    protected void updateHighlights ()
    {
        _hnode.detachAllChildren();
        if (!_showHighlights) {
            return;
        } 
        
        for (int x = 0, width = _board.getWidth(); x < width; x++) {
            for (int y = 0, height = _board.getHeight(); y < height; y++) {
                if (_board.exceedsMaxHeightDelta(x, y)) {
                    _highlights[x][y].updateVertices();
                    if (_highlights[x][y].getParent() != _hnode) {
                        _hnode.attachChild(_highlights[x][y]);
                    }
                                    
                } else if (_highlights[x][y].getParent() == _hnode) {
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

    /** The panel that contains additional interface elements with which
     * we interact. */
    protected EditorPanel _panel;

    /** Highlights indicating which tiles are occupiable. */    
    protected TerrainNode.Highlight[][] _highlights;
    
    /** Whether or not to show the highlights. */
    protected boolean _showHighlights;
    
    /** The color to use for highlights. */
    protected static final ColorRGBA HIGHLIGHT_COLOR =
        new ColorRGBA(1f, 0f, 0f, 0.25f);
}
