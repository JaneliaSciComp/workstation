package org.janelia.it.workstation.gui.browser.gui.listview;

import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JPanel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;

/**
 * An interface for a viewer that can display an AnnotatedDomainObjectList.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface AnnotatedDomainObjectListViewer {

    /**
     * Returns the actual GUI panel which implements the list viewer functionality.
     * @return a JPanel that can be added to a container for displaying the list
     */
    public JPanel getPanel();

    /**
     * Configure the search provider for re-sorting, etc. 
     */
    public void setSearchProvider(SearchProvider searchProvider);
    
    /**
     * Configure the selection model to use in the list viewer. 
     * @param selectionModel selection model
     */
    public void setSelectionModel(DomainObjectSelectionModel selectionModel);
    
    /**
     * Returns the current selection mode used in the list viewer. 
     * @return selection model
     */
    public DomainObjectSelectionModel getSelectionModel();

    /**
     * Tell the viewer that the selection should change.
     * @param domainObjects list of domain objects for which to change selection
     * @param select select if true, deselect if false
     * @param clearAll clear the existing selection before selecting?
     * @param isUserDriven is this action driven directly by the user?
     */
    public void selectDomainObjects(List<DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven);
    
    /**
     * Show the objects in the list in the viewer, along with their annotations. 
     * @param domainObjectList 
     */
    public void showDomainObjects(AnnotatedDomainObjectList domainObjectList, final Callable<Void> success);

    /**
     * Refresh the given domain object.
     * @param domainObject updated domain object
     */
    public void refreshDomainObject(DomainObject domainObject);

    /**
     * Called when the viewer acquires focus.
     */
    public void activate();
    
    /**
     * Called when the viewer loses focus.
     */
    public void deactivate();

    /**
     * Check if the displayed text for the given object contains the given string.
     * @param resultPage page containing domainObject
     * @param domainObject a domain object currently being displayed by the viewer
     * @param text search string
     * @return true if the object contains the given text
     */
    public boolean matches(ResultPage resultPage, DomainObject domainObject, String text);
}
