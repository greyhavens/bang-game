//
// $Id$

package com.threerings.bang.editor;

import javax.swing.JPanel;

import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.event.MouseEvent;

import com.threerings.crowd.util.CrowdContext;
import com.threerings.jme.sprite.Sprite;
import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;

/**
 * The superclass of editor tools, which have option panels to display in the
 * user interface and perform operations on input received from the view.
 */
public abstract class EditorTool
{
    public EditorTool (EditorContext ctx, EditorPanel panel)
    {
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("editor");
        _panel = panel;
        _ctrl = (EditorController)panel.getController();
    }

    /**
     * Returns the name of this tool.
     */
    public abstract String getName ();

    /**
     * Returns the panel displaying this tool's options.
     */
    public JPanel getOptions ()
    {
        if (_options == null) {
            _options = createOptions();
        }
        return _options;
    }

    /**
     * Called when we are made the active tool.
     */
    public void activate ()
    {
    }

    /**
     * Called when we are no longer the active tool.
     */
    public void deactivate ()
    {
    }

    /**
     * Called by the tool palette when we're the active tool.
     */
    public void mousePressed (MouseEvent e)
    {
    }

    /**
     * Called by the tool palette when we're the active tool.
     */
    public void mouseReleased (MouseEvent e)
    {
    }

    /**
     * Called by the tool palette when we're the active tool.
     */
    public void mouseMoved (MouseEvent e)
    {
    }

    /**
     * Called by the tool palette when we're the active tool.
     */
    public void mouseDragged (MouseEvent e)
    {
    }

    /**
     * Called by the tool palette when we're the active tool.
     */
    public void mouseWheeled (MouseEvent e)
    {
    }

    /**
     * Called by the tool palette when we're the active tool.
     */
    public void keyPressed (KeyEvent e)
    {
    }

    /**
     * Called by the tool palette when we're the active tool.
     */
    public void keyReleased (KeyEvent e)
    {
    }

    /**
     * Notifies the tool that the tile over which the mouse is hovering has
     * changed.
     */
    public void hoverTileChanged (int tx, int ty)
    {
    }

    /**
     * Notifies the tool that the sprite over which the mouse is hovering has
     * changed.
     */
    public void hoverSpriteChanged (Sprite hover)
    {
    }

    // documentation inherited
    public String toString ()
    {
        return _msgs.get("m.tool_" + getName());
    }

    /** Returns a reference to the game object. */
    protected BangObject getBangObject ()
    {
        return (BangObject)((CrowdContext)
            _ctx).getLocationDirector().getPlaceObject();
    }

    /**
     * Creates the options panel for this tool.
     */
    protected abstract JPanel createOptions ();

    protected EditorContext _ctx;
    protected MessageBundle _msgs;
    protected EditorPanel _panel;
    protected EditorController _ctrl;
    protected JPanel _options;
}
