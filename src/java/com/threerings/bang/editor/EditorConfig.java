//
// $Id$

package com.threerings.bang.editor;

import com.threerings.bang.editor.EditorController;
import com.threerings.bang.game.data.BangConfig;

/**
 * Used to configure the Bang! editor.
 */
public class EditorConfig extends BangConfig
{
    @Override // documentation inherited
    public String getBundleName ()
    {
        return "bang";
    }

    @Override // documentation inherited
    public Class getControllerClass ()
    {
        return EditorController.class;
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.editor.EditorManager";
    }
}
