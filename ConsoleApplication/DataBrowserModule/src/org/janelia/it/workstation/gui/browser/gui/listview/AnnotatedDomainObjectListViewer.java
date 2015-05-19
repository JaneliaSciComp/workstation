package org.janelia.it.workstation.gui.browser.gui.listview;

import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;

import javax.swing.JPanel;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;

/**
 * A viewer that can display an AnnotatedDomainObjectList.
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
}
