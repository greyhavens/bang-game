//
// $Id$

package com.threerings.bang.ranch.client;

import com.jme.image.Image;
import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;

import com.jmex.bui.BGeomView;
import com.jmex.bui.util.Dimension;

import com.threerings.bang.client.Model;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a fancy animated version of a particular unit.
 */
public class UnitView extends BGeomView
{
    public UnitView (BangContext ctx)
    {
        super(new Node("Unit Model"));
        setStyleClass("unit_view");

        _ctx = ctx;
        _geom.setRenderState(RenderUtil.lequalZBuf);
        _geom.updateRenderState();
        _frame = ctx.loadImage("ui/barber/avatar_frame.png");

        // position and point up our camera
        Vector3f loc = new Vector3f(
            10*TILE_SIZE/16, -18*TILE_SIZE/16, 7*TILE_SIZE/16);
        _camera.setLocation(loc);
        Matrix3f rotm = new Matrix3f();
        rotm.fromAngleAxis(-FastMath.PI/2, _camera.getLeft());
        rotm.mult(_camera.getDirection(), _camera.getDirection());
        rotm.mult(_camera.getUp(), _camera.getUp());
        rotm.mult(_camera.getLeft(), _camera.getLeft());

        rotm.fromAngleAxis(FastMath.PI/6, _camera.getUp());
        rotm.mult(_camera.getDirection(), _camera.getDirection());
        rotm.mult(_camera.getUp(), _camera.getUp());
        rotm.mult(_camera.getLeft(), _camera.getLeft());

//         rotm.fromAngleAxis(FastMath.PI/6, _camera.getLeft());
//         rotm.mult(_camera.getDirection(), _camera.getDirection());
//         rotm.mult(_camera.getUp(), _camera.getUp());
//         rotm.mult(_camera.getLeft(), _camera.getLeft());
        _camera.update();
    }

    /**
     * Configures the unit displayed by this view.
     */
    public void setUnit (UnitConfig config)
    {
        if (_binding != null) {
            _binding.detach();
        }

        Model model = _ctx.loadModel("units", config.type);
        Model.Animation anim = model.getAnimation("standing");
        _binding = anim.bind((Node)_geom, 0);
    }

    @Override // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return new Dimension(234, 300);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        if (_binding != null) {
            _binding.detach();
        }
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        com.jmex.bui.util.RenderUtil.blendState.apply();
        com.jmex.bui.util.RenderUtil.renderImage(_frame, 0, 0);
        super.renderComponent(renderer);
    }

    protected BangContext _ctx;
    protected Node _unode;
    protected Model.Binding _binding;
    protected Image _frame;
}
