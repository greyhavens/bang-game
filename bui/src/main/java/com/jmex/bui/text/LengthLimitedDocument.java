//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.text;

import com.jmex.bui.BTextField;

/**
 * A document for use with a {@link BTextField} that limits the input to a
 * maximum length.
 */
public class LengthLimitedDocument extends Document
{
    /**
     * Creates a document that will limit its maximum length to the specified
     * value.
     */
    public LengthLimitedDocument (int maxLength)
    {
        _maxLength = maxLength;
    }

    // documentation inherited
    protected boolean validateEdit (String oldText, String newText)
    {
        return newText.length() <= _maxLength;
    }

    protected int _maxLength;
}
