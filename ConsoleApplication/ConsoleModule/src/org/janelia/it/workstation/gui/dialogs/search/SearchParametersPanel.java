package org.janelia.it.workstation.gui.dialogs.search;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.AbstractAction;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EtchedBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.solr.SolrQueryBuilder;
import org.janelia.it.jacs.shared.solr.SolrUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.dialogs.search.SearchAttribute.DataType;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;

/**
 * A reusable panel for defining general search parameters.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SearchParametersPanel extends JPanel implements SearchConfigurationListener {

    /**
     * Number of historical search terms in the drop down
     */
    protected static final int MAX_HISTORY_LENGTH = 10;

    // UI Elements
    protected final JLabel titleLabel;
    protected final JComboBox inputField;
    protected final JLabel titleLabel2;
    protected final JButton deleteContextButton;
    protected final JCheckBox advancedSearchCheckbox;
    protected final JPanel advancedSearchPanel;
    protected final JPanel adhocPanel;
    protected final JPanel criteriaPanel;
    protected final JButton searchButton;
    protected final JButton clearButton;
    
    // Search state
    protected SearchConfiguration searchConfig;
    protected Entity searchRoot;
    protected List<SearchCriteria> searchCriteriaList = new ArrayList<SearchCriteria>();
    protected String searchString = "";
      
    public SearchParametersPanel() {

        titleLabel = new JLabel("Search for ");
        titleLabel2 = new JLabel();
                
        inputField = new JComboBox();
        inputField.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));
        inputField.setEditable(true);
        inputField.setToolTipText("Enter search terms...");

        inputField.addPopupMenuListener(new PopupMenuListener() {

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

        AbstractAction mySearchAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearchInternal(true);
            }
        };
        searchButton = new JButton("Search");
        searchButton.addActionListener(mySearchAction);
        
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0,true), "enterAction");
        getActionMap().put("enterAction", mySearchAction);
        
        clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clear();
            }
        });

        deleteContextButton = new JButton(Icons.getIcon("close.png"));
        deleteContextButton.setBorderPainted(false);
        deleteContextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSearchRoot(null);
                performSearchInternal(false);
            }
        });

        JPanel searchBox = new JPanel();
        searchBox.setLayout(new BoxLayout(searchBox, BoxLayout.LINE_AXIS));
        searchBox.add(titleLabel);
        searchBox.add(inputField);
        searchBox.add(titleLabel2);
        searchBox.add(deleteContextButton);
        searchBox.add(Box.createHorizontalStrut(5));
        searchBox.add(searchButton);
        searchBox.add(clearButton);

        criteriaPanel = new JPanel();
        criteriaPanel.setLayout(new BoxLayout(criteriaPanel, BoxLayout.PAGE_AXIS));

        JButton addCriteriaButton = new JButton("Add search criteria", Icons.getIcon("add.png"));
        addCriteriaButton.setBorderPainted(false);
        addCriteriaButton.setBorder(BorderFactory.createEmptyBorder(10, 13, 10, 10));
        addCriteriaButton.setIconTextGap(20);
        addCriteriaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addSearchCriteria(true);
            }
        });

        adhocPanel = new JPanel();
        adhocPanel.setLayout(new BoxLayout(adhocPanel, BoxLayout.PAGE_AXIS));
        criteriaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        adhocPanel.add(criteriaPanel);
        addCriteriaButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        adhocPanel.add(addCriteriaButton);

        advancedSearchPanel = new JPanel();
        advancedSearchPanel.setMinimumSize(new Dimension(500, 200));
        advancedSearchPanel.setLayout(new BoxLayout(advancedSearchPanel, BoxLayout.PAGE_AXIS));
        advancedSearchPanel.setVisible(false);
        advancedSearchPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)));

        adhocPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        advancedSearchPanel.add(adhocPanel);

        advancedSearchCheckbox = new JCheckBox("Advanced search options");
        advancedSearchCheckbox.setEnabled(false);
        advancedSearchCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                advancedSearchPanel.setVisible(!advancedSearchPanel.isVisible());
                if (searchCriteriaList.isEmpty()) {
                    addSearchCriteria(true);
                }
            }
        });

        JButton infoButton = new JButton(Icons.getIcon("info.png"));
        infoButton.setBorderPainted(false);
        infoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO: make a custom help page later
                try {
                    Desktop.getDesktop().browse(new URI("http://lucene.apache.org/core/old_versioned_docs/versions/3_5_0/queryparsersyntax.html"));
                }
                catch (Exception ex) {
                    SessionMgr.getSessionMgr().handleException(ex);
                }
            }
        });

        setSearchRoot(null);
        
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(infoButton, BorderLayout.EAST);

        // Add everything to the content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.weightx = c.weighty = 1.0;
        contentPanel.add(searchBox, c);

        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = c.weighty = 1.0;
        contentPanel.add(advancedSearchCheckbox, c);

        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.FIRST_LINE_END;
        c.weightx = c.weighty = 1.0;
        JPanel advancedSearchHolder = new JPanel(new BorderLayout());
        advancedSearchHolder.add(advancedSearchPanel, BorderLayout.WEST);
        contentPanel.add(advancedSearchHolder, c);

        c.gridx = 2;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LAST_LINE_START;
        c.weightx = c.weighty = 1.0;
        contentPanel.add(infoPanel, c);

        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);
    }

    public void init(SearchConfiguration searchConfig) {
        if (searchConfig.isReady()) {
            this.searchConfig = searchConfig;
            advancedSearchCheckbox.setEnabled(true);
            revalidate();
        }
    }

    @Override
    public void configurationChange(SearchConfigurationEvent evt) {
        init(evt.getSearchConfig());
    }

    public SolrQueryBuilder getQueryBuilder() {
        SolrQueryBuilder builder = new SolrQueryBuilder();
        return getQueryBuilder(builder);
    }
    
    /**
     * Returns a query builder for the current search parameters.
     *
     * @return
     */
    public SolrQueryBuilder getQueryBuilder(SolrQueryBuilder builder) {

        this.searchString = getSearchString();

        for (String subjectKey : SessionMgr.getSubjectKeys()) {
            builder.addOwnerKey(subjectKey);
        }

        builder.setSearchString(searchString);

        if (searchRoot != null) {
            builder.setRootId(searchRoot.getId());
        }

        if (advancedSearchCheckbox.isSelected()) {

            StringBuilder aux = new StringBuilder();
            StringBuilder auxAnnot = new StringBuilder();

            for (SearchCriteria criteria : searchCriteriaList) {

                String value1 = null;
                String value2 = null;

                SearchAttribute sa = criteria.getAttribute();
                if (sa == null) {
                    continue;
                }

                if (sa.getDataType().equals(DataType.DATE)) {
                    Date startCal = (Date) criteria.getValue1();
                    Date endCal = (Date) criteria.getValue2();
                    value1 = startCal == null ? "*" : SolrUtils.formatDate(startCal);
                    value2 = endCal == null ? "*" : SolrUtils.formatDate(endCal);
                }
                else {
                    value1 = (String) criteria.getValue1();
                    value2 = (String) criteria.getValue2();
                }

                if (value1 == null && value2 == null) {
                    continue;
                }

                if ("annotations".equals(sa.getName())) {
                    if (auxAnnot.length()>1) {
                        auxAnnot.append(" ");
                    }
                    switch (criteria.getOp()) {
                        case NOT_NULL:
                            auxAnnot.append("*");
                            break;
                        default:
                            auxAnnot.append(value1);
                            break;
                    }
                    continue;
                }
                
                if (aux.length() > 0) {
                    aux.append(" ");
                }
                aux.append("+");
                aux.append(sa.getName());
                aux.append(":");

                switch (criteria.getOp()) {
                    case CONTAINS:
                        aux.append(value1);
                        break;
                    case BETWEEN:
                        aux.append("[");
                        aux.append(value1);
                        aux.append(" TO ");
                        aux.append(value2);
                        aux.append("]");
                        break;
                    case NOT_NULL:
                        aux.append("*");
                        break;
                }
            }

            builder.setAuxString(aux.toString());
            builder.setAuxAnnotationQueryString(auxAnnot.toString());
        }

        return builder;
    }
    
    /**
     * Override this method to provide custom search history persistence. The
     * global search history is used by default. 
     * @return Current search history. May be null or empty if there is no history.
     */
    protected List<String> getSearchHistory() {
        return SessionMgr.getBrowser().getSearchHistory();
    }
    
    /**
     * Override this method to provide custom search history persistence. The
     * global search history is used by default.
     * @param searchHistory The search history to persist. May be empty or null
     * if there is no history.
     */
    protected void setSearchHistory(List<String> searchHistory) {
        SessionMgr.getBrowser().setSearchHistory(searchHistory);
    }

    private void addCurrentSearchTermToHistory() {

        String searchString = getSearchString();
        if (StringUtils.isEmpty(searchString)) return;
        
        DefaultComboBoxModel model = (DefaultComboBoxModel) inputField.getModel();

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
        inputField.setSelectedItem(searchString);
        
        List<String> searchHistory = new ArrayList<String>();
        for (int i = 0; i < model.getSize(); i++) {
            searchHistory.add((String) model.getElementAt(i));
        }        
        
        setSearchHistory(searchHistory);
    }

    private void loadSearchHistory() {
        
        String searchString = getSearchString();
        
        List<String> searchHistory = getSearchHistory();
        
        DefaultComboBoxModel model = (DefaultComboBoxModel) inputField.getModel();
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
            inputField.setSelectedItem(searchString);
        }
        else {
            inputField.setSelectedItem("");
        }
    }

    private void addSearchCriteria(boolean enableDelete) {
        SearchCriteria searchCriteria = new SearchCriteria(searchConfig.getAttributes(), enableDelete) {
            @Override
            protected void removeSearchCriteria() {
                searchCriteriaList.remove(this);
                criteriaPanel.remove(this);
                adhocPanel.revalidate();
            }
        };
        searchCriteriaList.add(searchCriteria);
        criteriaPanel.add(searchCriteria);
        adhocPanel.revalidate();
    }

    public void setSearchRoot(Entity searchRoot) {
        this.searchRoot = searchRoot;

        if (searchRoot == null) {
            titleLabel2.setText(" in all data");
            deleteContextButton.setVisible(false);
        }
        else {
            titleLabel2.setText(" in " + searchRoot.getName());
            deleteContextButton.setVisible(true);
        }
    }
    
    public Entity getSearchRoot() {
        return searchRoot;
    }

    public List<SearchCriteria> getSearchCriteriaList() {
        return searchCriteriaList;
    }

    /**
     * The default implementation sets the current search string to the 
     * input field value every time this method is called. You can override
     * this method to provide alternate behavior, such as post-processing of the 
     * search string. 
     * @return 
     */
    public String getSearchString() {
        this.searchString = getInputFieldValue();
        if (searchString!=null) {
            this.searchString = searchString.trim();
        }
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }
    
    public String getInputFieldValue() {
        return (String)inputField.getSelectedItem();
    }
    
    public void setInputFieldValue(String searchString) {
        inputField.setSelectedItem(searchString);
    }
        
    public JLabel getTitleLabel() {
        return titleLabel;
    }

    public JLabel getTitleLabel2() {
        return titleLabel2;
    }

    public JComboBox getInputField() {
        return inputField;
    }

    public JButton getSearchButton() {
        return searchButton;
    }

    public JCheckBox getAdvancedSearchCheckbox() {
        return advancedSearchCheckbox;
    }

    public JPanel getAdhocPanel() {
        return adhocPanel;
    }

    public JPanel getCriteriaPanel() {
        return criteriaPanel;
    }

    private void clear() {
        searchString = "";
        inputField.setSelectedItem(searchString);
        setSearchRoot(null);
        searchCriteriaList.clear();
        criteriaPanel.removeAll();
        adhocPanel.revalidate();
        advancedSearchPanel.setVisible(false);
        advancedSearchCheckbox.setSelected(false);
    }

    private void performSearchInternal(boolean clear) {
        addCurrentSearchTermToHistory();
        performSearch(clear);
    }
    
    /**
     * Override this method to provide search behavior. 
     * @param clear
     */
    public void performSearch(boolean clear) {
        
    }
}
