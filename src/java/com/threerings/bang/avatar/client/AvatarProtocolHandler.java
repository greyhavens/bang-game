//
// $Id$

package com.threerings.bang.avatar.client;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import java.security.Permission;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import com.samskivert.io.ByteArrayOutInputStream;
import com.samskivert.net.AttachableURLFactory;
import com.samskivert.util.StringUtil;

import com.threerings.geom.GeomUtil;

import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Implements <code>avatar://hex_avatar_fingerprint</code> URLs.
 */
public class AvatarProtocolHandler extends URLStreamHandler
{
    /**
     * Register this class to handle "avatar" urls
     * ("avatar://hex_avatar_fingerprint") with the specified context.
     */
    public static void registerHandler (BasicContext ctx)
    {
        // if we already have a context; don't register twice
        if (_ctx != null) {
            log.warning("Refusing duplicate avatar:// handler registration.");
            Thread.dumpStack();
            return;
        }
        _ctx = ctx;

        // wire up our handler with the handy dandy attachable URL factory
        AttachableURLFactory.attachHandler(
            "avatar", AvatarProtocolHandler.class);
    }

    // documentation inherited
    protected int hashCode (URL url)
    {
        return String.valueOf(url).hashCode();
    }

    // documentation inherited
    protected boolean equals (URL u1, URL u2)
    {
        return String.valueOf(u1).equals(String.valueOf(u2));
    }

    // documentation inherited
    protected URLConnection openConnection (URL url)
        throws IOException
    {
        return new URLConnection(url) {
            // documentation inherited
            public void connect ()
                throws IOException
            {
                // strip off any leading slashes
                String path = this.url.getPath();
                while (path.startsWith("/")) {
                    path = path.substring(1);
                }

                // see if they want a special size
                int scale = 4;
                String query = this.url.getQuery();
                if (!StringUtil.isBlank(query)) {
                    String[] params = StringUtil.split(query, "&");
                    for (int ii = 0; ii < params.length; ii++) {
                        if (params[ii].startsWith("scale=")) {
                            String sstr = params[ii].substring(6);
                            try {
                                scale = Integer.parseInt(sstr);
                            } catch (Exception e) {
                                log.warning("Specified invalid scale in " +
                                            "avatar: URL [url=" + this.url +
                                            ", scale=" + sstr + "].");
                            }
                        } else {
                            log.warning("Specified unknown paramater in " +
                                        "avatar: URL [url=" + this.url +
                                        ", param=" + params[ii] + "].");
                        }
                    }
                }

                // decode the avatar fingerprint and get the avatar image
                int[] avatar = StringUtil.parseIntArray(path);
                BufferedImage image = AvatarView.getImage(_ctx, avatar);

                // scale it to the requested size
                int width = AvatarLogic.WIDTH/scale,
                    height = AvatarLogic.HEIGHT/scale;
                Image simage = image.getScaledInstance(
                    width, height, BufferedImage.SCALE_SMOOTH);
                BufferedImage nimage = new BufferedImage(
                    width, height, image.getType());
                Graphics2D gfx = nimage.createGraphics();
                try {
                    gfx.drawImage(simage, 0, 0, null);
                } finally {
                    gfx.dispose();
                }

                // now write that back out in PNG format into memory and return
                // an input stream for that
                ByteArrayOutInputStream data = new ByteArrayOutInputStream();
                ImageIO.write(nimage, "PNG", data);
                _stream = data.getInputStream();
                this.connected = true;
            }

            // documentation inherited
            public InputStream getInputStream ()
                throws IOException
            {
                if (!this.connected) {
                    connect();
                }
                return _stream;
            }

            // documentation inherited
            public Permission getPermission ()
                throws IOException
            {
                return null;
            }

            protected InputStream _stream;
        };
    }

    /** Our juicy and delicious context. */
    protected static BasicContext _ctx;
}
