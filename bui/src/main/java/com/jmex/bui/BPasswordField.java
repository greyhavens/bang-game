//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

/**
 * A derivation of {@link BTextField} that does not display the actual
 * text, but asterisks instead.
 */
public class BPasswordField extends BTextField
{
    public BPasswordField ()
    {
    }

    public BPasswordField (int maxLength)
    {
        super(maxLength);
    }

    public BPasswordField (String text)
    {
        super(text);
    }

    public BPasswordField (String text, int maxLength)
    {
        super(text, maxLength);
    }

    // documentation inherited
    protected String getDisplayText ()
    {
        String text = super.getDisplayText();
        if (text == null) {
            return null;
        } else if (_stars == null || _stars.length() != text.length()) {
            StringBuffer stars = new StringBuffer();
            for (int ii = 0; ii < text.length(); ii++) {
                stars.append("*");
            }
            _stars = stars.toString();
        }
        return _stars;
    }

    protected String _stars;
}
