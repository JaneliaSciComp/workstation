package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.MaterializedView;
import org.janelia.it.workstation.gui.browser.nodes.children.TreeNodeChildFactory;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.openide.actions.CopyAction;
import org.openide.actions.CutAction;
import org.openide.actions.DeleteAction;
import org.openide.actions.MoveDownAction;
import org.openide.actions.MoveUpAction;
import org.openide.actions.PasteAction;
import org.openide.nodes.BeanNode;
import org.openide.nodes.ChildFactory;
import org.openide.util.datatransfer.ExTransferable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectNode extends BeanNode<DomainObject> {

    private final static Logger log = LoggerFactory.getLogger(DomainObjectNode.class);
    
    protected final ChildFactory parentChildFactory;
    
    public DomainObjectNode(ChildFactory parentChildFactory, DomainObject domainObject) throws Exception {
        super(domainObject);
        this.parentChildFactory = parentChildFactory;
    }
    
    public String getPrimaryLabel() {
        return getBean().getId().toString();
    }
    
    public String getSecondaryLabel() {
        return getBean().getOwnerKey();
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
        if (getBean() instanceof MaterializedView) {
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
        log.info("clipboard COPY "+getBean());
        Transferable deflt = super.clipboardCopy();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR) {
            @Override
            protected DomainObject getData() {
                return (DomainObject)getBean();
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
        log.info("clipboard CUT "+getBean());
        Transferable deflt = super.clipboardCut();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DomainObjectFlavor.DOMAIN_OBJECT_FLAVOR) {
            @Override
            protected DomainObject getData() {
                return (DomainObject)getBean();
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
            treeNodeChildFactory.removeChild(getBean());
        }
        else {
            throw new IllegalStateException("Cannot destroy sample without treeNode parent");
        }
    }
}
