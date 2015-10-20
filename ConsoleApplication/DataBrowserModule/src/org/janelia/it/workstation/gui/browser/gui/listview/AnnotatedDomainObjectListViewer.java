package org.janelia.it.workstation.gui.browser.gui.listview;

import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;

import javax.swing.JPanel;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;

/**
 * An interface for a viewer that can display an AnnotatedDomainObjectList.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface AnnotatedDomainObjectListViewer {

    /**
     * Show the objects in the list in the viewer, along with their annotations. 
     * @param domainObjectList 
     */
    public void showDomainObjects(AnnotatedDomainObjectList domainObjectList);

    /**
     * Refresh the given domain object as the metadata in the AnnotatedDomainObjectList may have changed. 
     */
    public void refreshDomainObject(DomainObject domainObject);
    
    /**
     * Returns the actual GUI panel which implements the list viewer functionality.
     * @return a JPanel that can be added to a container for displaying the list
     */
    public JPanel getPanel();

    /**
     * Configure the selection model to use in the list viewer. 
     * @param selectionModel 
     */
    public void setSelectionModel(DomainObjectSelectionModel selectionModel);
    
    /**
     * Returns the current selection mode used in the list viewer. 
     * @return Selection Model
     */
    public DomainObjectSelectionModel getSelectionModel();
    
    /**
     * Tell the viewer that the selection should change.
     * @param domainObject
     * @param select
     * @param clearAll 
     */
    public void selectDomainObject(DomainObject domainObject, boolean select, boolean clearAll);
}
