//
// $Id$

package com.threerings.bang;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;

import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * A simple class for testing the damage arc clipping routines.
 */
public class ArcTest
{
    public static void main (String[] args)
    {
        if (args.length == 0) {
            System.err.println("Usage: ArcTest damage (0 - 100)");
            System.exit(-1);
        }
        int damage = 0;
        try {
            damage = Integer.parseInt(args[0]);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        try {
            BufferedImage empty = ImageIO.read(
                new FileInputStream(PATH + "health_meter_empty.png"));
            BufferedImage full = ImageIO.read(
                new FileInputStream(PATH + "health_meter_full.png"));
            int width = empty.getWidth(), height = empty.getHeight();
            BufferedImage comp = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);

            Graphics2D gfx = (Graphics2D)comp.getGraphics();
            try {
                gfx.drawImage(empty, 0, 0, null);
                float percent = (100 - damage) / 100f;
                float extent = percent * (90 - 2*ARC_INSETS);
                gfx.setColor(Color.blue);
                gfx.drawLine(width/2, 0, width/2, height);
                gfx.drawLine(0, height/2, width, height/2);
                gfx.drawLine(0, height, width, 0);
                // expand the width and height a smidge to avoid funny
                // business around the edges
                Arc2D.Float arc = new Arc2D.Float(
                    -5*width/4, -height/4, 10*width/4, 10*height/4,
                    ARC_INSETS, extent, Arc2D.PIE);
//                 gfx.fill(arc);
                gfx.setClip(arc);
                gfx.drawImage(full, 0, 0, null);

            } finally {
                gfx.dispose();
            }

            JFrame frame = new JFrame("Arc Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new JLabel(new ImageIcon(comp)));
            frame.pack();
            frame.setVisible(true);

        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        }
    }

    protected static final String PATH = "rsrc/textures/ustatus/";

    /** Defines the amount by which the damage arc image is inset from a
     * full quarter circle (on each side): 8 degrees. */
    protected static final float ARC_INSETS = 7;
}
