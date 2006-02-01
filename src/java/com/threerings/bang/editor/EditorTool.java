//
// $Id$

package com.threerings.bang.editor;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseListener;
import com.jmex.bui.event.MouseMotionListener;
import com.jmex.bui.event.MouseWheelListener;

import com.threerings.crowd.util.CrowdContext;
import com.threerings.jme.sprite.Sprite;
import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;

/**
 * The superclass of editor tools, which have option panels to display in the
 * user interface and perform operations on input received from the view.
 */
public abstract class EditorTool
    implements MouseListener, MouseMotionListener, MouseWheelListener
{
    public EditorTool (EditorContext ctx, EditorPanel panel)
    {
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("editor");
        _panel = panel;
    }
    
    /**
     * Returns the name of this tool.
     */
    public abstract String getName ();
    
    /**
     * Activates this tool.
     */
    public void activate ()
    {
        _panel.view.addListener(this);
    }
    
    /**
     * Deactivates this tool.
     */
    public void deactivate ()
    {
        _panel.view.removeListener(this);
    }
    
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
    
    // documentation inherited from interface MouseListener
    public void mouseClicked (MouseEvent e)
    {
    }

    // documentation inherited from interface MouseListener
    public void mousePressed (MouseEvent e)
    {
    }

    // documentation inherited from interface MouseListener
    public void mouseReleased (MouseEvent e)
    {
    }

    // documentation inherited from interface MouseListener
    public void mouseEntered (MouseEvent e)
    {
    }

    // documentation inherited from interface MouseListener
    public void mouseExited (MouseEvent e)
    {
    }
    
    // documentation inherited from interface MouseMotionListener
    public void mouseMoved (MouseEvent e)
    {
    }
    
    // documentation inherited from interface MouseMotionListener
    public void mouseDragged (MouseEvent e)
    {
    }
    
    // documentation inherited from interface MouseWheelListener
    public void mouseWheeled (MouseEvent e)
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
    protected JPanel _options;
}
