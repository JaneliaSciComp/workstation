package org.janelia.it.workstation.browser.gui.listview;

import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JPanel;

import org.janelia.it.workstation.browser.events.selection.ChildSelectionModel;
import org.janelia.it.workstation.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.browser.gui.support.PreferenceSupport;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.browser.model.AnnotatedObjectList;
import org.janelia.it.workstation.browser.model.search.ResultPage;

/**
 * An interface for the a viewer of a list of objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ListViewer<T,S> {

    /**
     * Configure the selection model to use in the list viewer. 
     * @param selectionModel selection model
     */
    public void setSelectionModel(ChildSelectionModel<T,S> selectionModel);
    
    /**
     * Returns the current selection mode used in the list viewer. 
     * @return selection model
     */
    public ChildSelectionModel<T,S> getSelectionModel();

    /**
     * Configure the preference support implementation to use for saving preferences.
     * @param preferenceSupport
     */
    public void setPreferenceSupport(PreferenceSupport preferenceSupport);

    /**
     * Returns the preference support implementation to use for saving preferences.
     * The default implementation uses the selection model parent object as the preference category.
     * @return
     */
    public PreferenceSupport getPreferenceSupport();

    /**
     * Configure the search provider for re-sorting, etc. 
     */
    public void setSearchProvider(SearchProvider searchProvider);
    
    /**
     * Returns the current image model.
     * @return
     */
    public ImageModel<T, S> getImageModel();
    
    /**
     * Configure the image model to use for showing objects in the list viewer.
     * @param imageModel
     */
    public void setImageModel(ImageModel<T,S> imageModel);
    
    /**
     * Set a listener for actions from this list viewer.
     * @param listener
     */
    public void setActionListener(ListViewerActionListener listener);

    /**
     * Show the objects in the list in the viewer, along with their annotations. 
     * @param domainObjectList 
     */
    public void show(AnnotatedObjectList<T,S> domainObjectList, final Callable<Void> success);

    /**
     * Returns the number of items currently hidden by the viewer.
     * @return
     */
    public int getNumItemsHidden();
    
    /**
     * Refresh the given domain object.
     * @param domainObject updated domain object
     */
    public void refresh(T object);
    
    /**
     * Tell the viewer that the selection should change.
     * @param domainObjects list of domain objects for which to change selection
     * @param select select if true, deselect if false
     * @param clearAll clear the existing selection before selecting?
     * @param isUserDriven is this action driven directly by the user?
     * @Param notifyModel should we notify the DomainSelectionModel?
     */
    public void select(List<T> objects, boolean select, boolean clearAll, boolean isUserDriven, boolean notifyModel);

    /**
     * Check if the displayed text for the given object contains the given string.
     * @param resultPage page containing domainObject
     * @param domainObject a domain object currently being displayed by the viewer
     * @param text search string
     * @return true if the object contains the given text
     */
    public boolean matches(ResultPage<T,S> resultPage, T object, String text);
    
    /**
     * Returns the actual GUI panel which implements the list viewer functionality.
     * @return a JPanel that can be added to a container for displaying the list
     */
    public JPanel getPanel();

    /**
     * Show a loading indicator.
     */
    public void showLoadingIndicator();
    
    /**
     * Called when the viewer acquires focus.
     */
    public void activate();
    
    /**
     * Called when the viewer loses focus.
     */
    public void deactivate();

    /**
     * Save the current state of the viewer.
     * @return the current state
     */
    public ListViewerState saveState();

    /**
     * Restore the given state of the viewer.
     * @param viewerState state to restore
     */
    public void restoreState(ListViewerState viewerState);

    /**
     * Configure the edit selection model to use in the viewer.
     * @param editSelectionModel selection model
     */
    public void setEditSelectionModel(ChildSelectionModel<T, S> editSelectionModel);

    /**
     * Returns the current edit selections.
     * @return selection model
     */
    public ChildSelectionModel<T, S> getEditSelectionModel();
    
    /**
     * Called on the specific viewer to toggle edit mode
     */
    public void toggleEditMode(boolean editMode);

    /**
     * Used when you have to perform two UI actions sequentially in the same thread
     * TODO: provide a mechanism for consistently executing UI callback queues, ala Javascript
     */
    public void refreshEditMode();

    /**
     * subgroup of edit items to toggle selection; might want to consider swapping selection model to use checkboxes instead
     * @param domainObjects list of domain objects for which to change edit selection
     * @param select select if true, deselect if false
     */
    public void selectEditObjects(List<T> domainObjects, boolean select);
}
