//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.text;

import junit.framework.Test;
import junit.framework.TestCase;

/**
 * A unit test for {@link Document}.
 */
public class DocumentUTest extends TestCase
{
    public static Test suite ()
    {
        return new DocumentUTest("testEdit");
    }

    public static void main (String[] args)
    {
        try {
            DocumentUTest test = new DocumentUTest("testEdit");
            test.runTest();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    public DocumentUTest (String method)
    {
        super(method);
    }

    public void testEdit ()
    {
        Document doc = new Document();
        int lineidx = 0;
        doc.insert(0, "The quick brown fox jumped over the lazy dog.");
        assertTrue("Check line: " + OUTPUT[lineidx],
                   doc.getText().equals(OUTPUT[lineidx++]));
        doc.insert(0, "I heard that ");
        assertTrue("Check line " + OUTPUT[lineidx],
                   doc.getText().equals(OUTPUT[lineidx++]));
        doc.remove(0, "I heard that ".length());
        assertTrue("Check line " + OUTPUT[lineidx],
                   doc.getText().equals(OUTPUT[lineidx++]));

        doc.insert(0, "Some guy said, \"");
        doc.insert(doc.getLength(), "\"");
        assertTrue("Check line " + OUTPUT[lineidx],
                   doc.getText().equals(OUTPUT[lineidx++]));

        doc.replace(doc.getText().indexOf("lazy"), "lazy".length(), "spritely");
        assertTrue("Check line " + OUTPUT[lineidx],
                   doc.getText().equals(OUTPUT[lineidx++]));
    }

    protected static final String[] OUTPUT = {
        "The quick brown fox jumped over the lazy dog.",
        "I heard that The quick brown fox jumped over the lazy dog.",
        "The quick brown fox jumped over the lazy dog.",
        "Some guy said, \"The quick brown fox jumped over the lazy dog.\"",
        "Some guy said, \"The quick brown fox jumped over the spritely dog.\"",
    };
}
