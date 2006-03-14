//
// $Id$

package com.threerings.bang.ranch.client;

import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.system.DisplaySystem;

import com.jmex.bui.BGeomView;
import com.jmex.bui.BImage;
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
    public UnitView (BangContext ctx, boolean small)
    {
        super(new Node("Unit Model"));
        String prefix = small ? "medium_" : "big_";
        setStyleClass(prefix + "unit_view");

        _ctx = ctx;
        _geom.setRenderState(RenderUtil.lequalZBuf);
        _geom.updateRenderState();
        _frame = ctx.loadImage("ui/frames/" + prefix + "frame.png");
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
        _binding = anim.bind((Node)_geom, 0, null, null);
    }

    @Override // documentation inherited
    public Dimension getPreferredSize (int whint, int hhint)
    {
        // avoid accounting for insets and all the other bits
        return new Dimension(_frame.getWidth(), _frame.getHeight());
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
    protected Camera createCamera (DisplaySystem ds)
    {
        Camera cam = super.createCamera(ds);

        // position and point up our camera
        Vector3f loc = new Vector3f(
            10*TILE_SIZE/16, -18*TILE_SIZE/16, 7*TILE_SIZE/16);
        cam.setLocation(loc);
        Matrix3f rotm = new Matrix3f();
        rotm.fromAngleAxis(-FastMath.PI/2, cam.getLeft());
        rotm.mult(cam.getDirection(), cam.getDirection());
        rotm.mult(cam.getUp(), cam.getUp());
        rotm.mult(cam.getLeft(), cam.getLeft());

        rotm.fromAngleAxis(FastMath.PI/6, cam.getUp());
        rotm.mult(cam.getDirection(), cam.getDirection());
        rotm.mult(cam.getUp(), cam.getUp());
        rotm.mult(cam.getLeft(), cam.getLeft());

//         rotm.fromAngleAxis(FastMath.PI/6, cam.getLeft());
//         rotm.mult(cam.getDirection(), cam.getDirection());
//         rotm.mult(cam.getUp(), cam.getUp());
//         rotm.mult(cam.getLeft(), cam.getLeft());
        cam.update();

        return cam;
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        _frame.render(renderer, 0, 0, _alpha);
        super.renderComponent(renderer);
    }

    protected BangContext _ctx;
    protected Node _unode;
    protected Model.Binding _binding;
    protected BImage _frame;
}
