package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.FlyWorkstation.gui.framework.tool_manager.Tool;
import org.janelia.it.FlyWorkstation.gui.framework.tool_manager.ToolConfigurationDialog;
import org.janelia.it.FlyWorkstation.gui.util.SystemInfo;
import org.janelia.it.FlyWorkstation.shared.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import java.util.prefs.BackingStoreException;


/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 3:47 PM
 */
public class ToolsMenu extends JMenu {
    private JMenuItem vaa3dMenuItem;
    private JMenuItem vaa3dNAMenuItem;
    private JMenuItem fijiMenuItem;
    private JMenuItem toolsConfiguration;
    private JFrame parentFrame;
    public static final String VAA3D_PATH_MAC="vaa3d64.app/Contents/MacOS/vaa3d64";
    public static final String VAA3D_PATH_LINUX="vaa3d";
    public static String rootExecutablePath = ToolsMenu.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    public static String vaa3dExePath;
    public static String vaa3dNAExePath;
    
    public ToolsMenu(Browser console) {
        super("Tools");
        this.setMnemonic('T');
        try {

            if (null!=SessionMgr.getSessionMgr().getModelProperty(SessionMgr.FIJI_PATH)){
                SessionMgr.TOOL_MGR.addTool(new Tool("Fiji", SessionMgr.getSessionMgr().getModelProperty(SessionMgr.FIJI_PATH).toString(), "", "SYSTEM"));
//            TOOL_MGR.addTool(new Tool("Vaa3d", getModelProperty(PATH_VAA3D).toString(), "", "SYSTEM"));
            }

            this.parentFrame = console;
            System.out.println("Base root executable path  = "+rootExecutablePath);
//            rootExecutablePath="/Applications/FlySuite.app/Contents/Resources/workstation.jar";
            rootExecutablePath=rootExecutablePath.substring(0,rootExecutablePath.lastIndexOf(File.separator)+1);
            vaa3dExePath = (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.PATH_VAA3D);
            if (null==vaa3dExePath || "".equals(vaa3dExePath)) {
                if (SystemInfo.isMac || SystemInfo.isWindows) {
                    vaa3dExePath = rootExecutablePath+VAA3D_PATH_MAC;
                }
                else if (SystemInfo.isLinux) {
                    vaa3dExePath = rootExecutablePath+VAA3D_PATH_LINUX;
                }
            }
            File testFile = new File(vaa3dExePath);
            if (testFile.exists() && testFile.canExecute()) {
                SessionMgr.getSessionMgr().setModelProperty(SessionMgr.PATH_VAA3D, vaa3dExePath);
            }
            else {
                SessionMgr.getSessionMgr().setModelProperty(SessionMgr.PATH_VAA3D, "");
            }
//            vaa3dExePath = "/Applications/FlySuite.app/Contents/Resources/vaa3d64.app/Contents/MacOS/vaa3d64";   //DEBUG LINE
            System.out.println("Vaa3d root executable path = "+vaa3dExePath);

            vaa3dMenuItem = new JMenuItem("Vaa3D", Utils.getClasspathImage("v3d_16x16x32.png"));
            vaa3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        Runtime.getRuntime().exec(vaa3dExePath);
                    }
                    catch (IOException e) {
                        JOptionPane.showMessageDialog(vaa3dMenuItem.getParent(), "Could not launch Vaa3D", "Tool Launch ERROR", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            // Start in NA mode
            vaa3dNAExePath = vaa3dExePath+" -na";
            vaa3dNAMenuItem = new JMenuItem("Vaa3D - NeuronAnnotator", Utils.getClasspathImage("v3d_16x16x32.png"));
            vaa3dNAMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        Runtime.getRuntime().exec(vaa3dNAExePath);
                    }
                    catch (IOException e) {
                        JOptionPane.showMessageDialog(vaa3dNAMenuItem.getParent(), "Could not launch Vaa3D - NeuroAnnotator", "Tool Launch ERROR", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            ImageIcon fijiImageIcon = Utils.getClasspathImage("fijiicon.png");
            Image img = fijiImageIcon.getImage();
            Image newimg = img.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            fijiImageIcon = new ImageIcon(newimg);
            fijiMenuItem = new JMenuItem("FIJI", fijiImageIcon);
            fijiMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        String fijiPath;
                        File tmpFile;
                        if (null == SessionMgr.getSessionMgr().getModelProperty(SessionMgr.FIJI_PATH)){
                            JFileChooser choosePath = new JFileChooser();
                            choosePath.showOpenDialog(SessionMgr.getBrowser());
                            tmpFile = choosePath.getSelectedFile();
                            fijiPath = tmpFile.getCanonicalPath();
                            if (SystemInfo.isWindows || SystemInfo.isLinux) {
                                SessionMgr.getSessionMgr().setModelProperty(SessionMgr.FIJI_PATH, fijiPath);
                                Runtime.getRuntime().exec(fijiPath);
                            }
                            else if (SystemInfo.isMac && fijiPath.endsWith("Fiji.app")) {
                                SessionMgr.getSessionMgr().setModelProperty(SessionMgr.FIJI_PATH, fijiPath+"/Contents/MacOS/fiji-macosx");
                                Runtime.getRuntime().exec(fijiPath);
                            }
                            SessionMgr.getSessionMgr().saveUserSettings();
                        }
                        else{
//                        fijiExePath = "C:\\Users\\kimmelr\\Documents\\Fiji.app\\fiji-win64.exe"; // DEBUG ONLY
                            fijiPath = SessionMgr.getSessionMgr().getModelProperty(SessionMgr.FIJI_PATH).toString();
                            tmpFile = new File(fijiPath);
                            if (tmpFile.exists()&&tmpFile.canExecute()) {
                                Runtime.getRuntime().exec(fijiPath);
                            }
                            else {
                                JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Could not launch Fiji. Please choose the appropriate file path from the Tools Menu", "Tool Launch ERROR", JOptionPane.ERROR_MESSAGE);
                                SessionMgr.getSessionMgr().setModelProperty(SessionMgr.FIJI_PATH, null);
                            }
                        }
                    }
                    catch (IOException e) {
                        JOptionPane.showMessageDialog(fijiMenuItem.getParent(), "Could not launch Fiji. Please choose the appropriate file path from the Tools Menu",
                                "Tool Launch Error",
                                JOptionPane.ERROR_MESSAGE);
                        SessionMgr.getSessionMgr().setModelProperty(SessionMgr.FIJI_PATH, null);
                    }
                }
            });

            toolsConfiguration = new JMenuItem("Configure Tools...");
            toolsConfiguration.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        new ToolConfigurationDialog(parentFrame);
                    }
                    catch (BackingStoreException e) {
                        e.printStackTrace();
                    }
                }
            });

            Set keySet = SessionMgr.TOOL_MGR.toolTreeMap.keySet();
            for (final Object o : keySet) {
                add(new JMenuItem(o.toString().replaceAll("Tools.", "").replaceFirst("SYSTEM.", "").replaceFirst(SessionMgr.getUsername() + ".", ""))).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Tool tmpTool = SessionMgr.TOOL_MGR.toolTreeMap.get(o);
                        File tmpFile = new File(tmpTool.getToolPath());
                        if (tmpFile.exists()&&tmpFile.canExecute()) {
                            try {
                                Runtime.getRuntime().exec(tmpTool.getToolPath());
                            }
                            catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                        else {
                            JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Could not launch this tool. " +
                                    "Please choose the appropriate file path from the Configure Tools Dialogue", "Tool Launch ERROR", JOptionPane.ERROR_MESSAGE);
                            SessionMgr.getSessionMgr().setModelProperty(SessionMgr.FIJI_PATH, null);
                        }
                    }
                });

            }

            // Add the tools
            add(fijiMenuItem);
            add(vaa3dMenuItem);
            add(vaa3dNAMenuItem);
            add(new JSeparator());
            add(toolsConfiguration);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        SessionMgr.getSessionMgr().addSessionModelListener(new SessionModelListener() {
            @Override
            public void browserAdded(BrowserModel browserModel) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void browserRemoved(BrowserModel browserModel) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void sessionWillExit() {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
                if (((String)key).startsWith("Tools.")){
                    rebuildMenu();
                }
            }
        });

    }

    public void rebuildMenu() {
        this.removeAll();

        Set keySet = SessionMgr.TOOL_MGR.toolTreeMap.keySet();
        for (final Object o : keySet) {
            add(new JMenuItem(o.toString().replaceAll("Tools.", "").replaceFirst("SYSTEM.", "").replaceFirst(SessionMgr.getUsername() + ".", ""))).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Tool tmpTool = SessionMgr.TOOL_MGR.toolTreeMap.get(o);
                    File tmpFile = new File(tmpTool.getToolPath());
                    if (tmpFile.exists()&&tmpFile.canExecute()) {
                        try {
                            Runtime.getRuntime().exec(tmpTool.getToolPath());
                            }
                        catch (IOException e1) {
                            e1.printStackTrace();
                            }
                        }
                    else {
                        JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Could not launch this tool. " +
                            "Please choose the appropriate file path from the Configure Tools Dialogue", "Tool Launch ERROR", JOptionPane.ERROR_MESSAGE);
                        SessionMgr.getSessionMgr().setModelProperty(SessionMgr.FIJI_PATH, null);
                    }
                }
            });

        }

        // Add the tools
        add(fijiMenuItem);
        add(vaa3dMenuItem);
        add(vaa3dNAMenuItem);
        add(new JSeparator());
        add(toolsConfiguration);
    }

}
