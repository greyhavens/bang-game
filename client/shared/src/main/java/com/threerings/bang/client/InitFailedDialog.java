//
// $Id$

package com.threerings.bang.client;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import com.samskivert.swing.MultiLineLabel;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.util.ResultListener;

// import com.threerings.hemiptera.data.Report;
// import com.threerings.hemiptera.util.SendReportUtil;

import com.threerings.util.BrowserUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.DeploymentConfig;

import static com.threerings.bang.Log.log;

/**
 * Displays some useful help when we fail entirely to initialize the game.
 */
public class InitFailedDialog extends JFrame
{
    public InitFailedDialog (MessageManager msgmgr, final Throwable error)
    {
        super(msgmgr.getBundle(BangCodes.BANG_MSGS).get("m.init_failed_title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // set up our contents
        MessageBundle msgs = msgmgr.getBundle(BangCodes.BANG_MSGS);
        JPanel content = (JPanel)getContentPane();
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.setLayout(new VGroupLayout(
                              VGroupLayout.NONE, VGroupLayout.CONSTRAIN,
                              15, VGroupLayout.TOP));

        // if LWJGL failed to find a valid pixel format they probably have out
        // of date OpenGL drivers, so let's point them toward a page where they
        // can install new ones
        boolean canReport = true;
        String errmsg = (error.getMessage() == null) ? "" : error.getMessage();
        boolean drivers = (errmsg.indexOf("valid pixel format") != -1),
            mode = (errmsg.indexOf("Cannot find display mode") != -1);
        if (drivers || mode) {
            canReport = false;
            content.add(createLabel(msgs.get("m.init_failed_" + (drivers ? "drivers" : "mode"))));
            final String driverPage = msgs.get("url.init_failed_drivers");
            AbstractAction showDriverPage = new AbstractAction() {
                public void actionPerformed (ActionEvent event) {
                    showURL(driverPage);
                }
            };
            showDriverPage.putValue(AbstractAction.NAME, driverPage);
            content.add(new JButton(showDriverPage));
        } else {
            content.add(createLabel(msgs.get("m.init_failed_header")));
            content.add(new MultiLineLabel(errmsg));
        }

        // add buttons for going to the forums
        content.add(createLabel(msgs.get("m.init_failed_forums")));
        final String forumPage = msgs.get("url.init_failed_forums");
        AbstractAction showForumPage = new AbstractAction() {
            public void actionPerformed (ActionEvent event) {
                showURL(forumPage);
            }
        };
        showForumPage.putValue(AbstractAction.NAME, forumPage);
        content.add(new JButton(showForumPage));

        // create buttons for submitting a report or not
        JPanel row = VGroupLayout.makeButtonBox(VGroupLayout.CENTER);
        // if (canReport) {
        //     content.add(createLabel(msgs.get("m.init_failed_report")));
        //     AbstractAction exitReport = new AbstractAction() {
        //         public void actionPerformed (ActionEvent event) {
        //             submitReport(error);
        //             System.exit(-1);
        //         }
        //     };
        //     exitReport.putValue(
        //         AbstractAction.NAME, msgs.get("m.report_and_exit"));
        //     row.add(new JButton(exitReport));
        // } else {
            content.add(createLabel(msgs.get("m.init_failed_sorry")));
        // }

        AbstractAction exitNoReport = new AbstractAction() {
            public void actionPerformed (ActionEvent event) {
                System.exit(-1);
            }
        };
        String label = canReport ? "m.exit_no_report" : "m.exit_no_can_do";
        exitNoReport.putValue(AbstractAction.NAME, msgs.get(label));
        row.add(new JButton(exitNoReport));
        content.add(row);
    }

    protected MultiLineLabel createLabel (String text)
    {
        MultiLineLabel lbl = new MultiLineLabel(
            text, MultiLineLabel.LEFT, MultiLineLabel.HORIZONTAL, 600);
        lbl.setFont(LABEL_FONT);
        return lbl;
    }

    protected void showURL (String path)
    {
        try {
            BrowserUtil.browseURL(new URL(path), new ResultListener.NOOP<Void>());
        } catch (Exception e) {
            log.warning("Failed to create URL from path '" + path + "': " + e);
        }
    }

    // protected void submitReport (Throwable error)
    // {
    //     // fill in a bug report
    //     Report report = new Report();
    //     report.submitter = "[initbug]";
    //     report.summary = "Initialization failed: " + error;
    //     report.version = String.valueOf(DeploymentConfig.getVersion());

    //     try {
    //         report.setAttribute("Driver", Display.getAdapter());
    //         report.setAttribute("GL Display Mode", "" + Display.getDisplayMode());
    //         if (Display.isCreated()) {
    //             // These GL calls can only be made with a valid context
    //             report.setAttribute("GL Version", GL11.glGetString(GL11.GL_VERSION));
    //             report.setAttribute("GL Vendor", GL11.glGetString(GL11.GL_VENDOR));
    //             report.setAttribute("GL Renderer", GL11.glGetString(GL11.GL_RENDERER));
    //             report.setAttribute("GL Extensions", GL11.glGetString(GL11.GL_EXTENSIONS));
    //         }
    //     } catch (Throwable t) {
    //         log.warning("Failed to get GL info.", t);
    //     }

    //     URL submitURL = DeploymentConfig.getBugSubmitURL();
    //     if (submitURL == null) {
    //         log.warning("Unable to submit bug report, no submit URL.");
    //         return;
    //     }

    //     // and send it along with our debug logs
    //     try {
    //         log.info("Submitting init failed bug report.");
    //         String[] files = { BangClient.localDataDir("bang.log")};
    //         SendReportUtil.submitReport(submitURL, report, files);
    //     } catch (Exception e) {
    //         log.warning("Failed to submit bug report.", e);
    //     }
    // }

    protected static Font LABEL_FONT = new Font("Dialog", Font.BOLD, 16);
}
