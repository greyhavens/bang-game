//
// $Id$

package com.threerings.bang.admin.data;

import java.lang.reflect.Field;

import com.threerings.presents.dobj.DObject;

import com.threerings.bang.admin.client.BooleanFieldEditor;
import com.threerings.bang.admin.client.AsStringFieldEditor;
import com.threerings.bang.admin.client.FieldEditor;

/**
 * A shared base class for our runtime configuration objects that provide BUI
 * based field editors.
 */
public class ConfigObject extends DObject
{
    /**
     * Returns the editor component for the specified field.
     */
    public FieldEditor createEditor (Field field)
    {
        if (field.getType().equals(Boolean.TYPE)) {
            return new BooleanFieldEditor(field, this);
        } else {
            return new AsStringFieldEditor(field, this);
        }
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.presents.dobj.DObject.class, "_oid", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.presents.dobj.DObject.class, "_oid", this, out);
    }
}
