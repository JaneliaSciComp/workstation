package org.janelia.it.workstation.gui.framework.outline.ontology;

import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.*;
import org.janelia.it.jacs.model.ontology.types.Enum;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.workstation.gui.framework.actions.Action;
import org.janelia.it.workstation.gui.framework.actions.CreateOntologyTermAction;
import org.janelia.it.workstation.gui.framework.actions.RemoveAnnotationTermAction;
import org.janelia.it.workstation.gui.framework.outline.EntityContextMenu;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Context pop up menu for ontology elements.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyContextMenu extends EntityContextMenu {

    // Current selection
    protected final OntologyElement ontologyElement;

    public OntologyContextMenu(RootedEntity rootedEntity, OntologyElement ontologyElement) {
        super(rootedEntity);
        this.ontologyElement = ontologyElement;
        checkNotNull(rootedEntity, "Rooted entity cannot be null");
    }
    
    @Override
    public void addMenuItems() {
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());
        add(getDetailsItem());
        setNextAddRequiresSeparator(true);
        add(getImportOntologyItem());
        add(getExportOntologyItem());
        setNextAddRequiresSeparator(true);
        add(getAssignShortcutItem());
        add(getAddItemMenu());
        add(getDeleteItem());
        setNextAddRequiresSeparator(true);
        add(getRemoveAnnotationItem());
    }

    protected JMenuItem getAssignShortcutItem() {
        
        JMenuItem menuItem = new JMenuItem("  Assign Shortcut...");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SessionMgr.getBrowser().getOntologyOutline().assignShortcutForCurrentNode();
            }
        });
        return menuItem;

    }

    protected JMenuItem getImportOntologyItem() {
       return null;
        //return getActionItem(new OntologyImportAction(ontologyElement));
    }

    protected JMenuItem getExportOntologyItem() {
        return null;
        //return getActionItem(new OntologyExportAction(ontologyElement));
    }

    protected JMenu getAddItemMenu() {

        OntologyElementType type = ontologyElement.getType();
        if (type==null) return null;
        
        JMenu addMenuPopup = new JMenu("  Add...");
        
        if (type instanceof Enum) {
            // Alternative "Add" menu for enumeration nodes
            JMenuItem smi = new JMenuItem("Item");
            smi.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Action action = new CreateOntologyTermAction(EnumItem.class.getSimpleName());
                    action.doAction();
                }
            });
            addMenuPopup.add(smi);
        }
        else if (type.allowsChildren() || type instanceof Tag) {

            Class[] nodeTypes = {Category.class, Tag.class, Enum.class, EnumText.class, Interval.class, Text.class, Custom.class};
            for (final Class<? extends OntologyElementType> nodeType : nodeTypes) {
                try {
                    JMenuItem smi = new JMenuItem(nodeType.newInstance().getName());
                    smi.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Action action = new CreateOntologyTermAction(nodeType.getSimpleName());
                            action.doAction();
                        }
                    });
                    addMenuPopup.add(smi);
                }
                catch (Exception ex) {
                    SessionMgr.getSessionMgr().handleException(ex);
                }
            }
        }

        if (!ModelMgrUtils.hasWriteAccess(rootedEntity.getEntity())) {
            addMenuPopup.setEnabled(false);
        }
        
        return addMenuPopup;
    }
    
    protected JMenuItem getRemoveAnnotationItem() {
        Long keyTermId = (ontologyElement.getType() instanceof EnumItem)?ontologyElement.getParent().getId():ontologyElement.getId();
        String keyTermValue = ontologyElement.getName();
        RemoveAnnotationTermAction action = new RemoveAnnotationTermAction(keyTermId, keyTermValue) {
            @Override
            public String getName() {
                return "  Remove Annotation From Selected Items";
            }
        };
        return getActionItem(action);
    }
}
