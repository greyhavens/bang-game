//
// $Id$

package com.threerings.bang.client;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.bang.util.BangContext;
import com.threerings.util.MessageBundle;

import static com.threerings.bang.Log.log;

/**
 * Displays field editors for all runtime configurations.
 */
public class ConfigEditorView extends BDecoratedWindow
{
    public ConfigEditorView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), ctx.xlate("options", "m.config_title"));
        setLayoutManager(GroupLayout.makeVert(GroupLayout.TOP));
        ((GroupLayout)getLayoutManager()).setGap(25);
        setModal(true);

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("options");

        BContainer cont = new BContainer(new TableLayout(2, 10, 10));
        Field[] fields = Config.class.getDeclaredFields();
        for (int ii = 0; ii < fields.length; ii++) {
            final Field field = fields[ii];
            if ((field.getModifiers() & Modifier.STATIC) == 0 ||
                (field.getModifiers() & Modifier.PUBLIC) == 0) {
                continue;
            }

            BComponent editor = null;
            Class<?> ftype = field.getType();
            try {
                if (ftype.equals(Boolean.TYPE)) {
                    BCheckBox checkbox = new BCheckBox("");
                    checkbox.setSelected(field.getBoolean(null));
                    checkbox.addListener(new ActionListener() {
                        public void actionPerformed (ActionEvent event) {
                            BCheckBox cb = (BCheckBox)event.getSource();
                            Config.updateValue(field, cb.isSelected());
                        }
                    });
                    editor = checkbox;

//                 } else if (ftype.equals(Float.TYPE)) {
//                     float val = field.getFloat(null);
//                     field.setFloat(null, _prefs.getFloat(field.getName(), val));
//                 } else if (ftype.equals(Integer.TYPE)) {
//                     int val = field.getInt(null);
//                     field.setInt(null, _prefs.getInt(field.getName(), val));

                } else {
                    log.warning("Unhandled config field type", "field", field.getName(),
                                "type", ftype);
                }

                if (editor != null) {
                    String label = _msgs.get("m.runtime_" + field.getName());
                    cont.add(new BLabel(label, "right_label"));
                    cont.add(editor);
                }

            } catch (Exception e) {
            }
        }

        add(cont);

        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        bcont.add(new BButton(_msgs.get("m.dismiss"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _ctx.getBangClient().clearPopup(ConfigEditorView.this, true);
            }
        }, "dismiss"));
        add(bcont);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
}
