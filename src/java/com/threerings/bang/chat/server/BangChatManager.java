//
// $Id$

package com.threerings.bang.chat.server;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Lifecycle;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;
import com.threerings.presents.data.ClientObject;
import com.threerings.crowd.chat.server.SpeakUtil;


import com.threerings.bang.data.BangCodes;
import com.threerings.bang.server.ServerConfig;

import static com.threerings.bang.Log.log;

/**
 * Handles Bang chat stuffs.
 */
@Singleton
public class BangChatManager
    implements Lifecycle.InitComponent
{
    @Inject public BangChatManager (Lifecycle cycle)
    {
        cycle.addComponent(this);
    }

    // from Lifecycle.InitComponent
    public void init ()
    {
        // read in our whitelist dictionaries (if any)
        for (String dict : ServerConfig.config.getValue("whitelist_dicts", new String[0])) {
            try {
                InputStream din = getClass().getClassLoader().getResourceAsStream(dict);
                if (din == null) {
                    log.warning("Missing whitelist dictionary", "path", dict);
                    continue;
                }
                BufferedReader bin = new BufferedReader(new InputStreamReader(din));
                String word;
                while ((word = bin.readLine()) != null) {
                    _whitelist.add(word);
                }
            } catch (Exception e) {
                log.warning("Failed to read whitelist dictionary", "dict", dict, e);
            }
        }

        // if we don't have a minimum number of words, our dictionary loading failed, so don't
        // enable the whitelist
        if (_whitelist.size() > MIN_WHITELIST_SIZE) {
            log.info("Chat system using whitelist", "size", _whitelist.size());
        } else {
            _whitelist.clear();
        }
    }

    /**
     * Checks the supplied message against the chat whitelist.
     *
     * @return true if the chat passes, false if it fails. In the event of failure a feedback
     * message will be sent to the client explaining the failure.
     */
    public boolean validateChat (ClientObject speaker, String message)
    {
        if (_whitelist.isEmpty()) {
            return true;
        }

        // hackasaur! do some english word sanitizing
        String norm = message.toLowerCase();
        norm = norm.replace("'s", "");
        norm = norm.replace("'ll", "");
        norm = norm.replace("'ve", "");
        norm = norm.replace("'d", "");
        norm = norm.replace("'re", "");
        norm = norm.replace("'m", "");
        norm = norm.replace("n't", "");

        // now strip out any remaining non-word characters and split up the rest
        List<String> invalid = null;
        for (String word : norm.replaceAll("[^\\w ]", "").split(" ")) {
            if (!_whitelist.contains(word)) {
                if (invalid == null) {
                    invalid = Lists.newArrayList();
                }
                invalid.add(word);
            }
        }
        if (invalid == null) {
            return true;
        }

        log.info("Nixing chat", "who", speaker.who(), "message", message, "invalid", invalid);

        SpeakUtil.sendInfo(
            speaker, BangCodes.CHAT_MSGS,
            MessageBundle.tcompose("e.not_in_whitelist", StringUtil.toString(invalid, "", "")));
        return false;
    }

    /** Our whitelist chat dictionary. */
    protected Set<String> _whitelist = Sets.newTreeSet();

    protected static final int MIN_WHITELIST_SIZE = 50000;
}
