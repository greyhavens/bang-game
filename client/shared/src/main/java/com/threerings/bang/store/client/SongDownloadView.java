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
import java.util.HashSet;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.io.StreamUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangClient;
import com.threerings.bang.client.PlayerService;
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

    /**
     * Returns true if the specified song is already downloaded in full.
     */
    public static boolean songDownloaded (String song)
    {
        // avoid going to the filesystem once we know we've downloaded this song
        if (_downloaded.contains(song) || new File(getSoundtrackDir(), song + ".done").exists()) {
            _downloaded.add(song);
            return true;
        }
        return false;
    }

    public SongDownloadView (BangContext ctx, String song)
    {
        this(ctx, song, false);
    }

    public SongDownloadView (BangContext ctx, String song, boolean force)
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
        add(_note = new BLabel(_msgs.get("m.download_agree"), "song_note"), GroupLayout.FIXED);
        add(_agree = new BCheckBox(_msgs.get("m.download_iagree")));

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(_actbtn = new BButton(_msgs.get("m.start_download"), this, "start"));
        buttons.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(buttons, GroupLayout.FIXED);

        // if the song is already downloaded, switch straight to copy mode
        if (!force && songDownloaded(song)) {
            setCopyMode();

        } else {
            // otherwise wire up the terms and conditions agreement button
            _actbtn.setEnabled(false);
            _agree.addListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    _actbtn.setEnabled(_agree.isSelected());
                }
            });
        }
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        _actbtn.setEnabled(false);

        if (event.getAction().equals("start")) {
            startDownload();

        } else if (event.getAction().equals("copy")) {
            _agree.setEnabled(false);
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
        // request a temporary identifier through which to download this song
        PlayerService psvc = _ctx.getClient().requireService(PlayerService.class);
        psvc.prepSongForDownload(_song, new PlayerService.ResultListener() {
            public void requestProcessed (Object result) {
                actuallyStartDownload((String)result);
            }
            public void requestFailed (String reason) {
                reportFailure(_msgs.xlate(reason));
            }
        });
    }

    protected void actuallyStartDownload (String ident)
    {
        try {
            _note.setText(_msgs.get("m.download_note"));
            _action = "download";
            _copier = new SongDownloader(
                new URL("http", DeploymentConfig.getServerHost(_ctx.getUserObject().townId),
                        "/downloads/" + ident), _song);
            _copier.start();

        } catch (Exception e) {
            log.warning("Unable to start download", "ident", ident, "error", e);
            reportFailure(_msgs.get("m.internal_error"));
        }
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
        _agree.setVisible(false);
        _actbtn.setText(_msgs.get("m.copy_to_desktop"));
        _actbtn.setAction("copy");
        _actbtn.setEnabled(true);

        // note in our cache that this song has been downloaded
        _downloaded.add(_song);
    }

    protected static File getSoundtrackDir ()
    {
        File tgtdir = new File(BangClient.localDataDir("soundtrack"));
        if (!tgtdir.exists()) {
            if (!tgtdir.mkdir()) {
                log.warning("Unable to create " + tgtdir + ". Breakage imminent.");
            }
        }
        return tgtdir;
    }

    /** Copies data from an input stream to a file. */
    protected abstract class Copier extends Thread
    {
        protected Copier (File target, File done)
        {
            _target = target;
            _done = done;
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

                // close and flush the output file
                out.close();
                out = null;

                // create a "done" file if desired
                if (_done != null) {
                    _done.createNewFile();
                }

                // be sure to report completion
                if (percent != 100) {
                    log.warning("Didnt't make it to 100%?", "expected", length, "got", totalin,
                                "pct", percent);
                    reportProgress(100);
                }

            } catch (IOException ioe) {
                log.warning("Download failed", "source", getInput(), "target", _target, ioe);
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
        protected File _target, _done;
    }

    /** Handles the downloading of a music track. */
    protected class SongDownloader extends Copier
    {
        public SongDownloader (URL source, String song) {
            super(new File(getSoundtrackDir(), song + ".mp3"),
                  new File(getSoundtrackDir(), song + ".done"));
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
            super(target, null);
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
    protected BCheckBox _agree;
    protected BButton _actbtn;

    /** A cache tracking which songs have been downloaded to avoid repeat lookups. */
    protected static HashSet<String> _downloaded = new HashSet<String>();
}
