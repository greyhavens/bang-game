//
// $Id$

package com.threerings.bang.data;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import com.jmex.bui.BImage;
import com.jmex.bui.icon.ImageIcon;

import com.samskivert.util.IntListUtil;
import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Represents an article of clothing or an accessory.
 */
public class Article extends Item
{
    /**
     * Creates a comparator to sort articles.
     */
    public static Comparator<Article> createComparator (final PlayerObject user)
    {
        return new Comparator<Article>() {
            public int compare (Article a1, Article a2) {
                // sort wearable articles before unwearable ones
                if (a1.isWearable(user) != a2.isWearable(user)) {
                    return a1.isWearable(user) ? -1 : +1;
                }
                int a1idx = AvatarLogic.getSlotIndex(a1.getSlot());
                int a2idx = AvatarLogic.getSlotIndex(a2.getSlot());
                return (a1idx == a2idx) ?
                    (a2.getItemId() - a1.getItemId()) : (a1idx - a2idx);
            }
        };
    }

    /**
     * Returns the path to the icon for an article of the specified type.
     */
    public static String getIconPath (String type)
    {
        int sidx = type.lastIndexOf("/");
        String townId;
        if (sidx == -1) {
            // legacy articles
            townId = BangCodes.FRONTIER_TOWN;
        } else {
            townId = type.substring(0, sidx);
            type = type.substring(sidx+1);
        }
        return "goods/" + townId + "/articles/" + type + ".png";
    }

    /**
     * Returns a translatable name for an article of the specified type.
     */
    public static String getName (String type)
    {
        type = type.substring(type.lastIndexOf("/")+1);
        return MessageBundle.qualify(AvatarCodes.ARTICLE_MSGS, "m." + type);
    }

    /** A blank constructor used during unserialization. */
    public Article ()
    {
    }

    /**
     * Creates a new article item with the specified slot and components.
     */
    public Article (int ownerId, String slot, String name, int[] components)
    {
        this(ownerId, slot, name, components, null);
    }

    /**
     * Creates a new article item with the specified slot and components.
     */
    public Article (int ownerId, String slot, String name, int[] components, Date expires)
    {
        super(ownerId);
        _slot = slot;
        _name = name;
        _components = components;
        _expires = (expires == null ? 0 : expires.getTime());
    }

    /**
     * Returns the slot into which this article fits on an avatar.
     */
    public String getSlot ()
    {
        return _slot;
    }

    /**
     * Returns the component ids (and associated colorizations) for the various
     * avatar components that should be "applied" when wearing this article.
     */
    public int[] getComponents ()
    {
        return _components;
    }

    /**
     * Returns the id of the gang with which this article is associated, or 0 for none.
     * Gang articles can only be worn by members of the gang.
     */
    public int getArticleGangId ()
    {
        return _gangId;
    }

    /**
     * Sets the gang id.
     */
    public void setArticleGangId (int gangId)
    {
        _gangId = gangId;
    }

    /**
     * Returns a string representation of this article that we can use in config files.
     */
    public String getPrint ()
    {
        return _slot + ":" + _name + ":" + StringUtil.toString(_components, "", "");
    }

    /**
     * Returns string identifier name for this article.
     */
    public String getArticleName ()
    {
        return _name;
    }

    /**
     * Determines whether the user can wear this article at present.
     */
    public boolean isWearable (PlayerObject user)
    {
        // gang articles can only be worn by current members
        return (_gangId == 0 || _gangId == user.gangId);
    }

    /**
     * Determines whether this article has expired.
     */
    public boolean isExpired (long timestamp)
    {
        if (_expires == 0) {
            return super.isExpired(timestamp);
        }
        Date expire = new Date(_expires);
        return expire.before(new Date(timestamp));
    }

    @Override // documentation inherited
    public String getName ()
    {
        return getName(_name);
    }

    @Override // documentation inherited
    public String getTooltip (PlayerObject user)
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.article_tip");
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return getIconPath(_name);
    }

    @Override // documentation inherited
    public boolean isEquivalent (Item other)
    {
        Article oarticle;
        return super.isEquivalent(other) &&
            (oarticle = (Article)other)._name.equals(_name) &&
            Arrays.equals(oarticle._components, _components) &&
            oarticle._gangId == _gangId;
    }

    @Override // documentation inherited
    public boolean isDestroyable (PlayerObject user)
    {
        // an article can be destroyed if it is not used in any of the player's looks
        for (Look look : user.looks) {
            if (IntListUtil.contains(look.articles, _itemId)) {
                return false;
            }
        }
        return true;
    }

    @Override // documentation inherited
    public boolean canBeOwned (PlayerObject user)
    {
        return user.isMale == (getArticleName().indexOf("female") == -1);
    }

    @Override // documentation inherited
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", slot=").append(_slot);
        buf.append(", name=").append(_name);
        buf.append(", components=");
        StringUtil.toString(buf, _components);
        if (_gangId > 0) {
            buf.append(", gangId=").append(_gangId);
        }
        if (_expires != 0) {
            buf.append(", expires=").append(_expires);
        }
    }

    @Override // documentation inherited
    protected ImageIcon buildIcon (BasicContext ctx, String iconPath)
    {
        AvatarLogic al = ctx.getAvatarLogic();
        ArticleCatalog.Article aca = al.getArticleCatalog().getArticle(_name);
        if (aca == null) {
            log.warning("Article no longer exists? " + this);
            return super.buildIcon(ctx, iconPath);
        }

        Colorization[] zations = al.decodeColorizations(
            getComponents()[0], al.getColorizationClasses(aca));
        if (zations == null) {
            return super.buildIcon(ctx, iconPath);
        }

        BImage image = new BImage(
            ImageUtil.recolorImage(
                ctx.getImageCache().getBufferedImage(iconPath), zations));
        return new ImageIcon(image);
    }

    protected String _slot, _name;
    protected int[] _components;
    protected int _gangId;
    protected long _expires;
}
