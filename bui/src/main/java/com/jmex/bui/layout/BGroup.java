//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.layout;

import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;

/**
 * A builder for {@link GroupLayout} instances. You use it like so:
 *
 * <pre>
 * BContainer cont = new BContainer(BGroup.vert().offStretch().onTop().make());
 * // or even more compact:
 * BContainer cont = BGroup.vert().offStretch().onTop().makeBox();
 * </pre>
 *
 * The layout constructed defaults to constrained off-axis layout, on and off-axis centering and a
 * gap of 5 pixels.
 */
public class BGroup
{
    /**
     * Starts vertical layout with a policy of {@link GroupLayout#NONE}.
     */
    public static BGroup vert ()
    {
        return new BGroup(false, GroupLayout.NONE);
    }

    /**
     * Starts a vertical layout with a policy of {@link GroupLayout#STRETCH}.
     */
    public static BGroup vertStretch ()
    {
        return new BGroup(false, GroupLayout.STRETCH);
    }

    /**
     * Starts a vertical layout with a policy of {@link GroupLayout#EQUALIZE}.
     */
    public static BGroup vertEqual ()
    {
        return new BGroup(false, GroupLayout.EQUALIZE);
    }

    /**
     * Starts horizontal layout with a policy of {@link GroupLayout#NONE}.
     */
    public static BGroup horiz ()
    {
        return new BGroup(true, GroupLayout.NONE);
    }

    /**
     * Starts a horizontal layout with a policy of {@link GroupLayout#STRETCH}.
     */
    public static BGroup horizStretch ()
    {
        return new BGroup(true, GroupLayout.STRETCH);
    }

    /**
     * Starts a horizontal layout with a policy of {@link GroupLayout#EQUALIZE}.
     */
    public static BGroup horizEqual ()
    {
        return new BGroup(true, GroupLayout.EQUALIZE);
    }

    /**
     * Configures the off-axis policy for our layout to {@link GroupLayout#NONE}.
     */
    public BGroup offNone ()
    {
        _offPolicy = GroupLayout.NONE;
        return this;
    }

    /**
     * Configures the off-axis policy for our layout to {@link GroupLayout#STRETCH}.
     */
    public BGroup offStretch ()
    {
        _offPolicy = GroupLayout.STRETCH;
        return this;
    }

    /**
     * Configures the off-axis policy for our layout to {@link GroupLayout#EQUALIZE}.
     */
    public BGroup offEqual ()
    {
        _offPolicy = GroupLayout.EQUALIZE;
        return this;
    }

    /**
     * Configures the on-axis justification of this group to {@link GroupLayout#TOP}.
     */
    public BGroup alignTop ()
    {
        _onJust = GroupLayout.TOP;
        return this;
    }

    /**
     * Configures the on-axis justification of this group to {@link GroupLayout#LEFT}.
     */
    public BGroup alignLeft ()
    {
        _onJust = GroupLayout.LEFT;
        return this;
    }

    /**
     * Configures the on-axis justification of this group to {@link GroupLayout#CENTER}.
     */
    public BGroup alignCenter ()
    {
        _onJust = GroupLayout.CENTER;
        return this;
    }

    /**
     * Configures the on-axis justification of this group to {@link GroupLayout#BOTTOM}.
     */
    public BGroup alignBottom ()
    {
        _onJust = GroupLayout.BOTTOM;
        return this;
    }

    /**
     * Configures the on-axis justification of this group to {@link GroupLayout#RIGHT}.
     */
    public BGroup alignRight ()
    {
        _onJust = GroupLayout.RIGHT;
        return this;
    }

    /**
     * Configures the off-axis justification of this group to {@link GroupLayout#TOP}.
     */
    public BGroup offAlignTop ()
    {
        _offJust = GroupLayout.TOP;
        return this;
    }

    /**
     * Configures the off-axis justification of this group to {@link GroupLayout#LEFT}.
     */
    public BGroup offAlignLeft ()
    {
        _offJust = GroupLayout.LEFT;
        return this;
    }

    /**
     * Configures the off-axis justification of this group to {@link GroupLayout#CENTER}.
     */
    public BGroup offAlignCenter ()
    {
        _offJust = GroupLayout.CENTER;
        return this;
    }

    /**
     * Configures the off-axis justification of this group to {@link GroupLayout#BOTTOM}.
     */
    public BGroup offAlignBottom ()
    {
        _offJust = GroupLayout.BOTTOM;
        return this;
    }

    /**
     * Configures the off-axis justification of this group to {@link GroupLayout#RIGHT}.
     */
    public BGroup offAlignRight ()
    {
        _offJust = GroupLayout.RIGHT;
        return this;
    }

    /**
     * Configures our gap to the specified value.
     */
    public BGroup gap (int gap)
    {
        _gap = gap;
        return this;
    }

    /**
     * Creates a {@link GroupLayout} instance with our configuration.
     */
    public GroupLayout make ()
    {
        GroupLayout layout = _horizonal ? new HGroupLayout() : new VGroupLayout();
        layout.setPolicy(_onPolicy);
        layout.setOffAxisPolicy(_offPolicy);
        layout.setJustification(_onJust);
        layout.setOffAxisJustification(_offJust);
        layout.setGap(_gap);
        return layout;
    }

    /**
     * Creates a {@link BContainer} instance using a {@link GroupLayout} created via {@link #make}.
     *
     * @param children any children to add to the box.
     */
    public BContainer makeBox (BComponent ... children)
    {
        BContainer box = new BContainer(make());
        for (BComponent child : children) {
            box.add(child);
        }
        return box;
    }

    protected BGroup (boolean horizontal, GroupLayout.Policy onPolicy)
    {
        _horizonal = horizontal;
        _onPolicy = onPolicy;
    }

    protected boolean _horizonal;
    protected GroupLayout.Policy _onPolicy;
    protected GroupLayout.Policy _offPolicy = GroupLayout.CONSTRAIN;
    protected GroupLayout.Justification _onJust = GroupLayout.CENTER;
    protected GroupLayout.Justification _offJust = GroupLayout.CENTER;
    protected int _gap = GroupLayout.DEFAULT_GAP;
}
