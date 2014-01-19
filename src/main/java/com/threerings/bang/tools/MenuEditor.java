//
// $Id$

package com.threerings.bang.tools;

import java.io.File;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Displays a menu image and eases the process of creating menu regions
 * upon it.
 */
public class MenuEditor extends JFrame
{
    public MenuEditor (BufferedImage image)
    {
        super("Menu Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        _image = image;

        Liney ilabel = new Liney(new ImageIcon(image));
        ilabel.addMouseListener(_listener);
        getContentPane().add(ilabel, BorderLayout.CENTER);
        _status = new JTextField(
            "Click the left button to start a menu region, " +
            "and extend the region with right clicks.");
        getContentPane().add(_status, BorderLayout.SOUTH);
        pack();
    }

    public void startPoly (float x, float y)
    {
        _status.setText(x + ", " + (1-y));
        _poly = new Polygon();
        _poly.addPoint(Math.round(_image.getWidth() * x),
                       Math.round(_image.getHeight() * y));
        repaint();
    }

    public void extendPoly (float x, float y)
    {
        if (_poly == null) {
            return;
        }
        _status.setText(_status.getText() + ", " + x + ", " + (1-y));
        _poly.addPoint(Math.round(_image.getWidth() * x),
                       Math.round(_image.getHeight() * y));
        repaint();
    }

    public static void main (String[] args)
    {
        if (args.length == 0) {
            System.err.println("Usage: MenuEditor menu_image.png");
            System.exit(-1);
        }

        try {
            MenuEditor editor = new MenuEditor(ImageIO.read(new File(args[0])));
            editor.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    protected class Mousey extends MouseAdapter
    {
        public void mousePressed (MouseEvent event) {
            float x = (float)event.getX() / _image.getWidth();
            float y = (float)event.getY() / _image.getHeight();
            if (event.getButton() == MouseEvent.BUTTON1) {
                startPoly(x, y);
            } else {
                extendPoly(x, y);
            }
        }
    }

    protected class Liney extends JLabel
    {
        public Liney (ImageIcon icon)
        {
            super(icon);
        }

        public void paintComponent (Graphics g)
        {
            super.paintComponent(g);
            if (_poly != null) {
                g.setColor(Color.blue);
                ((Graphics2D)g).draw(_poly);
            }
        }
    }

    protected BufferedImage _image;
    protected JTextField _status;
    protected Polygon _poly;
    protected Mousey _listener = new Mousey();
}
