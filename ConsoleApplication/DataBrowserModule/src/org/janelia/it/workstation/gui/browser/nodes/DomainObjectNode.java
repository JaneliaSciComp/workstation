package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import static javax.swing.Action.NAME;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.janelia.it.workstation.gui.browser.nb_action.RemoveAction;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.browser.nb_action.MoveToFolderAction;
import org.janelia.it.workstation.gui.browser.nb_action.PopupLabelAction;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.openide.nodes.AbstractNode;
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
public class DomainObjectNode extends AbstractNode implements Has2dRepresentation, HasIdentifier {

    private final static Logger log = LoggerFactory.getLogger(DomainObjectNode.class);

    private final ChildFactory parentChildFactory;
    private final InstanceContent lookupContents;

    public DomainObjectNode(ChildFactory parentChildFactory, Children children, DomainObject domainObject) {
        this(new InstanceContent(), parentChildFactory, children, domainObject);
    }

    public DomainObjectNode(InstanceContent lookupContents, ChildFactory parentChildFactory, Children children, DomainObject domainObject) {
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

    public String getExtraLabel() {
        return null;
    }
    
    @Override
    public Long getId() {
        return getDomainObject().getId();
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
        return true;
    }

    @Override
    public Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<>();
        actions.add(PopupLabelAction.get());
        actions.add(null);
        actions.add(new CopyNameAction());
        actions.add(new CopyGUIDAction());
        actions.add(null);
        actions.add(new OpenInNewViewerAction());
        actions.add(null);
        actions.add(MoveToFolderAction.get());
        actions.add(new RenameAction());
        actions.add(RemoveAction.get());
        actions.add(null);
        
//        DomainExplorerTopComponent detc = (DomainExplorerTopComponent)WindowLocator.getByName(DomainExplorerTopComponent.TC_NAME);
//        actions.add(new DebugAction(detc.getLookup()));
        
        //actions.add(CutAction.get(CutAction.class));
//        actions.add(CopyAction.get(CopyAction.class));
//        actions.add(PasteAction.get(PasteAction.class));
//        actions.add(DeleteAction.get(DeleteAction.class));
//        actions.add(MoveUpAction.get(MoveUpAction.class));
//        actions.add(MoveDownAction.get(MoveDownAction.class));
        actions.add(null);
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public SystemAction[] getActions() {
        return null;
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
        String extra = getExtraLabel();
        StringBuilder sb = new StringBuilder();
        if (primary!=null) {
            sb.append("<font color='!Label.foreground'>");
            sb.append(primary);
            sb.append("</font>");
        }
        if (secondary!=null) {
            sb.append(" <font color='#957D47'>");
            sb.append(secondary);
            sb.append("</font>");
        }
        if (extra!=null) {
            sb.append(" <font color='#959595'>");
            sb.append(extra);
            sb.append("</font>");
        }
        return sb.toString();
    }

    @Override
    public Transferable clipboardCopy() throws IOException {
        log.info("clipboard COPY "+getDomainObject());
        Transferable deflt = super.clipboardCopy();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DataFlavor.stringFlavor) {
            @Override
            protected String getData() {
                return getPrimaryLabel();
            }
        });
        added.put(new ExTransferable.Single(DomainObjectFlavor.SINGLE_FLAVOR) {
            @Override
            protected DomainObject getData() {
                return (DomainObject) getDomainObject();
            }
        });
        return added;
    }

    @Override
    public Transferable clipboardCut() throws IOException {
        log.info("clipboard CUT "+getDomainObject());
        Transferable deflt = super.clipboardCut();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DomainObjectFlavor.SINGLE_FLAVOR) {
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
            TreeNodeChildFactory treeNodeChildFactory = (TreeNodeChildFactory) parentChildFactory;
            try {
                treeNodeChildFactory.removeChild(getDomainObject());
            }
            catch (Exception e) {
                throw new IOException("Error destroying node",e);
            }
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

            for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors()) {
                Method getter = propertyDescriptor.getReadMethod();
                if (getter==null) continue;
                Method setter = propertyDescriptor.getWriteMethod();
                PropertySupport.Reflection prop = new PropertySupport.Reflection(obj, getter.getReturnType(), getter, setter);
                prop.setName(DomainUtils.unCamelCase(getter.getName().replaceFirst("get", "")));
                set.put(prop);
            }

        }
        catch (IntrospectionException ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }

        sheet.put(set);
        return sheet;
    }

    @Override
    public String get2dImageFilepath(String role) {
        DomainObject domainObject = getDomainObject();
        if (domainObject instanceof HasFiles) {
            return DomainUtils.getFilepath((HasFiles) domainObject, role);
        }
        return null;
    }

    /**
     * Subclasses should override this method to add their their paste type to the set. 
     */
    @Override
    public PasteType getDropType(final Transferable t, int action, int index) {
        // Let subclasses define their paste types by overriding this method
        return null;
    }
    
    @Override
    protected void createPasteTypes(Transferable t, List<PasteType> s) {
        super.createPasteTypes(t, s);
        PasteType p = getDropType(t, 0, 0);
        if (p != null) {
            s.add(p);
        }
    }

    protected final class CopyNameAction extends AbstractAction {

        public CopyNameAction() {
            putValue(NAME, "Copy Name To Clipboard");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Transferable t = new StringSelection(getDisplayName());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
        }
    }

    protected final class CopyGUIDAction extends AbstractAction {

        public CopyGUIDAction() {
            putValue(NAME, "Copy GUID To Clipboard");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            HasIdentifier hasId = getLookup().lookup(HasIdentifier.class);
            if (hasId==null) {
                return;
            }
            Transferable t = new StringSelection(hasId.getId()+"");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
        }
    }
    
    protected final class RenameAction extends AbstractAction {

        public RenameAction() {
            putValue(NAME, "Rename");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DomainObject domainObject = getLookup().lookup(DomainObject.class);
            if (domainObject==null) {
                return;
            }
            String newName = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), "Name:\n", "Rename "
                    + domainObject.getName(), JOptionPane.PLAIN_MESSAGE, null, null, domainObject.getName());
            if ((newName == null) || (newName.length() <= 0)) {
                return;
            }
            try {
                DomainDAO dao = DomainMgr.getDomainMgr().getDao();
                final String oldName = domainObject.getName();
                dao.updateProperty(SessionMgr.getSubjectKey(), domainObject, "name", newName);
                fireDisplayNameChange(oldName, newName); 
            } 
            catch (Exception ex) {
                SessionMgr.getSessionMgr().handleException(ex);
            }
        }

        @Override
        public boolean isEnabled() {
            DomainObject domainObject = getLookup().lookup(DomainObject.class);
            return DomainUtils.hasWriteAccess(domainObject);
        }
    }
    
    protected final class OpenInNewViewerAction extends AbstractAction {

        public OpenInNewViewerAction() {
            putValue(NAME, "Open In New Viewer");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DomainListViewTopComponent browser = new DomainListViewTopComponent();
            browser.open();
            browser.requestActive();
            DomainExplorerTopComponent explorer = (DomainExplorerTopComponent)WindowLocator.getByName(DomainExplorerTopComponent.TC_NAME);
            // Deselect it first, so that this generates another selection event, since the browser didn't exist when the first one was generated
            explorer.getSelectionModel().deselect(DomainObjectNode.this);
            explorer.getSelectionModel().select(DomainObjectNode.this, true);
        }

        @Override
        public boolean isEnabled() {
            DomainObject domainObject = getLookup().lookup(DomainObject.class);
            return DomainUtils.hasWriteAccess(domainObject);
        }
    }
}
