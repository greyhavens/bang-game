//
// $Id$

package com.threerings.bang.client.bui;

import java.io.StringReader;
import java.net.URL;
import javax.swing.text.html.HTMLDocument;

import com.jmex.bui.text.HTMLView;

import com.threerings.bang.client.BangUI;

import static com.threerings.bang.Log.log;

/**
 * Displays HTML content using our standard stylesheet and other bits.
 */
public class BangHTMLView extends HTMLView
{
    /**
     * Creates a blank HTML view which can be populated with a later call to
     * {@link #setContents}.
     */
    public BangHTMLView ()
    {
    }

    /**
     * Creates an HTML view with the supplied contents.
     */
    public BangHTMLView (String text)
    {
        setContents(text);
    }

    /**
     * Configures the contents of this HTML view.
     */
    public void setContents (String text)
    {
        setContents(null, text);
    }

    /**
     * Configures the contents of this HTML view using the supplied URL as a
     * base for paths in the document.
     */
    public void setContents (URL docbase, String text)
    {
        HTMLDocument doc = new HTMLDocument(BangUI.css);
        if (docbase != null) {
            doc.setBase(docbase);
        }
        try {
            getEditorKit().read(new StringReader(text), doc, 0);
            setContents(doc);
        } catch (Throwable t) {
            log.warning("Failed to parse HTML '" + text + "'.", t);
        }
    }
}
