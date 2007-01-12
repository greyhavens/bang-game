//
// $Id$

package com.threerings.bang.store.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.io.StreamUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangClient;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.store.data.StoreCodes;

import static com.threerings.bang.Log.log;

/**
 * Displays a dialog that manages the downloading of a music track.
 */
public class SongDownloadView extends BDecoratedWindow
    implements ActionListener
{
    public static final int PREF_WIDTH = 500;

    public SongDownloadView (BangContext ctx, String song)
    {
        super(ctx.getStyleSheet(), ctx.xlate(StoreCodes.STORE_MSGS, "m.download_title"));
        ((GroupLayout)getLayoutManager()).setGap(15);
        setStyleClass("dialog_window");
        setModal(true);

        _ctx = ctx;
        _song = song;
        _msgs = ctx.getMessageManager().getBundle(StoreCodes.STORE_MSGS);

        add(_main = new BLabel(_msgs.get("m.download_info")), GroupLayout.FIXED);
        add(new BLabel(ctx.xlate(BangCodes.GOODS_MSGS, "m.song_" + song)), GroupLayout.FIXED);
        add(_note = new BLabel(_msgs.get("m.download_tip"), "song_note"), GroupLayout.FIXED);

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(_actbtn = new BButton(_msgs.get("m.start_download"), this, "start"));
        buttons.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(buttons, GroupLayout.FIXED);

        // TODO: check to see if the music file exists, seems to be the right size, etc. to avoid
        // pointless redownloading; add "copy to desktop" option in that case
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        _actbtn.setEnabled(false);

        if (event.getAction().equals("start")) {
            startDownload();

        } else if (event.getAction().equals("copy")) {
            startCopy();

        } else if (event.getAction().equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }

    @Override // from BComponent
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // stop our downloader or copier thread if it is still going
        if (_copier != null) {
            _copier.shutdown();
        }
    }

    protected void startDownload ()
    {
        // TODO: get the source by making a server request; the server will confirm that we own the
        // song and create a temporarily available location from which to download it

        String file = _song + ".mp3";
        URL source;
        try {
            source = new URL(DeploymentConfig.getDocBaseURL(), "/soundtrack/" + file);
        } catch (Exception e) {
            log.warning("Unable to create download URL [song=" + _song + ", error=" + e + "].");
            reportFailure(_msgs.get("m.internal_error"));
            return;
        }

        // start the download thread
        _action = "download";
        _copier = new SongDownloader(source, new File(getSoundtrackDir(), file));
        _copier.start();
    }

    protected void startCopy ()
    {
        File target = new File(System.getProperty("user.home") + File.separator +
                               "Desktop" + File.separator +
                               _ctx.xlate(BangCodes.GOODS_MSGS, "m.song_" + _song) + ".mp3");
        _action = "copy";
        _copier = new DesktopCopier(new File(getSoundtrackDir(), _song + ".mp3"), target);
        _copier.start();
    }

    protected File getSoundtrackDir ()
    {
        File tgtdir = new File(BangClient.localDataDir("soundtrack"));
        if (!tgtdir.exists()) {
            if (!tgtdir.mkdir()) {
                log.warning("Unable to create " + tgtdir + ". Breakage imminent.");
            }
        }
        return tgtdir;
    }

    protected void updateProgress (int percent)
    {
        if (percent < 100) {
            _main.setText(_msgs.get("m." + _action + "ing", String.valueOf(percent)));
        } else if (_action.equals("download")) {
            setCopyMode();
        } else {
            _main.setText(_msgs.get("m.copy_complete"));
            _note.setText(_msgs.get("m.copy_complete_note"));
            pack(PREF_WIDTH, -1);
            center();
        }
    }

    protected void reportFailure (String errmsg)
    {
        _note.setText(_msgs.get("m." + _action + "_failed", errmsg));
        pack(PREF_WIDTH, -1);
        center();
    }

    protected void setCopyMode ()
    {
        _main.setText(_msgs.get("m.download_complete"));
        _note.setText(_msgs.get("m.download_copy"));
        _actbtn.setText(_msgs.get("m.copy_to_desktop"));
        _actbtn.setAction("copy");
        _actbtn.setEnabled(true);
    }

    /** Copies data from an input stream to a file. */
    protected abstract class Copier extends Thread
    {
        protected Copier (File target)
        {
            _target = target;
        }

        public synchronized void shutdown ()
        {
            _running = false;
        }

        public void run ()
        {
            reportProgress(0);

            FileOutputStream out = null;
            InputStream in = null;

            try {
                out = new FileOutputStream(_target);
                prepareInput();
                int length = getInputLength(), percent = 0, totalin = 0;
                in = getInputStream();
                byte[] buffer = new byte[4096];

                // TODO: support download resuming

                while (isRunning()) {
                    // read a bufferful and write it to our target file
                    int read = in.read(buffer);
                    if (read == -1) {
                        break;
                    }
                    out.write(buffer, 0, read);

                    // update our progress metrics
                    totalin += read;
                    int npercent = (totalin * 100) / length;
                    if (npercent != percent) {
                        reportProgress(percent = npercent);
                    }
                }

                // be sure to report completion
                if (percent != 100) {
                    log.warning("Didnt't make it to 100%? [expected=" + length +
                                ", got=" + totalin + ", pct=" + percent + "].");
                    reportProgress(100);
                }

            } catch (IOException ioe) {
                log.log(Level.WARNING, "Download failed [source=" + getInput() +
                        ", target=" + _target + "].", ioe);
                reportError(ioe.getMessage());
            }

            StreamUtil.close(in);
            StreamUtil.close(out);
        }

        protected synchronized boolean isRunning ()
        {
            return _running;
        }

        protected void reportProgress (final int percent)
        {
            _ctx.getApp().postRunnable(new Runnable() {
                public void run () {
                    updateProgress(percent);
                }
            });
        }

        protected void reportError (final String errmsg)
        {
            _ctx.getApp().postRunnable(new Runnable() {
                public void run () {
                    reportFailure(errmsg);
                }
            });
        }

        protected abstract void prepareInput () throws IOException;
        protected abstract Object getInput(); // for logging
        protected abstract InputStream getInputStream () throws IOException ;
        protected abstract int getInputLength ();

        protected boolean _running = true;
        protected File _target;
    }

    /** Handles the downloading of a music track. */
    protected class SongDownloader extends Copier
    {
        public SongDownloader (URL source, File target) {
            super(target);
            _source = source;
        }

        protected void prepareInput () throws IOException {
            _uconn = _source.openConnection();
        }
        protected Object getInput () {
            return _source;
        }
        protected int getInputLength () {
            return _uconn.getContentLength();
        }
        protected InputStream getInputStream () throws IOException  {
            return _uconn.getInputStream();
        }

        protected URL _source;
        protected URLConnection _uconn;
    }

    /** Handles copying songs to the desktop. */
    protected class DesktopCopier extends Copier
    {
        public DesktopCopier (File source, File target) {
            super(target);
            _source = source;
        }

        protected void prepareInput () throws IOException {
        }
        protected Object getInput () {
            return _source;
        }
        protected int getInputLength () {
            return (int)_source.length();
        }
        protected InputStream getInputStream () throws IOException {
            return new FileInputStream(_source);
        }

        protected File _source;
    }

    protected BangContext _ctx;
    protected String _song;
    protected MessageBundle _msgs;
    protected Copier _copier;
    protected String _action;

    protected BLabel _main, _note;
    protected BButton _actbtn;
}
