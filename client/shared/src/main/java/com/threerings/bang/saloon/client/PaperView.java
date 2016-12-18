//
// $Id$

package com.threerings.bang.saloon.client;

import java.net.URL;

import java.util.HashMap;

import com.badlogic.gdx.Input.Keys;

import com.jme.input.KeyInput;
import com.jmex.bui.BContainer;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BToggleButton;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.ResultListener;
import com.threerings.util.MessageBundle;

import com.threerings.bang.chat.client.PlaceChatView;
import com.threerings.bang.client.bui.BangHTMLView;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.CachedDocument;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;

import static com.threerings.bang.Log.log;

/**
 * Contains the various Saloon information displays: news, friendly folks, top scores.
 */
public class PaperView extends BContainer
{
    public PaperView (BangContext ctx)
    {
        super(GroupLayout.makeVStretch());
        ((GroupLayout)getLayoutManager()).setGap(8);
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

        // this container will display the friendly folks UI
        _folks = new BContainer(GroupLayout.makeVStretch());
        BangHTMLView help = new BangHTMLView();
        help.setContents(_msgs.get("m.saloon_help"));
        help.setStyleClass("news_help");
        BScrollPane helpscroll = new BScrollPane(help);
        helpscroll.setStyleClass("news_scroll");
        helpscroll.setShowScrollbarAlways(false);
        _folks.add(_chat = new PlaceChatView(_ctx, _msgs.get("m.saloon_info"), helpscroll) {
            protected boolean displayTabs () {
                displayPage(1);
                return true;
            }
        });

        // when the news is loaded; it will display the news tab, but we need to hand set the
        // proper navigation button to selected
        _navi[0].setSelected(true);

        // any time after the first that we enter the saloon during a session, start on the
        // friendly folks page
        if (_shownNews) {
            displayPage(1);
        } else {
            // load and display the news page
            refreshNews(false);
            _shownNews = true;
        }
    }

    /**
     * Provides us with a reference to our saloon object.
     */
    public void init (SaloonObject salobj)
    {
        _salobj = salobj;
        // create the folkview now that we have our saloon object
        _folks.add(0, new FolkView(_ctx, _salobj, true, false), GroupLayout.FIXED);
    }

    /**
     * Called when we're going away.
     */
    public void shutdown ()
    {
        // unregister our chat display
        _chat.shutdown();
    }

    protected BToggleButton createMastheadButton (String id)
    {
        BToggleButton button = new BToggleButton("", id);
        button.addListener(_navigator);
        button.setStyleClass("news_" + id);
        return button;
    }

    protected void displayPage (int pageNo)
    {
        if (_pageNo == pageNo) {
            _navi[_pageNo].setSelected(true);
            return;
        }

        // configure our navigation buttons properly
        for (int ii = 0; ii < _navi.length; ii++) {
            _navi[ii].setSelected(pageNo == ii);
        }

        switch (_pageNo = pageNo) {
        case 0:
            CachedDocument news = _news.get(_ctx.getUserObject().townId);
            if (news == null) {
                refreshNews(false);
            } else if (KeyInput.get().isKeyDown(Keys.CONTROL_LEFT)) {
                refreshNews(true);
            } else {
                setContents(news.getDocument());
            }
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
            _contents = new BangHTMLView();
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
        _contents.setContents(DeploymentConfig.getDocBaseURL(), contents);
    }

    protected void refreshNews (boolean force)
    {
        String townId = _ctx.getUserObject().townId;
        CachedDocument news = _news.get(townId);
        if (news == null) {
            URL base = DeploymentConfig.getDocBaseURL();
            String npath = townId + NEWS_URL;
            try {
                URL nurl = new URL(base, npath);
                news = new CachedDocument(nurl, NEWS_REFRESH_INTERVAL);
                _news.put(townId, news);
            } catch (Exception e) {
                log.warning("Failed to create news URL", "base", base, "path", npath, e);
                return;
            }
        }

        if (!news.refreshDocument(force, _newsup)) {
            setContents(news.getDocument());
        }
    }

    /** Used to asynchronously update the news. */
    protected ResultListener<String> _newsup = new ResultListener<String>() {
        public void requestCompleted (String result) {
            updateNews(result);
        }
        public void requestFailed (Exception cause) {
            log.warning("Failed to load the news.", cause);
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

    /** Listens for navigation button presses. */
    protected ActionListener _navigator = new ActionListener() {
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

    protected int _pageNo;
    protected BContainer _contcont;
    protected BToggleButton[] _navi;

    protected BangHTMLView _contents;
    protected BScrollPane _contscroll;

    protected BContainer _folks;
    protected PlaceChatView _chat;

    protected TopScoreView _topscore;

    protected static HashMap<String,CachedDocument> _news = new HashMap<String,CachedDocument>();
    protected static boolean _shownNews;

    protected static final long NEWS_REFRESH_INTERVAL = 60 * 60 * 1000L;
    protected static final String NEWS_URL = "_news_incl.html";
}
