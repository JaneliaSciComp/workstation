package org.janelia.it.workstation.gui.browser.components.viewer;

import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;

import javax.swing.JPanel;

/**
 * A viewer that can display an AnnotatedDomainObjectList.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface AnnotatedDomainObjectListViewer {

    public void showDomainObjects(AnnotatedDomainObjectList domainObjectList);

    public JPanel getViewerPanel();
    
    public String getSelectionCategory();
}
