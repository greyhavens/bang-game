//
// $Id$

package com.threerings.bang.game.client;

import com.jme.renderer.Renderer;

import com.jmex.bui.BImage;
import com.jmex.bui.util.Insets;
import com.jmex.bui.background.ImageBackground;

/**
 * A specialized ImageBackground for talk bubbles.
 */
public class TutorialTalkBackground extends ImageBackground
{
    public TutorialTalkBackground (BImage image)
    {
        super(FRAME_XY, image, new Insets(45, 19, 21, 19));
    }

    @Override // documentation inherited
    public int getMinimumHeight ()
    {
        return _image.getHeight();
    }

    public void setTailPosition (int pos)
    {
        _tailPos = pos;
    }

    @Override // documentation inherited
    protected void renderFramed (
        Renderer renderer, int x, int y, int width, int height, float alpha)
    {
        // render each of our image sections appropriately
        int twidth = _image.getWidth(), theight = _image.getHeight();

        // draw the corners
        _image.render(renderer, 0, 0, _frame.left, _frame.bottom, x, y, alpha);
        _image.render(renderer, twidth-_frame.right, 0, _frame.right, _frame.bottom,
                      x+width-_frame.right, y, alpha);
        _image.render(renderer, 0, theight-_frame.top, _frame.left, _frame.top,
                      x, y+height-_frame.top, alpha);
        _image.render(renderer, twidth-_frame.right, theight-_frame.top, _frame.right, _frame.top,
                      x+width-_frame.right, y+height-_frame.top, alpha);

        // draw the "gaps"
        int wmiddle = twidth - _frame.getHorizontal(), hmiddle = theight - _frame.getVertical();
        int gwmiddle = width - _frame.getHorizontal(), ghmiddle = height - _frame.getVertical();
        _image.render(renderer, _frame.left, 0, wmiddle, _frame.bottom,
                      x+_frame.left, y, gwmiddle, _frame.bottom, alpha);
        _image.render(renderer, _frame.left, theight-_frame.top, wmiddle, _frame.top, x+_frame.left,
                      y+height-_frame.top, gwmiddle, _frame.top, alpha);
        _image.render(renderer, twidth-_frame.right, _frame.bottom, _frame.right, hmiddle,
                      x+width-_frame.right, y+_frame.bottom, _frame.right, ghmiddle, alpha);

        // draw the tail side
        int tpos = Math.min(_tailPos - _frame.bottom, ghmiddle - 1);
        int tailheight = hmiddle - 2;
        _image.render(renderer, 0, _frame.bottom, _frame.left, 1, x, y+_frame.bottom,
                      _frame.left, tpos - tailheight, alpha);
        _image.render(renderer, 0, _frame.bottom + 1, _frame.left, tailheight,
                x, y + _frame.bottom + tpos - tailheight, _frame.left, tailheight, alpha);
        _image.render(renderer, 0, hmiddle + _frame.bottom - 1, _frame.left, 1,
                x, y + _frame.bottom + tpos, _frame.left, ghmiddle - tpos, alpha);

        // draw the center
        _image.render(renderer, _frame.left, _frame.bottom, wmiddle, hmiddle,
                      x+_frame.left, y+_frame.bottom, gwmiddle, ghmiddle, alpha);
    }

    protected int _tailPos;
}
