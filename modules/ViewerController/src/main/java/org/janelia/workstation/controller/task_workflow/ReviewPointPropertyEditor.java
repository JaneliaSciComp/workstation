package org.janelia.workstation.controller.task_workflow;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyModel;

public class ReviewPointPropertyEditor extends PropertyEditorSupport implements ExPropertyEditor, InplaceEditor.Factory {
    private String value;

    @Override
    public String getAsText() {
        System.out.println("ASDFASDF");
        return value;
    }

    @Override
    public void setAsText(String s) {
        System.out.println("ASDFASDF");
        value = s;
    }

    @Override
    public void attachEnv(PropertyEnv env) {
        env.registerInplaceEditorFactory(this);
    }

    private InplaceEditor ed = null;

    @Override
    public InplaceEditor getInplaceEditor() {
        if (ed == null) {
            ed = new Inplace();
        }
        return ed;
    }

    private static class Inplace implements InplaceEditor {

        private PropertyEditor editor = null;
        private String[] reviewOptions = new String[]{"Good", "Problem", "Reviewed"};
        JComboBox comboEditor = new JComboBox(reviewOptions);

        @Override
        public void connect(PropertyEditor pe, PropertyEnv pe1) {
            editor = pe;
            reset();
        }

        @Override
        public JComponent getComponent() {
            return comboEditor;
        }

        @Override
        public void clear() {
            editor = null;
            model = null;
        }

        @Override
        public Object getValue() {
            return comboEditor.getSelectedItem();
        }

        @Override
        public void setValue(Object o) {
            String value = (String) o;
            for (int i = 0; i < reviewOptions.length; i++) {
                if (value == reviewOptions[i]) {
                    comboEditor.setSelectedItem(i);
                }
            }
        }

        @Override
        public boolean supportsTextEntry() {
            return true;
        }

        @Override
        public void reset() {
            String v = (String) editor.getValue();
            if (v != null) {
                for (int i = 0; i < reviewOptions.length; i++) {
                    if (v == reviewOptions[i]) {
                        comboEditor.setSelectedItem(i);
                    }
                }
            }
        }

        @Override
        public void addActionListener(ActionListener al) {
        }

        @Override
        public void removeActionListener(ActionListener al) {
        }

        @Override
        public KeyStroke[] getKeyStrokes() {
            return new KeyStroke[0];
        }

        private PropertyModel model;

        @Override
        public PropertyEditor getPropertyEditor() {
            return editor;
        }

        @Override
        public PropertyModel getPropertyModel() {
            return model;
        }

        @Override
        public void setPropertyModel(PropertyModel pm) {
            model = pm;
        }

        @Override
        public boolean isKnownComponent(Component cmpnt) {
            return cmpnt == comboEditor || comboEditor.isAncestorOf(cmpnt);
        }

    }
}
