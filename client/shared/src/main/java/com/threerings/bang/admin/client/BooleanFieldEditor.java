//
// $Id$

package com.threerings.bang.admin.client;

import java.lang.reflect.Field;

import com.jmex.bui.BCheckBox;

import com.threerings.bang.admin.data.ConfigObject;

/**
 * Allows for the editing of boolean config object fields.
 */
public class BooleanFieldEditor extends FieldEditor
{
    public BooleanFieldEditor (Field field, ConfigObject confobj)
    {
        super(field, confobj);
        add(_checkbox = new BCheckBox(""));
        _checkbox.addListener(this);
    }

    @Override // documentation inherited
    protected Object getDisplayValue ()
        throws Exception
    {
        return Boolean.valueOf(_checkbox.isSelected());
    }

    @Override // documentation inherited
    protected void displayValue (Object value)
    {
        _checkbox.setSelected((Boolean)value);
    }

    protected BCheckBox _checkbox;
}
