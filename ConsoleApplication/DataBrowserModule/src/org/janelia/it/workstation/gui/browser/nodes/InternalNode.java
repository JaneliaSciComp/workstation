package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.Action;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.components.DatePropertyEditor;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.ErrorManager;
import org.openide.actions.CopyAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.lookup.Lookups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InternalNode<T> extends AbstractNode implements HasUniqueId, Has2dRepresentation {
    
    private long uniqueId;
    
    private final static Logger log = LoggerFactory.getLogger(InternalNode.class);
    
    public InternalNode(Children children, T object) throws Exception {
        super(children, Lookups.singleton(object));
        this.uniqueId = IdGenerator.getNextId();
    }
    
    public Long getUniqueId() {
        return uniqueId;
    }
    
    public T getObject() {
        return (T)getLookup().lookup(Object.class);
    }
    
    public String getPrimaryLabel() {
        return getObject().toString();
    }
    
    public String getSecondaryLabel() {
        return null;
    }
    
    @Override
    public boolean canCut() {
        return false;
    }

    @Override
    public boolean canCopy() {
        return true;
    }

    @Override
    public boolean canRename() {
        return false;
    }
    
    @Override
    public Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<Action>();
        actions.add(CopyAction.get(CopyAction.class));
        return actions.toArray(new Action[0]);
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("package.png").getImage();
    }
    
    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
    }
    
    @Override
    public String getDisplayName() {
        return getPrimaryLabel();
    }
    
    @Override
    public String getHtmlDisplayName() {
        String primary = getPrimaryLabel();
        String secondary = getSecondaryLabel();
        StringBuilder sb = new StringBuilder();
        if (primary!=null) {
            sb.append("<font color='!Label.foreground'>");
            sb.append(primary);
            sb.append("</font>");
        }
        if (secondary!=null) {
            sb.append(" <font color='#957D47'><i>");
            sb.append(secondary);
            sb.append("</i></font>");
        }
        return sb.toString();
    }
    
    @Override
    public Transferable clipboardCopy() throws IOException {
        log.info("clipboard COPY "+getObject());
        Transferable deflt = super.clipboardCopy();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DataFlavor.stringFlavor) {
            @Override
            protected String getData() {
                return getPrimaryLabel();
            }
        });
        return added;
    }
    
    @Override
    protected Sheet createSheet() {

        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
        T obj = getObject();
        
        try {

            for(PropertyDescriptor propertyDescriptor : 
                Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors()) {
                Method getter = propertyDescriptor.getReadMethod();
                Method setter = propertyDescriptor.getWriteMethod();
                PropertySupport.Reflection prop = 
                        new PropertySupport.Reflection(obj, getter.getReturnType(), getter, setter);
                prop.setName(DomainUtils.unCamelCase(getter.getName().replaceFirst("get", "")));
                set.put(prop);
                
                if (getter.getReturnType().isAssignableFrom(Date.class)) {
                    prop.setPropertyEditorClass(DatePropertyEditor.class);        
                }
            }

        } catch (Exception ex) {
            ErrorManager.getDefault();
        }

        sheet.put(set);
        return sheet;
    }

    @Override
    public String get2dImageFilepath(String role) {
        return null;
    }
}
