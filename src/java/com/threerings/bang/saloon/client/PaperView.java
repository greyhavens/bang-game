//
// $Id$

package com.threerings.bang.saloon.client;

import java.io.StringReader;
import java.net.URL;
import javax.swing.text.html.HTMLDocument;

import java.util.logging.Level;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BToggleButton;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.text.HTMLView;

import com.samskivert.util.ResultListener;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.CachedDocument;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;
import com.threerings.bang.saloon.data.TopRankedList;

import static com.threerings.bang.Log.log;

/**
 * Contains the various Saloon information displays: news, friendly folks, top
 * scores.
 */
public class PaperView extends BContainer
{
    public PaperView (BangContext ctx)
    {
        super(GroupLayout.makeVStretch());
        ((GroupLayout)getLayoutManager()).setGap(12);
        setStyleClass("news_view");

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)buttons.getLayoutManager()).setGap(0);
        _navi = new BToggleButton[3];
        buttons.add(_navi[0] = createMastheadButton("news"));
        buttons.add(_navi[1] = createMastheadButton("folks"));
        buttons.add(_navi[2] = createMastheadButton("top_scores"));
        add(buttons, GroupLayout.FIXED);

        add(_contcont = new BContainer(new BorderLayout()));

        // read in the main news page
        refreshNews(false);

        // when the news is loaded; it will display the news tab, but we need
        // to hand set the proper navigation button to selected
        _navi[0].setSelected(true);
    }

    /**
     * Provides us with a reference to our saloon object.
     */
    public void init (SaloonObject salobj)
    {
        _salobj = salobj;
        // create the folkview as soon as we're ready
        _folks = new FolkView(_ctx, this, _salobj);
    }

    /**
     * Called when the FolkView chat interface demands focus.
     */
    public void folkChatAlert ()
    {
        displayPage(1);
    }

    protected BToggleButton createMastheadButton (String id)
    {
        BToggleButton button = new BToggleButton("", id);
        button.addListener(_listener);
        button.setStyleClass("news_" + id);
        return button;
    }

    protected void displayPage (int pageNo)
    {
        if (_pageNo == pageNo) {
            return;
        }

        // configure our navigation buttons properly
        for (int ii = 0; ii < _navi.length; ii++) {
            _navi[ii].setSelected(pageNo == ii);
        }

        switch (_pageNo = pageNo) {
        case 0:
            setContents(_news.getDocument());
            break;

        case 1:
            if (_folks.getParent() == null) {
                _contcont.removeAll();
                _contcont.add(_folks, BorderLayout.CENTER);
            }
            break;

        case 2:
            if (_topscore == null) {
                _topscore = new TopScoreView(_ctx, _salobj);
            }
            if (_topscore.getParent() == null) {
                _contcont.removeAll();
                _contcont.add(_topscore, BorderLayout.CENTER);
            }
            break;
        }
    }

    protected void setContents (String contents)
    {
        if (_contents == null) {
            _contents = new HTMLView();
            _contents.setStyleClass("news_contents");
        }
        if (_contscroll == null) {
            _contscroll = new BScrollPane(_contents);
            _contscroll.setShowScrollbarAlways(false);
        }
        if (_contscroll.getParent() == null) {
            _contcont.removeAll();
            _contcont.add(_contscroll, BorderLayout.CENTER);
        }

        if (contents == null) {
            contents = _msgs.get(SaloonCodes.INTERNAL_ERROR);
        }
        HTMLDocument doc = new HTMLDocument(BangUI.css);
        doc.setBase(DeploymentConfig.getDocBaseURL());
        try {
            _contents.getEditorKit().read(new StringReader(contents), doc, 0);
            _contents.setContents(doc);
        } catch (Throwable t) {
            log.log(Level.WARNING, "Failed to parse HTML " +
                    "[contents=" + contents + "].", t);
        }
    }

    protected void refreshNews (boolean force)
    {
        if (_news == null) {
            URL base = DeploymentConfig.getDocBaseURL();
            String npath = _ctx.getUserObject().townId + NEWS_URL;
            try {
                URL news = new URL(base, npath);
                _news = new CachedDocument(news, NEWS_REFRESH_INTERVAL);
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to create news URL " +
                    "[base=" + base + ", path=" + npath + "].", e);
                return;
            }
        }
        if (!_news.refreshDocument(force, _newsup)) {
            setContents(_news.getDocument());
        }
    }

    protected ResultListener<String> _newsup = new ResultListener<String>() {
        public void requestCompleted (String result) {
            updateNews(result);
        }
        public void requestFailed (Exception cause) {
            log.log(Level.WARNING, "Failed to load the news.", cause);
            updateNews("m.news_load_failed");
        }
        protected void updateNews (final String text) {
            _ctx.getApp().postRunnable(new Runnable() {
                public void run () {
                    if (text.startsWith("m.")) {
                        setContents(_msgs.xlate(text));
                    } else {
                        setContents(text);
                    }
                }
            });
        }
    };

    protected ActionListener _listener = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            String action = event.getAction();
            if (action.equals("news")) {
                displayPage(0);
            } else if (action.equals("folks")) {
                displayPage(1);
            } else if (action.equals("top_scores")) {
                displayPage(2);
            }
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected SaloonObject _salobj;

    protected BContainer _contcont;
    protected BToggleButton[] _navi;
    protected TopScoreView _topscore;
    protected FolkView _folks;
    protected HTMLView _contents;
    protected BScrollPane _contscroll;

    protected int _pageNo;

    protected static CachedDocument _news;

    protected static final long NEWS_REFRESH_INTERVAL = 60 * 60 * 1000L;
    protected static final String NEWS_URL = "_news_incl.html";
}
