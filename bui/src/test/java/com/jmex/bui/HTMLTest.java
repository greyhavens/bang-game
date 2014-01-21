//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import java.io.StringReader;
import java.util.logging.Level;

import java.awt.Font;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS;
import javax.swing.text.html.StyleSheet;

import com.jme.util.LoggingSystem;

import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.text.HTMLView;

/**
 * Tests our HTML view.
 */
public class HTMLTest extends BaseTest
{
    protected void createWindows (BRootNode root, BStyleSheet style)
    {
        // test out custom font handling
        StyleSheet sheet = new StyleSheet() {
            public Font getFont (AttributeSet attrs) {
                // Java's style sheet parser annoyingly looks up whatever is
                // supplied for font-family and if it doesn't map to an
                // internal Java font; it discards it. Thanks! So we do this
                // hackery with the font-variant which it passes through
                // unmolested.
                String variant = (String)
                    attrs.getAttribute(CSS.Attribute.FONT_VARIANT);
                if (variant != null && variant.equalsIgnoreCase("Test")) {
                    int style = Font.PLAIN;
                    if (StyleConstants.isBold(attrs)) {
                        style |= Font.BOLD;
                    }
                    if (StyleConstants.isItalic(attrs)) {
                        style |= Font.ITALIC;
                    }
                    int size = StyleConstants.getFontSize(attrs);
                    if (StyleConstants.isSuperscript(attrs) ||
                        StyleConstants.isSubscript(attrs)) {
                        size -= 2;
                    }
                    return new Font("Serif", style, size);
                } else {
                    return super.getFont(attrs);
                }
            }
        };
        try {
            String styledef = ".test { " +
                "font-variant: \"Test\"; " +
                "font-size: 16; }";
            sheet.loadRules(new StringReader(styledef), null);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }

        BWindow window = new BWindow(style, new BorderLayout(5, 5));
        HTMLView view = new HTMLView();
        view.setStyleSheet(sheet);
        view.setContents(
            "<html><body>" +
            "<span class=\"test\">This is some test <b>HTML!</b></span> " +
            "With a fairly long first line that we might even wrap."+
            "<p><table border=1 width=\"100%\">" +
            "<tr><td>We</td><td>even</td><td>support</td></tr>" +
            "<tr><td colspan=3 align=center><i>tables!</i></td></tr></table>" +
            "</body></html>");
        window.add(view, BorderLayout.CENTER);
        root.addWindow(window);
        window.setBounds(100, 100, 200, 100);
        window.center();
    }

    public static void main (String[] args)
    {
        LoggingSystem.getLogger().setLevel(Level.WARNING);
        HTMLTest test = new HTMLTest();
        test.start();
    }
}
