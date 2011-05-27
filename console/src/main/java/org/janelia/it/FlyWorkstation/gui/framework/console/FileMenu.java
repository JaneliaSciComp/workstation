package org.janelia.it.FlyWorkstation.gui.framework.console;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 1:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileMenu extends JMenu {
    ConsoleFrame console;
    JMenuItem menuFileExit;
    JMenuItem menuFilePrint;
    ArrayList<JMenuItem> addedMenus = new ArrayList<JMenuItem>();
    private boolean workSpaceHasBeenSaved = false;
    private boolean isworkspaceDirty = false;
    private JDialog openDataSourceDialog = new JDialog();
//    private MyWorkSpaceObserver workSpaceObserver;
//    GenomeVersion workspaceGenomeVersion;
//    private AxisObserver myAxisObserver = new MyAxisObserver();

    public FileMenu(ConsoleFrame console) {
        super("File");
        this.setMnemonic('F');
        this.console = console;
//       SessionMgr.getSessionMgr().addSessionModelListener(new MySessionModelListener());
        //This puts login and password info into the console properties.  Checking the login
        // save checkbox writes out to the session-persistent collection object.
//       console.getBrowserModel().setModelProperty("LOGIN", SessionMgr.getSessionMgr().getModelProperty("LOGIN"));
//       console.getBrowserModel().setModelProperty("PASSWORD", SessionMgr.getSessionMgr().getModelProperty("PASSWORD"));

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
    }

    private void addMenuItems() {
        removeAll();

        add(menuFilePrint);
        if (addedMenus.size() > 0)
            add(new JSeparator());
        for (JMenuItem addedMenu : addedMenus) {
            add(addedMenu);
        }
        add(new JSeparator());
        add(menuFileExit);

    }

    private void fileExit_actionPerformed() {
        System.exit(0);
//       SessionMgr.getSessionMgr().systemExit();
    }

    private void filePrint_actionPerformed() {
//       console.printBrowser();
    }

}
