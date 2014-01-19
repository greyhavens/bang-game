//
// $Id$

package com.threerings.bang.editor;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.jme.math.Vector2f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.WireframeState;
import com.jme.util.geom.Debugger;

import com.jmex.terrain.util.AbstractHeightMap;
import com.jmex.terrain.util.FaultFractalHeightMap;
import com.jmex.terrain.util.MidPointHeightMap;
import com.jmex.terrain.util.ParticleDepositionHeightMap;

import com.samskivert.util.RandomUtil;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.data.TerrainConfig;
import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.BigPiece;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.ModifiableDSet;
import com.threerings.bang.game.util.PointSet;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.editor.EditorContext;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the board when in editor mode.
 */
public class EditorBoardView extends BoardView
    implements PieceCodes
{
    public EditorBoardView (BasicContext ctx, EditorPanel panel)
    {
        super(ctx, true);
        _panel = panel;
        addListener(this);

        // put piece sprites in editor mode
        PieceSprite.setEditorMode(true);
    }

    @Override // documentation inherited
    public void prepareForRound (BangObject bangobj, BangConfig cfg, int pidx)
    {
        super.prepareForRound(bangobj, cfg, pidx);

        // add a listener to update highlights for pieces
        _bangobj.addListener(new SetListener<Piece>() {
            public void entryAdded (EntryAddedEvent<Piece> e) {
                if (e.getName().equals(BangObject.PIECES)) {
                    updateHighlights(e.getEntry());
                }
            }
            public void entryRemoved (EntryRemovedEvent<Piece> e) {
                if (e.getName().equals(BangObject.PIECES)) {
                    updateHighlights(e.getOldEntry());
                }
            }
            public void entryUpdated (EntryUpdatedEvent<Piece> e) {
                if (e.getName().equals(BangObject.PIECES)) {
                    updateHighlights(e.getOldEntry());
                    updateHighlights(e.getEntry());
                }
            }
            protected void updateHighlights (Piece piece) {
                if (piece instanceof BigPiece) {
                    Rectangle bounds = (Rectangle)
                        ((BigPiece)piece).getBounds().clone();
                    bounds.grow(1, 1);
                    Rectangle isect = _board.getPlayableArea().intersection(
                        bounds);
                    if (!isect.isEmpty()) {
                        EditorBoardView.this.updateHighlights(isect.x, isect.y,
                            isect.x + isect.width - 1,
                            isect.y + isect.height - 1);
                    }
                } else if (_board.getPlayableArea().contains(
                        piece.x, piece.y)) {
                    EditorBoardView.this.updateHighlights(piece.x, piece.y,
                        1, 1);
                }
            }
        });
    }

    @Override // documentation inherited
    public void refreshBoard ()
    {
        super.refreshBoard();

        // recenter the dolly
        _panel.tools.cameraDolly.recenter();

        // make sure highlights are reset to new size
        _hnode.detachAllChildren();
        _highlights = null;
        _crosslights = null;
        updateHighlights();

        // clear out the undo stack
        ((EditorController)_panel.getController()).clearEdits();
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
     * Activates or deactivates bounding volume rendering.
     */
    public void toggleBounds ()
    {
        _showBounds = !_showBounds;
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
     * Sets the number of elevation units there are in the length of a tile,
     * which determines the heightfield scale.
     *
     * @param edit true if the user is directly modifying the value
     */
    public void setElevationUnitsPerTile (int units, boolean edit)
    {
        if (edit && _elevationUnitsEdit == null) {
            _elevationUnitsEdit = new SwapEdit() {
                public void commit () {
                    if (_units != _board.getElevationUnitsPerTile()) {
                        super.commit();
                    }
                }
                public void swapSaved () {
                    byte tmp = _board.getElevationUnitsPerTile();
                    setElevationUnitsPerTile(_units, false);
                    _units = tmp;
                }
                protected byte _units = _board.getElevationUnitsPerTile();
            };
        }
        _board.setElevationUnitsPerTile((byte)units);
        heightfieldChanged();
    }

    /**
     * Commits the elevation units edit, if any.
     */
    public void commitElevationUnitsEdit ()
    {
        if (_elevationUnitsEdit != null) {
            _elevationUnitsEdit.commit();
            _elevationUnitsEdit = null;
        }
    }

    /**
     * Sets the heightfield to the contents of the specified image.
     */
    public void setHeightfield (BufferedImage image)
    {
        // store the original heightfield state as an edit
        new HeightfieldEdit().commit();

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
     *
     * @param fill if true, perform a flood fill instead of simply painting
     */
    public void paintTerrain (
        float x, float y, float radius, TerrainConfig terrain, boolean fill)
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

        // create/update the current terrain edit
        if (_tedit == null) {
            _tedit = new TerrainEdit();
        }
        Rectangle dirty = new Rectangle();
        if (!fill) {
            dirty.setBounds(x1, y1, 1 + x2 - x1, 1 + y2 - y1);
        }

        // scan over the sub-tile coordinates, setting any that fall in the
        // circle
        Vector2f vec = new Vector2f();
        for (int ty = y1; ty <= y2; ty++) {
            for (int tx = x1; tx <= x2; tx++) {
                vec.set(x - tx*stscale, y - ty*stscale);
                if (vec.lengthSquared() <= rr) {
                    if (fill) {
                        floodFill(tx, ty, code, dirty);
                    } else {
                        _board.setTerrainValue(tx, ty, code);
                    }
                }
            }
        }

        // dirty the edit and update the terrain splats
        _tedit.dirty(dirty);
        _tnode.refreshTerrain(dirty.x, dirty.y, dirty.x + dirty.width - 1,
            dirty.y + dirty.height - 1);
    }

    /**
     * Clears the entire board to the specified terrain type.
     */
    public void clearTerrain (TerrainConfig terrain)
    {
        // store the original terrain as an edit
        new TerrainEdit().commit();

        // fill 'er up!
        _board.fillTerrain((byte)terrain.code);

        // update the terrain splats
        _tnode.refreshTerrain();
    }

    /**
     * Commits the current terrain edit to the undo buffer.
     */
    public void commitTerrainEdit ()
    {
        if (_tedit == null) {
            return;
        }
        _tedit.commit();
        _tedit = null;
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

        // create/update the current heightfield edit
        if (_hfedit == null) {
            _hfedit = new HeightfieldEdit();
        }
        _hfedit.dirty(x1, y1, x2, y2);

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
     * Commits the current heightfield edit to the undo buffer.
     */
    public void commitHeightfieldEdit ()
    {
        if (_hfedit == null) {
            return;
        }
        _hfedit.commit();
        _hfedit = null;
    }

    /**
     * Adds some random noise to the heightfield (just enough to create some
     * interesting texture).
     */
    public void addHeightfieldNoise ()
    {
        // store the original heightfield state as an edit
        new HeightfieldEdit().commit();

        int width = _board.getHeightfieldWidth(),
            height = _board.getHeightfieldHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                _board.addHeightfieldValue(x, y, RandomUtil.getInRange(-2, +3));
            }
        }

        heightfieldChanged();
    }

    /**
     * Smooths the heightfield using a simple blur.
     */
    public void smoothHeightfield ()
    {
        // store the original heightfield state as an edit
        new HeightfieldEdit().commit();

        byte[] smoothed = new byte[_board.getHeightfield().length];

        int width = _board.getHeightfieldWidth(),
            height = _board.getHeightfieldHeight(), idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // average this pixel and its eight neighbors
                smoothed[idx++] = (byte)
                    ((_board.getHeightfieldValue(x-1, y-1) +
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
     * Sets the parameters of the board's light.
     *
     * @param edit true if the user is directly modifying the value
     */
    public void setLightParams (final int idx, float azimuth, float elevation,
        int diffuseColor, int ambientColor, boolean edit)
    {
        if (edit && _lightEdits[idx] == null) {
            _lightEdits[idx] = new SwapEdit() {
                public void commit () {
                    if (_azimuth != _board.getLightAzimuth(idx) ||
                        _elevation != _board.getLightElevation(idx) ||
                        _diffuseColor != _board.getLightDiffuseColor(idx) ||
                        _ambientColor != _board.getLightAmbientColor(idx)) {
                        super.commit();
                    }
                }
                public void swapSaved () {
                    float azimuth = _board.getLightAzimuth(idx),
                        elevation = _board.getLightElevation(idx);
                    int diffuseColor = _board.getLightDiffuseColor(idx),
                        ambientColor = _board.getLightAmbientColor(idx);
                    setLightParams(idx, _azimuth, _elevation, _diffuseColor,
                        _ambientColor, false);
                    _azimuth = azimuth;
                    _elevation = elevation;
                    _diffuseColor = diffuseColor;
                    _ambientColor = ambientColor;
                }
                protected float _azimuth = _board.getLightAzimuth(idx),
                    _elevation = _board.getLightElevation(idx);
                protected int _diffuseColor = _board.getLightDiffuseColor(idx),
                    _ambientColor = _board.getLightAmbientColor(idx);
            };
        }
        _board.setLightParams(idx, azimuth, elevation, diffuseColor,
            ambientColor);
        refreshLight(idx);
    }

    /**
     * Commits the light edit, if any.
     */
    public void commitLightEdit (int idx)
    {
        if (_lightEdits[idx] != null) {
            _lightEdits[idx].commit();
            _lightEdits[idx] = null;
        }
    }

    /**
     * Sets the shadow intensity.
     *
     * @param edit true if the user is directly modifying the value
     */
    public void setShadowIntensity (float intensity, boolean edit)
    {
        if (edit && _shadowIntensityEdit == null) {
            _shadowIntensityEdit = new SwapEdit() {
                public void commit () {
                    if (_intensity != _board.getShadowIntensity()) {
                        super.commit();
                    }
                }
                public void swapSaved () {
                    float tmp = _board.getShadowIntensity();
                    setShadowIntensity(_intensity, false);
                    _intensity = tmp;
                }
                protected float _intensity = _board.getShadowIntensity();
            };
        }
        _board.setShadowIntensity(intensity);
        _tnode.refreshShadows();
    }

    /**
     * Commits the shadow intensity edit, if any.
     */
    public void commitShadowIntensityEdit ()
    {
        if (_shadowIntensityEdit != null) {
            _shadowIntensityEdit.commit();
            _shadowIntensityEdit = null;
        }
    }

    /**
     * Sets the parameters of the board's sky.
     *
     * @param edit true if the user is directly modifying the value
     */
    public void setSkyParams (int horizonColor, int overheadColor,
        float falloff, boolean edit)
    {
        if (edit && _skyEdit == null) {
            _skyEdit = new SwapEdit() {
                public void commit () {
                    if (_horizonColor != _board.getSkyHorizonColor() ||
                        _overheadColor != _board.getSkyOverheadColor() ||
                        _falloff != _board.getSkyFalloff()) {
                        super.commit();
                    }
                }
                public void swapSaved () {
                    int horizonColor = _board.getSkyHorizonColor(),
                        overheadColor = _board.getSkyOverheadColor();
                    float falloff = _board.getSkyFalloff();
                    setSkyParams(_horizonColor, _overheadColor, _falloff,
                        false);
                    _horizonColor = horizonColor;
                    _overheadColor = overheadColor;
                    _falloff = falloff;
                }
                protected int _horizonColor = _board.getSkyHorizonColor(),
                    _overheadColor = _board.getSkyOverheadColor();
                protected float _falloff = _board.getSkyFalloff();
            };
        }
        int oocolor = _board.getSkyOverheadColor();
        _board.setSkyParams(horizonColor, overheadColor, falloff);
        if (oocolor != overheadColor) {
            _wnode.refreshColors();
        }
        _snode.refreshGradient();
        refreshBackgroundColor();
    }

    /**
     * Commits the sky edit, if any.
     */
    public void commitSkyEdit ()
    {
        if (_skyEdit != null) {
            _skyEdit.commit();
            _skyEdit = null;
        }
    }

    /**
     * Sets the board's water parameters.
     *
     * @param edit true if the user is directly modifying the value
     */
    public void setWaterParams (int level, int color, float amplitude,
        boolean edit)
    {
        if (edit && _waterEdit == null) {
            _waterEdit = new SwapEdit() {
                public void commit () {
                    if (_level != _board.getWaterLevel() ||
                        _color != _board.getWaterColor() ||
                        _amplitude != _board.getWaterAmplitude()) {
                        super.commit();
                    }
                }
                public void swapSaved () {
                    byte level = _board.getWaterLevel();
                    int color = _board.getWaterColor();
                    float amplitude = _board.getWaterAmplitude();
                    setWaterParams(_level, _color, _amplitude, false);
                    _level = level;
                    _color = color;
                    _amplitude = amplitude;
                }
                protected byte _level = _board.getWaterLevel();
                protected int _color = _board.getWaterColor();
                protected float _amplitude = _board.getWaterAmplitude();
            };
        }
        int ocolor = _board.getWaterColor();
        float oamplitude = _board.getWaterAmplitude();
        _board.setWaterParams((byte)level, color, amplitude);
        if (ocolor != color) {
            _wnode.refreshColors();
        }
        if (oamplitude != amplitude) {
            _wnode.refreshWaveAmplitudes();
        }
        _wnode.refreshSurface();
        updateHighlights();
    }

    /**
     * Commits the water edit, if any.
     */
    public void commitWaterEdit ()
    {
        if (_waterEdit != null) {
            _waterEdit.commit();
            _waterEdit = null;
        }
    }

    /**
     * Sets the board's wind parameters.
     *
     * @param edit true if the user is directly modifying the value
     */
    public void setWindParams (float direction, float speed, boolean edit)
    {
        if (edit && _windEdit == null) {
            _windEdit = new SwapEdit() {
                public void commit () {
                    if (_direction != _board.getWindDirection() ||
                        _speed != _board.getWindSpeed()) {
                        super.commit();
                    }
                }
                public void swapSaved () {
                    float direction = _board.getWindDirection(),
                        speed = _board.getWindSpeed();
                    setWindParams(_direction, _speed, false);
                    _direction = direction;
                    _speed = speed;
                }
                protected float _direction = _board.getWindDirection(),
                    _speed = _board.getWindSpeed();
            };
        }
        _board.setWindParams(direction, speed);
        _wnode.refreshWaveAmplitudes();
        refreshWindInfluence();
    }

    /**
     * Commits the wind edit, if any.
     */
    public void commitWindEdit ()
    {
        if (_windEdit != null) {
            _windEdit.commit();
            _windEdit = null;
        }
    }

    /**
     * Sets the board's fog parameters.
     *
     * @param edit true if the user is directly modifying the value
     */
    public void setFogParams (int color, float density, boolean edit)
    {
        if (edit && _fogEdit == null) {
            _fogEdit = new SwapEdit() {
                public void commit () {
                    if (_color != _board.getFogColor() ||
                        _density != _board.getFogDensity()) {
                        super.commit();
                    }
                }
                public void swapSaved () {
                    int color = _board.getFogColor();
                    float density = _board.getFogDensity();
                    setFogParams(_color, _density, false);
                    _color = color;
                    _density = density;
                }
                protected int _color = _board.getFogColor();
                protected float _density = _board.getFogDensity();
            };
        }
        _board.setFogParams(color, density);
        refreshFog();
        _wnode.refreshShader();
        _tnode.refreshShaders();
        refreshBackgroundColor();
    }

    /**
     * Commits the fog edit, if any.
     */
    public void commitFogEdit ()
    {
        if (_fogEdit != null) {
            _fogEdit.commit();
            _fogEdit = null;
        }
    }

    /**
     * Sets the board's grid color.
     */
    public void setGridColor (int color, boolean edit)
    {
        if (edit && color != _board.getGridColor()) {
            new SwapEdit() {
                public void swapSaved () {
                    int color = _board.getGridColor();
                    setGridColor(_color, false);
                    _color = color;
                }
                protected int _color = _board.getGridColor();

            }.commit();
        }
        _board.setGridColor(color);
        updateGrid();
    }

    /**
     * Creates a fresh new board.
     */
    public void createNewBoard (int width, int height)
    {
        _bangobj.board = new BangBoard(width, height);
        _bangobj.setPieces(new ModifiableDSet<Piece>());
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
        nboard.setElevationUnitsPerTile(_board.getElevationUnitsPerTile());
        for (int i = 0; i < BangBoard.NUM_LIGHTS; i++) {
            nboard.setLightParams(i, _board.getLightAzimuth(i),
                _board.getLightElevation(i), _board.getLightDiffuseColor(i),
                _board.getLightAmbientColor(i));
        }
        nboard.setShadowIntensity(_board.getShadowIntensity());
        nboard.setSkyParams(_board.getSkyHorizonColor(),
            _board.getSkyOverheadColor(), _board.getSkyFalloff());
        nboard.setWaterParams(_board.getWaterLevel(), _board.getWaterColor(),
            _board.getWaterAmplitude());
        nboard.setWindParams(_board.getWindDirection(), _board.getWindSpeed());
        _bangobj.board = nboard;

        // then move the pieces
        xoff = (width - _board.getWidth())/2;
        yoff = (height - _board.getHeight())/2;
        for (Piece piece : _bangobj.pieces) {
            // set location directly in order to retain fine positions
            piece.x += xoff;
            piece.y += yoff;
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
        return _bangobj.pieces.get(pid);
    }

    @Override // documentation inherited
    protected Node createPieceNode ()
    {
        return new Node("pieces") {
            public void draw (Renderer r) {
                super.draw(r);
                if (_showBounds) {
                    for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
                        Debugger.drawBounds(getChild(ii), r);
                    }
                }
            }
        };
    }

    @Override // documentation inherited
    protected boolean shouldShowStarter (Piece piece)
    {
        // always show everything
        return true;
    }

    /**
     * Flood-fills the board terrain.
     */
    protected void floodFill (
        int x, int y, byte code, Rectangle dirty)
    {
        byte ocode = _board.getTerrainValue(x, y);
        if (ocode == code) {
            return;
        }
        PointSet fringe = new PointSet();
        fringe.add(x, y);
        while (fringe.size() > 0) {
            int fx = fringe.getX(0), fy = fringe.getY(0);
            fringe.remove(fx, fy);
            _board.setTerrainValue(fx, fy, code);
            dirty.add(fx, fy);
            for (int ii = 0; ii < DIRECTIONS.length; ii++) {
                int ax = fx + DX[ii], ay = fy + DY[ii];
                if (_board.heightfieldContains(ax, ay) &&
                    _board.getTerrainValue(ax, ay) == ocode) {
                    fringe.add(ax, ay);
                }
            }
        }
        dirty.grow(1, 1);
        dirty.setBounds(dirty.intersection(
            new Rectangle(0, 0, _board.getHeightfieldWidth(),
                _board.getHeightfieldHeight())));
    }

    /**
     * Sets the heightfield to the contents of the given JME height map (whose
     * size must be equal to or greater than that of the heightfield).
     */
    protected void setHeightfield (AbstractHeightMap map)
    {
        // store the original heightfield state as an edit
        new HeightfieldEdit().commit();

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
        _tnode.refreshHeightfield(x1, y1, x2, y2);

        int txmax = _board.getWidth() - 1, tymax = _board.getHeight() - 1,
            tx1 = clamp(x1 / BangBoard.HEIGHTFIELD_SUBDIVISIONS, 0, txmax),
            ty1 = clamp(y1 / BangBoard.HEIGHTFIELD_SUBDIVISIONS, 0, tymax),
            tx2 = clamp(x2 / BangBoard.HEIGHTFIELD_SUBDIVISIONS, 0, txmax),
            ty2 = clamp(y2 / BangBoard.HEIGHTFIELD_SUBDIVISIONS, 0, tymax);
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
        for (Piece piece : _bangobj.pieces) {
            queuePieceUpdate(piece, piece);
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
        if (_crosslights == null) {
            _crosslights = new CrossStatus[_board.getWidth()][
                _board.getHeight()];
        }
        _board.shadowPieces(
                new FilterPieceIterator(_bangobj.pieces.iterator()),
                x1, y1, 1 + x2 - x1,
            1 + y2 - y1);
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (_showHighlights) {
                    boolean isBridge = _board.isBridge(x, y);
                    if (_highlights[x][y] != null &&
                            _highlights[x][y].flatten != isBridge) {
                        if (_highlights[x][y].getParent() != null) {
                            _hnode.detachChild(_highlights[x][y]);
                        }
                        _highlights[x][y] = null;
                    }
                    if (_highlights[x][y] == null) {
                        _highlights[x][y] = _tnode.createHighlight(x, y,
                            isBridge, isBridge);
                    }
                    if (!_board.isTraversable(x, y)) {
                        _highlights[x][y].setDefaultColor(HIGHLIGHT_COLOR);
                    } else if (isBridge) {
                        _highlights[x][y].setDefaultColor(BRIDGE_COLOR);
                    } else {
                        _highlights[x][y].setDefaultColor(MOVE_COLOR);
                    }
                    _highlights[x][y].updateVertices();
                    if (_highlights[x][y].getParent() == null) {
                        _hnode.attachChild(_highlights[x][y]);
                    }
                } else {
                    if (_highlights[x][y] != null &&
                        _highlights[x][y].getParent() != null) {
                        _hnode.detachChild(_highlights[x][y]);
                    }
                }

                if (_showHighlights &&
                        _board.getPlayableArea().contains(x, y)) {
                    boolean isBridge = _board.isBridge(x, y);
                    if (_crosslights[x][y] != null &&
                            _crosslights[x][y].highlight.flatten != isBridge) {
                        if (_crosslights[x][y].getParent() != null) {
                            _hnode.detachChild(_crosslights[x][y]);
                        }
                        _crosslights[x][y] = null;
                    }
                    if (_crosslights[x][y] == null) {
                        _crosslights[x][y] = new CrossStatus(_ctx,
                            _tnode.createHighlight(x, y, isBridge, isBridge));
                        _crosslights[x][y].setDefaultColor(CROSS_COLOR);
                    }
                    if (_crosslights[x][y].update(_bangobj.board, x, y) &&
                            _crosslights[x][y].getParent() == null) {
                        _hnode.attachChild(_crosslights[x][y]);
                    }
                } else {
                    if (_crosslights[x][y] != null &&
                            _crosslights[x][y].getParent() != null) {
                        _hnode.detachChild(_crosslights[x][y]);
                    }
                }
            }
        }
    }

    @Override // documentation inherited
    protected void createMarquee (String text)
    {
        // no marquee required for editor
    }

    @Override // documentation inherited
    protected void hoverTileChanged (int tx, int ty)
    {
        super.hoverTileChanged(tx, ty);
        _panel.tools.getActiveTool().hoverTileChanged(tx, ty);
        ((EditorContext)_ctx).displayCoords(tx, ty);
    }

    @Override // documentation inherited
    protected void hoverSpritesChanged (Sprite hover, Sprite thover)
    {
        super.hoverSpritesChanged(hover, thover);
        _panel.tools.getActiveTool().hoverSpriteChanged(hover);
    }

    /** Superclass for edits that can be reversed by swapping the current
     * value with the saved value. */
    protected abstract class SwapEdit
        implements EditorController.Edit
    {
        /**
         * Commits this edit to the undo buffer if it differs from the current
         * state.
         */
        public void commit ()
        {
            ((EditorController)_panel.getController()).addEdit(this);
        }

        // documentation inherited from interface EditorController.Edit
        public void undo ()
        {
            swapSaved();
        }

        // documentation inherited from interface EditorController.Edit
        public void redo ()
        {
            swapSaved();
        }

        /**
         * Swaps the saved data with the current data.
         */
        protected abstract void swapSaved ();
    }

    /** Superclass for heightfield and terrain edits, which work in almost
     * exactly the same way. */
    protected abstract class BufferEdit extends SwapEdit
    {
        public BufferEdit () {
            // on construction, save the entire buffer; when we're commited,
            // we can choose what to throw away
            _saved = getBuffer().clone();
        }

        /**
         * Marks the specified region of the buffer as dirty, so that it
         * will be included in the edit.
         */
        public void dirty (int x1, int y1, int x2, int y2)
        {
            dirty(new Rectangle(x1, y1, 1 + x2 - x1, 1 + y2 - y1));
        }

        /**
         * Marks the specified region of the buffer as dirty.
         */
        public void dirty (Rectangle drect)
        {
            if (_modified == null) {
                _modified = drect;
            } else {
                _modified.add(drect);
            }
        }

        @Override // documentation inherited
        public void commit ()
        {
            // keep only the modified portion if any portions were marked as
            // dirty
            if (_modified != null) {
                if (_modified.width == 0 && _modified.height == 0) {
                    return;
                } else if (_modified.x == 0 && _modified.y == 0 &&
                    _modified.width == _board.getHeightfieldWidth() &&
                    _modified.height == _board.getHeightfieldHeight()) {
                    _modified = null;

                } else {
                    _saved = getRegion(_saved, _modified);
                }
            }
            super.commit();
        }

        @Override // documentation inherited
        protected void swapSaved ()
        {
            byte[] buf = getBuffer();
            if (_modified == null) {
                byte[] tmp = buf.clone();
                System.arraycopy(_saved, 0, buf, 0, _saved.length);
                _saved = tmp;

            } else {
                byte[] tmp = getRegion(buf, _modified);
                setRegion(getBuffer(), _modified, _saved);
                _saved = tmp;
            }
        }

        /**
         * Returns a reference to the current contents of the buffer.
         */
        protected abstract byte[] getBuffer ();

        /**
         * Creates and returns a new array containing the contents of the
         * specified region of the given heightfield-sized data array.
         */
        protected byte[] getRegion (byte[] data, Rectangle rect)
        {
            byte[] region = new byte[_modified.width * _modified.height];
            int hfwidth = _board.getHeightfieldWidth(), ridx = 0;
            for (int y = rect.y, ymax = y + rect.height; y < ymax; y++) {
                for (int x = rect.x, xmax = x + rect.width; x < xmax; x++) {
                    region[ridx++] = data[y*hfwidth + x];
                }
            }
            return region;
        }

        /**
         * Sets the specified region of the given heightfield-sized data
         * array to the provided values.
         */
        protected void setRegion (byte[] data, Rectangle rect, byte[] region)
        {
            int hfwidth = _board.getHeightfieldWidth(), ridx = 0;
            for (int y = rect.y, ymax = y + rect.height; y < ymax; y++) {
                for (int x = rect.x, xmax = x + rect.width; x < xmax; x++) {
                    data[y*hfwidth + x] = region[ridx++];
                }
            }
        }

        /** The modified region of the buffer, or <code>null</code> if the
         * entire buffer is dirty. */
        protected Rectangle _modified;

        /** The saved buffer data. */
        protected byte[] _saved;
    }

    /** Represents a change to the heightfield that can be done and undone. */
    protected class HeightfieldEdit extends BufferEdit
    {
        // documentation inherited
        protected void swapSaved ()
        {
            super.swapSaved();
            if (_modified == null) {
                heightfieldChanged();
            } else {
                heightfieldChanged(_modified.x, _modified.y,
                    _modified.x + _modified.width - 1,
                    _modified.y + _modified.height - 1);
            }
        }

        // documentation inherited
        protected byte[] getBuffer ()
        {
            return _board.getHeightfield();
        }
    }

    /** Represents a change to the terrain that can be done and undone. */
    protected class TerrainEdit extends BufferEdit
    {
        // documentation inherited
        protected void swapSaved ()
        {
            super.swapSaved();
            if (_modified == null) {
                _tnode.refreshTerrain();
            } else {
                _tnode.refreshTerrain(_modified.x, _modified.y,
                    _modified.x + _modified.width - 1,
                    _modified.y + _modified.height - 1);
            }
        }

        // documentation inherited
        protected byte[] getBuffer ()
        {
            return _board.getTerrain();
        }
    }

    /** An iterator that filters pieces based on the scenario. */
    protected class FilterPieceIterator
        implements Iterator<Piece>
    {
        public FilterPieceIterator (Iterator<Piece> iter) {
            _iter = iter;
            _scenId = ((EditorController)_panel.getController()).
                getScenarioId();
            getNext();
        }

        public boolean hasNext () {
            return _piece != null;
        }

        public Piece next ()
            throws NoSuchElementException
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Piece ret = _piece;
            getNext();
            return ret;
        }

        public void remove ()
            throws UnsupportedOperationException
        {
            throw new UnsupportedOperationException();
        }

        protected void getNext () {
            _piece = null;
            while (_iter.hasNext()) {
                Piece p = _iter.next();
                if (p.isValidScenario(_scenId)) {
                    _piece = p;
                    break;
                }
            }
        }

        protected Iterator<Piece> _iter;
        protected String _scenId;
        protected Piece _piece;
    };

    /** The panel that contains additional interface elements with which
     * we interact. */
    protected EditorPanel _panel;

    /** Highlights indicating which tiles are occupiable. */
    protected TerrainNode.Highlight[][] _highlights;

    /** Highlights which tile edges can be crossed. */
    protected CrossStatus[][] _crosslights;

    /** Whether or not to show the highlights. */
    protected boolean _showHighlights;

    /** Whether or not to show the bounding volumes. */
    protected boolean _showBounds;

    /** The in-progress edits, if any. */
    protected HeightfieldEdit _hfedit;
    protected TerrainEdit _tedit;
    protected SwapEdit _elevationUnitsEdit, _shadowIntensityEdit, _skyEdit,
        _waterEdit, _windEdit, _fogEdit;
    protected SwapEdit[] _lightEdits = new SwapEdit[BangBoard.NUM_LIGHTS];

    /** The color to use for highlights. */
    protected static final ColorRGBA HIGHLIGHT_COLOR =
        new ColorRGBA(1f, 0f, 0f, 0.25f);
    protected static final ColorRGBA MOVE_COLOR =
        new ColorRGBA(0f, 1f, 0f, 0.25f);
    protected static final ColorRGBA BRIDGE_COLOR =
        new ColorRGBA(0f, 0f, 1f, 0.25f);

    /** The color to use for crosslights. */
    protected static final ColorRGBA CROSS_COLOR =
        new ColorRGBA(1f, 0f, 1f, 0.25f);
}
