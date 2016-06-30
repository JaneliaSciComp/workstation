package org.janelia.it.workstation.gui.browser.gui.support;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A search box that remembers its history based on a model property.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SmartSearchBox extends JComboBox {

    private static final Logger log = LoggerFactory.getLogger(SmartSearchBox.class);

    /**
     * Number of historical search terms in the drop down
     */
    private static final int MAX_HISTORY_LENGTH = 10;

    private final String modelPropertyName;

    public SmartSearchBox(String modelPropertyName) {

        this.modelPropertyName = modelPropertyName;

        setPreferredSize(new Dimension(200, 30));
        setEditable(true);
        setToolTipText("Enter search terms...");

        addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                loadSearchHistory();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }

    /**
     * The default implementation sets the current search string to the
     * input field value every time this method is called. You can override
     * this method to provide alternate behavior, such as post-processing of the
     * search string.
     * @return
     */
    public String getSearchString() {
        String searchString = (String)getSelectedItem();
        if (searchString!=null) {
            return searchString.trim();
        }
        return searchString;
    }

    /**
     * Set the search string.
     * @param searchString
     */
    public void setSearchString(String searchString) {
        setSelectedItem(searchString);
    }

    /**
     * Override this method to provide custom search history persistence. The
     * global search history is used by default.
     * @return Current search history. May be null or empty if there is no history.
     */
    protected List<String> getSearchHistory() {
        List<String> searchHistory = (List<String>) SessionMgr.getSessionMgr().getModelProperty(modelPropertyName);
        log.trace("Returning current search history: {} ",searchHistory);
        return searchHistory;
    }

    /**
     * Override this method to provide custom search history persistence. The
     * global search history is used by default.
     * @param searchHistory The search history to persist. May be empty or null
     * if there is no history.
     */
    protected void setSearchHistory(List<String> searchHistory) {
        log.trace("Saving search history: {} ",searchHistory);
        SessionMgr.getSessionMgr().setModelProperty(modelPropertyName, searchHistory);
    }

    public void addCurrentSearchTermToHistory() {

        String searchString = getSearchString();
        if (StringUtils.isEmpty(searchString)) return;

        DefaultComboBoxModel model = (DefaultComboBoxModel) getModel();

        // Trim history
        while (model.getSize() >= MAX_HISTORY_LENGTH) {
            model.removeElementAt(model.getSize() - 1);
        }

        // Remove any current instance of the search term
        int currIndex = model.getIndexOf(searchString);
        if (currIndex>=0) {
            model.removeElementAt(currIndex);
        }

        // Add it to the front
        model.insertElementAt(searchString, 0);
        setSelectedItem(searchString);

        List<String> searchHistory = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) {
            searchHistory.add((String) model.getElementAt(i));
        }

        setSearchHistory(searchHistory);
    }

    private void loadSearchHistory() {

        String searchString = getSearchString();

        List<String> searchHistory = getSearchHistory();

        DefaultComboBoxModel model = (DefaultComboBoxModel) getModel();
        model.removeAllElements();

        if (searchHistory == null || searchHistory.isEmpty()) {
            return;
        }

        boolean selectedInHistory = false;

        for (String s : searchHistory) {
            if (s.equals(searchString)) {
                selectedInHistory = true;
            }
            model.addElement(s);
        }

        if (!StringUtils.isEmpty(searchString)) {
            if (!selectedInHistory) {
                model.insertElementAt(searchString, 0);
            }
            setSelectedItem(searchString);
        }
        else {
            setSelectedItem("");
        }
    }

}
