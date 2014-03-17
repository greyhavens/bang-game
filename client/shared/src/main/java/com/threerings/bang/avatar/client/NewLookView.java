//
// $Id$

package com.threerings.bang.avatar.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.jme.renderer.Renderer;
import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.StringUtil;

import com.threerings.cast.CharacterComponent;
import com.threerings.cast.ComponentRepository;
import com.threerings.cast.NoSuchComponentException;
import com.threerings.media.image.Colorization;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Article;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BangUtil;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.data.BarberObject;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.data.LookConfig;
import com.threerings.bang.avatar.util.AspectCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

/**
 * Allows the configuration of a new avatar look.
 */
public class NewLookView extends BContainer
    implements ActionListener
{
    public NewLookView (BangContext ctx, StatusLabel status)
    {
        super(new AbsoluteLayout());

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BarberCodes.BARBER_MSGS);
        _status = status;

        add(_avatar = new AvatarView(ctx, 2, true, false), new Point(707, 171));

        boolean isMale = _ctx.getUserObject().isMale;
        _gender = isMale ? "male/" : "female/";

        BImage icon = _ctx.loadImage("ui/barber/caption_name.png");
        add(new BLabel(new ImageIcon(icon)), new Point(726, 135));
        add(_name = new BTextField("", BarberCodes.MAX_LOOK_NAME_LENGTH),
                new Rectangle(786, 135, 164, 29));

        BContainer cost = GroupLayout.makeHBox(GroupLayout.LEFT);
        cost.add(new BLabel(_msgs.get("m.look_price")));
        cost.add(_cost = new MoneyLabel(ctx));
        _cost.setMoney(0, 0, false);
        add(cost, new Point(704, 51));

        add(_buy = new BButton(_msgs.get("m.buy"), this, "buy"),
            new Point(870, 43));
        _buy.setStyleClass("big_button");

        _palette = new IconPalette(null, 4, 3, ChoiceIcon.ICON_SIZE, 1);
        _palette.setPaintBackground(true);
        _palette.setAllowsEmptySelection(false);
        add(_palette, new Rectangle(139, 5, ChoiceIcon.ICON_SIZE.width*4,
                                    ChoiceIcon.ICON_SIZE.height*3+27));

        // create handlers for each aspect
        for (int ii = 0; ii < AvatarLogic.ASPECTS.length; ii++) {
            if (isMale || !AvatarLogic.ASPECTS[ii].maleOnly) {
                AvatarLogic.Aspect aspect = AvatarLogic.ASPECTS[ii];
                _handlers.put(aspect.name, new AspectHandler(aspect));
            }
        }

        // create our color selectors
        add(_collabel = new BLabel("", "colorsel_label"),
            new Rectangle(487, 510, 145, 35));

        ColorSelector s, h;
        _colsels.put("head", s = new ColorSelector(_ctx, AvatarLogic.SKIN, _handlers.get("head")));
        _colsels.put("hair", h = new ColorSelector(_ctx, AvatarLogic.HAIR, _handlers.get("hair")));
        _colsels.put("eyes", new ColorSelector(_ctx, AvatarLogic.EYES, _handlers.get("eyes")));
// TODO: give women a colorization for lips?
//         _colsels.put("mouth", new ColorSelector(_ctx, AvatarLogic.MAKEUP));

        // configure our color selections based on the current look
        Look look = _ctx.getUserObject().getLook(Look.Pose.DEFAULT);
        if (look != null) {
            s.setSelectedColorId(AvatarLogic.decodeSkin(look.aspects[0]));
            h.setSelectedColorId(AvatarLogic.decodeHair(look.aspects[0]));
            // TODO: search through the look for an eye component and extract
            // eye color? what a PITA
        }

        // create our tab display which will trigger the avatar display
        ArrayList<String> tabs = new ArrayList<String>();
        for (int ii = 0; ii < AvatarLogic.ASPECTS.length; ii++) {
            if (!AvatarLogic.ASPECTS[ii].maleOnly || isMale) {
                tabs.add(AvatarLogic.ASPECTS[ii].name);
            }
        }
        String[] tarray = tabs.toArray(new String[tabs.size()]);
        final BImage blankbg = _ctx.loadImage("ui/barber/tabs_back.png");
        final BImage tabbg = _ctx.loadImage("ui/barber/side_new_look.png");
        final BImage malebg = isMale ?
            _ctx.loadImage("ui/barber/side_new_look_male.png") : null;
        HackyTabs htabs = new HackyTabs(
            ctx, true, "ui/barber/tab_", tarray, 54, 30) {
            protected void wasAdded () {
                super.wasAdded();
                blankbg.reference();
                tabbg.reference();
                if (malebg != null) {
                    malebg.reference();
                }
            }
            protected void wasRemoved () {
                super.wasRemoved();
                blankbg.release();
                tabbg.release();
                if (malebg != null) {
                    malebg.release();
                }
            }
            protected void renderBackground (Renderer renderer) {
                super.renderBackground(renderer);
                blankbg.render(
                    renderer, -5, _height - blankbg.getHeight() - 5, _alpha);
                tabbg.render(
                    renderer, 0, _height - tabbg.getHeight() - 42, _alpha);
                if (malebg != null) {
                    malebg.render(
                        renderer, 0, _height - malebg.getHeight() - 42,
                        _alpha);
                }
            }
            protected void tabSelected (int index) {
                _current = _handlers.get(AvatarLogic.ASPECTS[index].name);
                _current.selected();
            }
        };
        htabs.setDefaultTab(1); // default to hair which is more interesting
        add(htabs, new Rectangle(10, 35, 140, 470));
    }

    /**
     * Called by the {@link BarberView} to give us a reference to our barber
     * object when needed.
     */
    public void setBarberObject (BarberObject barbobj)
    {
        _barbobj = barbobj;
    }

    /**
     * Creates a {@link LookConfig} for the currently configured look.
     */
    public LookConfig getLookConfig ()
    {
        LookConfig config = new LookConfig();
        config.name = (_name == null) ? "" : _name.getText();
        config.hair = _colsels.get("hair").getSelectedColor();
        config.skin = _colsels.get("head").getSelectedColor();

        // look up the selection for each aspect class
        config.aspects = new String[AvatarLogic.ASPECTS.length];
        config.colors = new int[config.aspects.length];
        for (int ii = 0; ii < config.aspects.length; ii++) {
            if (AvatarLogic.ASPECTS[ii].maleOnly &&
                !_ctx.getUserObject().isMale) {
                continue;
            }
            String aspect = AvatarLogic.ASPECTS[ii].name;
            AspectCatalog.Aspect choice = _handlers.get(aspect).getChoice();
            config.aspects[ii] = (choice == null) ? null : choice.name;
            config.colors[ii] = getColorizations(aspect);
        }

        return config;
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        // make sure they specified a name for the new look
        String name = _name.getText();
        if (StringUtil.isBlank(name)) {
            _status.setText(_msgs.get("m.name_required"));
            return;
        }

        // prevent double clicks or other lag related fuckolas
        _buy.setEnabled(false);

        BarberService.ConfirmListener cl = new BarberService.ConfirmListener() {
            public void requestProcessed () {
                _status.setText(_msgs.get("m.look_bought"));
                _name.setText("");
                _buy.setEnabled(true);
                BangUI.play(BangUI.FeedbackSound.ITEM_PURCHASE);
            }
            public void requestFailed (String reason) {
                _status.setText(_msgs.xlate(reason));
                _buy.setEnabled(true);
            }
        };
        _barbobj.service.purchaseLook(getLookConfig(), cl);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        if (_barbobj != null && _current != null) {
            // fake the current tab being selected to reselect the active
            // aspect and update the avatar (in case our outfit changed)
            _current.selected();
        }
    }

    protected void setActiveColor (ColorSelector colsel)
    {
        if (_active != null) {
            remove(_active);
        }
        _active = colsel;
        if (_active != null) {
            _collabel.setText(_msgs.get("m.col_" + colsel.getColorClass()));
            add(_active, new Point(637, 510));
        } else {
            _collabel.setText("");
        }
    }

    protected int getColorizations (String aspect)
    {
        // we need to skip hair and head because those have color selectors but
        // they're used for the globals
        if ("hair".equals(aspect) || "head".equals(aspect)) {
            return 0;
        }

        ColorSelector cs = _colsels.get(aspect);
        return (cs == null) ? 0 : AvatarLogic.composeZation(
            cs.getColorClass(), cs.getSelectedColor());
    }

    protected void updateAvatar ()
    {
        int scrip = AvatarCodes.BASE_LOOK_SCRIP_COST,
            coins = AvatarCodes.BASE_LOOK_COIN_COST;

        // obtain the component ids of the various aspect selections and total
        // up the cost of this look while we're at it
        ArrayIntSet compids = new ArrayIntSet();
        for (AspectHandler handler : _handlers.values()) {
            AspectCatalog.Aspect choice = handler.getChoice();
            if (choice == null) {
                continue;
            }
            scrip += choice.scrip;
            coins += choice.coins;

            int zations = getColorizations(handler.getAspect().name);
            CharacterComponent[] components = handler.getChoiceComponents();
            for (int ii = 0; ii < components.length; ii++) {
                if (components[ii] != null) {
                    compids.add(zations | components[ii].componentId);
                }
            }
        }

        // copy in any required articles from their active look
        PlayerObject user = _ctx.getUserObject();
        Look current = user.getLook(Look.Pose.DEFAULT);
        if (current != null && current.articles.length != 0) {
            for (int ii = 0; ii < AvatarLogic.SLOTS.length; ii++) {
                if (AvatarLogic.SLOTS[ii].optional) {
                    continue;
                }
                Article article = (Article)
                    user.inventory.get(current.articles[ii]);
                compids.add(article.getComponents());
            }
        }

        int[] avatar = new int[compids.size()+1];
        avatar[0] = (_colsels.get("hair").getSelectedColor() << 5) |
            _colsels.get("head").getSelectedColor();
        compids.toIntArray(avatar, 1);

        // update the avatar and cost displays
        _avatar.setAvatar(new AvatarInfo(avatar));

        // no coin cost for gold pass holders
        if (user.holdsGoldPass(user.townId)) {
            coins = 0;
        }
        if (_cost != null) {
            _cost.setMoney(scrip, coins, false);
        }
    }

    protected static class ChoiceIcon extends PaletteIcon
    {
        public AspectCatalog.Aspect aspect;

        public CharacterComponent[] components;

        public ChoiceIcon (String prefix, AspectCatalog.Aspect aspect) {
            this.aspect = aspect;
            _prefix = prefix;
        }

        public void updateIcon (BangContext ctx, ColorSelector colsel) {
            BImage image;
            if (aspect == null) {
                // this only happens for optional male aspects so we use a
                // blank head
                image = ctx.loadImage("aspects/male/head/normal.png");
            } else if (colsel == null) {
                image = ctx.loadImage(getIconPath());
            } else {
                image = ctx.getImageCache().createColorizedBImage(
                    getIconPath(), new Colorization[] {
                        colsel.getSelectedColorization() }, true);
            }
            setIcon(new ImageIcon(image));
        }

        protected String getIconPath () {
            return "aspects/" + _prefix + "/" + aspect.name + ".png";
        }

        protected String _prefix;
    }

    protected class AspectHandler
        implements IconPalette.Inspector, ActionListener
    {
        public AspectHandler (AvatarLogic.Aspect aspect)
        {
            _aspect = aspect;

            String prefix = _gender + _aspect.name;
            ComponentRepository crepo =
                _ctx.getCharacterManager().getComponentRepository();
            Collection<AspectCatalog.Aspect> aspects =
                _ctx.getAvatarLogic().getAspectCatalog().getAspects(prefix);

            // default to the NONE if we have one, then later override it if we
            // see that their current look contains one of our components
            if (aspect.optional) {
                _icons.add(_choice = new ChoiceIcon(prefix, null));
            }

            int townIdx = BangUtil.getTownIndex(_ctx.getUserObject().townId);
            Look look = _ctx.getUserObject().getLook(Look.Pose.DEFAULT);
            for (AspectCatalog.Aspect entry : aspects) {
                if (BangUtil.getTownIndex(entry.townId) > townIdx) {
                    continue;
                }
                ChoiceIcon choice = new ChoiceIcon(prefix, entry);
                choice.components =
                    new CharacterComponent[aspect.classes.length];
                for (int ii = 0; ii < aspect.classes.length; ii++) {
                    String cclass = _gender + aspect.classes[ii];
                    try {
                        choice.components[ii] =
                            crepo.getComponent(cclass, entry.name);
                    } catch (NoSuchComponentException nsce) {
                        // not a problem, not all aspects use all of their
                        // underlying component types
                    }
                }
                // if we have a default look and it contains one of our
                // component ids, then this should be our default choice
                if (look != null &&
                    look.containsAspect(choice.components[0].componentId)) {
                    _choice = choice;
                }
                _icons.add(choice);
            }

            // this shouldn't happen, but just in case
            if (_choice == null && _icons.size() > 0) {
                _choice = _icons.get(0);
            }

            // TODO: sort components based on cost?
        }

        public AvatarLogic.Aspect getAspect ()
        {
            return _aspect;
        }

        public AspectCatalog.Aspect getChoice ()
        {
            return _choice.aspect;
        }

        public CharacterComponent[] getChoiceComponents ()
        {
            return _choice.components;
        }

        public void selected ()
        {
            _palette.clear();
            _palette.setInspector(this);
            for (ChoiceIcon icon : _icons) {
                _palette.addIcon(icon);
            }
            _choice.setSelected(true);
            setActiveColor(_colsels.get(_aspect.name));
            actionPerformed(null);
        }

        // documentation inherited from interface IconPalette.Inspector
        public void iconUpdated (SelectableIcon icon, boolean selected)
        {
            // TODO: disallow deselection
            if (!selected) {
                return;
            }

            if (_choice != icon) {
                _choice = (ChoiceIcon)icon;
                // post a runnable to update the avatar so that this UI action
                // can complete and the interface remains more responsive
                _ctx.getApp().postRunnable(new Runnable() {
                    public void run () {
                        updateAvatar();
                    }
                });
            }
        }

        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            ColorSelector colsel = _colsels.get(_aspect.name);
            for (ChoiceIcon icon : _icons) {
                icon.updateIcon(_ctx, colsel);
            }
            updateAvatar();
        }

        protected AvatarLogic.Aspect _aspect;
        protected ArrayList<ChoiceIcon> _icons = new ArrayList<ChoiceIcon>();
        protected ChoiceIcon _choice;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected StatusLabel _status;

    protected AvatarView _avatar;
    protected IconPalette _palette;
    protected ColorSelector _active;
    protected AspectHandler _current;

    protected BTextField _name;
    protected MoneyLabel _cost;
    protected BButton _buy;

    protected BarberObject _barbobj;
    protected String _gender;

    protected BLabel _collabel;
    protected HashMap<String,ColorSelector> _colsels =
        new HashMap<String,ColorSelector>();

    protected HashMap<String,AspectHandler> _handlers =
        new HashMap<String,AspectHandler>();
}
