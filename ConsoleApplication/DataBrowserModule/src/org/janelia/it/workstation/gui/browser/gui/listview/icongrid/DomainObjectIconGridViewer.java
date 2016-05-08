package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.actions.AnnotationContextMenu;
import org.janelia.it.workstation.gui.browser.actions.DomainObjectContextMenu;
import org.janelia.it.workstation.gui.browser.actions.RemoveItemsFromFolderAction;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.it.workstation.gui.browser.gui.hud.Hud;
import org.janelia.it.workstation.gui.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.it.workstation.gui.browser.gui.listview.AnnotatedDomainObjectListViewer;
import org.janelia.it.workstation.gui.browser.gui.support.ImageTypeSelectionButton;
import org.janelia.it.workstation.gui.browser.gui.support.ResultSelectionButton;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;
import org.janelia.it.workstation.gui.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An IconGridViewer implementation for viewing domain objects. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectIconGridViewer extends IconGridViewerPanel<DomainObject,Reference> implements AnnotatedDomainObjectListViewer {
    
    private static final Logger log = LoggerFactory.getLogger(DomainObjectIconGridViewer.class);

    private ResultSelectionButton resultButton;
    private ImageTypeSelectionButton typeButton;
    
    private AnnotatedDomainObjectList domainObjectList;
    private DomainObjectSelectionModel selectionModel;
    private SearchProvider searchProvider;
    
    private final ImageModel<DomainObject,Reference> imageModel = new ImageModel<DomainObject, Reference>() {
        
        @Override
        public Reference getImageUniqueId(DomainObject domainObject) {
            return Reference.createFor(domainObject);
        }

        @Override
        public String getImageFilepath(DomainObject domainObject) {
            HasFiles result = null;
            if (domainObject instanceof Sample) {
                Sample sample = (Sample)domainObject;
                result = DomainModelViewUtils.getResult(sample, resultButton.getResultDescriptor());
            }
            else if (domainObject instanceof HasFiles) {
                result = (HasFiles)domainObject;
            }
            return result==null? null : DomainUtils.getFilepath(result, typeButton.getImageType());
        }

        @Override
        public BufferedImage getStaticIcon(DomainObject imageObject) {
            String filename = "question_block_large.png";
            if (imageObject instanceof Filter) {
                filename = "search_large.png";
            }
            else if (imageObject instanceof TreeNode) {
                filename = "folder_large.png";
            }
            ImageIcon icon = Icons.getIcon(filename);
            if (icon==null) return null;
            return Utils.toBufferedImage(icon.getImage());
        }

        @Override
        public DomainObject getImageByUniqueId(Reference id) {
            return DomainMgr.getDomainMgr().getModel().getDomainObject(id);
        }
        
        @Override
        public String getImageLabel(DomainObject domainObject) {
            return domainObject.getName();
        }
        
        @Override
        public List<Annotation> getAnnotations(DomainObject domainObject) {
            return domainObjectList.getAnnotations(domainObject.getId());
        }
    };

    public DomainObjectIconGridViewer() {
        setImageModel(imageModel);
        resultButton = new ResultSelectionButton() {
            @Override
            protected void resultChanged(ResultDescriptor resultDescriptor) {
                log.info("Setting result preference: "+resultDescriptor.toString());
                setPreference(DomainConstants.PREFERENCE_CATEGORY_SAMPLE_RESULT, resultDescriptor.toString());
                typeButton.setResultDescriptor(resultDescriptor);
                typeButton.populate(domainObjectList.getDomainObjects());
            }
        };
        typeButton = new ImageTypeSelectionButton() {
            @Override
            protected void imageTypeChanged(String typeName) {
                log.info("Setting image type preference: "+typeName);
                setPreference(DomainConstants.PREFERENCE_CATEGORY_IMAGE_TYPE, typeName);
            }
        };
        getToolbar().addCustomComponent(resultButton);
        getToolbar().addCustomComponent(typeButton);
    }

    private void setPreference(final String name, final String value) {

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
                if (parentObject.getId()!=null) {
                    Preference preference = DomainMgr.getDomainMgr().getPreference(name, parentObject.getId().toString());
                    if (preference==null) {
                        preference = new Preference(AccessManager.getSubjectKey(), name, parentObject.getId().toString(), value);
                    }
                    else {
                        preference.setValue(value);
                    }
                    DomainMgr.getDomainMgr().savePreference(preference);
                }
            }

            @Override
            protected void hadSuccess() {
                showDomainObjects(domainObjectList, null);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
    }
    
    @Override
    public JPanel getPanel() {
        return this;
    }
    
    @Override
    public void setSelectionModel(DomainObjectSelectionModel selectionModel) {
        super.setSelectionModel(selectionModel);
        this.selectionModel = selectionModel;
    }
    
    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
    }
    
    @Override
    public void selectDomainObjects(List<DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven) {
        log.info("selectDomainObjects(domainObjects.size={},select={},clearAll={},isUserDriven={})",domainObjects.size(),select,clearAll,isUserDriven);
        
        if (domainObjects.isEmpty()) {
            return;
        }

        if (select) {
            selectObjects(domainObjects, clearAll, isUserDriven);
        }
        else {
            deselectObjects(domainObjects, isUserDriven);
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrollSelectedObjectsToCenter();
            }
        });   
    }

    @Override
    public void showDomainObjects(AnnotatedDomainObjectList objects, final Callable<Void> success) {

        this.domainObjectList = objects;
        log.debug("showDomainObjects(domainObjectList.size={})",domainObjectList.getDomainObjects().size());
        
        final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
        if (parentObject!=null && parentObject.getId()!=null) {
            Preference preference = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_SAMPLE_RESULT, parentObject.getId().toString());
            log.info("Got result preference: "+preference);
            if (preference!=null) {
                resultButton.setResultDescriptor(new ResultDescriptor(preference.getValue()));
            }
            Preference preference2 = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_IMAGE_TYPE, parentObject.getId().toString());
            log.info("Got image type preference: "+preference2);
            if (preference2!=null) {
                typeButton.setImageType(preference2.getValue());
            }
        }
        
        resultButton.populate(objects.getDomainObjects());
        typeButton.setResultDescriptor(resultButton.getResultDescriptor());
        typeButton.populate(domainObjectList.getDomainObjects());
                
        showObjects(domainObjectList.getDomainObjects(), success);
    }
    
    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }

    @Override
    public boolean matches(DomainObject domainObject, String text) {
        String name = getImageModel().getImageLabel(domainObject);
        return name.toUpperCase().contains(text.toUpperCase());
    }

    @Override
    public void refreshDomainObject(DomainObject domainObject) {
        refreshObject(domainObject);
    }

    @Override
    protected DomainObjectContextMenu getContextualPopupMenu() {
        DomainObjectContextMenu popupMenu = new DomainObjectContextMenu((DomainObject)selectionModel.getParentObject(), getSelectedObjects(), resultButton.getResultDescriptor(), typeButton.getImageType());
        popupMenu.addMenuItems();
        return popupMenu;
    }

    @Override
    protected JPopupMenu getAnnotationPopupMenu(Annotation annotation) {
        AnnotationContextMenu menu = new AnnotationContextMenu(annotation, getSelectedObjects(), imageModel);
        menu.addMenuItems();
        return menu;
    }

    @Override
    protected void moreAnnotationsButtonDoubleClicked(DomainObject domainObject) {
        new DomainDetailsDialog().showForDomainObject(domainObject, DomainInspectorPanel.TAB_NAME_ANNOTATIONS);
    }
    
    @Override
    protected void objectDoubleClick(DomainObject object) {
        getContextualPopupMenu().runDefaultAction();
    }
    
    @Override
    protected void deleteKeyPressed() {
        IsParent parent = selectionModel.getParentObject();
        if (parent instanceof TreeNode) {
            TreeNode treeNode = (TreeNode)parent; 
            if (ClientDomainUtils.hasWriteAccess(treeNode)) {                
                List<DomainObject> selectedObjects = DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
                RemoveItemsFromFolderAction action = new RemoveItemsFromFolderAction(treeNode, selectedObjects);
                action.doAction();
            }
        }
    }
    
    @Override
    protected void updateHud(boolean toggle) {

        Hud hud = Hud.getSingletonInstance();
        hud.setKeyListener(keyListener);
        
        List<DomainObject> selected = getSelectedObjects();
        
        if (selected.size() != 1) {
            hud.hideDialog();
            return;
        }
        
        DomainObject domainObject = selected.get(0);
        if (toggle) {
            hud.setObjectAndToggleDialog(domainObject, resultButton.getResultDescriptor(), typeButton.getImageType());
        }
        else {
            hud.setObject(domainObject, resultButton.getResultDescriptor(), typeButton.getImageType(), false);
        }
    }
    
    private List<DomainObject> getSelectedObjects() {
        return DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
    }

    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
    }

    public SearchProvider getSearchProvider() {
        return searchProvider;
    }
}
