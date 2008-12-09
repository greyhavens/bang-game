//
// $Id$

package com.threerings.bang.game.data;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.Savable;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.TermUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.util.StreamableHashMap;
import com.threerings.media.util.AStarPathUtil;

import com.threerings.bang.data.TerrainConfig;
import com.threerings.bang.game.data.piece.BigPiece;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.game.data.piece.Track;
import com.threerings.bang.game.data.piece.Train;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;
import com.threerings.bang.game.util.ArrayDiffUtil;

/**
 * Describes the terrain of the game board.
 */
public class BangBoard extends SimpleStreamableObject
    implements AStarPathUtil.ExtendedTraversalPred, Cloneable, Savable,
        PieceCodes
{
    /** The basic traversal cost. */
    public static final int BASE_TRAVERSAL = 10;

    /** The number of subdivisions in the heightfield for each tile. */
    public static final int HEIGHTFIELD_SUBDIVISIONS = 4;

    /** The maximum difference between adjacent height points allowed before a tile becomes
     * unoccupiable. */
    public static final int MAX_OCCUPIABLE_HEIGHT_DELTA = 16;

    /** The maximum water level (above ground in elevation units) that ground units can occupy. */
    public static final byte MAX_OCCUPIABLE_WATER_LEVEL = 1;

    /** The size in tiles of the border between the edge of the heightfield and the edge of the
     * playable region. */
    public static final int BORDER_SIZE = 12;

    /** The default board size is 16x16 of playable area. */
    public static final int DEFAULT_SIZE = 2*BORDER_SIZE + 16;

    /** The number of directional lights on the board. */
    public static final int NUM_LIGHTS = 2;

    /** Creates a board with the specified dimensions. */
    public BangBoard (int width, int height)
    {
        _width = width;
        _height = height;
        _hfwidth = _width * HEIGHTFIELD_SUBDIVISIONS + 1;
        _hfheight = _height * HEIGHTFIELD_SUBDIVISIONS + 1;

        _heightfield = new byte[_hfwidth * _hfheight];
        _elevationUnitsPerTile = 64;
        _terrain = new byte[_hfwidth * _hfheight];
        fillTerrain((byte)2); // dirt!
        _shadows = new byte[_hfwidth * _hfheight];
        _patchMap = new StreamableHashMap<String, byte[]>();
        fillShadows(0);
        _shadowIntensity = 1f;

        _waterLevel = (byte)-128;
        _waterColor = 0x003232;
        _waterAmplitude = 25f;

        _lightAzimuths = new float[] { 0f, (float)Math.PI };
        _lightElevations = new float[] { (float)(Math.PI / 4),
            (float)(-Math.PI / 4)};
        _lightDiffuseColors = new int[] { 0xFFFFFF, 0x0 };
        _lightAmbientColors = new int[] { 0x323232, 0x0 };

        _skyHorizonColor = 0xFFFFFF;
        _skyOverheadColor = 0x00FFFF;
        _skyFalloff = 10f;

        _windSpeed = 20f;

        _fogColor = 0xFFFFFF;
        _gridColor = 0x808080;

        initTransientFields();
    }

    /** A default constructor for unserialization. */
    public BangBoard ()
    {
    }

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            BangBoard board = (BangBoard)super.clone();
            board.initTransientFields();
            return board;

        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }

    /**
     * Tells the board information it needs to know about the game.
     */
    public void init (int[] teams, Iterator<Piece> iter)
    {
        _teams = teams;
        shadowPieces(iter);
    }

    /** Returns the width of the board in tiles. */
    public int getWidth ()
    {
        return _width;
    }

    /** Returns the height of the board in tiles. */
    public int getHeight ()
    {
        return _height;
    }

    /** Returns the width of the board's heightfield. */
    public int getHeightfieldWidth ()
    {
        return _hfwidth;
    }

    /** Returns the height of the board's heightfield. */
    public int getHeightfieldHeight ()
    {
        return _hfheight;
    }

    /** Returns the height at the specified sub-tile coordinates. */
    public byte getHeightfieldValue (int x, int y)
    {
        // return the minimum edge height for values beyond the edge
        if (!heightfieldContains(x, y)) {
            return _minEdgeHeight;

        } else {
            return _heightfield[y*_hfwidth + x];
        }
    }

    /**
     * Determines whether the heightfield contains the specified heightfield coordinates.
     */
    public boolean heightfieldContains (int x, int y)
    {
        return x >= 0 && y >= 0 && x < _hfwidth && y < _hfheight;
    }

    /**
     * Updates the board's minimum edge height, which is returned for values outside the board.
     */
    public void updateMinEdgeHeight ()
    {
        int toff = (_hfheight - 1)*_hfwidth, roff = _hfwidth - 1;
        byte min = Byte.MAX_VALUE;
        for (int x = 0; x < _hfwidth; x++) {
            min = (byte)Math.min(min, _heightfield[x]);
            min = (byte)Math.min(min, _heightfield[toff + x]);
        }
        for (int y = 0; y < _hfheight; y++) {
            min = (byte)Math.min(min, _heightfield[y*_hfwidth]);
            min = (byte)Math.min(min, _heightfield[y*_hfwidth + roff]);
        }
        _minEdgeHeight = min;
    }

    /**
     * Sets the value at the specified sub-tile coordinates.
     */
    public void setHeightfieldValue (int x, int y, byte value)
    {
        _heightfield[y*_hfwidth + x] = value;
        _heightfieldChanged = true;
    }

    /** Adds to or subtracts from the height at the specified sub-tile coordinates, clamping to the
     * allowed range. */
    public void addHeightfieldValue (int x, int y, int value)
    {
        int idx = y*_hfwidth + x;
        _heightfield[idx] = (byte)Math.min(Math.max(-128, _heightfield[idx] + value), +127);
        _heightfieldChanged = true;
    }

    /** Returns a reference to the heightfield array. */
    public byte[] getHeightfield ()
    {
        return _heightfield;
    }

    /** Returns the number of elevation units per tile, which determines the elevation scale. */
    public byte getElevationUnitsPerTile ()
    {
        return _elevationUnitsPerTile;
    }

    /** Sets the number of elevation units per tile. */
    public void setElevationUnitsPerTile (byte units)
    {
        _elevationUnitsPerTile = units;
    }

    /** Returns the elevation scale (the distance between elevation units). */
    public float getElevationScale (float tileSize)
    {
        return tileSize / _elevationUnitsPerTile;
    }

    /** Returns the terrain value at the specified terrain coordinates. */
    public byte getTerrainValue (int x, int y)
    {
        // return the predominant edge terrain for values beyond the edge
        if (!heightfieldContains(x, y)) {
            return _edgeTerrain;

        } else {
            return _terrain[y*_hfwidth + x];
        }
    }

    /**
     * Updates the board's predominant edge terrain, which is returned for values outside the
     * board.
     */
    public void updateEdgeTerrain ()
    {
        IntIntMap counts = new IntIntMap();
        int toff = (_hfheight - 1)*_hfwidth, roff = _hfwidth - 1;
        for (int x = 0; x < _hfwidth; x++) {
            counts.increment(_terrain[x], 1);
            counts.increment(_terrain[toff + x], 1);
        }
        for (int y = 1; y < _hfheight - 1; y++) {
            counts.increment(_terrain[y*_hfwidth], 1);
            counts.increment(_terrain[y*_hfwidth + roff], 1);
        }
        int idx = IntListUtil.getMaxValueIndex(counts.getValues());
        _edgeTerrain = (byte)counts.getKeys()[idx];
    }

    /** Sets a single terrain value. */
    public void setTerrainValue (int x, int y, byte value)
    {
        _terrain[y*_hfwidth + x] = value;
    }

    /** Fills the terrain array with the specified terrain code. */
    public void fillTerrain (byte value)
    {
        Arrays.fill(_terrain, value);
    }

    /** Returns a reference to the terrain array. */
    public byte[] getTerrain ()
    {
        return _terrain;
    }

    /** Returns the height above terrain of the shadow volume at the specified sub-tile
     * coordinates. */
    public int getShadowValue (int x, int y)
    {
        if (!heightfieldContains(x, y)) {
            return 0;

        } else {
            return _shadows[y*_hfwidth + x] + 128;
        }
    }

    /** Sets the height above terrain of the shadow volume at the specified sub-tile
     * coordinates. */
    public void setShadowValue (int x, int y, int value)
    {
        _shadows[y*_hfwidth + x] = (byte)(value - 128);
    }

    /** Fills the shadow array with the specified value. */
    public void fillShadows (int value)
    {
        Arrays.fill(_shadows, (byte)(value - 128));
    }

    /** Returns a reference to the shadow map. */
    public byte[] getShadows ()
    {
        return _shadows;
    }

    /** Adds a shadow patch associated to the id */
    public void addShadowPatch (String id, byte[] patch)
    {
        _patchMap.put(id, patch);
    }

    /** Clears all the shadow patches. */
    public void clearShadowPatches ()
    {
        _patchMap.clear();
    }

    /** Applies the specified diff to the shadows. */
    public void applyShadowPatch (String id)
    {
        byte[] patch = _patchMap.get(id);
        if (patch != null) {
            ArrayDiffUtil.applyPatch(_shadows, patch);
        }
    }

    /** Returns the intensity of the shadows on the board. */
    public float getShadowIntensity ()
    {
        return _shadowIntensity;
    }

    /** Sets the intensity of the shadows on the board. */
    public void setShadowIntensity (float intensity)
    {
        _shadowIntensity = intensity;
    }

    /** Returns the level of the water on the board in heightfield units (-128 for no water). */
    public byte getWaterLevel ()
    {
        return _waterLevel;
    }

    /** Returns the color of the water. */
    public int getWaterColor ()
    {
        return _waterColor;
    }

    /** Returns the amplitude of the water waves. */
    public float getWaterAmplitude ()
    {
        return _waterAmplitude;
    }

    /** Sets the water parameters. */
    public void setWaterParams (byte level, int color, float amplitude)
    {
        _waterLevel = level;
        _waterColor = color;
        _waterAmplitude = amplitude;
    }

    /**
     * Returns the azimuth in radians of the directional light at the specified index, where zero
     * has the light in the positive x direction and increasing values rotate the light
     * counter-clockwise.
     */
    public float getLightAzimuth (int idx)
    {
        return _lightAzimuths[idx];
    }

    /**
     * Returns the elevation in radians of the directional light at the specified index, where zero
     * has the light on the horizon and pi/2 has the light exactly overhead.
     */
    public float getLightElevation (int idx)
    {
        return _lightElevations[idx];
    }

    /**
     * Returns the diffuse RGB color of the directional light at the specified index.
     */
    public int getLightDiffuseColor (int idx)
    {
        return _lightDiffuseColors[idx];
    }

    /**
     * Returns the ambient RGB color of the directional light at the specified index.
     */
    public int getLightAmbientColor (int idx)
    {
        return _lightAmbientColors[idx];
    }

    /**
     * Sets the parameters of the directional light at the specified index.
     *
     * @param azimuth the azimuth about the board in radians
     * @param elevation the elevation above the horizon in radians
     * @param diffuseColor the RGB diffuse color
     * @param ambientColor the RGB ambient color
     */
    public void setLightParams (int idx, float azimuth, float elevation, int diffuseColor,
                                int ambientColor)
    {
        _lightAzimuths[idx] = azimuth;
        _lightElevations[idx] = elevation;
        _lightDiffuseColors[idx] = diffuseColor;
        _lightAmbientColors[idx] = ambientColor;
    }

    /**
     * Returns the RGB color at the bottom of the sky dome.
     */
    public int getSkyHorizonColor ()
    {
        return _skyHorizonColor;
    }

    /**
     * Returns the RGB color at the top of the sky dome.
     */
    public int getSkyOverheadColor ()
    {
        return _skyOverheadColor;
    }

    /**
     * Returns the exponential falloff factor for the transition between the horizon color and the
     * overhead color.  Higher numbers represent more rapid falloff.
     */
    public float getSkyFalloff ()
    {
        return _skyFalloff;
    }

    /**
     * Sets the parameters of the sky dome.
     *
     * @param horizonColor the color at the bottom of the sky dome
     * @param overheadColor the color at the top of the sky dome
     */
    public void setSkyParams (int horizonColor, int overheadColor, float falloff)
    {
        _skyHorizonColor = horizonColor;
        _skyOverheadColor = overheadColor;
        _skyFalloff = falloff;
    }

    /**
     * Returns the direction in which the wind is blowing.
     */
    public float getWindDirection ()
    {
        return _windDirection;
    }

    /**
     * Returns the speed at which the wind is blowing.
     */
    public float getWindSpeed ()
    {
        return _windSpeed;
    }

    /**
     * Sets the wind parameters.
     *
     * @param direction the direction in which the wind is blowing
     * @param speed the speed at which the wind is blowing
     */
    public void setWindParams (float direction, float speed)
    {
        _windDirection = direction;
        _windSpeed = speed;
    }

    /**
     * Returns the RGB color of the fog.
     */
    public int getFogColor ()
    {
        return _fogColor;
    }

    /**
     * Returns the density of the fog.
     */
    public float getFogDensity ()
    {
        return _fogDensity;
    }

    /**
     * Sets the fog parameters.
     *
     * @param color the RGB fog color
     * @param density the fog density
     */
    public void setFogParams (int color, float density)
    {
        _fogColor = color;
        _fogDensity = density;
    }

    /**
     * Returns the RGB color of the board grid.
     */
    public int getGridColor ()
    {
        return _gridColor;
    }

    /**
     * Sets the RGB color of the board grid.
     */
    public void setGridColor (int color)
    {
        _gridColor = color;
    }

    /**
     * Returns the bounds of the playable area on the board. <em>Do not modify</em> the returned
     * rectangle.
     */
    public Rectangle getPlayableArea ()
    {
        return _playarea;
    }

    /**
     * Computes and returns a path from the specified coordinates to the supplied piece's current
     * coordinates. Returns null if no path could be found.
     */
    public List<Point> computePath (int ox, int oy, Piece piece)
    {
        return computePath(ox, oy, piece, true);
    }

    /**
     * Computes and returns a path from the specified coordinates to the supplied piece's current
     * coordinates. Returns null if no path could be found.
     *
     * @param pdist if true, use the piece move distance
     */
    public List<Point> computePath (int ox, int oy, Piece piece, boolean pdist)
    {
        return computePath(ox, oy, piece.x, piece.y, piece, pdist);
    }

    /**
     * Computes and returns a path from the specified coordinates to the supplied
     * coordinates. Returns null if no path could be found.
     */
    public List<Point> computePath (int ox, int oy, int dx, int dy, Piece piece)
    {
        return computePath(ox, oy, dx, dy, piece, true);
    }

    /**
     * Computes and returns a path from the specified coordinates to the supplied
     * coordinates. Returns null if no path could be found.
     */
    public List<Point> computePath (int ox, int oy, int dx, int dy, Piece piece, boolean pdist)
    {
//         log.info("Computing path from " + ox + "/" + oy + " to " + piece.x + "/" + piece.y +
//                  " maxdist:" + piece.getMoveDistance() + ".");
        return AStarPathUtil.getPath(this, piece.getStepper(), piece,
                pdist ? piece.getMoveDistance() : MAX_PATH, ox, oy, dx, dy, false);
    }

    /**
     * Dumps the board's occupiability state to stderr.
     */
    public void dumpOccupiability (PointSet moves)
    {
        for (int yy = getHeight()-1; yy >= 0; yy--) {
            System.err.print(StringUtil.prepad(String.valueOf(yy), 2) + ":");
            for (int xx = 0; xx < getWidth()-1; xx++) {
                String spot = String.valueOf(_tstate[_width*yy+xx]);
                if (moves.contains(xx, yy)) {
                    spot = TermUtil.makeBold(spot);
                }
                System.err.print(" " + spot);
            }
            System.err.println("");
        }
        System.err.print("   ");
        for (int xx = 0; xx < getWidth()-1; xx++) {
            System.err.print(" -");
        }
        System.err.println("");
        System.err.print("   ");
        for (int xx = 0; xx < getWidth()-1; xx++) {
            System.err.print(" " + (xx/10));
        }
        System.err.println("");
        System.err.print("   ");
        for (int xx = 0; xx < getWidth()-1; xx++) {
            System.err.print(" " + (xx%10));
        }
        System.err.println("");
    }

    /**
     * Returns a set of coordinates for locations near to the specified coordinates into which a
     * piece can be spawned. First the coordinates immediately surrounding the location are
     * searched, then one unit away, and so on. Within a particular "shell" the coordinates are
     * searched randomly. The list may be fewer than the requested count if an insufficient number
     * of spots could be located within the specified maximum distance.
     */
    public ArrayList<Point> getOccupiableSpots (int count, int cx, int cy, int maxdist)
    {
        return getOccupiableSpots(count, cx, cy, 1, maxdist, null);
    }

    /**
     * Returns a set of coordinates for locations near to the specified coordinates into which a
     * piece can be spawned. First the coordinates immediately surrounding the location are
     * searched, then one unit away, and so on. Within a particular "shell" the coordinates are
     * searched randomly. The list may be fewer than the requested count if an insufficient number
     * of spots could be located within the specified maximum distance.
     *
     * @param rnd a random number generator, or <code>null</code> to use the default
     */
    public ArrayList<Point> getOccupiableSpots (
        int count, int cx, int cy, int mindist, int maxdist, Random rnd)
    {
        return getOccupiableSpots(count, cx, cy, mindist, maxdist, rnd, null);
    }

    /**
     * Returns a set of coordinates for locations near to the specified coordinates into which a
     * piece can be spawned. First the coordinates immediately surrounding the location are
     * searched, then one unit away, and so on. Within a particular "shell" the coordinates are
     * searched randomly. The list may be fewer than the requested count if an insufficient number
     * of spots could be located within the specified maximum distance.
     *
     * @param rnd a random number generator, or <code>null</code> to use the default
     * @param reserved a set of points that are not valid
     */
    public ArrayList<Point> getOccupiableSpots (
        int count, int cx, int cy, int mindist, int maxdist, Random rnd, PointSet reserved)
    {
        ArrayList<Point> ospots = new ArrayList<Point>();
        PointSet spots = new PointSet();
      SEARCH:
        for (int dist = mindist; dist <= maxdist; dist++) {
            spots.clear();
            spots.addFrame(cx, cy, dist, _playarea);
            int[] coords = spots.toIntArray();
            if (rnd == null) {
                ArrayUtil.shuffle(coords);
            } else {
                ArrayUtil.shuffle(coords, rnd);
            }
            for (int ii = 0; ii < coords.length; ii++) {
                int hx = PointSet.decodeX(coords[ii]), hy = PointSet.decodeY(coords[ii]);
                if (isOccupiable(hx, hy) && (reserved == null || !reserved.contains(hx, hy))) {
                    ospots.add(new Point(hx, hy));
                    if (ospots.size() == count) {
                        break SEARCH;
                    }
                }
            }
        }
        return ospots;
    }

    /**
     * Returns the coordinates of a location near to the specified coordinates into which a piece
     * can be spawned. First the coordinates immediately surrounding the location are searched,
     * then one unit away, and so on. Within a particular "shell" the coordinates are searched
     * randomly. Returns null if no occupiable spot could be located.
     */
    public Point getOccupiableSpot (int cx, int cy, int maxdist)
    {
        return getOccupiableSpot(cx, cy, 1, maxdist, null);
    }

    /**
     * Returns the coordinates of a location near to the specified coordinates into which a piece
     * can be spawned. First the coordinates immediately surrounding the location are searched,
     * then one unit away, and so on. Within a particular "shell" the coordinates are searched
     * randomly. Returns null if no occupiable spot could be located.
     *
     * @param rnd a random number generator, or <code>null</code> to use the default
     */
    public Point getOccupiableSpot (int cx, int cy, int mindist, int maxdist, Random rnd)
    {
        return getOccupiableSpot (cx, cy, mindist, maxdist, rnd, null);
    }

    /**
     * Returns the coordinates of a location near to the specified coordinates into which a piece
     * can be spawned. First the coordinates immediately surrounding the location are searched,
     * then one unit away, and so on. Within a particular "shell" the coordinates are searched
     * randomly. Returns null if no occupiable spot could be located.
     *
     * @param rnd a random number generator, or <code>null</code> to use the default
     * @param reserved a set of points that are not valid
     */
    public Point getOccupiableSpot (
            int cx, int cy, int mindist, int maxdist, Random rnd, PointSet reserved)
    {
        ArrayList<Point> spots = getOccupiableSpots(1, cx, cy, mindist, maxdist, rnd, reserved);
        return (spots.size() > 0) ? spots.get(0) : null;
    }

    /**
     * Returns a set of coordinates for locations surrounding specified coordinates into which a
     * piece can be spawned.
     */
    public ArrayList<Point> getRandomOccupiableSpots (
        int count, int cx, int cy, int mindist, int maxdist)
    {
        ArrayList<Point> ospots = new ArrayList<Point>();
        PointSet spots = new PointSet();
        for (int dist = mindist; dist <= maxdist; dist++) {
            spots.addFrame(cx, cy, dist, _playarea);
        }
        int[] coords = spots.toIntArray();
        ArrayUtil.shuffle(coords);
      SEARCH:
        for (int ii = 0; ii < coords.length; ii++) {
            int hx = PointSet.decodeX(coords[ii]), hy = PointSet.decodeY(coords[ii]);
            if (isOccupiable(hx, hy)) {
                ospots.add(new Point(hx, hy));
                if (ospots.size() == count) {
                    break SEARCH;
                }
            }
        }
        return ospots;
    }

    /**
     * Adds the supplied set of pieces to our board "shadow" data. This is done at the start of the
     * game; all subsequent changes are incremental.
     */
    public void shadowPieces (Iterator<Piece> iter)
    {
        shadowPieces(iter, 0, 0, _width, _height);
    }

    /**
     * Adds the supplied set of pieces to our board "shadow" data. This is done at the start of the
     * game; all subsequent changes are incremental.  Only the specified region will be affected.
     */
    public void shadowPieces (Iterator<Piece> iter, int x, int y, int width, int height)
    {
        // start out with _tstate configured according to the board
        for (int yy = y, ymax = y + height; yy < ymax; yy++) {
            for (int xx = x, xmax = x + width; xx < xmax; xx++) {
                byte tvalue;
                if (isUnderDeepWater(xx, yy)) {
                    tvalue = O_IMPASS;
                } else if (exceedsMaxHeightDelta(xx, yy)) {
                    tvalue = O_ROUGH;
                } else {
                    tvalue = O_FLAT;
                }
                int pos = _width*yy+xx;
                _tstate[pos] = tvalue;
                _btstate[pos] = tvalue;
                _dstate[pos] = 0;
                _estate[pos] = 0;
            }
        }

        Rectangle rect = new Rectangle(x, y, width, height);
        while (iter.hasNext()) {
            Piece piece = iter.next();
            if (piece.intersects(rect)) {
                shadowPiece(piece);
            }
        }
    }

    /**
     * Clears the shadow for the specified piece, restoring that board tile to its default state.
     */
    public void clearShadow (Piece piece)
    {
        if (!_playarea.contains(piece.x, piece.y)) {
            return;
        }
        int pos = _width*piece.y+piece.x;
        if (piece instanceof Bonus && _tstate[pos] >= 0) {
            // we may clear a bonus after a piece has moved into place to pick it up, in which case
            // we want to do nothing
        } else if (_tstate[pos] == O_OCC_AND_AIR) {
            if (piece instanceof Train) {
                _tstate[pos] = O_AIRBORNE;
            } else {
                _tstate[pos] = O_OCCUPIED;
            }
        } else {
            _tstate[pos] = _btstate[pos];
        }
    }

    /**
     * Configures a shadow for the specified piece.
     */
    public void shadowPiece (Piece piece)
    {
        if (piece instanceof Prop && ((Prop)piece).isPassable()) {
            Prop p = (Prop)piece;
            Rectangle pbounds = p.getBounds();
            float pelev = p.getPassHeight();
            int elevation = (int)Math.ceil(pelev * _elevationUnitsPerTile * p.getScale().z) +
                p.felev;
            int telev = piece.computeElevation(this, p.x, p.y) - p.felev;

            for (int yy = pbounds.y, ly = yy + pbounds.height; yy < ly; yy++) {
                for (int xx = pbounds.x, lx = xx + pbounds.width; xx < lx; xx++) {
                    if (_playarea.contains(xx, yy)) {
                        int idx = _width * yy + xx;
                        byte dstate = _dstate[idx];
                        for (int dir : DIRECTIONS) {
                            if (!p.canEnter(dir, xx, yy)) {
                                dstate = (byte)(dstate | (ENTER_NORTH << dir));
                            }
                            if (!p.canExit(dir, xx, yy)) {
                                dstate = (byte)(dstate | (EXIT_NORTH << dir));
                            }
                        }
                        _dstate[idx] = dstate;
                        int tileelev = elevation + telev - getWaterElevation(xx, yy);
                        int oldelev = unsignedToInt(_estate[idx]);
                        if (tileelev > oldelev) {
                            if (pelev > 0) {
                                if (_btstate[idx] < 0) {
                                    _tstate[idx] = _btstate[idx] = O_BRIDGE;
                                }
                                _estate[idx] = (byte)tileelev;
                            }
                        }
                        if (p.shadowBonus() && _tstate[idx] >= O_BRIDGE) {
                            _tstate[idx] = O_BONUS;
                        }
                    }
                }
            }
            return;

        } else if (piece instanceof BigPiece) {
            BigPiece bpiece = (BigPiece)piece;
            byte ptype = createPropType(bpiece.isTall(), bpiece.isPenetrable(),
                                        bpiece.willBeTargetable());
            int elevation = (int)Math.ceil(bpiece.getHeight() * _elevationUnitsPerTile);
            if (bpiece instanceof Prop) {
                Prop prop = (Prop)bpiece;
                elevation *= prop.getScale().z;
                elevation += prop.felev;
            }

            Rectangle pbounds = bpiece.getBounds();
            for (int yy = pbounds.y, ly = yy + pbounds.height; yy < ly; yy++) {
                for (int xx = pbounds.x, lx = xx + pbounds.width; xx < lx; xx++) {
                    if (_playarea.contains(xx, yy)) {
                        int idx = _width*yy + xx;
                        int oldelev = unsignedToInt(_estate[idx]);
                        if (_btstate[idx] < O_BONUS || oldelev < elevation) {
                            _tstate[idx] = _btstate[idx] = combinePropTypes(_btstate[idx], ptype);
                        }
                        _estate[idx] = (byte)Math.max(unsignedToInt(_estate[idx]), elevation);
                    }
                }
            }

        } else if (!_playarea.contains(piece.x, piece.y)) {
            return;

        } else if (piece instanceof Marker) {
            if (Marker.isMarker(piece, Marker.IMPASS)) {
                int idx = _width * piece.y + piece.x;
                _tstate[idx] = _btstate[idx] = O_IMPASS;
            }
            return;

        } else if (piece instanceof Track) {
            int idx = _width*piece.y+piece.x;
            _estate[idx] = (byte)Math.max(
                unsignedToInt(_estate[idx]),
                (int)Math.ceil(piece.getHeight() * _elevationUnitsPerTile));
            if (((Track)piece).preventsGroundOverlap()) {
                _tstate[idx] = _btstate[idx] =
                    combinePropTypes(_btstate[idx], createPropType(false, true, false));
            }

        } else if (piece instanceof Bonus) {
            _tstate[_width*piece.y+piece.x] = O_BONUS;

        } else if (piece instanceof Train) {
            int idx = _width * piece.y + piece.x;
            _tstate[idx] = (_tstate[idx] == O_AIRBORNE ? O_OCC_AND_AIR : O_OCCUPIED);

        } else if (piece instanceof Cow || piece.owner < 0) {
            _tstate[_width*piece.y+piece.x] = O_OCCUPIED;

        } else {
            _tstate[_width*piece.y+piece.x] = (piece.isAirborne() && piece.isAlive()) ?
                O_AIRBORNE : getTeam(piece);
        }
    }

    /**
     * Used to set a temporary shadow value.
     */
    public byte shadowPieceTemp (Piece piece, int x, int y)
    {
        if (!_playarea.contains(x, y)) {
            return 0;
        }

        int idx = _width * y + x;
        byte old = _tstate[idx];
        if (piece instanceof Cow || piece instanceof Train || piece.owner < 0) {
            _tstate[idx] = O_OCCUPIED;
        } else {
            _tstate[idx] = getTeam(piece);
        }
        return old;
    }

    /**
     * Used to reset a temporary shadow value.
     */
    public void clearShadowTemp (byte state, int x, int y)
    {
        if (!_playarea.contains(x, y)) {
            return;
        }
        _tstate[_width * y + x] = state;
    }

    /**
     * Returns the combined elevation (heightfield elevation plus piece elevation, or the water
     * height if underwater) at the specified tile coordinates.
     */
    public int getElevation (int x, int y)
    {
        return Math.max(_waterLevel, getWaterElevation(x, y) + getPieceElevation(x, y));
    }

    /**
     * Returns the max of the water level and heightfield elevation.
     */
    public int getWaterElevation (int x, int y)
    {
        return Math.max(_waterLevel, getHeightfieldElevation(x, y));
    }

    /**
     * Returns the combined elevation (max heightfield elevation plus piece elevation, or the water
     * height if underwater) at the specified tile coordinates.
     */
    public int getMaxElevation (int x, int y)
    {
        return Math.max(getMaxHeightfieldElevation(x, y), getElevation(x, y));
    }

    /**
     * Returns the heightfield elevation (the elevation of the terrain in elevation units) at the
     * specified tile coordinates.
     */
    public int getHeightfieldElevation (int x, int y)
    {
        // for now, just grab the heightfield value in the center of the tile
        int offset = HEIGHTFIELD_SUBDIVISIONS/2;
        return getHeightfieldValue(x*HEIGHTFIELD_SUBDIVISIONS + offset,
                                   y*HEIGHTFIELD_SUBDIVISIONS + offset);
    }

    /**
     * Returns the max heightfield elevation (the elevation of the terrain in elevation units) at
     * the specified tile coordinates.
     */
    public int getMaxHeightfieldElevation (int x, int y)
    {
        if (_maxHeight == null) {
            _maxHeight = new int[_width * _height];
        }
        if (_heightfieldChanged) {
            for (int idx = 0; idx < _maxHeight.length; idx++) {
                _maxHeight[idx] = Integer.MIN_VALUE;
            }
            _heightfieldChanged = false;
        }

        int idx = y * _width + x;
        if (idx >= 0 && idx < _maxHeight.length && _maxHeight[idx] > Integer.MIN_VALUE) {
            return _maxHeight[idx];
        }

        int hx = Math.min((x + 1) * HEIGHTFIELD_SUBDIVISIONS, _hfwidth - 1);
        int hy = Math.min((y + 1) * HEIGHTFIELD_SUBDIVISIONS, _hfheight - 1);
        x = Math.max(x * HEIGHTFIELD_SUBDIVISIONS, 0);
        y = Math.max(y * HEIGHTFIELD_SUBDIVISIONS, 0);
        int height = Integer.MIN_VALUE;
        for (int dy = y; dy <= hy; dy++) {
            for (int dx = x; dx <= hx; dx++) {
                height = Math.max(height, _heightfield[dy * _hfwidth + dx]);
            }
        }
        if (idx >= 0 && idx < _maxHeight.length) {
            _maxHeight[idx] = height;
        }
        return height;
    }

    /**
     * Returns the piece elevation (the height of the piece in elevation units) at the specified
     * tile coordinates.
     */
    public int getPieceElevation (int x, int y)
    {
        return (x < 0 || y < 0 || x >= _width || y >= _height) ?
            0 : unsignedToInt(_estate[y*_width+x]);
    }

    /**
     * Checks whether any adjacent vertices in the heightfield under the specified tile coordinates
     * exceed the maximum height delta.
     */
    public boolean exceedsMaxHeightDelta (int tx, int ty)
    {
        int x1 = tx * HEIGHTFIELD_SUBDIVISIONS, y1 = ty * HEIGHTFIELD_SUBDIVISIONS,
            x2 = (tx+1) * HEIGHTFIELD_SUBDIVISIONS, y2 = (ty+1) * HEIGHTFIELD_SUBDIVISIONS;
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                int ll = getHeightfieldValue(x, y), d1 = getHeightfieldValue(x, y+1) - ll,
                    d2 = getHeightfieldValue(x+1, y+1) - ll, d3 = getHeightfieldValue(x+1, y) - ll;
                if (Math.abs(d1) > MAX_OCCUPIABLE_HEIGHT_DELTA ||
                    Math.abs(d2) > MAX_OCCUPIABLE_HEIGHT_DELTA ||
                    Math.abs(d3) > MAX_OCCUPIABLE_HEIGHT_DELTA) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether any portion of the specified tile is beneath any amount of water.
     */
    public boolean isUnderWater (int tx, int ty)
    {
        return isUnderWater(tx, ty, (byte)0);
    }

    /**
     * Checks whether any portion of the specified tile is beneath sufficient water that we can't
     * walk on it.
     */
    public boolean isUnderDeepWater (int tx, int ty)
    {
        return isUnderWater(tx, ty, MAX_OCCUPIABLE_WATER_LEVEL);
    }

    /**
     * Checks whether any portion of the specified tile is beneath the specified level of water.
     */
    public boolean isUnderWater (int tx, int ty, byte level)
    {
        int x1 = tx * HEIGHTFIELD_SUBDIVISIONS, y1 = ty * HEIGHTFIELD_SUBDIVISIONS,
            x2 = (tx+1) * HEIGHTFIELD_SUBDIVISIONS, y2 = (ty+1) * HEIGHTFIELD_SUBDIVISIONS;
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                if (getHeightfieldValue(x, y) < _waterLevel - level) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the specified location is ground traversable.
     */
    public boolean isGroundOccupiable (int x, int y)
    {
        return isGroundOccupiable(x, y, false);
    }

    /**
     * Returns true if the specified location is ground traversable.
     *
     * @param rough whether or not to allow the occupation of rough terrain and the border areas
     */
    public boolean isGroundOccupiable (int x, int y, boolean rough)
    {
        if (x < 0 || x >= _width || y < 0 || y >= _height || !(rough || _playarea.contains(x, y))) {
            return false;
        }
        byte btstate = _btstate[y*_width+x];
        return (btstate == O_FLAT) || (btstate == O_BRIDGE) || (rough && btstate == O_ROUGH);
    }

    /**
     * Returns the downslope direction of rough terrain or -1 if flat.
     */
    public int getTerrainSlope (int x, int y)
    {
        if (!_playarea.contains(x, y)) {
            return -1;
        }

        int x1 = x * HEIGHTFIELD_SUBDIVISIONS, y1 = y * HEIGHTFIELD_SUBDIVISIONS,
            x2 = x1 + HEIGHTFIELD_SUBDIVISIONS / 2, y2 = y1 + HEIGHTFIELD_SUBDIVISIONS / 2,
            x3 = x1 + HEIGHTFIELD_SUBDIVISIONS, y3 = y1 + HEIGHTFIELD_SUBDIVISIONS;
        int dx = getHeightfieldValue(x1, y2) - getHeightfieldValue(x3, y2),
            dy = getHeightfieldValue(x2, y1) - getHeightfieldValue(x2, y3);
        if (Math.abs(dy) > Math.abs(dx) &&
            (float)Math.abs(dy) / _elevationUnitsPerTile > MIN_SLOPE) {
            return (dy < 0 ? NORTH : SOUTH);
        } else if ((float)Math.abs(dx) / _elevationUnitsPerTile > MIN_SLOPE) {
            return (dx > 0 ? EAST : WEST);
        }
        return -1;
    }

    // from interface AStarPathUtil.TraversalPred
    public boolean canTraverse (Object traverser, int x, int y)
    {
        return canTravel((Piece)traverser, x, y, false);
    }

    // from interface AStarPathUtil.ExtendedTraversalPred
    public boolean canTraverse (Object traverser, int sx, int sy, int dx, int dy)
    {
        return canTravel((Piece)traverser, sx, sy, dx, dy, false);
    }

    /**
     * Returns true if the specified piece can occupy the specified coordinate.
     */
    public boolean canOccupy (Piece piece, int x, int y)
    {
        return canTravel(piece, x, y, true);
    }

    /**
     * If remain is true, returns true if the specified piece can occupy the specified coordinate.
     * If remain is false, returns true if the piece can traverse (but not necessarily remain on)
     * the specified coordinate.
     */
    public boolean canTravel (Piece piece, int x, int y, boolean remain)
    {
        return canTravel(piece, x, y, x, y, remain);
    }

    /**
     * If remain is true, returns true if the specified piece can move to and occupy the specified
     * coordinate.  If remain is false, returns true if the piece can traverse (but not necessarily
     * remain on) the specified coordinate.
     */
    public boolean canTravel (Piece piece, int sx, int sy, int dx, int dy, boolean remain)
    {
        if (!_playarea.contains(dx, dy)) {
            return false;
        }

        // the piece can always occupy it's current location
        if (piece.x == dx && piece.y == dy && canCross(sx, sy, dx, dy)) {
            return true;
        }

        // we accord flyer status to trains as they need to go "over" some props, but will
        // otherwise not do funny things
        int idx = dy*_width+dx;
        byte tstate = _tstate[idx];
        boolean flightstate = (remain ? piece.isAirborne() :
                               (piece.isFlyer() || !piece.isCorporeal()));
        if ((flightstate && (!remain || (tstate <= O_BRIDGE && (tstate & TALL_FLAG) != 0)) &&
             (!piece.isCorporeal() || tstate > O_PROP || (tstate & TALL_FLAG) != 0 ||
              piece instanceof Train ||
              (!remain && (tstate & TARGETABLE_FLAG) == 0 && piece.getMinFireDistance() == 0)))) {
            return true;

        } else if ((tstate == O_FLAT || tstate == O_BRIDGE) ||
                   (piece instanceof Unit && tstate == O_BONUS) ||
                   ((tstate == getTeam(piece) || tstate == O_AIRBORNE) &&
                    (_btstate[idx] == O_FLAT || _btstate[idx] == O_BRIDGE) && !remain)) {
            return canCross(sx, sy, dx, dy);
        }

        return false;
    }

    /**
     * Returns true if a unit can move between these two tiles.
     */
    public boolean canCross (int sx, int sy, int dx, int dy)
    {
        if (sx == dx && sy == dy) {
            return true;
        }
        if (!_playarea.contains(sx, sy) || !_playarea.contains(dx, dy)) {
            return false;
        }
        if (Math.abs(sx - dx) + Math.abs(sy - dy) > 1) {
            return false;
        }
        if (((isBridge(sx, sy) && !isTargetable(dx, dy)) ||
             (isBridge(dx, dy) && !isTargetable(sx, sy))) &&
            Math.abs(getElevation(sx, sy) - getElevation(dx, dy)) >
            MAX_OCCUPIABLE_HEIGHT_DELTA) {
            return false;
        }
        return canCrossSide(sx, sy, dx, dy);
    }

    /**
     * Returns true if a unit can move between these two tiles based on direction.
     */
    public boolean canCrossSide (int sx, int sy, int dx, int dy)
    {
        // the source direction is the opposite of the destination
        int dir = (sx == dx) ? (dy > sy ? NORTH : SOUTH) : (dx < sx ? EAST : WEST);

        // the side is not crossible
        int sdir = (dir + 2) % 4;
        return (((_dstate[dy*_width+dx] & (ENTER_NORTH << dir)) == 0) &&
                ((_dstate[sy*_width+sx] & (EXIT_NORTH << sdir)) == 0));
    }

    /**
     * Returns true if the specified coordinate is both unoccupied by any other piece and
     * traversable.
     */
    public boolean isOccupiable (int x, int y)
    {
        if (!_playarea.contains(x, y)) {
            return false;
        }
        byte tstate = _tstate[y*_width+x];
        return (tstate == O_FLAT || tstate == O_BRIDGE);
    }

    /**
     * Returns true if the specified coordinate is unoccupied or occupied by a teammate and is
     * traversable.
     */
    public boolean isWalkable (int x, int y, Piece piece)
    {
        return isTraversable(x, y) && (isOccupiable(x, y) || _tstate[y*_width+x] == getTeam(piece));
    }

    /**
     * Returns true if the specified coordinate is traversable.
     */
    public boolean isTraversable (int x, int y)
    {
        if (!_playarea.contains(x, y)) {
            return false;
        }
        byte btstate = _btstate[y*_width+x];
        return (btstate == O_FLAT || btstate == O_BRIDGE);
    }

    /**
     * Returns true if the specified coordinate is a bridge.
     */
    public boolean isBridge (int x, int y)
    {
        if (!_playarea.contains(x, y)) {
            return false;
        }
        return (_btstate[y*_width+x] == O_BRIDGE);
    }

    /**
     * Returns true if the specified coordinate has a targetable prop.
     */
    public boolean isTargetable (int x, int y)
    {
        if (!_playarea.contains(x, y)) {
            return false;
        }
        byte tstate = _tstate[y*_width+x];
        return tstate <= O_PROP && (_tstate[y*_width+x] & TARGETABLE_FLAG) == 0;
    }

    /**
     * Returns true if the specified coordinate is penatrable
     */
    public boolean isPenetrable (int x, int y)
    {
        if (!_playarea.contains(x, y)) {
            return false;
        }
        byte btstate = _btstate[y*_width + x];
        return !(btstate <= O_PROP && (btstate & PENETRABLE_FLAG) != 0);
    }

    /**
     * Returns true if the specified coordinate contains a bonus.
     */
    public boolean containsBonus (int x, int y)
    {
        if (!_playarea.contains(x, y)) {
            return false;
        }
        return (_tstate[y*_width+x] == O_BONUS);
    }

    /**
     * Computes the supplied piece's move sets based on its current
     * location and the state of the board. A set of attackable locations
     * is also computed, but note that these do not take into account a
     * piece's minimum fire distance. Targets made unfirable due to being
     * too close must be pruned by the caller.
     */
    public void computeMoves (Piece piece, PointSet moves, PointSet attacks)
    {
        computeMoves(piece, moves, attacks, piece.getMoveDistance());
    }

    /**
     * Computes the supplied piece's move sets based on its current
     * location and the state of the board. A set of attackable locations
     * is also computed, but note that these do not take into account a
     * piece's minimum fire distance. Targets made unfirable due to being
     * too close must be pruned by the caller.
     */
    public void computeMoves (
            Piece piece, PointSet moves, PointSet attacks, int mdist)
    {
        // clear out the planning grid
        Arrays.fill(_pgrid, (byte)0);

//         log.info("Recomputing sets for " + piece, "mdist", mdist);

        // start with 10x our movement points at our current coordinate
        // (and add one to ensure that we always end up with 1 in our
        // final coordinate)
        byte remain = (byte)(mdist * BASE_TRAVERSAL + 1);
        _pgrid[piece.y*_width+piece.x] = remain;

        // now consider each of our four neighbors
        considerMoving(
                piece, moves, piece.x, piece.y, piece.x+1, piece.y, remain);
        considerMoving(
                piece, moves, piece.x, piece.y, piece.x-1, piece.y, remain);
        considerMoving(
                piece, moves, piece.x, piece.y, piece.x, piece.y+1, remain);
        considerMoving(
                piece, moves, piece.x, piece.y, piece.x, piece.y-1, remain);

        // next prune any moves that "land" us on a tile occupied by a piece
        // (we allow units to move through certain pieces on their way but not
        // to stop ontop of another unit)
        remain = (byte)piece.getMaxFireDistance();
        for (int ii = 0, ll = moves.size(); ii < ll; ii++) {
            int x = moves.getX(ii), y = moves.getY(ii), idx = y*_width+x;
            if (!canOccupy(piece, x, y)) {
                moves.remove(x, y);
                if (attacks != null && remain == 0 &&
                        hasReachableNeighbor(piece, x, y)) {
                    attacks.add(x, y);
                }
                _pgrid[idx] = 0;
                ii--;
                ll--;
            }
        }

        // finally add the piece's current location back to the list
        moves.add(piece.x, piece.y);

        // if the attack set is non-null, compute our attacks as well
        if (attacks != null && remain > 0) {
            considerFiring(attacks, piece.x, piece.y, remain, false);
            for (int ii = 0, ll = moves.size(); ii < ll; ii++) {
                int mx = moves.getX(ii), my = moves.getY(ii);
                considerFiring(attacks, mx, my, remain, false);
            }
        }
    }

    /**
     * Computes a set of possible attacks given the specified fire
     * distance.
     */
    public void computeAttacks (int minFireDistance, int maxFireDistance,
                                int px, int py, PointSet attacks)
    {
        for (int dd = minFireDistance; dd <= maxFireDistance; dd++) {
            for (int xx = px, yy = py - dd; yy < py; xx++, yy++) {
                if (_playarea.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
            for (int xx = px + dd, yy = py; xx > px; xx--, yy++) {
                if (_playarea.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
            for (int xx = px, yy = py + dd; yy > py; xx--, yy--) {
                if (_playarea.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
            for (int xx = px - dd, yy = py; xx < px; xx++, yy--) {
                if (_playarea.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
        }
    }

    /** Returns a string representation of this board. */
    public String toString ()
    {
        return "[" + _width + "x" + _height + "]";
    }

    // documentation inherited from interface Savable
    public Class<?> getClassTag ()
    {
        return getClass();
    }

    // documentation inherited from interface Savable
    public void read (JMEImporter im)
        throws IOException
    {
        InputCapsule capsule = im.getCapsule(this);
        _width = capsule.readInt("width", 0);
        _height = capsule.readInt("height", 0);
        _heightfield = capsule.readByteArray("heightfield", null);
        _elevationUnitsPerTile = capsule.readByte("elevationUnitsPerTile",
            (byte)64);
        _terrain = capsule.readByteArray("terrain", null);
        _shadows = capsule.readByteArray("shadows", null);
        _shadowIntensity = capsule.readFloat("shadowIntensity", 1f);
        _waterLevel = capsule.readByte("waterLevel", (byte)-128);
        _waterColor = capsule.readInt("waterColor", 0x003232);
        _waterAmplitude = capsule.readFloat("waterAmplitude", 25f);
        _lightAzimuths = capsule.readFloatArray("lightAzimuths", null);
        _lightElevations = capsule.readFloatArray("lightElevations", null);
        _lightDiffuseColors = capsule.readIntArray("lightDiffuseColors", null);
        _lightAmbientColors = capsule.readIntArray("lightAmbientColors", null);
        _skyHorizonColor = capsule.readInt("skyHorizonColor", 0xFFFFFF);
        _skyOverheadColor = capsule.readInt("skyOverheadColor", 0x00FFFF);
        _skyFalloff = capsule.readFloat("skyFalloff", 10f);
        _windDirection = capsule.readFloat("windDirection", 0f);
        _windSpeed = capsule.readFloat("windSpeed", 20f);
        _fogColor = capsule.readInt("fogColor", 0xFFFFFF);
        _fogDensity = capsule.readFloat("fogDensity", 0f);
        _gridColor = capsule.readInt("gridColor", 0x808080);
        String[] keys = capsule.readStringArray("patchMapKeys", null);
        byte[][] values = capsule.readByteArray2D("patchMapValues", null);
        _patchMap = new StreamableHashMap<String, byte[]>();
        for (int ii = 0; ii < keys.length; ii++) {
            _patchMap.put(keys[ii], values[ii]);
        }
        initTransientFields();
    }

    // documentation inherited from interface Savable
    public void write (JMEExporter ex)
        throws IOException
    {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_width, "width", 0);
        capsule.write(_height, "height", 0);
        capsule.write(_heightfield, "heightfield", null);
        capsule.write(_elevationUnitsPerTile, "elevationUnitsPerTile",
            (byte)64);
        capsule.write(_terrain, "terrain", null);
        capsule.write(_shadows, "shadows", null);
        capsule.write(_shadowIntensity, "shadowIntensity", 1f);
        capsule.write(_waterLevel, "waterLevel", (byte)-128);
        capsule.write(_waterColor, "waterColor", 0x003232);
        capsule.write(_waterAmplitude, "waterAmplitude", 25f);
        capsule.write(_lightAzimuths, "lightAzimuths", null);
        capsule.write(_lightElevations, "lightElevations", null);
        capsule.write(_lightDiffuseColors, "lightDiffuseColors", null);
        capsule.write(_lightAmbientColors, "lightAmbientColors", null);
        capsule.write(_skyHorizonColor, "skyHorizonColor", 0xFFFFFF);
        capsule.write(_skyOverheadColor, "skyOverheadColor", 0x00FFFF);
        capsule.write(_skyFalloff, "skyFalloff", 10f);
        capsule.write(_windDirection, "windDirection", 0f);
        capsule.write(_windSpeed, "windSpeed", 20f);
        capsule.write(_fogColor, "fogColor", 0xFFFFFF);
        capsule.write(_fogDensity, "fogDensity", 0f);
        capsule.write(_gridColor, "gridColor", 0x808080);
        capsule.write(_patchMap.keySet().toArray(new String[_patchMap.size()]),
            "patchMapKeys", null);
        capsule.write(_patchMap.values().toArray(new byte[_patchMap.size()][]),
            "patchMapValues", null);
    }

    /**
     * Extends default behavior to initialize transient members.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        initTransientFields();
    }

    /**
     * Updates the array of predominant terrain values for all tiles.
     */
    public void updatePredominantTerrain ()
    {
        updatePredominantTerrain(0, 0, _width, _height);
    }

    /**
     * Updates the array of predominant terrain values for the tiles in the
     * specified rectangle.
     */
    public void updatePredominantTerrain (int x, int y, int width, int height)
    {
        for (int ty = y, tymax = y + height; ty < tymax; ty++) {
            for (int tx = x, txmax = x + width; tx < txmax; tx++) {
                _pterrain[ty*_width + tx] = computePredominantTerrain(tx, ty);
            }
        }
    }

    /**
     * Returns the most common terrain code under the specified tile.
     */
    public byte getPredominantTerrain (int x, int y)
    {
        return _pterrain[y*_width + x];
    }

    /**
     * Determines whether a line of sight exists between the two specified
     * tile coordinates (plus elevations).
     */
    public boolean checkLineOfSight (
        int x1, int y1, int e1, int x2, int y2, int e2)
    {
        // adjacent/coincident tiles are always in line of sight
        int mlen = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (mlen <= 1) {
            return true;
        }

        // iterate along the longer of horizontal and vertical lengths
        float dx = (float)(x2 - x1) / mlen, dy = (float)(y2 - y1) / mlen,
            de = (float)(e2 - e1) / mlen, xx = x1 + dx, yy = y1 + dy,
            ee = e1 + de;
        for (int ii = 1; ii < mlen; ii++, xx += dx, yy += dy, ee += de) {
            int ix = (int)xx, iy = (int)yy,
                hfelev = getHeightfieldElevation(ix, iy);
            if (hfelev > ee) {
                return false; // terrain is in the way
            }
            if (hfelev + getPieceElevation(ix, iy) > ee) {
                if (!isPenetrable(ix, iy)) {
                    return false; // non-penetrable prop is in the way
                }
            }
        }
        return true;
    }

    /**
     * Initializes the transient fields to their default states.
     */
    protected void initTransientFields ()
    {
        _hfwidth = _width * HEIGHTFIELD_SUBDIVISIONS + 1;
        _hfheight = _height * HEIGHTFIELD_SUBDIVISIONS + 1;

        int size = _width*_height;
        _pterrain = new byte[size];
        _btstate = new byte[size];
        _estate = new byte[size];
        _tstate = new byte[size];
        _dstate = new byte[size];
        _pgrid = new byte[size];

        _playarea = new Rectangle(BORDER_SIZE, BORDER_SIZE,
                                  _width - 2*BORDER_SIZE,
                                  _height - 2*BORDER_SIZE);

        updateMinEdgeHeight();
        updateEdgeTerrain();
        updatePredominantTerrain();
    }

    /**
     * Finds and returns the predominant terrain code under the specified tile.
     */
    protected byte computePredominantTerrain (int tx, int ty)
    {
        IntIntMap counts = new IntIntMap();
        int x1 = tx * HEIGHTFIELD_SUBDIVISIONS,
            y1 = ty * HEIGHTFIELD_SUBDIVISIONS,
            x2 = (tx+1) * HEIGHTFIELD_SUBDIVISIONS,
            y2 = (ty+1) * HEIGHTFIELD_SUBDIVISIONS,
            code, count, maxCode = 0, maxCount = 0;
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                code = getTerrainValue(x, y);
                if ((count = counts.increment(code, 1)) > maxCount) {
                    maxCode = code;
                    maxCount = count;
                }
            }
        }
        return (byte)maxCode;
    }

    /** Helper function for {@link #computeMoves}. */
    protected void considerMoving (Piece piece, PointSet moves,
            int sx, int sy, int xx, int yy, byte remain)
    {
        // make sure this coordinate is traversable
        if (!_playarea.contains(xx, yy) ||
                !canTraverse(piece, sx, sy, xx, yy)) {
            return;
        }

        // see if we can move into this square with a higher remaining
        // point count than has already been accomplished
        int pos = yy*_width+xx;
        byte premain = (byte)(remain - piece.traversalCost(
            TerrainConfig.getConfig(getPredominantTerrain(xx, yy))));
        byte current = _pgrid[pos];
        if (premain <= current) {
            return;
        }

        // if so, do it
        moves.add(xx, yy);
        _pgrid[pos] = premain;

        // and then check all of our neighbors
        considerMoving(piece, moves, xx, yy, xx+1, yy, premain);
        considerMoving(piece, moves, xx, yy, xx-1, yy, premain);
        considerMoving(piece, moves, xx, yy, xx, yy+1, premain);
        considerMoving(piece, moves, xx, yy, xx, yy-1, premain);
    }

    /** Helper function for {@link #computeMoves}. */
    protected void considerFiring (
        PointSet attacks, int xx, int yy, byte remain, boolean checkThisSpot)
    {
        // make sure this coordinate is on the board
        if (!_playarea.contains(xx, yy)) {
            return;
        }

        byte premain = remain;
        if (checkThisSpot) {
            // if this position is non-zero bail
            int pos = yy*_width+xx;
            if (_pgrid[pos] != 0) {
                return;
            }

            // otherwise fill it with our current remaining fire distance
            attacks.add(xx, yy);
            premain = (byte)(remain - 1);
            _pgrid[pos] = (byte)(-1 * premain);
        }

        // and then check all of our neighbors
        if (premain > 0) {
            considerFiring(attacks, xx+1, yy, premain, true);
            considerFiring(attacks, xx-1, yy, premain, true);
            considerFiring(attacks, xx, yy+1, premain, true);
            considerFiring(attacks, xx, yy-1, premain, true);
        }
    }

    /** Helper function for {@link computeMoves} */
    protected boolean hasReachableNeighbor (Piece piece, int x, int y)
    {
        byte weight = _pgrid[y * _width + x];
        return canMoveTo(piece, x + 1, y, weight) ||
               canMoveTo(piece, x - 1, y, weight) ||
               canMoveTo(piece, x, y + 1, weight) ||
               canMoveTo(piece, x, y - 1, weight);
    }

    /** Helper fucntion for {@link hasReachableNeighbor} */
    protected boolean canMoveTo (Piece piece, int x, int y, byte weight)
    {
        int idx = y * _width + x;
        weight -= Math.abs(weight - _pgrid[idx]);
        if (weight <= 0 || (piece.x == x && piece.y == y)) {
            return false;
        }
        if (canOccupy(piece, x, y)) {
            return true;
        }
        return canMoveTo(piece, x + 1, y, weight) ||
               canMoveTo(piece, x - 1, y, weight) ||
               canMoveTo(piece, x, y + 1, weight) ||
               canMoveTo(piece, x, y - 1, weight);
    }

    // final to encourage inlining
    protected final byte getTeam (Piece piece)
    {
        return piece.owner < 0 ? (byte)-1 : (byte)_teams[piece.owner];
    }

    /**
     * Combines an existing prop type with the type that occupies the same
     * space.  If any prop is tall, the tile is tall.  If all props are
     * penetrable, the tile is penetrable.
     */
    protected static byte combinePropTypes (byte t1, byte t2)
    {
        return (t1 > O_PROP) ? t2 : createPropType(
            (t1 & TALL_FLAG) == 0 || (t2 & TALL_FLAG) == 0,
            (t1 & PENETRABLE_FLAG) == 0 && (t2 & PENETRABLE_FLAG) == 0,
            (t1 & TARGETABLE_FLAG) == 0 || (t2 & TARGETABLE_FLAG) == 0);
    }

    /**
     * Creates a prop type using the given flags.
     */
    protected static byte createPropType (
            boolean tall, boolean penetrable, boolean targetable)
    {
        return (byte)(O_PROP ^ (tall ? TALL_FLAG : 0) ^
            (penetrable ? PENETRABLE_FLAG : 0) ^
            (targetable ? TARGETABLE_FLAG : 0));
    }

    /**
     * Converts an unsigned byte value to an integer.
     */
    protected static int unsignedToInt (byte value)
    {
        return (value >= 0) ? value : (256 + value);
    }

    /** The width and height of our board. */
    protected int _width, _height;

    /** Initialized from our BangObject, maps units to teams. */
    protected int[] _teams;

    /** The heightfield that describes the board terrain. */
    protected byte[] _heightfield;

    /** The number of elevation units in the length of one tile. */
    protected byte _elevationUnitsPerTile;

    /** The terrain codes for each heightfield vertex. */
    protected byte[] _terrain;

    /** The height of the shadow volume at each heightfield vertex. */
    protected byte[] _shadows;

    /** The intensity of the shadows on the board. */
    protected float _shadowIntensity;

    /** The level of the water on the board (-128 for no water). */
    protected byte _waterLevel;

    /** The color of the water. */
    protected int _waterColor;

    /** The amplitude scale of the waves in the water. */
    protected float _waterAmplitude;

    /** The azimuths and elevations of the directional lights. */
    protected float[] _lightAzimuths, _lightElevations;

    /** The diffuse and ambient colors of the directional lights. */
    protected int[] _lightDiffuseColors, _lightAmbientColors;

    /** The color of the horizon and the sky overhead. */
    protected int _skyHorizonColor, _skyOverheadColor;

    /** The falloff factor that determines how quickly the horizon color fades
     * into the overhead color. */
    protected float _skyFalloff;

    /** The speed and direction of the wind. */
    protected float _windDirection, _windSpeed;

    /** The color of the board fog. */
    protected int _fogColor;

    /** The density of the board fog. */
    protected float _fogDensity;

    /** The color of the board grid. */
    protected int _gridColor;

    /** The shadow patches for different prop configurations. */
    protected StreamableHashMap<String, byte[]> _patchMap;

    /** The dimensions of the heightfield and terrain arrays. */
    protected transient int _hfwidth, _hfheight;

    /** The minimum height at the edge of the board, used for values beyond the
     * edge. */
    protected transient byte _minEdgeHeight;

    /** The most common terrain type along the edge of the board, used values
     * beyond the edge. */
    protected transient byte _edgeTerrain;

    /** The predominant terrain codes for each tile. */
    protected transient byte[] _pterrain;

    /** Tracks coordinate traversability. */
    protected transient byte[] _tstate, _btstate, _estate, _dstate;

    /** A temporary array for computing move and fire sets. */
    protected transient byte[] _pgrid;

    /** A rectangle containing our playable area. */
    protected transient Rectangle _playarea;

    /** The maximum height per tile. */
    protected transient int[] _maxHeight;

    /** If the heightfield has changed since generating max heights. */
    protected transient boolean _heightfieldChanged = true;

    /** Indicates that this tile is occupied by an airborne and mobile non-unit. */
    protected static final byte O_OCC_AND_AIR = 12;

    /** Indicates that this tile is occupied by an airborne unit. */
    protected static final byte O_AIRBORNE = 11;

    /** Indicates that this tile is occupied by a mobile non-unit. */
    protected static final byte O_OCCUPIED = 10;

    /** Indicates that this tile is flat and traversable. */
    protected static final byte O_FLAT = -1;

    /** Indicates that this tile has a bridge and is traversable. */
    protected static final byte O_BRIDGE = -2;

    /** Indicates that this tile is flat and traversable but occupied by a
     * bonus. */
    protected static final byte O_BONUS = -3;

    /** Indicates that this tile is rough and only traversable by some units. */
    protected static final byte O_ROUGH = -4;

    /** Indicates that this tile is impassable by non-air units. */
    protected static final byte O_IMPASS = -5;

    /** Indicates that this tile is occupied by a prop.  Anything less is also
     * a prop, with the flags below indicating various attributes when
     * cleared. */
    protected static final byte O_PROP = -6;

    /** Flags the prop as tall (can't be passed, even by air units). */
    protected static final byte TALL_FLAG = 1 << 3;

    /** Flags the prop as penetrable (doesn't affect line of sight). */
    protected static final byte PENETRABLE_FLAG = 1 << 4;

    /** Flags the prop as targetable (can be shot at). */
    protected static final byte TARGETABLE_FLAG = 1 << 5;

    /** Flags the prop as being unable to enter in this direction. */
    protected static final byte ENTER_NORTH = 1 << 0;

    /** Flags the prop as being unable to exit in this direction. */
    protected static final byte EXIT_NORTH = 1 << 4;

    /** Minimum delta for a tile to be considered sloping. */
    protected static final float MIN_SLOPE = 0.5f;

    /** Maximum path distnace. */
    protected static final int MAX_PATH = 50;
}
