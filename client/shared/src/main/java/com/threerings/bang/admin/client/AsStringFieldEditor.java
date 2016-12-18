//
// $Id$

package com.threerings.bang.admin.client;

import java.lang.reflect.Field;

import com.jmex.bui.BTextField;

import com.samskivert.util.StringUtil;

import com.threerings.bang.admin.data.ConfigObject;

import static com.threerings.bang.Log.log;

/**
 * Allows for the editing of arbitrary config object fields by converting them
 * to and from strings.
 */
public class AsStringFieldEditor extends FieldEditor
{
    public AsStringFieldEditor (Field field, ConfigObject confobj)
    {
        super(field, confobj);
        add(_value = new BTextField());
        _value.addListener(this);
    }

    @Override // documentation inherited
    protected Object getDisplayValue ()
        throws Exception
    {
        String text = _value.getText();
        if (_field.getType().equals(Integer.class) ||
            _field.getType().equals(Integer.TYPE)) {
            return new Integer(text);

        } else if (_field.getType().equals(Long.class) ||
                   _field.getType().equals(Long.TYPE)) {
            return new Long(text);

        } else if (_field.getType().equals(Float.class) ||
                   _field.getType().equals(Float.TYPE)) {
            return new Float(text);

        } else if (_field.getType().equals(Double.class) ||
                   _field.getType().equals(Double.TYPE)) {
            return new Double(text);

        } else if (_field.getType().equals(String.class)) {
            return text;

        } else if (_field.getType().equals(STRING_ARRAY_PROTO.getClass())) {
            return StringUtil.parseStringArray(_value.getText());

        } else if (_field.getType().equals(INT_ARRAY_PROTO.getClass())) {
            return StringUtil.parseIntArray(_value.getText());

        } else if (_field.getType().equals(FLOAT_ARRAY_PROTO.getClass())) {
            return StringUtil.parseFloatArray(_value.getText());

        } else if (_field.getType().equals(LONG_ARRAY_PROTO.getClass())) {
            return StringUtil.parseLongArray(_value.getText());

        } else if (_field.getType().equals(Boolean.TYPE)) {
            return Boolean.valueOf(_value.getText().equalsIgnoreCase("true"));

        } else {
            log.warning("Unknown field type '" + _field.getName() + "': " +
                        _field.getType().getName() + ".");
            return null;
        }
    }

    @Override // documentation inherited
    protected void displayValue (Object value)
    {
        _value.setText(StringUtil.toString(value, "", ""));
    }

    protected BTextField _value;

    protected static final String[] STRING_ARRAY_PROTO = new String[0];
    protected static final int[] INT_ARRAY_PROTO = new int[0];
    protected static final float[] FLOAT_ARRAY_PROTO = new float[0];
    protected static final long[] LONG_ARRAY_PROTO = new long[0];
}
