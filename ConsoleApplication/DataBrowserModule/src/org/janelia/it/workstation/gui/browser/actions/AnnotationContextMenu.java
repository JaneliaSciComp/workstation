package org.janelia.it.workstation.gui.browser.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.EnumText;
import org.janelia.it.jacs.model.domain.ontology.Interval;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.Text;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.StateMgr;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.gui.browser.gui.support.PopupContextMenu;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context pop up menu for annotations.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationContextMenu extends PopupContextMenu {

    private static final Logger log = LoggerFactory.getLogger(AnnotationContextMenu.class);

    // Current selection
    private ImageModel<DomainObject, Reference> imageModel;
    protected Annotation annotation;
    protected List<DomainObject> domainObjectList;
    protected boolean multiple;

    public AnnotationContextMenu(Annotation annotation, List<DomainObject> domainObjectList, ImageModel<DomainObject, Reference> imageModel) {
        this.imageModel = imageModel;
        this.annotation = annotation;
        this.domainObjectList = domainObjectList;
        this.multiple = domainObjectList.size() > 1;
    }
    
    public void addMenuItems() {
        
        if (domainObjectList.isEmpty()) {
            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
            titleMenuItem.setEnabled(false);
            add(titleMenuItem);
            return;
        }

        OntologyTerm keyTerm = DomainMgr.getDomainMgr().getModel().getOntologyTermByReference(annotation.getKeyTerm());
        
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());
        add(getCopyAnnotationItem());
        add(getRemoveAnnotationItem());
        add(getEditAnnotationItem(keyTerm)); 
//        add(getViewDetailsItem()); 
    }

    protected JMenuItem getTitleItem() {
        String name = multiple ? "(Multiple selected)" : annotation.getName();
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }

    protected JMenuItem getCopyNameToClipboardItem() {
        if (multiple) return null;
        
        JMenuItem copyMenuItem = new JMenuItem("  Copy Name To Clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Transferable t = new StringSelection(annotation.getName());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getCopyIdToClipboardItem() {
        if (multiple) return null;
        
        JMenuItem copyMenuItem = new JMenuItem("  Copy GUID To Clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Transferable t = new StringSelection(annotation.getId().toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getCopyAnnotationItem() {
        if (multiple) return null;
        if (!SessionMgr.getSubjectKey().equals(annotation.getOwnerKey())) return null;
    
        JMenuItem deleteByTermItem = new JMenuItem("  Copy Annotation");
        deleteByTermItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                StateMgr.getStateMgr().setCurrentSelectedOntologyAnnotation(annotation);
            }
        });
        return deleteByTermItem;
    }

    protected JMenuItem getRemoveAnnotationItem() {
        if (!SessionMgr.getSubjectKey().equals(annotation.getOwnerKey())) return null;    
        final RemoveAnnotationsAction removeAction = new RemoveAnnotationsAction(imageModel, domainObjectList, annotation, true);
        return getNamedActionItem(removeAction);
    }

    protected JMenuItem getEditAnnotationItem(OntologyTerm keyTerm) {
        if (keyTerm==null) return null;
        if (keyTerm instanceof EnumText || keyTerm instanceof Text || keyTerm instanceof Interval) {
            final BulkEditAnnotationKeyValueAction bulkEditAction = new BulkEditAnnotationKeyValueAction(domainObjectList, annotation);
            return getNamedActionItem(bulkEditAction);
        }
        return null;
    }

    // TODO: port this
//    protected JMenuItem getViewDetailsItem() {
//        if (multiple) return null;
//        JMenuItem detailsItem = new JMenuItem("  View Details");
//        detailsItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//                OntologyOutline.viewAnnotationDetails(tag);
//            }
//        });
//        return detailsItem;
//    }
}
