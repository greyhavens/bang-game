//
// $Id$

package com.threerings.bang.client.bui;

import com.jmex.bui.BButton;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BIcon;

/**
 * A button that may have alternate icons for states other than {@link #DEFAULT}.
 */
public class MultiIconButton extends BButton
{
    public MultiIconButton (BIcon icon, ActionListener listener, String action)
    {
        super(icon, listener, action);
        initIcons();
        _icons[DEFAULT] = icon;
    }

    /**
     * Sets the icon for the specified state.
     */
    public void setIcon (BIcon icon, int state)
    {
        initIcons();
        _icons[state] = icon;
        updateIcon();
    }

    /**
     * Returns the icon for the specified state.
     */
    public BIcon getIcon (int state)
    {
        initIcons();
        BIcon icon = _icons[state];
        return (icon == null) ? _icons[DEFAULT] : icon;
    }

    @Override // documentation inherited
    public void setIcon (BIcon icon)
    {
        setIcon(icon, DEFAULT);
    }

    @Override // documentation inherited
    public BIcon getIcon ()
    {
        return getIcon(getState());
    }

    @Override // documentation inherited
    protected void stateDidChange () {
        super.stateDidChange();
        updateIcon();
    }

    protected void updateIcon ()
    {
        _label.setIcon(getIcon());
    }

    protected void initIcons ()
    {
        if (_icons == null) {
            _icons = new BIcon[getStateCount()];
        }
    }

    /** Icons for each state. */
    protected BIcon[] _icons;
}
