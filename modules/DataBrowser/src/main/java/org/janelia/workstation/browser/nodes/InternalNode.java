package org.janelia.workstation.browser.nodes;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.IOException;

import org.apache.commons.lang.StringEscapeUtils;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.model.domain.interfaces.HasIdentifier;
import org.janelia.workstation.core.nodes.IdentifiableNode;
import org.janelia.workstation.core.nodes.NodeTracker;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An internal tree node which is not based on a domain object, but has an identifier.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InternalNode<T extends HasIdentifier> extends IdentifiableNode<T> {
        
    private final static Logger log = LoggerFactory.getLogger(InternalNode.class);
    
    private final ChildFactory<?> parentChildFactory;
    private final InstanceContent lookupContents;
    
    InternalNode(ChildFactory<?> parentChildFactory, Children children, T object) {
        this(new InstanceContent(), parentChildFactory, children, object);
    }

    private InternalNode(InstanceContent lookupContents, ChildFactory<?> parentChildFactory, Children children, T object) {
        super(children, new AbstractLookup(lookupContents));
        this.parentChildFactory = parentChildFactory;
        this.lookupContents = lookupContents;
        lookupContents.add(object);
        NodeTracker.getInstance().registerNode(this);
    }

    @Override
    public void destroy() throws IOException {
        NodeTracker.getInstance().deregisterNode(this);
    }
    
    protected InstanceContent getLookupContents() {
        return lookupContents;
    }

    public ChildFactory<?> getParentChildFactory() {
        return parentChildFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {
        return (T)getLookup().lookup(Object.class);
    }

    @Override
    public Long getId() {
        return getObject().getId();
    }
    
    public String getPrimaryLabel() {
        return getObject().toString();
    }
    
    public String getSecondaryLabel() {
        return null;
    }
    
    public String getExtraLabel() {
        return null;
    }

    @Override
    public void update(T refreshed) {
        if (refreshed==null) throw new IllegalStateException("Cannot update with null object");
        String oldName = getName();
        String oldDisplayName = getDisplayName();
        log.debug("Updating node with: {}",refreshed);
        lookupContents.remove(getObject());
        lookupContents.add(refreshed);
        fireCookieChange();
        fireNameChange(oldName, getName());
        log.debug("Display name changed {} -> {}",oldDisplayName, getDisplayName());
        fireDisplayNameChange(oldDisplayName, getDisplayName());
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
        String extra = getExtraLabel();
        StringBuilder sb = new StringBuilder();
        if (primary!=null) {
            sb.append("<font color='!Tree.textForeground'>");
            sb.append(StringEscapeUtils.escapeHtml(primary));
            sb.append("</font>");
        }
        if (secondary!=null) {
            sb.append(" <font color='!ws.TreeSecondaryLabel'><i>");
            sb.append(StringEscapeUtils.escapeHtml(secondary));
            sb.append("</i></font>");
        }
        if (extra!=null) {
            sb.append(" <font color='!ws.TreeExtraLabel'>");
            sb.append(StringEscapeUtils.escapeHtml(extra));
            sb.append("</font>");
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
}
