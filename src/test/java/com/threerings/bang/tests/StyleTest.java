//
// $Id$

package com.threerings.bang.tests;

import java.util.logging.Level;

import com.jme.renderer.ColorRGBA;
import com.jme.util.LoggingSystem;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BScrollBar;
import com.jmex.bui.BTextArea;
import com.jmex.bui.BTextField;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

/**
 * A simple app for testing stylesheet stuff.
 */
public class StyleTest extends TestApp
{
    public static void main (String[] args)
    {
        LoggingSystem.getLogger().setLevel(Level.WARNING);
        StyleTest test = new StyleTest();
        if (test.init()) {
            test.initTest();
            test.run();
        } else {
            System.exit(-1);
        }
    }

    protected void createInterface (BWindow window)
    {
        BContainer cont = new BContainer(new BorderLayout());
        cont.add(_text = new BTextArea(), BorderLayout.CENTER);
        cont.add(_input = new BTextField(), BorderLayout.SOUTH);
        cont.add(new BScrollBar(BScrollBar.VERTICAL, _text.getScrollModel()),
                 BorderLayout.EAST);
        cont.add(new BScrollBar(BScrollBar.HORIZONTAL, 0, 25, 50, 100),
                 BorderLayout.NORTH);
        _input.addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                String input = _input.getText();
                if (input != null && !input.equals("")) {
                    _text.appendText("You said: ", ColorRGBA.red);
                    _text.appendText(_input.getText() + "\n");
                    _input.setText("");
                }
            }
        });
        cont.setPreferredSize(new Dimension(400, 250));
        window.add(cont);

        cont = new BContainer(new BorderLayout());
        cont.add(new BButton("East"), BorderLayout.EAST);
        cont.add(new BButton("West"), BorderLayout.WEST);
        window.add(cont, GroupLayout.FIXED);

        _ctx.getRenderer().setBackgroundColor(ColorRGBA.gray);
    }

    protected BTextArea _text;
    protected BTextField _input;
}
