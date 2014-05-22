package org.janelia.it.workstation.gui.framework.navigation_tools;

/**
 * Title:        Genome Browser Client
 * Description:  This project is for JBuilder 4.0
 * @author
 * @version $Id: SearchManager.java,v 1.2 2011/03/08 16:16:49 saffordt Exp $
 */

import org.janelia.it.workstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.workstation.shared.util.text_component.StandardTextField;
import org.janelia.it.jacs.model.entity.Entity;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Keymap;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class SearchManager {
  private String STATUS = "Status: ";
  private String READY  = "Ready";
  private static SearchManager searchManager = new SearchManager();
  /**
   * This session persistent generic property exists to order the last three search types.
   */
  public static final String FREQUENT_SEARCH_TYPES = "FrequentSearchTypes";
  public static final String FOCUS_SUBVIEWS_UPON_NAVIGATION = "FocusSubviewsUponNavigation";

  // Dialog Stuff
  JDialog userDialog;
  JPanel mainPanel = new JPanel();
  JPanel searchPanel = new JPanel();
  Border border1;
  JPanel resultsPanel = new JPanel();
  JLabel searchLabel = new JLabel();
  JRadioButton currentGVRadioButton = new JRadioButton();
  JRadioButton loadedGVRadioButton = new JRadioButton();
  JRadioButton availableGVRadioButton = new JRadioButton();
  JLabel typeLabel = new JLabel();
  JComboBox typeComboBox = new JComboBox();
  JLabel findLabel = new JLabel();
  StandardTextField findTextField = new StandardTextField();
  JButton searchButton = new JButton();
  JButton stopButton = new JButton();
  JLabel statusLabel = new JLabel();
  ButtonGroup searchButtonGroup = new ButtonGroup();
  JLabel resultsLabel = new JLabel();
  JScrollPane resultsScrollPane = new JScrollPane();
  JList resultsList = new JList();
  JButton navigateButton = new JButton();
  JButton closeButton = new JButton();

  private org.janelia.it.workstation.api.stub.data.ControlledVocabulary searchTypeControlledVocabulary;
  private org.janelia.it.workstation.gui.framework.console.Browser browser;
  private JFrame mainFrame;
//  private GenomeVersion currentGenomeVersion;
  private static final String lineSep=System.getProperty("line.separator");
  private String searchType = "";
  private String searchString = "";
  private MyBrowserModelListener browserModelListener = new MyBrowserModelListener();
  private SessionModelListener sessionModelListener = new MySessionModelListener();
  private MyModelMgrObserver myModelMgrObserver = new MyModelMgrObserver();
  private MyWindowListener myWindowListener = new MyWindowListener();
  private MyBrowserModelCurrentSelectionListener browserCurrentSelectionListener = new MyBrowserModelCurrentSelectionListener();
  private boolean listenersSet = false;
  ArrayList navPaths = new ArrayList();
  JCheckBox newBrowserCheckBox = new JCheckBox();
  JCheckBox subviewFocusCheckBox = new JCheckBox();


  //Modified the text field so that it will be able to trigger the default button
  static {
   StandardTextField f = new StandardTextField();
   KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
   Keymap map = f.getKeymap();
   map.removeKeyStrokeBinding(enter);
  }

  private SearchManager() {
    try  {
      mainFrame = org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getMainFrame();
      browser = org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getBrowser();
      userDialog = new JDialog(mainFrame,"Search Known Features", false);
      userDialog.addWindowListener(myWindowListener);
      browser.getBrowserModel().addBrowserModelListener(browserModelListener);
      org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().addSessionModelListener(sessionModelListener);
      jbInit();

      /**
       * Set up the search type combo box. Add the user list to the search types
       * if they are still valid, then add any new searches to the list.
       */
//      searchTypeControlledVocabulary = GenomeVersion.getNavigationSearchTypes();
      ArrayList frequentSearchList = (ArrayList) org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().getModelProperty(FREQUENT_SEARCH_TYPES);
      ArrayList newSearchList = new ArrayList(searchTypeControlledVocabulary.getNames());

      if (frequentSearchList != null) {
        for (Iterator it = frequentSearchList.iterator(); it.hasNext();) {
          String tmpType = (String)it.next();
          // Only add search type to list if it is still a valid
          if (newSearchList.contains(tmpType)) typeComboBox.addItem(tmpType);
        }
        for (Iterator it = newSearchList.iterator(); it.hasNext();) {
          String tmpType = (String)it.next();
          //  Add any new search types to the list
          if (!frequentSearchList.contains(tmpType)) typeComboBox.addItem(tmpType);
        }
      }
      else {
        for (Iterator it = newSearchList.iterator(); it.hasNext();) {
          typeComboBox.addItem(it.next());
        }
      }

      if (org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().getModelProperty(FOCUS_SUBVIEWS_UPON_NAVIGATION)==null) {
        org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().setModelProperty(FOCUS_SUBVIEWS_UPON_NAVIGATION, Boolean.TRUE);
      }
      else {
        boolean tmpBoolean = ((Boolean)
          org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().getModelProperty(FOCUS_SUBVIEWS_UPON_NAVIGATION)).booleanValue();
        subviewFocusCheckBox.setSelected(tmpBoolean);
      }
    }

    catch(Exception ex) {
     ex.printStackTrace();
    }
  }

  public JDialog getSearchDialog() { return userDialog; }
  public static SearchManager getSearchManager() { return searchManager; }

  public void launchSearch(String searchType, String searchString) {
    if (searchType==null) {
      JOptionPane.showMessageDialog(mainFrame, "Requested search type does not exist.", "Search Error",
        JOptionPane.WARNING_MESSAGE);
      return;
    }
    findTextField.setText("");

    this.searchType = searchType;
    this.searchString = searchString;
    findTextField.setText(searchString);

    typeComboBox.setSelectedItem(searchType);
    showSearchDialog(browser, true);
  }

  public void showSearchDialog(org.janelia.it.workstation.gui.framework.console.Browser browser, boolean autoSearch) {
//    Set selectedGenomeVersions= ModelMgr.getModelMgr().getSelectedGenomeVersions();
//    Set allGenomeVersions=ModelMgr.getModelMgr().getAvailableGenomeVersions();
    Entity entity = browser.getBrowserModel().getCurrentSelection();
//    if (entity!=null) currentGenomeVersion = entity.getGenomeVersion();
    this.browser=browser;
    // Since this method gets called so many times, we only want the listeners added once.
    if (!listenersSet) {
      browser.getBrowserModel().addBrowserModelListener(browserCurrentSelectionListener, false);
      org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().addModelMgrObserver(myModelMgrObserver);
      listenersSet = true;
    }

    // Set up radio button search choices.
//    if (currentGenomeVersion!=null) {
//      currentGVRadioButton.setSelected(true);
//      currentGVRadioButton.setEnabled(true);
//      currentGVRadioButton.setText(currentGenomeVersion.getDescription());
//    }
//    else {
      availableGVRadioButton.setSelected(true);
      availableGVRadioButton.setEnabled(true);
      currentGVRadioButton.setEnabled(false);
//    }
//    if (selectedGenomeVersions.size()>=1 && currentGenomeVersion!=null) {
//      loadedGVRadioButton.setEnabled(true);
//    }
//    else {
      loadedGVRadioButton.setEnabled(false);
//    }
//    loadedGVRadioButton.setText(selectedGenomeVersions.size()+" Loaded Genome Version(s)");
//    availableGVRadioButton.setText(allGenomeVersions.size()+
//      " Available Genome Version(s)");
    if (autoSearch) search();
    userDialog.setVisible(true);
    userDialog.toFront();
  }

  void jbInit() throws Exception {
    statusLabel.setText(STATUS+READY);
    resultsList.setMinimumSize(new Dimension(0,0));
    resultsList.setCellRenderer(new MyListCellRenderer());
    resultsList.setFixedCellHeight(20);
    resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    searchLabel.setText("Search:");
    searchLabel.setBounds(new Rectangle(7, 6, 73, 17));
    border1 = BorderFactory.createEtchedBorder(Color.white,new Color(149, 142, 130));
    resultsPanel.setBorder(border1);
    resultsPanel.setBounds(new Rectangle(6, 226, 260, 223));
    resultsPanel.setLayout(null);
    searchPanel.setBorder(border1);
    searchPanel.setBounds(new Rectangle(6, 6, 260, 214));
    searchPanel.setLayout(null);
    mainPanel.setLayout(null);

    currentGVRadioButton.setText("Current Genome Version");
    currentGVRadioButton.setBounds(new Rectangle(7, 21, 244, 24));
    loadedGVRadioButton.setBounds(new Rectangle(7, 43, 215, 24));
    loadedGVRadioButton.setText("Loaded Genome Version(s)");
    availableGVRadioButton.setText("Available Genome Version(s)");
    availableGVRadioButton.setBounds(new Rectangle(7, 66, 225, 24));
    newBrowserCheckBox.setText("Open New Browser");
    newBrowserCheckBox.setBounds(new Rectangle(7, 131, 139, 18));
    subviewFocusCheckBox.setText("Focus SubViews Upon Navigation");
    subviewFocusCheckBox.setBounds(new Rectangle(25, 199, 222, 19));
    subviewFocusCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().setModelProperty(FOCUS_SUBVIEWS_UPON_NAVIGATION,
          new Boolean(subviewFocusCheckBox.isSelected()));
      }
    });

    searchButtonGroup.add(currentGVRadioButton);
    searchButtonGroup.add(loadedGVRadioButton);
    searchButtonGroup.add(availableGVRadioButton);

    typeLabel.setText("Type:");
    typeLabel.setBounds(new Rectangle(7, 97, 38, 24));
    typeComboBox.setBounds(new Rectangle(46, 99, 203, 21));

    findLabel.setBounds(new Rectangle(7, 128, 36, 24));
    findLabel.setText("Find:");
    findTextField.setBounds(new Rectangle(46, 127, 203, 21));
    findTextField.getDocument().addDocumentListener(new DocumentListener(){
        public void insertUpdate(DocumentEvent e){
          if (!searchButton.isEnabled()) {
             searchButton.setEnabled(true);
             userDialog.getRootPane().setDefaultButton(searchButton);
          }
        }
        public void removeUpdate(DocumentEvent e){
          if (e.getDocument().getLength()==0) {
           searchButton.setEnabled(false);
           userDialog.getRootPane().setDefaultButton(stopButton);
          }
        }
        public void changedUpdate(DocumentEvent e){}
    });
    if (searchString!=null) findTextField.setText(searchString);

    searchButton.setText("Search");
    searchButton.setBounds(new Rectangle(82, 158, 95, 28));
    searchButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        search();
      }
    });
    searchButton.setEnabled(false);
    searchButton.setDefaultCapable(true);

    userDialog.getRootPane().setDefaultButton(stopButton);
    stopButton.setBounds(new Rectangle(134, 162, 95, 28));
    stopButton.setText("Stop");
    stopButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        stopNavigationButton_actionPerformed(e);
      }
    });
    stopButton.setDefaultCapable(true);

    statusLabel.setText("Status:");
    statusLabel.setBounds(new Rectangle(7, 192, 219, 19));
    resultsLabel.setText("Results:");
    resultsLabel.setBounds(new Rectangle(7, 2, 61, 21));
    resultsScrollPane.setBounds(new Rectangle(8, 23, 242, 97));

    navigateButton.setBounds(new Rectangle(25, 162, 95, 28));
    navigateButton.setText("Navigate");
    userDialog.getRootPane().setDefaultButton(navigateButton);
    navigateButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        navigateButton_actionPerformed(e);
      }
    });
    navigateButton.setDefaultCapable(true);

    closeButton.setText("Close");
    closeButton.setBounds(new Rectangle(94, 456, 73, 27));
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        closeButton_actionPerformed(e);
      }
    });

    mainPanel.add(searchPanel, null);
    searchPanel.add(currentGVRadioButton, null);
    searchPanel.add(loadedGVRadioButton, null);
    searchPanel.add(availableGVRadioButton, null);
    searchPanel.add(typeLabel, null);
    searchPanel.add(typeComboBox, null);
    searchPanel.add(findLabel, null);
    searchPanel.add(findTextField, null);
    searchPanel.add(searchLabel, null);
    searchPanel.add(searchButton, null);
    searchPanel.add(statusLabel, null);
    if (browser!=null) {
      resultsPanel.add(newBrowserCheckBox, null);
    }
    mainPanel.add(resultsPanel, null);
    resultsPanel.add(resultsLabel, null);
    resultsPanel.add(resultsScrollPane, null);
    resultsPanel.add(newBrowserCheckBox, null);
    resultsPanel.add(subviewFocusCheckBox, null);
    resultsPanel.add(stopButton, null);
    resultsPanel.add(navigateButton, null);
    mainPanel.add(closeButton, null);
    resultsScrollPane.getViewport().add(resultsList);
    mainPanel.setPreferredSize(new Dimension(272, 512));
    mainPanel.setMinimumSize(new Dimension(272, 512));
    mainPanel.setSize(272, 512);
    userDialog.getContentPane().setLayout(new BorderLayout());
    userDialog.getContentPane().add(mainPanel, BorderLayout.CENTER);
    userDialog.setSize(280, 520);
    userDialog.setLocation(10,10);
  }


  private String getSearchType() {
     return searchTypeControlledVocabulary.reverseLookup((String)typeComboBox.getSelectedItem());
  }

  private void addSearchResults(NavigationPath[] newNavPaths) {
    if (newNavPaths==null || newNavPaths.length==0) {
      JOptionPane.showMessageDialog(SearchManager.getSearchManager().getSearchDialog(),
        "Could not find anything to match "+getSearchString()+" in the selected Genome Version(s)",
        "Not Found", JOptionPane.INFORMATION_MESSAGE);
      findTextField.requestFocus();
      findTextField.setCaretPosition(0);
      findTextField.moveCaretPosition(findTextField.getDocument().getLength());
      return;
    }

    ArrayList tmpPathList=new ArrayList(newNavPaths.length);
    for (int i=0;i<newNavPaths.length;i++) {
       String targetPath = newNavPaths[i].getDisplayName();
       NavigationNode[] nodes=newNavPaths[i].getNavigationNodeArray();
       for (int j=0;j<nodes.length;j++) {
//         if (nodes[j].getNodeType()==NavigationNode.GENOME ||
//             nodes[j].getNodeType()==NavigationNode.AXIS ) {
//                targetPath=targetPath+" : "+nodes[j].getDisplayname();
//         }
       }
       tmpPathList.add(newNavPaths[i]);
       newNavPaths[i].setDisplayName(targetPath);
    }
    this.navPaths.addAll(tmpPathList);
    Collections.sort(navPaths);
    Collections.reverse(navPaths);
    resultsList.removeAll();
    resultsList.setListData(navPaths.toArray());
    resultsList.setSelectedIndex(0);
	resultsList.repaint();
  }

  private String getSearchString() {
     return findTextField.getText().trim();
  }

  private void search() {
    /**
     * Reset the combo box order to reflect the frequently used search.
     */
    searchType = (String)typeComboBox.getSelectedItem();
    ArrayList newList = new ArrayList();
    ArrayList tmpList = new ArrayList();
    //  Sun should have provided me a method to get a collection of items in the data model!!!!
    for (int x=0; x<typeComboBox.getItemCount(); x++) {
      tmpList.add(typeComboBox.getItemAt(x));
    }
    typeComboBox.removeAllItems();
    newList.add(searchType);
    typeComboBox.addItem(searchType);
    for (Iterator it = tmpList.iterator(); it.hasNext();) {
      String tmpString = (String)it.next();
      if (!tmpString.equals(searchType)) {
        newList.add(tmpString);
        typeComboBox.addItem(tmpString);
      }
    }
    // Zero should be the most recent search item anyway.
    typeComboBox.setSelectedIndex(0);
    org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().setModelProperty(FREQUENT_SEARCH_TYPES, newList);

    statusLabel.setText(STATUS + org.janelia.it.workstation.gui.framework.navigation_tools.AutoNavigationMgr.SEARCH_INITIATED);
    searchButton.setEnabled(false);
    if (availableGVRadioButton.isSelected()) {
      org.janelia.it.workstation.gui.framework.navigation_tools.AutoNavigationMgr.getAutoNavigationMgr().findEntity(browser,
        getSearchType(),getSearchString());
    }
    else if (currentGVRadioButton.isSelected()) {
      org.janelia.it.workstation.gui.framework.navigation_tools.AutoNavigationMgr.getAutoNavigationMgr().findEntityInGenomeVersion(browser,
        getSearchType(),getSearchString()/*,currentGenomeVersion*/);
    }
    else if (loadedGVRadioButton.isSelected()) {
      org.janelia.it.workstation.gui.framework.navigation_tools.AutoNavigationMgr.getAutoNavigationMgr().findEntityInSelectedGenomeVersions(
        browser,getSearchType(),getSearchString());
    }
  }


  /**
   * Briefly constructing an AutoNavigator in order to stop the current navigation action.
   * This is a little cheesy and the AutoNavigator should be refactored so that
   * there is a start/stop method.  The way it is done now is fairly good as it
   * uses the AutoNavigator itself to control the recursive navigation.
   */
  void stopNavigationButton_actionPerformed(ActionEvent e) {
    new AutoNavigator(browser);
  }


  /**
   * This class is only supposed to listen when the dialog is shown.
   * If the dialog is open it needs to update the text and state of the radio buttons.
   * Closing the dialog should remove the listeners.
   */
  void closeButton_actionPerformed(ActionEvent e) {
    org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().removeModelMgrObserver(myModelMgrObserver);
    browser.getBrowserModel().removeBrowserModelListener(browserCurrentSelectionListener);
    listenersSet = false;
    userDialog.setVisible(false);
  }


  /**
   * Remember to remove all possible listeners.
   */
  public void dispose() {
    browser.getBrowserModel().removeBrowserModelListener(browserCurrentSelectionListener);
    browser.getBrowserModel().removeBrowserModelListener(browserModelListener);
    org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().removeSessionModelListener(sessionModelListener);
    org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().removeModelMgrObserver(myModelMgrObserver);
    listenersSet = false;
    userDialog.removeWindowListener(myWindowListener);
    userDialog.dispose();
  }

  void navigateButton_actionPerformed(ActionEvent e) {
    if (resultsList==null || resultsList.getSelectedIndex()<0) return;

    NavigationPath navPath=(NavigationPath)navPaths.get(resultsList.getSelectedIndex());
    if (browser==null || this.newBrowserCheckBox.isSelected())
     org.janelia.it.workstation.gui.framework.navigation_tools.AutoNavigationMgr.getAutoNavigationMgr().navigate(
        navPath);
    else
     org.janelia.it.workstation.gui.framework.navigation_tools.AutoNavigationMgr.getAutoNavigationMgr().navigate(
        browser,navPath);
  }

  /**
   * Inner class that will help us hear when the session persistent attributes
   * change so that we can update the UI.  One example is the SubView focus
   * ability upon navigation.
   */
   private class MySessionModelListener implements SessionModelListener {
    public void browserAdded(org.janelia.it.workstation.gui.framework.session_mgr.BrowserModel browserModel){}
    public void browserRemoved(org.janelia.it.workstation.gui.framework.session_mgr.BrowserModel browserModel){}
    public void sessionWillExit(){}
    public void modelPropertyChanged(Object key, Object oldValue, Object newValue){
      if (key.equals(FOCUS_SUBVIEWS_UPON_NAVIGATION)) {
        subviewFocusCheckBox.setSelected(((Boolean)newValue).booleanValue());
      }
    }
   }


  /**
   * Comminucate state changes generically through the transient Browser Model
   * Generic Properties.
   */
  private class MyBrowserModelListener extends org.janelia.it.workstation.gui.framework.session_mgr.BrowserModelListenerAdapter {
    public void modelPropertyChanged(Object key, Object oldValue, Object newValue){
      if (key.equals(org.janelia.it.workstation.gui.framework.navigation_tools.AutoNavigationMgr.PATH_DISCOVERED)) {
        addSearchResults((NavigationPath[])newValue);
      }
      else if (key.equals(org.janelia.it.workstation.gui.framework.navigation_tools.AutoNavigationMgr.SEARCH_INITIATED)) {
        // Upon new search, remove all old items.
        navPaths.clear();
        resultsList.setListData(new Vector());

        statusLabel.setText(STATUS + (String)key);
        searchButton.setEnabled(false);
      }
      else if (key.equals(org.janelia.it.workstation.gui.framework.navigation_tools.AutoNavigationMgr.SEARCH_COMPLETE)) {
        searchButton.setEnabled(true);
        statusLabel.setText(STATUS + (String)key);
      }
    }
  }


  private class MyWindowListener extends WindowAdapter {
    public void windowClosing(WindowEvent e) {
      userDialog.setVisible(false);
    }
  }


  private class MyListCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus)
    {
      JLabel tmpComponent = (JLabel)super.getListCellRendererComponent(list, value,
        index, isSelected, cellHasFocus);
      tmpComponent.setMinimumSize(new Dimension(0,0));
      if (value instanceof NavigationPath) {
        tmpComponent.setText(((NavigationPath)value).getDisplayName());
      }
      return tmpComponent;
    }
  }

  /**
   * As the dialog is not model, this observer exists to update the choices in case the
   * user adds, removes, or selects a genome version.  Only needs to listen when the
   * dialog is showing.
   */
  private class MyModelMgrObserver extends ModelMgrAdapter {
//    public void genomeVersionAdded(GenomeVersion genomeVersion){
//      showSearchDialog(browser, false);
//    }
//    public void genomeVersionRemoved(GenomeVersion genomeVersion){
//      showSearchDialog(browser, false);
//    }
//    public void genomeVersionSelected(GenomeVersion genomeVersion){
//      showSearchDialog(browser, false);
//    }
//    public void genomeVersionUnselected(GenomeVersion genomeVersion){
//      showSearchDialog(browser, false);
//    }
  }

  /**
   * This is only supposed to listen when the dialog is shown in case navigating
   * loads in new Genome Versions.  If the dialog is open it needs to update the
   * text and state of the radio buttons.  This MUST be separate from the other
   * browser model listener!!!
   */
  private class MyBrowserModelCurrentSelectionListener extends org.janelia.it.workstation.gui.framework.session_mgr.BrowserModelListenerAdapter {
    public void browserCurrentSelectionChanged(Entity newSelection) {
      showSearchDialog(browser, false);
    }
  }
}