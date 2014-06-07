package org.janelia.it.workstation.gui.dialogs.search;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EtchedBorder;

import org.janelia.it.workstation.gui.dialogs.search.SearchAttribute.DataType;
import org.janelia.it.jacs.compute.api.support.SolrQueryBuilder;
import org.janelia.it.jacs.shared.solr.SolrUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;

/**
 * A reusable panel for defining general search parameters. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SearchParametersPanel extends JPanel implements SearchConfigurationListener {

	/** Number of historical search terms in the drop down */
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
        
        searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				performSearch(true);
			}
		});

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
				performSearch(false);
			}
		});
        deleteContextButton.setVisible(false);
        
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
        addCriteriaButton.setBorder(BorderFactory.createEmptyBorder(10,13,10,10));
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
        				BorderFactory.createEmptyBorder(10,10,10,10), 
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
    
	/**
	 * Returns a query builder for the current search parameters.
	 * @return
	 */
	public SolrQueryBuilder getQueryBuilder() {

		searchString = (String)inputField.getSelectedItem();
		
		SolrQueryBuilder builder = new SolrQueryBuilder();
		for(String subjectKey : SessionMgr.getSubjectKeys()) {
			builder.addOwnerKey(subjectKey);
		}
		
		builder.setSearchString(searchString);
		
		if (searchRoot!=null) {
			builder.setRootId(searchRoot.getId());
		}
		
    	if (advancedSearchCheckbox.isSelected()) {
    		
    		StringBuilder aux = new StringBuilder();
    		
    		for(SearchCriteria criteria : searchCriteriaList) {
    			
    			String value1 = null;
    			String value2 = null;
    			
    			SearchAttribute sa = criteria.getAttribute();
    			if (sa==null) continue;
    			
    			if (sa.getDataType().equals(DataType.DATE)) {
    				Calendar startCal = (Calendar)criteria.getValue1();
    				Calendar endCal = (Calendar)criteria.getValue2();
	    			value1 = startCal==null?"*":SolrUtils.formatDate(startCal.getTime());
	    			value2 =  endCal==null?"*":SolrUtils.formatDate(endCal.getTime());
    			}
    			else {
    				value1 = (String)criteria.getValue1();
    				value2 = (String)criteria.getValue2();
    			}

    			if (value1==null&&value2==null) continue;
    			
    			if (aux.length()>0) aux.append(" ");
    			aux.append("+");
    			aux.append(sa.getName());
    			aux.append(":");
    			
    			switch(criteria.getOp()) {
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
    	}
    	
    	return builder;
	}
	
	protected void populateHistory() {
		
		DefaultComboBoxModel model = (DefaultComboBoxModel)inputField.getModel();
		
		// Check if the current search term is already recorded in the history
		if (model.getSize()>0 && model.getElementAt(0)!=null && model.getElementAt(0).equals(searchString)) return;
		
		// Update the model
    	while (model.getSize()>=MAX_HISTORY_LENGTH) {
    		model.removeElementAt(model.getSize()-1);
    	}
    	model.insertElementAt(searchString, 0);
    }

	public void setSearchHistory(List<String> searchHistory) {
		if (searchHistory==null) return;
		DefaultComboBoxModel model = (DefaultComboBoxModel)inputField.getModel();
		model.removeAllElements();
		for(String s : searchHistory) {
			model.addElement(s);
		}
		inputField.setSelectedItem("");
	}
	
	public List<String> getSearchHistory() {
		DefaultComboBoxModel model = (DefaultComboBoxModel)inputField.getModel();
		List<String> searchHistory = new ArrayList<String>();
		for(int i=0; i<model.getSize(); i++) {
			searchHistory.add((String)model.getElementAt(i));
		}
		return searchHistory;
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
			titleLabel2.setText(" in "+searchRoot.getName());
			deleteContextButton.setVisible(true);
		}
	}

	public Entity getSearchRoot() {
		return searchRoot;
	}

	public List<SearchCriteria> getSearchCriteriaList() {
		return searchCriteriaList;
	}

	public String getSearchString() {
		return searchString;
	}

	public void setSearchString(String searchString) {
	    this.searchString = searchString;
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
	
	public void performSearch(boolean clear) {
		populateHistory();
	}
}
