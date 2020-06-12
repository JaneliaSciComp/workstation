package org.janelia.workstation.controller.task_workflow;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyModel;

public class ReviewGroupPropertyEditor extends PropertyEditorSupport implements ExPropertyEditor, InplaceEditor.Factory {
    private Boolean reviewed;

    @Override
    public String getAsText() {
        return reviewed.toString();
    }

    @Override
    public void setAsText(String s) {
        reviewed = Boolean.parseBoolean(s);
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
        JCheckBox reviewed = new JCheckBox("Reviewed");

        @Override
        public void connect(PropertyEditor pe, PropertyEnv pe1) {
            editor = pe;
            reset();
        }

        @Override
        public JComponent getComponent() {
            return reviewed;
        }

        @Override
        public void clear() {
            editor = null;
            model = null;
        }

        @Override
        public Object getValue() {
            if (reviewed.getSelectedObjects()==null) {
                return false;
            } 
            return true;
        }

        @Override
        public void setValue(Object o) {
            reviewed.setSelected((Boolean)o);
        }

        @Override
        public boolean supportsTextEntry() {
            return true;
        }

        @Override
        public void reset() {
            reviewed.setSelected(false);
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
            return cmpnt == reviewed || reviewed.isAncestorOf(cmpnt);
        }

    }
}
