//
// $Id$

package com.threerings.underwire.web.server;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Ensures the character encoding is set to UTF-8 if the content-type is text/x-gwt-rpc.  Due to a
 * bug in mod-jk (which is ancient and should be migrated away from),
 * ServletRequest.getCharacterEncoding() returns null, even though the headers are correct. This
 * hacky work-around ensures the encoding is set properly on the request.
 */
public class GWTEncodingFilter
    implements Filter
{
    public void doFilter (ServletRequest req, ServletResponse res, FilterChain chain)
        throws IOException, ServletException
    {
        // set the character encoding to utf-8 for all GWT RPC calls
        if (req.getContentType() != null) {
            if (req.getContentType().toLowerCase().contains("text/x-gwt-rpc") &&
                    !"utf-8".equals(req.getCharacterEncoding())) {
                req.setCharacterEncoding("utf-8");
            }
        }
        chain.doFilter(req, res);
    }

    public void init (FilterConfig arg0)
        throws ServletException
    {
        // nada
    }

    public void destroy ()
    {
        // nada
    }
}
