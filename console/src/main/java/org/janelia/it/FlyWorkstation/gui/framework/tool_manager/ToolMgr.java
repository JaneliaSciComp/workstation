package org.janelia.it.FlyWorkstation.gui.framework.tool_manager;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

import javax.swing.*;
import java.util.TreeMap;
import java.util.prefs.Preferences;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 6/1/12
 * Time: 10:39 AM
 */
public class ToolMgr {
    public Preferences pref = Preferences.userNodeForPackage(getClass());
    public TreeMap<String, Tool> toolTreeMap = new TreeMap<String, Tool>();
    public ToolMgr(){}

    public void addTool(Tool tool){
        String tmpName = tool.getToolName();
        String tmpPath = tool.getToolPath();


        if (!tmpName.equals("") && !tmpPath.equals("")){
            pref.put("Tools." + tool.getToolUser() + "." + tmpName, tmpPath);
            toolTreeMap.put("Tools." + tool.getToolUser() + "." + tmpName, tool);
        }
        else {
            JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Please make sure the tool name and path are correct.", "Tool Exception", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void removeTool(Tool tool){
        String tmpName = tool.getToolName();
        if(tool.getToolUser().equals("SYSTEM")){
            JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Cannot remove a system tool.", "Tool Exception", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (null!=pref.get("Tools." + tool.getToolUser() + "." + tmpName, null) && null!=toolTreeMap.get("Tools." + tool.getToolUser() + "." + tmpName)){
            pref.remove("Tools." + tool.getToolUser() + "." + tmpName);
            toolTreeMap.remove("Tools." + SessionMgr.getUsername() + "." + tmpName);
        }
        else {
            JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "The tool specified does not exist.", "Tool Exception", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Preferences getPref(){
        return pref;
    }

}
