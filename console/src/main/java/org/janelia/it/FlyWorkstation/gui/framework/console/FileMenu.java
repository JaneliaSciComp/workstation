package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserverAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.DataSourceSelector;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManagerBase;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.FlyWorkstation.gui.util.panels.DataSourceSettings;
import org.janelia.it.jacs.model.entity.Entity;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 1:16 PM
 */
public class FileMenu extends JMenu {
    Browser browser;
    JMenuItem menuOpenDataSource;
    JMenuItem menuFileExit;
    JMenuItem menuFilePrint;
    JMenuItem menuListOpen;
    JMenuItem setLoginMI;

    ArrayList<JMenuItem> addedMenus = new ArrayList<JMenuItem>();
//    private boolean workSpaceHasBeenSaved = false;
//    private boolean isworkspaceDirty = false;
    private JDialog openDataSourceDialog = new JDialog();
//    private MyWorkSpaceObserver workSpaceObserver;
//    GenomeVersion workspaceGenomeVersion;
//    private AxisObserver myAxisObserver = new MyAxisObserver();

    public FileMenu(Browser browser) {
        super("File");
        this.setMnemonic('F');
        this.browser = browser;
       SessionMgr.getSessionMgr().addSessionModelListener(new MySessionModelListener());
        //This puts login and password info into the console properties.  Checking the login
        // save checkbox writes out to the session-persistent collection object.
       browser.getBrowserModel().setModelProperty("LOGIN", SessionMgr.getSessionMgr().getModelProperty("LOGIN"));
       browser.getBrowserModel().setModelProperty("PASSWORD", SessionMgr.getSessionMgr().getModelProperty("PASSWORD"));

        menuOpenDataSource = new JMenuItem("Open Data Source...", 'D');
        menuOpenDataSource.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK, false));
        menuOpenDataSource.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileOpen_actionPerformed(e, FacadeManager.getEJBProtocolString(), null);
            }
        });

        setLoginMI = new JMenuItem("Set Login...", 'o');
        setLoginMI.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
              setLogin();
           }
        });

        menuListOpen = new JMenuItem("List Open Data Sources...", 'L');
        menuListOpen.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
              menuListOpen_actionPerformed();
           }
        });

        menuFilePrint = new JMenuItem("Print Screen...", 'P');
        menuFilePrint.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                filePrint_actionPerformed();
            }
        });

        menuFileExit = new JMenuItem("Exit", 'x');
        menuFileExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileExit_actionPerformed();
            }
        });

        addMenuItems();

        ModelMgr.getModelMgr().addModelMgrObserver(new MyModelManagerObserver());
//        ModifyManager.getModifyMgr().addObserver(new MyModifyManagerObserver());
//        browser.getBrowserModel().addBrowserModelListener(new MyBrowserModelListenerAdapter());

    }

    private void addMenuItems() {
        add(setLoginMI);
        add(new JSeparator());
        add(menuListOpen);
        add(menuFilePrint);
        if (addedMenus.size() > 0)
           add(new JSeparator());
         for (JMenuItem addedMenu : addedMenus) {
             add(addedMenu);
         }
        add(new JSeparator());
        add(menuFileExit);
    }

    private void setLogin() {
       PrefController.getPrefController().getPrefInterface(DataSourceSettings.class, browser);
    }

    private void fileExit_actionPerformed() {
       SessionMgr.getSessionMgr().systemExit();
    }

    private void filePrint_actionPerformed() {
       browser.printBrowser();
    }

   //File | Open action performed

   private void fileOpen_actionPerformed(ActionEvent e, String protocol, Object dataSource) {
      browser.repaint();
      if (SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME) == null
         || SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME).equals("")
         && ModelMgr.getModelMgr().getNumberOfLoadedOntologies() == 0) {
         int answer =
            JOptionPane.showConfirmDialog(browser, "Please enter your Workstation login information.", "Information Required", JOptionPane.OK_CANCEL_OPTION);
         if (answer == JOptionPane.CANCEL_OPTION)
            return;
         PrefController.getPrefController().getPrefInterface(DataSourceSettings.class, browser);
      }
      // Double check.  Exit if still empty or not useful.
      if (SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME) == null
         || SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME).equals("")
         && ModelMgr.getModelMgr().getNumberOfLoadedOntologies() == 0) {
         return;
      }
      DataSourceSelector dss = FacadeManager.getDataSourceSelectorForProtocol(protocol);
      FacadeManagerBase facadeManager = FacadeManager.getFacadeManager(protocol);
      if (dataSource == null)
         dss.selectDataSource(facadeManager);
      else
         dss.setDataSource(facadeManager, dataSource);
      ((JMenuItem) e.getSource()).setEnabled(//disable the menu if the protocol cannot support multiple datasources
      FacadeManager.canProtocolAddMoreDataSources(protocol));
   }


    private void menuListOpen_actionPerformed() {
       /**
        * There is no good way to get this information so I need to "splice" together
        * open genome versions and available GBW, GBF's.  No pun intended.
        * This implies refactoring of the Facades which is not scheduled for now
        * and going through each feature in the data model is probably the most accurate
        * but not the best way to get this information.
        */
       Entity ontology = ModelMgr.getModelMgr().getSelectedOntologies();
       ArrayList allDataSources = new ArrayList(Arrays.asList(FacadeManager.getFacadeManager().getOpenDataSources()));
       ArrayList<String> finalDataSources = new ArrayList<String>();
       finalDataSources.add(ontology.getName());
       Collections.sort(finalDataSources);
       for (Object allDataSource : allDataSources) {
            String tmpSource = ((String) allDataSource).trim();
            if (tmpSource.toLowerCase().endsWith(".gbf") || tmpSource.toLowerCase().endsWith(".gbw")) {
                StringTokenizer tok = new StringTokenizer(tmpSource, File.separator);
                while (tok.hasMoreTokens()) {
                    tmpSource = tok.nextToken();
                }
                finalDataSources.add(tmpSource);
            }
        }

       if (finalDataSources.size() == 0) {
          finalDataSources.add("No Sources Opened.");
       }
       openDataSourceDialog = new JDialog(browser, "Open Data Sources", true);
       openDataSourceDialog.setSize(400, 190);
       openDataSourceDialog.setResizable(false);
       JPanel mainPanel = new JPanel();
       JPanel topPanel = new JPanel();
       JPanel buttonPanel = new JPanel();
       mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
       mainPanel.setSize(400, 190);
       topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
       topPanel.setSize(400, 160);
       DefaultListModel listModel = new DefaultListModel();
       JList sources = new JList(listModel);
       sources.setRequestFocusEnabled(false);
        for (Object finalDataSource : finalDataSources) {
            listModel.addElement((String) finalDataSource);
        }
       JScrollPane sp = new JScrollPane();
       sp.setSize(380, 140);
       sp.getViewport().setView(sources);
       JButton okButton = new JButton("OK");
       okButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
             openDataSourceDialog.dispose();
          }
       });
       okButton.setSize(40, 40);
       topPanel.add(sp);
       buttonPanel.add(okButton);
       mainPanel.add(topPanel);
       mainPanel.add(buttonPanel);
       openDataSourceDialog.getContentPane().add(mainPanel);
       Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
       Dimension frameSize = openDataSourceDialog.getSize();
       if (frameSize.height > screenSize.height)
          frameSize.height = screenSize.height;
       if (frameSize.width > screenSize.width)
          frameSize.width = screenSize.width;
       openDataSourceDialog.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
       mainPanel.getRootPane().setDefaultButton(okButton);
       openDataSourceDialog.setVisible(true);
    }

    class MyModelManagerObserver extends ModelMgrObserverAdapter {
        @Override
        public void ontologySelected(Entity ontology) {
            super.ontologySelected(ontology);
       }

    }

    class MySessionModelListener implements SessionModelListener {
       public void browserAdded(BrowserModel browserModel) {
       }
       public void browserRemoved(BrowserModel browserModel) {
       }

       public void sessionWillExit() {
          // saveLastDataSources();
//          if (/*menuItemSaveAsXML.isEnabled() &&*/
//             SessionMgr.getSessionMgr().getNumberOfOpenBrowsers() < 2 && isworkspaceDirty) {
//             int answer = JOptionPane.showConfirmDialog(browser, "Would you like to save the workspace before closing?", "Save?", JOptionPane.YES_NO_OPTION);
//             if (answer == JOptionPane.YES_OPTION) {
//                saveAsXML();
//                writeAnnotationLog();
//             }
//          }
       }

       public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
       }
    }

}
