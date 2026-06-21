//
// $Id$
//
// jME3 cutover (Phase 3): a project-owned replacement for org.lwjgl.util.WaveData, which existed
// in LWJGL2 but was removed in LWJGL3. It decodes a WAV (or any AudioSystem-readable) stream to
// interleaved little-endian PCM in a native ByteBuffer and exposes the OpenAL format / sample
// rate, matching the old WaveData public API (fields format/samplerate/data + create(...)) so the
// vendored com.threerings.openal callers (Clip/WaveDataClipProvider/OpenALSoundPlayer) are
// unchanged.
//

package com.threerings.openal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.lwjgl.openal.AL10;

/**
 * Decodes WAV audio into an OpenAL-ready PCM buffer. Drop-in for the LWJGL2 {@code
 * org.lwjgl.util.WaveData}.
 */
public class WaveData
{
    /** The OpenAL format (AL_FORMAT_MONO8/MONO16/STEREO8/STEREO16). */
    public final int format;

    /** The sample rate in Hz. */
    public final int samplerate;

    /** The interleaved PCM data, little-endian, in a direct native buffer. */
    public final ByteBuffer data;

    private WaveData (int format, int samplerate, ByteBuffer data)
    {
        this.format = format;
        this.samplerate = samplerate;
        this.data = data;
    }

    /** Frees the backing buffer reference (the buffer is GC-managed; this matches the old API). */
    public void dispose ()
    {
    }

    /** Loads from a classpath resource path. */
    public static WaveData create (String path)
    {
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        if (url == null) {
            url = WaveData.class.getClassLoader().getResource(path);
        }
        return (url == null) ? null : create(url);
    }

    /** Loads from a URL. */
    public static WaveData create (URL url)
    {
        try {
            return create(AudioSystem.getAudioInputStream(new BufferedInputStream(url.openStream())));
        } catch (Exception e) {
            return null;
        }
    }

    /** Loads from a raw input stream. */
    public static WaveData create (InputStream is)
    {
        try {
            InputStream in = is.markSupported() ? is : new BufferedInputStream(is);
            return create(AudioSystem.getAudioInputStream(in));
        } catch (Exception e) {
            return null;
        }
    }

    /** Loads from an already-open audio input stream, converting to PCM as needed. */
    public static WaveData create (AudioInputStream ais)
        throws IOException
    {
        AudioFormat fmt = ais.getFormat();

        // OpenAL wants signed-16 / unsigned-8 PCM; convert encodings that are not already that.
        if (fmt.getEncoding() != AudioFormat.Encoding.PCM_SIGNED &&
            fmt.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {
            AudioFormat target = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED, fmt.getSampleRate(), 16,
                fmt.getChannels(), fmt.getChannels() * 2, fmt.getSampleRate(), false);
            ais = AudioSystem.getAudioInputStream(target, ais);
            fmt = ais.getFormat();
        }

        int channels = fmt.getChannels();
        int bits = fmt.getSampleSizeInBits();
        int alFormat;
        if (channels == 1) {
            alFormat = (bits == 8) ? AL10.AL_FORMAT_MONO8 : AL10.AL_FORMAT_MONO16;
        } else {
            alFormat = (bits == 8) ? AL10.AL_FORMAT_STEREO8 : AL10.AL_FORMAT_STEREO16;
        }

        byte[] bytes = ais.readAllBytes();
        ais.close();

        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
        if (bits == 16) {
            // Java audio bytes are big-endian by default for the WAV PCM we requested above
            // (false little-endian flag), so reinterpret to little-endian shorts.
            ByteBuffer src = ByteBuffer.wrap(bytes).order(
                fmt.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
            ShortBuffer dst = buffer.asShortBuffer();
            ShortBuffer ssrc = src.asShortBuffer();
            while (ssrc.hasRemaining()) {
                dst.put(ssrc.get());
            }
        } else {
            buffer.put(bytes);
        }
        buffer.flip();

        return new WaveData(alFormat, (int)fmt.getSampleRate(), buffer);
    }
}
