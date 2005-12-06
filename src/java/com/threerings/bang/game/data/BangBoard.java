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

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.StringUtil;
import com.samskivert.util.TermUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.media.util.AStarPathUtil;

import com.threerings.bang.game.data.piece.BigPiece;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Track;
import com.threerings.bang.game.data.piece.Train;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Describes the terrain of the game board.
 */
public class BangBoard extends SimpleStreamableObject
{
    /** The basic traversal cost. */
    public static final int BASE_TRAVERSAL = 10;

    /** The number of subdivisions in the heightfield for each tile. */
    public static final int HEIGHTFIELD_SUBDIVISIONS = 4;

    /** The number of elevation units per vertical tile size. */
    public static final int ELEVATION_UNITS_PER_TILE = 64;

    /** The maximum difference between adjacent height points allowed before a
     * tile becomes unoccupiable. */
    public static final int MAX_OCCUPIABLE_HEIGHT_DELTA = 16;

    /** The maximum water level (above ground in elevation units) that ground
     * units can occupy. */
    public static final byte MAX_OCCUPIABLE_WATER_LEVEL = 1;

    /** The size in tiles of the border between the edge of the heightfield and
     * the edge of the playable region. */
    public static final int BORDER_SIZE = 8;

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
        _terrain = new byte[_hfwidth * _hfheight];

        _waterLevel = (byte)-128;
        _waterDiffuseColor = 0x99CCFF;
        _waterAmbientColor = 0x001A33;

        _lightAzimuths = new float[] { 0f, (float)Math.PI };
        _lightElevations = new float[] { (float)(Math.PI / 4),
            (float)(-Math.PI / 4)};
        _lightDiffuseColors = new int[] { 0xFFFFFF, 0x0 };
        _lightAmbientColors = new int[] { 0xBEBEBE, 0x0 };

        _pterrain = new byte[width*height];
        _btstate = new byte[width*height];
        _estate = new byte[width*height];
        _tstate = new byte[width*height];
        _pgrid = new byte[width*height];

        _bbounds = new Rectangle(0, 0, _width, _height);
    }

    /** A default constructor for unserialization. */
    public BangBoard ()
    {
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
        if (x < 0 || y < 0 || x >= _hfwidth || y >= _hfheight) {
            return _minEdgeHeight;

        } else {
            return _heightfield[y*_hfwidth + x];
        }
    }

    /**
     * Updates the board's minimum edge height, which is returned for values
     * outside the board.
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
    }

    /** Adds to or subtracts from the height at the specified sub-tile
     * coordinates, clamping to the allowed range. */
    public void addHeightfieldValue (int x, int y, int value)
    {
        int idx = y*_hfwidth + x;
        _heightfield[idx] = (byte)Math.min(Math.max(-128,
            _heightfield[idx] + value), +127);
    }

    /** Returns a reference to the heightfield array. */
    public byte[] getHeightfield ()
    {
        return _heightfield;
    }

    /** Returns the terrain value at the specified terrain coordinates. */
    public byte getTerrainValue (int x, int y)
    {
        // clamp the coordinates to repeat the edge
        x = Math.min(Math.max(x, 0), _hfwidth - 1);
        y = Math.min(Math.max(y, 0), _hfheight - 1);
        return _terrain[y*_hfwidth + x];
    }

    /** Sets a single terrain value. */
    public void setTerrainValue (int x, int y, byte value)
    {
        _terrain[y*_hfwidth + x] = value;
    }

    /** Fills the terrain array with the specified terrain. */
    public void fillTerrain (Terrain terrain)
    {
        for (int i = 0; i < _terrain.length; i++) {
            _terrain[i] = (byte)terrain.code;
        }
    }

    /** Returns a reference to the terrain array. */
    public byte[] getTerrain ()
    {
        return _terrain;
    }

    /** Returns the level of the water on the board in heightfield units (-128
     * for no water. */
    public byte getWaterLevel ()
    {
        return _waterLevel;
    }

    /** Returns the diffuse color of the water. */
    public int getWaterDiffuseColor ()
    {
        return _waterDiffuseColor;
    }

    /** Returns the ambient color of the water. */
    public int getWaterAmbientColor ()
    {
        return _waterAmbientColor;
    }

    /** Sets the water level. */
    public void setWaterParams (byte level, int diffuseColor, int ambientColor)
    {
        _waterLevel = level;
        _waterDiffuseColor = diffuseColor;
        _waterAmbientColor = ambientColor;
    }

    /**
     * Returns the azimuth in radians of the directional light at the specified
     * index, where zero has the light in the positive x direction and
     * increasing values rotate the light counter-clockwise.
     */
    public float getLightAzimuth (int idx)
    {
        return _lightAzimuths[idx];
    }

    /**
     * Returns the elevation in radians of the directional light at the
     * specified index, where zero has the light on the horizon and pi/2 has
     * the light exactly overhead.
     */
    public float getLightElevation (int idx)
    {
        return _lightElevations[idx];
    }

    /**
     * Returns the diffuse RGB color of the directional light at the specified
     * index.
     */
    public int getLightDiffuseColor (int idx)
    {
        return _lightDiffuseColors[idx];
    }

    /**
     * Returns the ambient RGB color of the directional light at the specified
     * index.
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
    public void setLightParams (int idx, float azimuth, float elevation,
        int diffuseColor, int ambientColor)
    {
        _lightAzimuths[idx] = azimuth;
        _lightElevations[idx] = elevation;
        _lightDiffuseColors[idx] = diffuseColor;
        _lightAmbientColors[idx] = ambientColor;
    }

    /**
     * Returns the bounds of our board. <em>Do not modify</em> the
     * returned rectangle.
     */
    public Rectangle getBounds ()
    {
        return _bbounds;
    }

    /**
     * Computes and returns a path for the specified piece to the
     * specified coordinates. Returns null if no path could be found.
     */
    public List computePath (Piece piece, int tx, int ty)
    {
//         log.info("Computing path from " + piece.x + "/" + piece.y + " to " +
//                  tx + "/" + ty + " maxdist:" + piece.getMoveDistance() + ".");
        return AStarPathUtil.getPath(
            _tpred, piece.getStepper(), piece, piece.getMoveDistance(),
            piece.x, piece.y, tx, ty, true);
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
     * Returns a set of coordinates for locations near to the specified
     * coordinates into which a piece can be spawned. First the
     * coordinates immediately surrounding the location are searched, then
     * one unit away, and so on. Within a particular "shell" the
     * coordinates are searched randomly. The list may be fewer than the
     * requested count if an insufficient number of spots could be located
     * within the specified maximum distance.
     */
    public ArrayList<Point> getOccupiableSpots (
        int count, int cx, int cy, int maxdist)
    {
        ArrayList<Point> ospots = new ArrayList<Point>();
        PointSet spots = new PointSet();
      SEARCH:
        for (int dist = 1; dist <= maxdist; dist++) {
            spots.clear();
            spots.addFrame(cx, cy, dist, getBounds());
            int[] coords = spots.toIntArray();
            ArrayUtil.shuffle(coords);
            for (int ii = 0; ii < coords.length; ii++) {
                int hx = PointSet.decodeX(coords[ii]);
                int hy = PointSet.decodeY(coords[ii]);
                if (isOccupiable(hx, hy)) {
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
     * Returns the coordinates of a location near to the specified
     * coordinates into which a piece can be spawned. First the
     * coordinates immediately surrounding the location are searched, then
     * one unit away, and so on. Within a particular "shell" the
     * coordinates are searched randomly. Returns null if no occupiable
     * spot could be located.
     */
    public Point getOccupiableSpot (int cx, int cy, int maxdist)
    {
        ArrayList<Point> spots = getOccupiableSpots(1, cx, cy, maxdist);
        return (spots.size() > 0) ? spots.get(0) : null;
    }

    /**
     * Adds the supplied set of pieces to our board "shadow" data. This is
     * done at the start of the game; all subsequent changes are
     * incremental.
     */
    public void shadowPieces (Iterator iter)
    {
        // start out with _tstate configured according to the board
        for (int yy = 0; yy < _height; yy++) {
            for (int xx = 0; xx < _width; xx++) {
                byte tvalue;
                if (isUnderWater(xx, yy, MAX_OCCUPIABLE_WATER_LEVEL)) {
                    tvalue = O_FLYER;
                } else if (exceedsMaxHeightDelta(xx, yy)) {
                    tvalue = O_ROUGH;
                } else {
                    tvalue = O_EMPTY;
                }
                int pos = _width*yy+xx;
                _tstate[pos] = tvalue;
                _btstate[pos] = tvalue;
            }
        }

        while (iter.hasNext()) {
            updateShadow(null, (Piece)iter.next());
        }
    }

    /**
     * Updates the shadow for the specified piece.
     */
    public void updateShadow (Piece opiece, Piece piece)
    {
        // unshadow the piece's old position (big pieces never move)
        if (opiece != null) {
            int pos = _width*opiece.y+opiece.x;
            _tstate[pos] = _btstate[pos];
        }

        // now add a shadow for the new piece
        if (piece != null) {
            if (piece instanceof BigPiece) {
                Rectangle pbounds = ((BigPiece)piece).getBounds();
                for (int yy = pbounds.y, ly = yy + pbounds.height;
                     yy < ly; yy++) {
                    for (int xx = pbounds.x, lx = xx + pbounds.width;
                         xx < lx; xx++) {
                        if (_bbounds.contains(xx, yy)) {
                            _tstate[_width*yy+xx] = O_FLYER;
                            _btstate[_width*yy+xx] = O_FLYER;
                            _estate[_width*yy+xx] = O_FLYER;
                        }
                    }
                }

            } else if (piece instanceof Track) {
                int idx = _width*piece.y+piece.x;
                if (((Track)piece).preventsGroundOverlap()) {
                    _tstate[idx] = _btstate[idx] = _estate[idx] = O_FLYER;
                } else {
                    _tstate[idx] = _btstate[idx] = O_ANY;
                }

            } else if (piece instanceof Bonus) {
                _tstate[_width*piece.y+piece.x] = O_ANY;

            } else if (piece instanceof Train) {
                _tstate[_width*piece.y+piece.x] = O_FLYER;

            } else {
                _tstate[_width*piece.y+piece.x] = O_NONE;
            }
        }
    }

    /**
     * Returns the combined elevation (heightfield elevation plus piece
     * elevation) at the specified tile coordinates.
     */
    public int getElevation (int x, int y)
    {
        return getHeightfieldElevation(x, y) + getPieceElevation(x, y);
    }

    /**
     * Returns the heightfield elevation (the elevation of the terrain in
     * elevation units) at the specified tile coordinates.
     */
    public int getHeightfieldElevation (int x, int y)
    {
        // for now, just grab the heightfield value in the center of the tile
        int offset = HEIGHTFIELD_SUBDIVISIONS/2;
        return getHeightfieldValue(x*HEIGHTFIELD_SUBDIVISIONS + offset,
                y*HEIGHTFIELD_SUBDIVISIONS + offset);
    }

    /**
     * Returns the piece elevation (the height of the piece in elevation units)
     * at the specified tile coordinates.
     */
    public int getPieceElevation (int x, int y)
    {
        if (x < 0 || y < 0 || x >= _width || y >= _height) {
            return 0;
            
        } else {
            return _estate[y*_width+x] * ELEVATION_UNITS_PER_TILE;
        }
    }
 
    /**
     * Checks whether any adjacent vertices in the heightfield under the
     * specified tile coordinates exceed the maximum height delta.
     */
    public boolean exceedsMaxHeightDelta (int tx, int ty)
    {
        int x1 = tx * HEIGHTFIELD_SUBDIVISIONS,
            y1 = ty * HEIGHTFIELD_SUBDIVISIONS,
            x2 = (tx+1) * HEIGHTFIELD_SUBDIVISIONS,
            y2 = (ty+1) * HEIGHTFIELD_SUBDIVISIONS;
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                int ll = getHeightfieldValue(x, y),
                    d1 = getHeightfieldValue(x, y+1) - ll,
                    d2 = getHeightfieldValue(x+1, y+1) - ll,
                    d3 = getHeightfieldValue(x+1, y) - ll;
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
     * Checks whether any portion of the specified tile is beneath any
     * amount of water.
     */
    public boolean isUnderWater (int tx, int ty)
    {
        return isUnderWater(tx, ty, (byte)0);
    }

    /**
     * Checks whether any portion of the specified tile is beneath the
     * specified level of water.
     */
    public boolean isUnderWater (int tx, int ty, byte level)
    {
        int x1 = tx * HEIGHTFIELD_SUBDIVISIONS,
            y1 = ty * HEIGHTFIELD_SUBDIVISIONS,
            x2 = (tx+1) * HEIGHTFIELD_SUBDIVISIONS,
            y2 = (ty+1) * HEIGHTFIELD_SUBDIVISIONS;
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
     * @param rough whether or not to allow the occupation of rough
     * terrain
     */
    public boolean isGroundOccupiable (int x, int y, boolean rough)
    {
        if (!_bbounds.contains(x, y)) {
            return false;
        }
        return (_btstate[y*_width+x] < (rough ? O_FLYER : O_ROUGH));
    }

    /**
     * Returns true if the specified piece can occupy the specified
     * coordinate.
     */
    public boolean canOccupy (Piece piece, int x, int y)
    {
        if (!_bbounds.contains(x, y)) {
            return false;
        }
        int max = (piece.isFlyer() || piece instanceof Train) ? O_FLYER : O_ANY;
        return (_tstate[y*_width+x] <= max);
    }

    /**
     * Returns true if the specified coordinate is both unoccupied by any
     * other piece and traversable.
     */
    public boolean isOccupiable (int x, int y)
    {
        if (!_bbounds.contains(x, y)) {
            return false;
        }
        return (_tstate[y*_width+x] <= O_EMPTY);
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
        // clear out the planning grid
        Arrays.fill(_pgrid, (byte)0);

        int mdist = piece.getMoveDistance();
//         log.info("Recomputing sets for " + piece.info() +
//                  " [mdist=" + mdist + "].");

        // start with 10x our movement points at our current coordinate
        // (and add one to ensure that we always end up with 1 in our
        // final coordinate)
        byte remain = (byte)(mdist * BASE_TRAVERSAL + 1);
        _pgrid[piece.y*_width+piece.x] = remain;

        // now consider each of our four neighbors
        considerMoving(piece, moves, piece.x+1, piece.y, remain);
        considerMoving(piece, moves, piece.x-1, piece.y, remain);
        considerMoving(piece, moves, piece.x, piece.y+1, remain);
        considerMoving(piece, moves, piece.x, piece.y-1, remain);

        // if the attack set is non-null, compute our attacks as well
        if (attacks != null) {
            PointSet set = moves;
            remain = (byte)piece.getMaxFireDistance();
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
                if (_bbounds.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
            for (int xx = px + dd, yy = py; xx > px; xx--, yy++) {
                if (_bbounds.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
            for (int xx = px, yy = py + dd; yy > py; xx--, yy--) {
                if (_bbounds.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
            for (int xx = px - dd, yy = py; xx < px; xx++, yy--) {
                if (_bbounds.contains(xx, yy)) {
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

    /**
     * Extends default behavior to initialize transient members.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        _hfwidth = _width * HEIGHTFIELD_SUBDIVISIONS + 1;
        _hfheight = _height * HEIGHTFIELD_SUBDIVISIONS + 1;

        int size = _width*_height;
        _pterrain = new byte[size];
        _btstate = new byte[size];
        _estate = new byte[size];
        _tstate = new byte[size];
        _pgrid = new byte[size];
        _bbounds = new Rectangle(0, 0, _width, _height);

        updateMinEdgeHeight();
        updatePredominantTerrain();
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
     * Returns the most common terrain value under the specified tile.
     */
    public Terrain getPredominantTerrain (int x, int y)
    {
        return Terrain.fromCode(_pterrain[y*_width + x]);
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
            y2 = (ty+1) * HEIGHTFIELD_SUBDIVISIONS;
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                counts.increment(getTerrainValue(x, y), 1);
            }
        }

        int maxCount = 0, maxCode = 0;
        for (Iterator it = counts.entrySet().iterator(); it.hasNext(); ) {
            IntIntMap.Entry e = (IntIntMap.Entry)it.next();
            if (e.getIntValue() > maxCount) {
                maxCode = e.getIntKey();
                maxCount = e.getIntValue();
            }
        }
        return (byte)maxCode;
    }

    /** Helper function for {@link #computeMoves}. */
    protected void considerMoving (
        Piece piece, PointSet moves, int xx, int yy, byte remain)
    {
        // make sure this coordinate is occupiable
        if (!_bbounds.contains(xx, yy) || !canOccupy(piece, xx, yy)) {
            return;
        }

        // see if we can move into this square with a higher remaining
        // point count than has already been accomplished
        int pos = yy*_width+xx;
        byte premain = (byte)(remain -
            piece.traversalCost(getPredominantTerrain(xx, yy)));
        byte current = _pgrid[pos];
        if (premain <= current) {
            return;
        }

        // if so, do it
        moves.add(xx, yy);
        _pgrid[pos] = premain;

        // and then check all of our neighbors
        considerMoving(piece, moves, xx+1, yy, premain);
        considerMoving(piece, moves, xx-1, yy, premain);
        considerMoving(piece, moves, xx, yy+1, premain);
        considerMoving(piece, moves, xx, yy-1, premain);
    }

    /** Helper function for {@link #computeMoves}. */
    protected void considerFiring (
        PointSet attacks, int xx, int yy, byte remain, boolean checkThisSpot)
    {
        // make sure this coordinate is on the board
        if (!_bbounds.contains(xx, yy)) {
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

    /** Used when path finding. */
    protected transient AStarPathUtil.TraversalPred _tpred =
        new AStarPathUtil.TraversalPred() {
        public boolean canTraverse (Object traverser, int x, int y) {
            return canOccupy((Piece)traverser, x, y);
        }
    };

    /** The width and height of our board. */
    protected int _width, _height;

    /** The heightfield that describes the board terrain. */
    protected byte[] _heightfield;

    /** The terrain codes for each heightfield vertex. */
    protected byte[] _terrain;

    /** The level of the water on the board (-128 for no water). */
    protected byte _waterLevel;

    /** The diffuse and ambient colors of the water. */
    protected int _waterDiffuseColor, _waterAmbientColor;

    /** The azimuths and elevations of the directional lights. */
    protected float[] _lightAzimuths, _lightElevations;

    /** The diffuse and ambient colors of the directional lights. */
    protected int[] _lightDiffuseColors, _lightAmbientColors;

    /** The dimensions of the heightfield and terrain arrays. */
    protected transient int _hfwidth, _hfheight;

    /** The minimum height at the edge of the board, used for values beyond the
     * edge. */
    protected transient byte _minEdgeHeight;

    /** The predominant terrain codes for each tile. */
    protected transient byte[] _pterrain;

    /** Tracks coordinate traversability. */
    protected transient byte[] _tstate, _btstate, _estate;

    /** A temporary array for computing move and fire sets. */
    protected transient byte[] _pgrid;

    /** A rectangle containing our bounds, used when path finding. */
    protected transient Rectangle _bbounds;

    /** Indicates that this tile is completely empty. */
    protected static final byte O_EMPTY = 0;

    /** Indicates that a normal unit can occupy this tile. */
    protected static final byte O_ANY = 1;

    /** Indicates that a rough-terrain capable unit can occupy this tile. */
    protected static final byte O_ROUGH = 2;

    /** Indicates that a flying unit can occupy this tile. */
    protected static final byte O_FLYER = 3;

    /** Indicates that no unit can occupy this tile. */
    protected static final byte O_NONE = 4;
}
