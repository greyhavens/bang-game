//
// $Id$

package com.threerings.bang.viewer;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.io.File;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.ZBufferState;

import com.samskivert.swing.VGroupLayout;

import com.threerings.jme.JmeContext;
import com.threerings.jme.sprite.Sprite;
import com.threerings.util.KeyDispatcher;

import com.threerings.bang.client.Model;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Displays our various controls.
 */
public class ControlPanel extends JPanel
    implements ActionListener, KeyListener
{
    public ControlPanel (JFrame frame, BasicContext ctx)
    {
        _ctx = ctx;
        _keydisp = new KeyDispatcher(frame);
        _keydisp.addGlobalKeyListener(this);

        setLayout(new VGroupLayout(VGroupLayout.NONE, VGroupLayout.TOP));

        // create our user interface
        add(new JLabel("Look a moose!"));
        addButton("Load model", "load");
        add(_actions = new JComboBox());
        _actions.addActionListener(this);
        _actions.setActionCommand("select_action");
        add(_animate = new JCheckBox("Animate"));
        _animate.addActionListener(this);
        _animate.setActionCommand("toggle_animation");
    }

    protected void addButton (String label, String action)
    {
        JButton button;
        add(button = new JButton(label));
        button.setActionCommand(action);
        button.addActionListener(this);
    }

    public void init ()
    {
        // add a node to which we'll attach our model geometry
        _mnode = new Node("model");

        ZBufferState lequalZBuf = _ctx.getRenderer().createZBufferState();
        lequalZBuf.setEnabled(true);
        lequalZBuf.setFunction(ZBufferState.CF_LEQUAL);
        _mnode.setRenderState(lequalZBuf);

        _ctx.getGeometry().attachChild(_mnode);

        // load up any model specified on the command line
        if (ViewerApp.appArgs.length > 0) {
            loadModel(new File(ViewerApp.appArgs[0]));
        }
    }

    public Dimension getPreferredSize ()
    {
        Dimension d = super.getPreferredSize();
        d.width = 125;
        return d;
    }

    public void keyTyped (KeyEvent e)
    {
        // N/A
    }

    public void keyPressed (KeyEvent e)
    {
        switch (e.getKeyCode()) {
        case KeyEvent.VK_LEFT:
            rotate(FastMath.PI/50);
            break;
        case KeyEvent.VK_RIGHT:
            rotate(-FastMath.PI/50);
            break;
        case KeyEvent.VK_UP:
            zoom(1.5f);
            break;
        case KeyEvent.VK_DOWN:
            zoom(-1.5f);
            break;
        }
    }

    protected void rotate (float delta)
    {
        _angle += delta;
        _rotor.fromAngleAxis(_angle, _up);
        _mnode.setLocalRotation(_rotor);
        _mnode.updateGeometricState(0, true);
    }

    protected void zoom (float delta)
    {
        Camera cam = _ctx.getCameraHandler().getCamera();
        Vector3f loc = cam.getLocation();
        loc.addLocal(cam.getDirection().mult(delta, _temp));
        cam.setLocation(loc);
        cam.update();
    }

    public void keyReleased (KeyEvent e)
    {
        // N/A
    }

    public void actionPerformed (ActionEvent event)
    {
        String command = event.getActionCommand();
        if ("load".equals(command)) {
            loadModel();

        } else if ("select_action".equals(command)) {
            selectAction((String)_actions.getSelectedItem());

        } else if ("toggle_animation".equals(command)) {
            Sprite.setAnimationActive(_mnode, _animate.isSelected());

        } else {
            log.warning("Unknown action: " + event);
        }
    }

    protected void loadModel ()
    {
        String mdir = ViewerPrefs.config.getValue(
            "last_dir", System.getProperty("user.dir"));
        if (_chooser == null) {
            _chooser = new JFileChooser(new File(mdir));
        }
        if (_chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadModel(_chooser.getSelectedFile());
        }
    }

    protected void loadModel (File path)
    {
        log.info("Loading from: " + path);
        _model = new Model(_ctx, path);
        _actions.setModel(new DefaultComboBoxModel(_model.getActions()));
        ViewerPrefs.config.setValue("last_dir", path.getParent().toString());
        selectAction(_model.getActions()[0]);
    }

    protected void selectAction (String action)
    {
        if (_binding != null) {
            _binding.detach();
            _binding = null;
        }
        _mnode.detachAllChildren();
        _binding = _model.getAnimation(action).bind(_mnode, 0, null, _bindobs);
    }

    /** Displays an error feedback message to the user. */
    protected void displayMessage (String message, boolean attention)
    {
        // TODO: add some sort of status display
        log.warning(message);
    }

    protected Model.Binding.Observer _bindobs = new Model.Binding.Observer() {
        public void wasBound (Model.Animation anim, Model.Binding binding) {
            _animate.setSelected(false);
            Sprite.setAnimationSpeed(_mnode, 20);
            Sprite.setAnimationActive(_mnode, false);
        }
        public void wasSkipped (Model.Animation anim) {
        }
    };
    
    protected BasicContext _ctx;
    protected JFileChooser _chooser;
    protected KeyDispatcher _keydisp;
    protected JComboBox _actions;
    protected JCheckBox _animate;

    protected Node _mnode;
    protected Model _model;
    protected Model.Binding _binding;

    protected float _angle;
    protected Quaternion _rotor = new Quaternion();
    protected Vector3f _up = new Vector3f(0, 0, 1);
    protected Vector3f _temp = new Vector3f();
}
