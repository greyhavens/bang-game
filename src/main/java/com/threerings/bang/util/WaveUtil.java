//
// $Id$

package com.threerings.bang.util;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.jme.math.FastMath;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.util.geom.BufferUtils;

/**
 * Contains utility methods related to generating animated surface waves.
 * These methods are adapted from the formulae in Jerry Tessendorf's
 * <a href="http://www.finelightvisualtechnology.com/docs/coursenotes2004.pdf">
 * Simulating Ocean Water</a>, with help from Robert Bridson's
 * <a href=http://www.cs.ubc.ca/~rbridson/courses/533d-winter-2005/cs533d-slides-mar9.pdf">
 * course notes</a>.
 */
public class WaveUtil
{
    /**
     * A wave spectrum used to obtain the initial amplitudes for the IFFT
     * method.
     */
    public static abstract class EnergySpectrum
    {
        /**
         * Returns the energy of the specified wave vector.
         */
        public abstract float getEnergy (Vector2f k);
    }

    /**
     * The Phillips wave spectrum.
     */
    public static class PhillipsSpectrum extends EnergySpectrum
    {
        /**
         * Creates an instance of the Phillips spectrum for a fully developed
         * sea.
         *
         * @param amplitude the amplitude of the waves
         * @param windVelocity the velocity of the wind
         * @param gravity the acceleration due to gravity
         * @param smallest the smallest length scale to model
         */
        public PhillipsSpectrum (float amplitude, Vector2f windVelocity,
            float gravity, float smallest)
        {
            _amplitude = amplitude;
            _smallest = smallest;
            _largest = windVelocity.length() / gravity;
            _windDirection = windVelocity.normalize();
        }

        @Override // documentation inherited
        public float getEnergy (Vector2f k)
        {
            float kl = k.length();
            if (kl == 0f) {
                return 0f;
            }
            return _amplitude * FastMath.pow(kl, -4f) *
                FastMath.exp(-FastMath.pow(kl * _largest, -2f) -
                    FastMath.sqr(kl * _smallest)) *
                FastMath.sqr(k.dot(_windDirection) / kl);
        }

        protected float _amplitude, _smallest, _largest;
        protected Vector2f _windDirection;
    }

    /**
     * A dispersion model used to determine the current amplitudes for the
     * IFFT method.
     */
    public static abstract class DispersionModel
    {
        /**
         * Returns the dispersion of the specified wave vector.
         */
        public abstract float getDispersion (Vector2f k);
    }

    /**
     * The deep water dispersion model.
     */
    public static class DeepWaterModel extends DispersionModel
    {
        /**
         * Creates an instance of the deep water dispersion model.
         *
         * @param gravity the acceleration due to gravity
         */
        public DeepWaterModel (float gravity)
        {
            _gravity = gravity;
        }

        @Override // documentation inherited
        public float getDispersion (Vector2f k)
        {
            return FastMath.sqrt(_gravity * k.length());
        }

        protected float _gravity;
    }

    /**
     * A dispersion model wrapper that quantizes a model so that it repeats
     * after a specified time period.
     */
    public static class QuantizedModel extends DispersionModel
    {
        /**
         * Creates an instance of the quantized dispersion model.
         *
         * @param base the dispersion model to be quantized
         * @param t the period of the dispersion model
         */
        public QuantizedModel (DispersionModel base, float t)
        {
            _base = base;
            _w0 = FastMath.TWO_PI / t;
        }

        @Override // documentation inherited
        public float getDispersion (Vector2f k)
        {
            return (int)(_base.getDispersion(k) / _w0) * _w0;
        }

        protected DispersionModel _base;
        protected float _w0;
    }

    /**
     * Populates the supplied arrays with initial wave amplitudes to be used in
     * the IFFT method of generating surface waves.
     *
     * @param numSamplesX the number of samples in the x direction (must be a
     * power of 2)
     * @param numSamplesY the number of samples in the y direction (must be a
     * power of 2)
     * @param sizeX the size of the wave tile in the x direction
     * @param sizeY the size of the wave tile in the y direction
     * @param spectrum the energy spectrum of the amplitudes
     * @param ramps the array to hold the real components of the amplitudes
     * (must be of size numSamplesX by numSamplesY)
     * @param iamps the array to hold the imaginary amplitude components
     * (must be of size numSamplesX by numSamplesY)
     */
    public static void getInitialAmplitudes (int numSamplesX, int numSamplesY,
        float sizeX, float sizeY, EnergySpectrum spectrum, float[][] ramps,
        float[][] iamps)
    {
        Vector2f k = new Vector2f();
        int hnsx = numSamplesX / 2, hnsy = numSamplesY / 2, ni, nj;
        float ir2 = 1f / FastMath.sqrt(2f), ir2re;
        for (int ii = 0; ii < numSamplesX; ii++) {
            k.x = (FastMath.TWO_PI * (ii < hnsx ? ii : ii - numSamplesX)) /
                sizeX;
            for (int jj = (ii <= hnsx) ? 0 : hnsy,
                nn = (ii == 0 || ii > hnsx) ? hnsy : numSamplesY - 1;
                jj <= nn; jj++) {
                k.y = (FastMath.TWO_PI * (jj < hnsy ? jj : jj - numSamplesY)) /
                    sizeY;
                ir2re = ir2 * FastMath.sqrt(spectrum.getEnergy(k));
                ramps[ii][jj] = (float)(FastMath.rand.nextGaussian()*ir2re);
                iamps[ii][jj] = (float)(FastMath.rand.nextGaussian()*ir2re);
                if (ii != hnsx && jj != hnsy) {
                    ni = (numSamplesX - ii) % numSamplesX;
                    nj = (numSamplesY - jj) % numSamplesY;
                    ramps[ni][nj] = ramps[ii][jj];
                    iamps[ni][nj] = -iamps[ii][jj];
                }
            }
        }
    }

    /**
     * Given the initial wave amplitudes, computes the amplitudes at time t.
     *
     * @param iramps the real components of the initial amplitudes
     * @param iiamps the imaginary components of the initial amplitudes
     * @param model the dispersion model for the amplitudes
     * @param ramps the array to hold the real components of the amplitudes
     * @param iamps the array to hold the imaginary amplitude components
     */
    public static void getAmplitudes (int numSamplesX, int numSamplesY,
        float sizeX, float sizeY, float[][] iramps, float[][] iiamps,
        DispersionModel model, float t, float[][] ramps, float[][] iamps)
    {
        int hnsx = numSamplesX / 2, hnsy = numSamplesY / 2, ni, nj;
        float coswkt2;
        for (int ii = 0; ii < numSamplesX; ii++) {
            _k.x = (FastMath.TWO_PI * (ii < hnsx ? ii : ii - numSamplesX)) /
                sizeX;
            for (int jj = (ii <= hnsx) ? 0 : hnsy,
                nn = (ii == 0 || ii > hnsx) ? hnsy : numSamplesY - 1;
                jj <= nn; jj++) {
                _k.y = (FastMath.TWO_PI * (jj < hnsy ? jj : jj - numSamplesY)) /
                    sizeY;
                coswkt2 = 2f * FastMath.cos(model.getDispersion(_k)*t);
                ramps[ii][jj] = iramps[ii][jj] * coswkt2;
                iamps[ii][jj] = iiamps[ii][jj] * coswkt2;
                if (ii != hnsx && jj != hnsy) {
                    ni = (numSamplesX - ii) % numSamplesX;
                    nj = (numSamplesY - jj) % numSamplesY;
                    ramps[ni][nj] = ramps[ii][jj];
                    iamps[ni][nj] = -iamps[ii][jj];
                }
            }
        }
    }

    /**
     * Given the current wave amplitudes, computes the vertices of the waves
     * and stores them in the provided buffer.
     *
     * @param ramps the real components of the wave amplitudes (which will be
     * overwritten)
     * @param iamps the imaginary components of the wave amplitudes (also
     * overwritten)
     * @param vbuf the buffer to populate with vertices (must be of size
     * numSamplesX + 1 by numSamplesY + 1)
     */
    public static void getVertices (int numSamplesX, int numSamplesY,
        float sizeX, float sizeY, float[][] ramps, float[][] iamps,
        FloatBuffer vbuf)
    {
        // compute the ifft to get the wave heights
        ifft(ramps, iamps);

        // set the vertices in the buffer
        Vector3f vertex = new Vector3f();
        float xstep = sizeX / numSamplesX, ystep = sizeY / numSamplesY;
        for (int ii = 0, idx = 0; ii <= numSamplesX; ii++) {
            for (int jj = 0; jj <= numSamplesY; jj++) {
                BufferUtils.setInBuffer(vertex.set(ii * xstep, jj * ystep,
                        ramps[ii % numSamplesX][jj % numSamplesY]),
                    vbuf, idx++);
            }
        }
    }

    /**
     * Given the current wave amplitudes, computes the vertices of the waves
     * and adds them to the provided buffer.
     *
     * @param ramps the real components of the wave amplitudes (which will be
     * overwritten)
     * @param iamps the imaginary components of the wave amplitudes (also
     * overwritten)
     * @param vbuf the buffer to populate with vertices (must be of size
     * numSamplesX + 1 by numSamplesY + 1)
     */
    public static void addVertices (int numSamplesX, int numSamplesY,
        float sizeX, float sizeY, float[][] ramps, float[][] iamps,
        FloatBuffer vbuf)
    {
        // compute the ifft to get the wave heights
        ifft(ramps, iamps);

        // add vertices to displacements in the buffer
        float xstep = sizeX / numSamplesX, ystep = sizeY / numSamplesY;
        for (int ii = 0, idx = 0; ii <= numSamplesX; ii++) {
            for (int jj = 0; jj <= numSamplesY; jj++) {
                BufferUtils.addInBuffer(_vertex.set(ii * xstep, jj * ystep,
                        ramps[ii % numSamplesX][jj % numSamplesY]),
                    vbuf, idx++);
            }
        }
    }

    /**
     * Given the current wave amplitudes, computes the X/Y displacements
     * of the waves and stores them in the provided buffer.
     *
     * @param rgradx a temporary buffer for real gradient values
     * @param igradx a temporary buffer for imaginary gradient values
     * @param rgrady a temporary buffer for real gradient values
     * @param igrady a temporary buffer for imaginary gradient values
     * @param choppiness the displacement scaling factor
     * @param vbuf the vertex buffer to populate
     */
    public static void getDisplacements (int numSamplesX, int numSamplesY,
        float sizeX, float sizeY, float[][] ramps, float[][] iamps,
        float[][] rgradx, float[][] igradx, float[][] rgrady, float[][] igrady,
        float choppiness, FloatBuffer vbuf)
    {
        // compute the x and y components of the displacement in separate iffts
        int hnsx = numSamplesX / 2, hnsy = numSamplesY / 2, ni, nj;
        float rlenk;
        for (int ii = 0; ii < numSamplesX; ii++) {
            _k.x = (FastMath.TWO_PI * (ii < hnsx ? ii : ii - numSamplesX)) /
                sizeX;
            for (int jj = (ii <= hnsx) ? 0 : hnsy,
                nn = (ii == 0 || ii > hnsx) ? hnsy : numSamplesY - 1;
                jj <= nn; jj++) {
                _k.y = (FastMath.TWO_PI * (jj < hnsy ? jj : jj - numSamplesY)) /
                    sizeY;
                if ((rlenk = _k.length()) != 0f) {
                    rlenk = 1f / rlenk;
                }
                rgradx[ii][jj] = -iamps[ii][jj] * _k.x * rlenk;
                igradx[ii][jj] = ramps[ii][jj] * _k.x * rlenk;
                rgrady[ii][jj] = -iamps[ii][jj] * _k.y * rlenk;
                igrady[ii][jj] = ramps[ii][jj] * _k.y * rlenk;
                if (ii != hnsx && jj != hnsy) {
                    ni = (numSamplesX - ii) % numSamplesX;
                    nj = (numSamplesY - jj) % numSamplesY;
                    rgradx[ni][nj] = rgradx[ii][jj];
                    igradx[ni][nj] = -igradx[ii][jj];
                    rgrady[ni][nj] = rgrady[ii][jj];
                    igrady[ni][nj] = -igrady[ii][jj];
                }
            }
        }
        ifft(rgradx, igradx);
        ifft(rgrady, igrady);

        // combine and set in the buffer
        Vector3f displacement = new Vector3f();
        for (int ii = 0, idx = 0; ii <= numSamplesX; ii++) {
            for (int jj = 0; jj <= numSamplesY; jj++) {
                BufferUtils.setInBuffer(displacement.set(
                        rgradx[ii % numSamplesX][jj % numSamplesY],
                        rgrady[ii % numSamplesX][jj % numSamplesY],
                        0f).multLocal(choppiness),
                    vbuf, idx++);
            }
        }
    }

    /**
     * Given the current wave amplitudes, computes the normals of the waves
     * and stores them in the provided buffer.
     *
     * @param ramps the real components of the wave amplitudes
     * @param iamps the imaginary components of the wave amplitudes
     * @param rgradx a temporary buffer for real gradient values
     * @param igradx a temporary buffer for imaginary gradient values
     * @param rgrady a temporary buffer for real gradient values
     * @param igrady a temporary buffer for imaginary gradient values
     * @param nbuf the buffer to populate with normals (must be of size
     * numSamplesX + 1 by numSamplesY + 1)
     */
    public static void getNormals (int numSamplesX, int numSamplesY,
        float sizeX, float sizeY, float[][] ramps, float[][] iamps,
        float[][] rgradx, float[][] igradx, float[][] rgrady, float[][] igrady,
        FloatBuffer nbuf)
    {
        // compute the x and y components of the gradient in separate iffts
        int hnsx = numSamplesX / 2, hnsy = numSamplesY / 2, ni, nj;
        for (int ii = 0; ii < numSamplesX; ii++) {
            _k.x = (FastMath.TWO_PI * (ii < hnsx ? ii : ii - numSamplesX)) /
                sizeX;
            for (int jj = (ii <= hnsx) ? 0 : hnsy,
                nn = (ii == 0 || ii > hnsx) ? hnsy : numSamplesY - 1;
                jj <= nn; jj++) {
                _k.y = (FastMath.TWO_PI * (jj < hnsy ? jj : jj - numSamplesY)) /
                    sizeY;
                rgradx[ii][jj] = -iamps[ii][jj] * _k.x;
                igradx[ii][jj] = ramps[ii][jj] * _k.x;
                rgrady[ii][jj] = -iamps[ii][jj] * _k.y;
                igrady[ii][jj] = ramps[ii][jj] * _k.y;
                if (ii != hnsx && jj != hnsy) {
                    ni = (numSamplesX - ii) % numSamplesX;
                    nj = (numSamplesY - jj) % numSamplesY;
                    rgradx[ni][nj] = rgradx[ii][jj];
                    igradx[ni][nj] = -igradx[ii][jj];
                    rgrady[ni][nj] = rgrady[ii][jj];
                    igrady[ni][nj] = -igrady[ii][jj];
                }
            }
        }
        ifft(rgradx, igradx);
        ifft(rgrady, igrady);

        // combine, normalize, and set in the buffer
        Vector3f normal = new Vector3f();
        for (int ii = 0, idx = 0; ii <= numSamplesX; ii++) {
            for (int jj = 0; jj <= numSamplesY; jj++) {
                BufferUtils.setInBuffer(normal.set(
                        -rgradx[ii % numSamplesX][jj % numSamplesY],
                        -rgrady[ii % numSamplesX][jj % numSamplesY],
                        1f).normalizeLocal(),
                    nbuf, idx++);
            }
        }
    }

    /**
     * Computes the normals using adjacent vertices.
     *
     * @param nbuf the buffer to populate with normals (must be of size
     * numSamplesX + 1 by numSamplesY + 1)
     */
    public static void getNormals (int numSamplesX, int numSamplesY,
        float sizeX, float sizeY, FloatBuffer vbuf, FloatBuffer nbuf)
    {
        int vwidth = numSamplesX + 1, vheight = numSamplesY + 1,
            lidx, uidx, idx = 0;
        float loff, uoff;
        for (int ii = 0; ii < numSamplesX; ii++) {
            if (ii == 0) {
                lidx = vwidth - 2;
                loff = -sizeX;
            } else {
                lidx = ii - 1;
                loff = 0f;
            }
            for (int jj = 0; jj < numSamplesY; jj++) {
                if (jj == 0) {
                    uidx = vheight - 2;
                    uoff = -sizeY;
                } else {
                    uidx = jj - 1;
                    uoff = 0f;
                }
                BufferUtils.populateFromBuffer(_down, vbuf,
                    ii * vheight + jj + 1);
                BufferUtils.populateFromBuffer(_up, vbuf,
                    ii * vheight + uidx);
                _up.y += uoff;
                BufferUtils.populateFromBuffer(_left, vbuf,
                    lidx * vheight + jj);
                _left.x += loff;
                BufferUtils.populateFromBuffer(_right, vbuf,
                    (ii + 1) * vheight + jj);
                BufferUtils.setInBuffer(_down.subtractLocal(_up).
                    crossLocal(_left.subtractLocal(_right)).normalizeLocal(),
                    nbuf, idx++);
            }
            BufferUtils.copyInternalVector3(nbuf, idx - numSamplesY, idx++);
        }
        for (int jj = 0; jj < vheight; jj++) {
            BufferUtils.copyInternalVector3(nbuf, idx - vheight*numSamplesY,
                idx++);
        }
    }

    /**
     * Computes the normals using adjacent vertices and stores them in a normal map texture.
     *
     * @param nmap the buffer to contain the normal map (must be of size numSamplesX by
     * numSamplesY)
     */
    public static void getNormalMap (int numSamplesX, int numSamplesY,
        float sizeX, float sizeY, FloatBuffer vbuf, ByteBuffer nmap)
    {
        int vwidth = numSamplesX + 1, vheight = numSamplesY + 1, lidx, uidx;
        float loff, uoff;
        for (int ii = 0; ii < numSamplesX; ii++) {
            if (ii == 0) {
                lidx = vwidth - 2;
                loff = -sizeX;
            } else {
                lidx = ii - 1;
                loff = 0f;
            }
            for (int jj = 0; jj < numSamplesY; jj++) {
                if (jj == 0) {
                    uidx = vheight - 2;
                    uoff = -sizeY;
                } else {
                    uidx = jj - 1;
                    uoff = 0f;
                }
                BufferUtils.populateFromBuffer(_down, vbuf, ii * vheight + jj + 1);
                BufferUtils.populateFromBuffer(_up, vbuf, ii * vheight + uidx);
                _up.y += uoff;
                BufferUtils.populateFromBuffer(_left, vbuf, lidx * vheight + jj);
                _left.x += loff;
                BufferUtils.populateFromBuffer(_right, vbuf, (ii + 1) * vheight + jj);
                _down.subtractLocal(_up).crossLocal(_left.subtractLocal(_right)).normalizeLocal();
                nmap.put((byte)((_down.x + 1f) * 127f));
                nmap.put((byte)((_down.y + 1f) * 127f));
                nmap.put((byte)((_down.z + 1f) * 127f));
                nmap.put((byte)255);
            }
        }
        nmap.rewind();
    }

    /**
     * Performs an in-place two dimensional inverse fast Fourier transform on
     * the supplied arrays.
     */
    protected static void ifft (float[][] rex, float[][] imx)
    {
        // compute the ifft in the first dimension
        for (int ii = 0; ii < rex.length; ii++) {
            ifft(rex[ii], imx[ii]);
        }

        // transpose and compute in the second dimension
        transpose(rex);
        transpose(imx);
        for (int ii = 0; ii < rex.length; ii++) {
            ifft(rex[ii], imx[ii]);
        }

        // transpose back
        transpose(rex);
        transpose(imx);
    }

    /**
     * Performs an in-place two-dimensional fast Fourier transform on the
     * supplied arrays.
     */
    protected static void fft (float[][] rex, float[][] imx)
    {
        // compute the fft in the first dimension
        for (int ii = 0; ii < rex.length; ii++) {
            fft(rex[ii], imx[ii]);
        }

        // transpose and compute in the second dimension
        transpose(rex);
        transpose(imx);
        for (int ii = 0; ii < rex.length; ii++) {
            fft(rex[ii], imx[ii]);
        }

        // transpose back
        transpose(rex);
        transpose(imx);
    }

    /**
     * Performs an in-place inverse fast Fourier transform on the supplied
     * arrays.
     *
     * @param rex the real components of the input data
     * @param imx the imaginary components of the input data
     */
    protected static void ifft (float[] rex, float[] imx)
    {
        // take fft of swapped arrays
        fft(imx, rex);

        // rescale
        float rn = 1f / rex.length;
        for (int ii = 0; ii < rex.length; ii++) {
            rex[ii] *= rn;
            imx[ii] *= rn;
        }
    }

    /**
     * Performs an in-place fast Fourier transform on the supplied arrays.
     * Translated from BASIC source by Steven W. Smith in
     * <a href="http://www.dspguide.com/ch12.htm"> The Scientist and Engineer's
     * Guide to Digital Signal Processing</a>.
     *
     * @param rex the real components of the input data
     * @param imx the imaginary components of the input data
     */
    protected static void fft (float[] rex, float[] imx)
    {
        int nm1 = rex.length - 1, nd2 = rex.length / 2,
            mm = (int)(FastMath.log(rex.length) / FastMath.log(2f));
        float tr, ti;

        // bit reversal sorting
        for (int ii = 1, jj = nd2, kk; ii < nm1; ii++, jj += kk) {
            if (ii < jj) {
                tr = rex[jj];
                ti = imx[jj];
                rex[jj] = rex[ii];
                imx[jj] = imx[ii];
                rex[ii] = tr;
                imx[ii] = ti;
            }
            for (kk = nd2; kk <= jj; jj -= kk, kk /= 2);
        }

        // loop for each stage
        for (int ll = 0; ll < mm; ll++) {
            int le = 2 << ll, le2 = le / 2, ip;
            float ur = 1f, ui = 0f, sr = FastMath.cos(FastMath.PI / le2),
                si = -FastMath.sin(FastMath.PI / le2), rip, iip;

            // loop for each sub-DFT
            for (int jj = 1; jj <= le2; jj++) {

                // loop for each butterfly
                for (int ii = jj - 1; ii <= nm1; ii += le) {
                    ip = ii + le2;
                    rip = rex[ip];
                    iip = imx[ip];
                    tr = rip*ur - iip*ui;
                    ti = rip*ui + iip*ur;
                    rex[ip] = rex[ii] - tr;
                    imx[ip] = imx[ii] - ti;
                    rex[ii] += tr;
                    imx[ii] += ti;
                }
                tr = ur;
                ur = tr*sr - ui*si;
                ui = tr*si + ui*sr;
            }
        }
    }

    /**
     * Transposes the given (square) two-dimensional array in place.
     */
    protected static void transpose (float[][] data)
    {
        float tmp;
        for (int ii = 0; ii < data.length; ii++) {
            for (int jj = ii + 1; jj < data.length; jj++) {
                tmp = data[ii][jj];
                data[ii][jj] = data[jj][ii];
                data[jj][ii] = tmp;
            }
        }
    }

    /** Temporary vectors to reuse. */
    protected static Vector2f _k = new Vector2f();
    protected static Vector3f _vertex = new Vector3f();
    protected static Vector3f _left = new Vector3f(), _right = new Vector3f(),
        _up = new Vector3f(), _down = new Vector3f();
}
