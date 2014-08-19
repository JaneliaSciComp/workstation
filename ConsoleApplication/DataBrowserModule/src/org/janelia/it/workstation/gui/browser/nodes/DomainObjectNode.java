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

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.MaterializedView;
import org.janelia.it.workstation.gui.browser.components.DatePropertyEditor;
import org.janelia.it.workstation.gui.browser.nodes.children.TreeNodeChildFactory;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.ErrorManager;
import org.openide.actions.CopyAction;
import org.openide.actions.CutAction;
import org.openide.actions.DeleteAction;
import org.openide.actions.MoveDownAction;
import org.openide.actions.MoveUpAction;
import org.openide.actions.PasteAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectNode extends AbstractNode {

    private final static Logger log = LoggerFactory.getLogger(DomainObjectNode.class);
    
    protected final ChildFactory parentChildFactory;
    
    private final InstanceContent lookupContents;

    public DomainObjectNode(ChildFactory parentChildFactory, Children children, DomainObject domainObject) throws Exception {
        this(new InstanceContent(), parentChildFactory, children, domainObject);
    }
    
    public DomainObjectNode(InstanceContent lookupContents, ChildFactory parentChildFactory, Children children, DomainObject domainObject) throws Exception {
        super(children, new AbstractLookup(lookupContents));
        this.parentChildFactory = parentChildFactory;
        this.lookupContents = lookupContents;   
        lookupContents.add(domainObject);
    }
    
    protected InstanceContent getLookupContents() {
        return lookupContents;
    }
    
    public DomainObject getDomainObject() {
        DomainObject obj = getLookup().lookup(DomainObject.class);
        return obj;
    }
    
    public String getPrimaryLabel() {
        return getDomainObject().getId().toString();
    }
    
    public String getSecondaryLabel() {
        return getDomainObject().getOwnerKey();
    }
    
    @Override
    public boolean canCut() {
        return true;
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
    public boolean canDestroy() {
        if (getDomainObject() instanceof MaterializedView) {
            return false;
        }
        return true;
    }
    
    @Override
    public Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<Action>();
        actions.add(CutAction.get(CutAction.class));
        actions.add(CopyAction.get(CopyAction.class));
        actions.add(PasteAction.get(PasteAction.class));
        actions.add(DeleteAction.get(DeleteAction.class));
        actions.add(MoveUpAction.get(MoveUpAction.class));
        actions.add(CutAction.get(CutAction.class));
        actions.add(MoveDownAction.get(MoveDownAction.class));
        return actions.toArray(new Action[0]);
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("brick_grey.png").getImage();
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
        log.info("clipboard COPY "+getDomainObject());
        Transferable deflt = super.clipboardCopy();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR) {
            @Override
            protected DomainObject getData() {
                return (DomainObject)getDomainObject();
            }
        });
        added.put(new ExTransferable.Single(DataFlavor.stringFlavor) {
            @Override
            protected String getData() {
                return getPrimaryLabel();
            }
        });
        return added;
    }
    
    @Override
    public Transferable clipboardCut() throws IOException {
        log.info("clipboard CUT "+getDomainObject());
        Transferable deflt = super.clipboardCut();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR) {
            @Override
            protected DomainObject getData() {
                return getDomainObject();
            }
        });
        added.put(new ExTransferable.Single(DataFlavor.stringFlavor) {
            @Override
            protected String getData() {
                return getPrimaryLabel();
            }
        });
        return added;
    }
    
    @Override
    public void destroy() throws IOException {
        if (parentChildFactory==null) {
            throw new IllegalStateException("Cannot destroy node without parent");
        }
        if (parentChildFactory instanceof TreeNodeChildFactory) {
            TreeNodeChildFactory treeNodeChildFactory = (TreeNodeChildFactory)parentChildFactory;
            treeNodeChildFactory.removeChild(getDomainObject());
        }
        else {
            throw new IllegalStateException("Cannot destroy sample without treeNode parent");
        }
    }
    
    @Override
    protected Sheet createSheet() {

        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
        DomainObject obj = getDomainObject();
        
        try {

            for(PropertyDescriptor propertyDescriptor : 
                Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors()) {
                Method getter = propertyDescriptor.getReadMethod();
                Method setter = propertyDescriptor.getWriteMethod();
                PropertySupport.Reflection prop = 
                        new PropertySupport.Reflection(obj, getter.getReturnType(), getter, setter);
                prop.setName(unCamelCase(getter.getName().replaceFirst("get", "")));
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

    public static String unCamelCase(String s) {
        return s.replaceAll("(?<=\\p{Ll})(?=\\p{Lu})|(?<=\\p{L})(?=\\p{Lu}\\p{Ll})", " ");
    }
}
