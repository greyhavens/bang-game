//
// $Id$

package com.threerings.bang.client.util;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.gdx.records.TextureStateRecord;

import com.jmex.bui.BImage;

import com.threerings.bang.util.BasicContext;

/**
 * Maintains a fixed-size pool of OpenGL textures in order to avoid rapidly creating and
 * destroying them.
 */
public class TexturePool
    implements BImage.TexturePool
{
    public TexturePool (BasicContext ctx, int maxSize)
    {
        _ctx = ctx;
        _maxSize = maxSize;
        _dtstate = ctx.getRenderer().createTextureState();
    }

    // documentation inherited from interface BImage.TexturePool
    public void acquireTextures (TextureState tstate)
    {
        TextureStateRecord record =
            (TextureStateRecord)_ctx.getDisplay().getCurrentContext().getStateRecord(
                RenderState.RS_TEXTURE);

        for (int ii = 0, nn = tstate.getNumberOfSetTextures(); ii < nn; ii++) {
            Texture texture = tstate.getTexture(ii);
            if (texture == null || texture.getTextureId() > 0) {
                continue;
            }
            TextureKey key = new TextureKey(texture);
            ArrayList<Integer> texIds = _textureIds.get(key);
            if (texIds == null) {
                // no ids to reuse; must create a new one
                _dtstate.setTexture(texture);
                _dtstate.apply();
                _dtstate.setTexture(null);
                continue;
            }
            // change data of existing texture
            int texId = texIds.remove(0);
            if (texIds.isEmpty()) {
                _textureIds.remove(key);
            }
            texture.setTextureId(texId);
            _size -= key.getTextureSize();

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
            record.currentUnit = Math.max(record.currentUnit, 0);
            record.units[record.currentUnit].boundTexture = texId;

            ByteBuffer data = texture.getImage().getData();
            data.rewind();
            GL11.glTexSubImage2D(
                GL11.GL_TEXTURE_2D, 0, 0, 0, key.width, key.height,
                (key.type == Image.RGB888 ? GL11.GL_RGB : GL11.GL_RGBA),
                GL11.GL_UNSIGNED_BYTE, data);
        }
    }

    // documentation inherited from interface BImage.TexturePool
    public void releaseTextures (TextureState tstate)
    {
        for (int ii = 0, nn = tstate.getNumberOfSetTextures(); ii < nn; ii++) {
            Texture texture = tstate.getTexture(ii);
            if (texture == null || texture.getTextureId() <= 0) {
                continue;
            }
            TextureKey key = new TextureKey(texture);
            ArrayList<Integer> texIds = _textureIds.get(key);
            if (texIds == null) {
                _textureIds.put(key, texIds = new ArrayList<Integer>());
            }
            texIds.add(texture.getTextureId());
            texture.setTextureId(0);
            if ((_size += key.getTextureSize()) > _maxSize) {
                flush();
            }
        }
    }

    /**
     * Deletes textures from the pool until the size falls under the maximum.
     */
    protected void flush ()
    {
        Iterator<Map.Entry<TextureKey, ArrayList<Integer>>> it;
        for (it = _textureIds.entrySet().iterator(); it.hasNext() && _size > _maxSize; ) {
            Map.Entry<TextureKey, ArrayList<Integer>> entry = it.next();
            int texSize = entry.getKey().getTextureSize();
            ArrayList<Integer> texIds = entry.getValue();
            while (!texIds.isEmpty() && _size > _maxSize) {
                int texId = texIds.remove(0);
                _dtstate.deleteTextureId(texId);
                _size -= texSize;
            }
            if (texIds.isEmpty()) {
                it.remove();
            }
        }
    }

    /** Identifies a texture by its dimensions and format. */
    protected static class TextureKey
    {
        public int width, height, type;

        public TextureKey (Texture texture)
        {
            Image image = texture.getImage();
            width = image.getWidth();
            height = image.getHeight();
            type = image.getType();
        }

        public int getTextureSize ()
        {
            return width * height * (type == Image.RGB888 ? 3 : 4);
        }

        @Override // documentation inherited
        public boolean equals (Object obj)
        {
            TextureKey okey = (TextureKey)obj;
            return (width == okey.width && height == okey.height && type == okey.type);
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return width + height + type;
        }
    }

    protected BasicContext _ctx;

    /** Texture ids of available textures. */
    protected LinkedHashMap<TextureKey, ArrayList<Integer>> _textureIds =
        new LinkedHashMap<TextureKey, ArrayList<Integer>>(16, 0.75f, true);

    /** The current and maximum size of the cache. */
    protected int _size, _maxSize;

    /** A dummy texture state used to delete textures. */
    protected TextureState _dtstate;
}
