//
// $Id$

package com.threerings.bang.ranch.client;

import java.util.Properties;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;

import com.jmex.bui.BGeomView;
import com.jmex.bui.BImage;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.StringUtil;

import com.threerings.jme.model.Model;

import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a fancy animated version of a particular unit.
 *
 * <h3>jME3 cutover (Phase 2, cluster 9)</h3>
 *
 * The fork drove its own {@code com.jme.renderer.Camera} (created via {@code createCamera(
 * DisplaySystem)}) and rendered the unit model 3D into the BUI component through the fork
 * renderer's ortho dance. {@link BGeomView} is now engine-neutral: it holds a jME3
 * {@link com.jme3.scene.Spatial} and defers the actual 3D render to a dedicated jME3
 * {@code ViewPort} (or a {@link com.threerings.bang.util.BackTextureRenderer} RTT pass) installed
 * at the Phase-3 host cutover. So here we keep the model spatial + the portrait {@link Camera}
 * (built/positioned exactly as before, on jME3 math), and hand them off via {@link #getGeometry}/
 * {@link #getPortraitCamera} for the Phase-3 viewport path to consume. The fork
 * {@code setRenderState(lequalZBuf)} on the container is dropped — the loaded {@code .j3o} model
 * carries its own per-geometry materials (depth preset baked in by the converter / applied by
 * {@code RenderUtil} on the sprite/model side).
 */
public class UnitView extends BGeomView
{
    public UnitView (BangContext ctx, boolean small)
    {
        super(new Node("Unit Model"));
        String prefix = small ? "medium_" : "big_";
        setStyleClass(prefix + "unit_view");

        _ctx = ctx;
        _frame = ctx.loadImage("ui/frames/" + prefix + "frame.png");
    }

    /**
     * Returns the portrait camera, positioned for the current model (or null until the model and
     * a target size are available). The per-component viewport path ({@link BGeomView}) renders
     * {@link #getGeometry} through this camera into the component rect.
     */
    public Camera getPortraitCamera ()
    {
        return _camera;
    }

    /**
     * Configures the unit displayed by this view.
     */
    public void setUnit (final UnitConfig config)
    {
        _config = config;
        ((Node)getGeometry()).detachAllChildren();
        _ctx.loadModel("units", config.type,
            new ResultAttacher<Model>((Node)getGeometry()) {
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
        // build the portrait camera BEFORE super.wasAdded() so BGeomView.createViewport() (invoked
        // from super) picks it up via getViewCamera(); the frame gives us the target pixel size.
        if (_camera == null) {
            _camera = new Camera(_frame.getWidth(), _frame.getHeight());
            if (_model != null) {
                positionCamera(_camera, _model);
            }
        }

        super.wasAdded();
        _frame.reference();
    }

    @Override // documentation inherited
    protected Camera getViewCamera ()
    {
        // hand BGeomView's per-component viewport our model-framing portrait camera (BGeomView
        // manages its screen-rect viewport and frustum aspect).
        return (_camera != null) ? _camera : super.getViewCamera();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _frame.release();
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
        camera.setLocation(loc);
        float sinh = FastMath.sin(heading), cosh = FastMath.cos(heading),
            sinp = FastMath.sin(pitch), cosp = FastMath.cos(pitch);
        // jME3 Camera has no mutable left/up/direction accessors; set the basis atomically.
        camera.setAxes(
            new Vector3f(-cosh, -sinh, 0f),
            new Vector3f(sinh * sinp, -cosh * sinp, cosp),
            new Vector3f(-sinh * cosp, cosh * cosp, sinp));
    }

    @Override // documentation inherited
    protected void renderComponent (RenderManager renderer)
    {
        // the framed background; the 3D portrait render is the Phase-3 viewport/RTT pass
        _frame.render(renderer, 0, 0, _alpha);
        super.renderComponent(renderer);
    }

    protected BangContext _ctx;
    protected BImage _frame;
    protected UnitConfig _config;
    protected Model _model;

    /** The portrait camera, built lazily once a target size is known; consumed by the Phase-3
     * viewport/RTT render path. */
    protected Camera _camera;
}
