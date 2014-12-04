package org.janelia.it.workstation.gui.browser.icongrid.node;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.gui.search.SavedSearch;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.workstation.gui.browser.icongrid.AnnotatedImageButton;
import org.janelia.it.workstation.gui.browser.icongrid.IconGridViewer;
import org.janelia.it.workstation.gui.browser.search.SearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectIconGridViewer extends IconGridViewer<DomainObject> {
    
    private static final Logger log = LoggerFactory.getLogger(DomainObjectIconGridViewer.class);
    
    protected AtomicBoolean childrenLoadInProgress = new AtomicBoolean(false);
    
    private DomainObject contextObject;
    
    @Override
    protected void populateImageRoles(List<DomainObject> domainObjects) {
        Set<String> imageRoles = new HashSet<String>();
        for(DomainObject domainObject : domainObjects) {
            if (domainObject instanceof HasFiles) {
                HasFiles hasFiles = (HasFiles)domainObject;
                for(FileType fileType : hasFiles.getFiles().keySet()) {
                    if (fileType.isIs2dImage()) {
                        imageRoles.add(fileType.name());
                    }
                }
            }
        }
        allImageRoles.clear();
        allImageRoles.addAll(imageRoles);
        Collections.sort(allImageRoles);
    }

    @Override
    public Object getImageUniqueId(DomainObject domainObject) {
        return domainObject.getId();
    }

    @Override
    public String getImageFilepath(DomainObject domainObject) {
        return getImageFilepath(domainObject, FileType.SignalMip.toString());
    }

    @Override
    public String getImageFilepath(DomainObject domainObject, String role) {
//        if (domainObject instanceof Has2dRepresentation) {
//            Has2dRepresentation node2d = (Has2dRepresentation)node;
//            return node2d.get2dImageFilepath(role);
//        }
        // TODO: implement this
        return null;
    }

    @Override
    public Object getImageLabel(DomainObject domainObject) {
        return domainObject.getName();
    }
    
    @Override
    protected void buttonDrillDown(AnnotatedImageButton button) {
        
        DomainObject domainObject = (DomainObject)button.getImageObject();
    }

    
    Set<DomainObject> selected = new HashSet<DomainObject>();
    private DomainObject lastSelected = null;
    
    @Override
    protected void buttonSelection(AnnotatedImageButton button, boolean multiSelect, boolean rangeSelect) {
        
        DomainObject imageObject = (DomainObject)button.getImageObject();
        
        selectionButtonContainer.setVisible(false);
                
        if (multiSelect) {
            // With the meta key we toggle items in the current
            // selection without clearing it
            if (!button.isSelected()) {
                selected.add(imageObject);
                lastSelected = imageObject;
            }
            else {
                selected.remove(imageObject);
                lastSelected = null;
            }
        }
        else {        
            // With shift, we select ranges
            if (rangeSelect && lastSelected != null) {
                // Walk through the buttons and select everything between the last and current selections
                boolean selecting = false;
                List<DomainObject> imageObjects = getPageImageObjects();
                for (DomainObject otherImageObject : imageObjects) {
                    if (otherImageObject.equals(lastSelected) || otherImageObject.equals(imageObject)) {
                        if (otherImageObject.equals(imageObject)) {
                            // Always select the button that was clicked
                            selected.add(otherImageObject);
                        }
                        if (selecting) {
                            break;
                        }
                        selecting = true; // Start selecting
                        continue; // Skip selection of the first and last items, which should already be selected
                    }
                    if (selecting) {
                        selected.add(otherImageObject);
                    }
                }
            }
            else {
                selected.clear();
                selected.add(imageObject);
            }
            lastSelected = imageObject;
        }
        
//        try {
//            manager.setSelectedNodes(selected.toArray(new Node[selected.size()]));
//        }
//        catch (PropertyVetoException e) {
//            log.warn("Could not select node", e);
//        }

        button.requestFocus();
    }
    
    private SimpleWorker childLoadingWorker;
    
    public synchronized void loadDomainObjects() {
        
        childrenLoadInProgress.set(true);

        // Indicate a load
        showLoadingIndicator();
        
        // Cancel previous loads
        imagesPanel.cancelAllLoads();
        if (childLoadingWorker != null && !childLoadingWorker.isDone()) {
            childLoadingWorker.disregard();
        }
        
        childLoadingWorker = new SimpleWorker() {

            List<DomainObject> domainObjects;
            
            @Override
            protected void doStuff() throws Exception {
                
                
                
                
                
            }

            @Override
            protected void hadSuccess() {
                
                if (domainObjects==null) {
                    setContextObject(null);
                    clear();
                    return;
                }
                else {
                }
            
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        
        childLoadingWorker.execute();
    }
    
    @Override
    public DomainObject getContextObject() {
        return contextObject;
    }

    @Override
    public void setContextObject(DomainObject contextObject) {
        this.contextObject = contextObject;
    }
    
    private SearchResults searchResults;
    
    public void showSearchResults(SearchResults searchResults) {
        if (this.searchResults == searchResults) {
            // Already displaying these results, might have received more pages
            
        }
        else {
            this.searchResults = searchResults;
        }
        
        
        
        
    }
    
}
