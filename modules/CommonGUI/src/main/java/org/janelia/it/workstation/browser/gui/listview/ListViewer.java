package org.janelia.it.workstation.browser.gui.listview;

import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JPanel;

import org.janelia.it.workstation.browser.events.selection.ChildSelectionModel;
import org.janelia.it.workstation.browser.gui.model.ImageModel;
import org.janelia.it.workstation.browser.model.AnnotatedObjectList;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.gui.support.PreferenceSupport;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;

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
    void setSelectionModel(ChildSelectionModel<T,S> selectionModel);
    
    /**
     * Returns the current selection mode used in the list viewer. 
     * @return selection model
     */
    ChildSelectionModel<T,S> getSelectionModel();

    /**
     * Configure the preference support implementation to use for saving preferences.
     * @param preferenceSupport
     */
    void setPreferenceSupport(PreferenceSupport preferenceSupport);

    /**
     * Returns the preference support implementation to use for saving preferences.
     * The default implementation uses the selection model parent object as the preference category.
     * @return
     */
    PreferenceSupport getPreferenceSupport();

    /**
     * Configure the search provider for re-sorting, etc. 
     */
    void setSearchProvider(SearchProvider searchProvider);
    
    /**
     * Returns the current image model.
     * @return
     */
    ImageModel<T, S> getImageModel();
    
    /**
     * Configure the image model to use for showing objects in the list viewer.
     * @param imageModel
     */
    void setImageModel(ImageModel<T,S> imageModel);
    
    /**
     * Set a listener for actions from this list viewer.
     * @param listener
     */
    void setActionListener(ListViewerActionListener listener);

    /**
     * Show the objects in the list in the viewer, along with their annotations. 
     * @param domainObjectList 
     */
    void show(AnnotatedObjectList<T,S> domainObjectList, final Callable<Void> success);

    /**
     * Returns the number of items currently hidden by the viewer.
     * @return
     */
    int getNumItemsHidden();
    
    /**
     * Refresh the given domain object.
     * @param object updated domain object
     */
    void refresh(T object);
    
    /**
     * Tell the viewer that the selection should change.
     * @param select select if true, deselect if false
     * @param clearAll clear the existing selection before selecting?
     * @param isUserDriven is this action driven directly by the user?
     * @Param notifyModel should we notify the DomainSelectionModel?
     */
    void select(List<T> objects, boolean select, boolean clearAll, boolean isUserDriven, boolean notifyModel);

    /**
     * Check if the displayed text for the given object contains the given string.
     * @param resultPage page containing domainObject
     * @param text search string
     * @return true if the object contains the given text
     */
    boolean matches(ResultPage<T,S> resultPage, T object, String text);
    
    /**
     * Returns the actual GUI panel which implements the list viewer functionality.
     * @return a JPanel that can be added to a container for displaying the list
     */
    JPanel getPanel();

    /**
     * Show a loading indicator.
     */
    void showLoadingIndicator();
    
    /**
     * Called when the viewer acquires focus.
     */
    void activate();
    
    /**
     * Called when the viewer loses focus.
     */
    void deactivate();

    /**
     * Save the current state of the viewer.
     * @return the current state
     */
    ListViewerState saveState();

    /**
     * Restore the given state of the viewer.
     * @param viewerState state to restore
     */
    void restoreState(ListViewerState viewerState);

    /**
     * Configure the edit selection model to use in the viewer.
     * @param editSelectionModel selection model
     */
    void setEditSelectionModel(ChildSelectionModel<T, S> editSelectionModel);

    /**
     * Returns the current edit selections.
     * @return selection model
     */
    ChildSelectionModel<T, S> getEditSelectionModel();
    
    /**
     * Enable or disable edit mode, which allows a user to select items using checkboxes instead of 
     * button selection. This uses the editSelectionModel.
     */
    void toggleEditMode(boolean editMode);

    /**
     * Refresh edit mode selections.
     */
    void refreshEditMode();

    /**
     * Select or deselect a group of objects in edit mode. 
     * @param domainObjects list of domain objects for which to change edit selection
     * @param select select if true, deselect if false
     */
    void selectEditObjects(List<T> domainObjects, boolean select);
}
