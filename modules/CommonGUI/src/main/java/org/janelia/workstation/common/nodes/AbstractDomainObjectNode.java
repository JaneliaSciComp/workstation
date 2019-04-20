package org.janelia.workstation.common.nodes;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import javax.swing.Action;

import org.apache.commons.lang.StringEscapeUtils;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.workstation.common.flavors.DomainObjectFlavor;
import org.janelia.workstation.common.flavors.DomainObjectNodeFlavor;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.actions.DomainObjectAcceptorHelper;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.nodes.DomainObjectNode;
import org.janelia.workstation.core.nodes.IdentifiableNode;
import org.janelia.workstation.core.nodes.NodeTracker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A domain object node in the graph, usually a Filter or Object Set. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractDomainObjectNode<T extends DomainObject> 
        extends IdentifiableNode<T>
        implements DomainObjectNode<T>, Has2dRepresentation  {

    private final static Logger log = LoggerFactory.getLogger(AbstractDomainObjectNode.class);

    private final ChildFactory<?> parentChildFactory;
    private final InstanceContent lookupContents;

    public AbstractDomainObjectNode(ChildFactory<?> parentChildFactory, Children children, T domainObject) {
        this(new InstanceContent(), parentChildFactory, children, domainObject);
    }

    public AbstractDomainObjectNode(InstanceContent lookupContents, ChildFactory<?> parentChildFactory, Children children, T domainObject) {
        super(children, new AbstractLookup(lookupContents));
        this.parentChildFactory = parentChildFactory;
        this.lookupContents = lookupContents;
        if (domainObject==null) throw new IllegalStateException("Cannot create node with null object");
        lookupContents.add(domainObject);
        NodeTracker.getInstance().registerNode(this);
    }

    @Override
    public void destroy() throws IOException {
        NodeTracker.getInstance().deregisterNode(this);
    }
    
    @Override
    public void update(T domainObject) {
        if (domainObject==null) throw new IllegalStateException("Cannot update with null object");
        String oldName = getName();
        String oldDisplayName = getDisplayName();
        log.debug("Updating node with: {}",domainObject.getName());
        lookupContents.remove(getDomainObject());
        lookupContents.add(domainObject);
        fireCookieChange();
        fireNameChange(oldName, getName());
        log.debug("Display name changed {} -> {}",oldDisplayName, getDisplayName());
        fireDisplayNameChange(oldDisplayName, getDisplayName());
    }

    public ChildFactory<?> getParentChildFactory() {
        return parentChildFactory;
    }
    
    protected InstanceContent getLookupContents() {
        return lookupContents;
    }

    @SuppressWarnings("unchecked")
    public T getDomainObject() {
        return (T) getLookup().lookup(DomainObject.class);
    }

    @Override
    public T getObject() {
        return getDomainObject();
    }

    @Override
    public String getName() {
        return getDomainObject().getName();
    }
    
    @Override
    public Long getId() {
        return getDomainObject().getId();
    }

    public String getPrimaryLabel() {
        return getDomainObject().getId().toString();
    }

    public String getSecondaryLabel() {
    	return DomainUtils.getNameFromSubjectKey(getDomainObject().getOwnerKey());
    }

    public String getExtraLabel() {
        return null;
    }

    @Override
    public String getDisplayName() {
        String displayName = getPrimaryLabel()+" "+getSecondaryLabel();
        String extra = getExtraLabel();
        if (extra!=null) {
            displayName += " "+extra;    
        }
        return displayName;
    }

    @Override
    public String getHtmlDisplayName() {
        String primary = getPrimaryLabel();
        String secondary = getSecondaryLabel();
        String extra = getExtraLabel();
        StringBuilder sb = new StringBuilder();
        if (primary!=null) {
            sb.append("<font color='!Tree.textForeground'>");
            sb.append(StringEscapeUtils.escapeHtml(primary));
            sb.append("</font>");
        }
        if (secondary!=null) {
            sb.append(" <font color='!ws.TreeSecondaryLabel'>");
            sb.append(StringEscapeUtils.escapeHtml(secondary));
            sb.append("</font>");
        }
        if (extra!=null) {
            sb.append(" <font color='!ws.TreeExtraLabel'>");
            sb.append(StringEscapeUtils.escapeHtml(extra));
            sb.append("</font>");
        }
        return sb.toString();
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
    public String get2dImageFilepath(String role) {
        DomainObject domainObject = getDomainObject();
        if (domainObject instanceof HasFiles) {
            return DomainUtils.getFilepath((HasFiles) domainObject, role);
        }
        return null;
    }
    
    @Override
    public boolean canCut() {
        return ClientDomainUtils.hasWriteAccess(getDomainObject());
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
        return ClientDomainUtils.hasWriteAccess(getDomainObject());
    }

    @Override
    public Action[] getActions(boolean context) {
        Collection<Action> actions = DomainObjectAcceptorHelper.getNodeContextMenuItems(getDomainObject());
        return actions.toArray(new Action[0]);
    }

    @Override
    public SystemAction[] getActions() {
        return null;
    }
    
    @Override
    protected Sheet createSheet() {

        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
        DomainObject obj = getDomainObject();

        try {
            for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors()) {
                Method getter = propertyDescriptor.getReadMethod();
                if (getter==null) continue;
                Method setter = propertyDescriptor.getWriteMethod();
                PropertySupport.Reflection<?> prop = new PropertySupport.Reflection<>(obj, getter.getReturnType(), getter, setter);
                prop.setName(DomainUtils.unCamelCase(getter.getName().replaceFirst("get", "")));
                set.put(prop);
            }
        }
        catch (IntrospectionException ex) {
            FrameworkAccess.handleException(ex);
        }

        sheet.put(set);
        return sheet;
    }

    @Override
    public Transferable clipboardCopy() throws IOException {
        log.debug("Copy to clipboard: {}",getDomainObject());
        Transferable deflt = super.clipboardCopy();
        return addFlavors(ExTransferable.create(deflt));
    }

    @Override
    public Transferable clipboardCut() throws IOException {
        log.debug("Cut to clipboard: {}",getDomainObject());
        Transferable deflt = super.clipboardCut();
        return addFlavors(ExTransferable.create(deflt));
    }
    
    private Transferable addFlavors(ExTransferable added) {
        added.put(new ExTransferable.Single(DataFlavor.stringFlavor) {
            @Override
            protected String getData() {
                return getPrimaryLabel();
            }
        });
        added.put(new ExTransferable.Single(DomainObjectFlavor.SINGLE_FLAVOR) {
            @Override
            protected DomainObject getData() {
                return getDomainObject();
            }
        });
        added.put(new ExTransferable.Single(DomainObjectNodeFlavor.SINGLE_FLAVOR) {
            @Override
            protected AbstractDomainObjectNode<?> getData() {
                return AbstractDomainObjectNode.this;
            }
        });
        return added;
    }
    
    /**
     * Subclasses should override this method to add their their paste type to the set. 
     */
    @Override
    public PasteType getDropType(final Transferable t, int action, int index) {
        // Let subclasses define their paste types by overriding this method
        return null;
    }

    public static AbstractDomainObjectNode<?> getDomainObjectNode(Transferable t) {
        AbstractDomainObjectNode<?> node = null;
        try {
            node = (AbstractDomainObjectNode<?>)t.getTransferData(DomainObjectNodeFlavor.SINGLE_FLAVOR);
        }
        catch (UnsupportedFlavorException | IOException e) {
            log.error("Error getting transfer data", e);
        }
        return node;
    }

    @SuppressWarnings("unchecked")
    public static List<AbstractDomainObjectNode<?>> getDomainObjectNodeList(Transferable t) {
        List<AbstractDomainObjectNode<?>> node = null;
        try {
            node = (List<AbstractDomainObjectNode<?>>)t.getTransferData(DomainObjectNodeFlavor.LIST_FLAVOR);
        }
        catch (UnsupportedFlavorException | IOException e) {
            log.error("Error getting transfer data", e);
        }
        return node;
    }
}
