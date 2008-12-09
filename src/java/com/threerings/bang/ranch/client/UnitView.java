//
// $Id$

package com.threerings.bang.ranch.client;

import java.util.Properties;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.system.DisplaySystem;

import com.jmex.bui.BGeomView;
import com.jmex.bui.BImage;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.StringUtil;

import com.threerings.jme.model.Model;

import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.*;
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
    public void setUnit (final UnitConfig config)
    {
        _config = config;
        ((Node)_geom).detachAllChildren();
        _ctx.loadModel("units", config.type,
            new ResultAttacher<Model>((Node)_geom) {
            public void requestCompleted (Model model) {
                // make sure unit hasn't changed since we started loading
                if (_config != config) {
                    return;
                }
                super.requestCompleted(_model = model);
                if (model.hasAnimation("standing")) {
                    model.startAnimation("standing");
                }
                if (_camera != null) {
                    positionCamera(_camera, model);
                }
            }
        });
    }

    @Override // documentation inherited
    public Dimension getPreferredSize (int whint, int hhint)
    {
        // avoid accounting for insets and all the other bits
        return new Dimension(_frame.getWidth(), _frame.getHeight());
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _frame.reference();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _frame.release();
    }

    @Override // documentation inherited
    protected Camera createCamera (DisplaySystem ds)
    {
        Camera camera = super.createCamera(ds);
        if (_model != null) {
            positionCamera(camera, _model);
        }
        return camera;
    }
    
    /**
     * Positions the camera as appropriate for the model.
     */
    protected void positionCamera (Camera camera, Model model)
    {
        Properties props = model.getProperties();
        String cpos = props.getProperty("camera_position"),
            crot = props.getProperty("camera_rotation");
        Vector3f loc = new Vector3f(10*TILE_SIZE/16, -18*TILE_SIZE/16,
            7*TILE_SIZE/16);
        if (cpos != null) {
            float[] vals = StringUtil.parseFloatArray(cpos);
            if (vals != null && vals.length == 3) {
                loc = new Vector3f(vals[0], vals[1], vals[2]);
            } else {
                log.warning("Invalid camera position value", "model", model.getName(),
                            "value", cpos);
            }
        }
        float heading = FastMath.PI/6, pitch = 0f;
        if (crot != null) {
            float[] vals = StringUtil.parseFloatArray(crot);
            if (vals != null && vals.length == 2) {
                heading = vals[0] * FastMath.DEG_TO_RAD;
                pitch = vals[1] * FastMath.DEG_TO_RAD;
            } else {
                log.warning("Invalid camera rotation value", "model", model.getName(),
                            "value", crot);
            }
        }
        camera.getLocation().set(loc);
        float sinh = FastMath.sin(heading), cosh = FastMath.cos(heading),
            sinp = FastMath.sin(pitch), cosp = FastMath.cos(pitch);
        camera.getLeft().set(-cosh, -sinh, 0f);
        camera.getUp().set(sinh * sinp, -cosh * sinp, cosp);
        camera.getDirection().set(-sinh * cosp, cosh * cosp, sinp);
    }
    
    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        _frame.render(renderer, 0, 0, _alpha);
        super.renderComponent(renderer);
    }

    protected BangContext _ctx;
    protected Node _unode;
    protected BImage _frame;
    protected UnitConfig _config;
    protected Model _model;
}
