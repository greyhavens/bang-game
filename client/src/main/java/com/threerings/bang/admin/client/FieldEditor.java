//
// $Id$

package com.threerings.bang.admin.client;

import java.lang.reflect.Field;

import com.jmex.bui.BContainer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.ObjectUtil;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.admin.data.ConfigObject;

import static com.threerings.bang.Log.log;

/**
 * A base class for all field editors.
 */
public abstract class FieldEditor extends BContainer
    implements AttributeChangeListener, ActionListener
{
    public FieldEditor (Field field, ConfigObject object)
    {
        super(GroupLayout.makeVStretch());
        _field = field;
        _object = object;
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        displayValue(getValue());
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        valueDidChange();
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // listen to the object while we're visible
        _object.addListener(this);
        displayValue(getValue());
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // stop listening when we're hidden
        _object.removeListener(this);
    }

    /** Converts the currently displayed value to the appropriate type and
     * returns it. */
    protected abstract Object getDisplayValue ()
        throws Exception;

    /** Displays the supplied field value in the editor. */
    protected abstract void displayValue (Object value);

    /** Returns the current field value. */
    protected Object getValue ()
    {
        try {
            return _field.get(_object);
        } catch (Exception e) {
            log.warning("Failed to fetch field", "field", _field, "object", _object, e);
            return null;
        }
    }

    /** This should be called by derived field editors when a change to their
     * distributed value should be committed to the config object. */
    protected void valueDidChange ()
    {
        Object value = null;
        try {
            value = getDisplayValue();
        } catch (Exception e) {
            // updateBorder(true);
        }

        // submit an attribute changed event with the new value
        if (!ObjectUtil.equals(value, getValue())) {
            _object.changeAttribute(_field.getName(), value);
        }
    }

    protected Field _field;
    protected ConfigObject _object;
}
