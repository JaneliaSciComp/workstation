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
    //public Preferences pref = Preferences.userNodeForPackage(getClass());
    public TreeMap<String, Tool> toolTreeMap = new TreeMap<String, Tool>();
    public ToolMgr(){}

    public void addTool(Tool tool){
        String tmpName = tool.getToolName();
        String tmpPath = tool.getToolPath();
        String key = "Tools." + tool.getToolUser() + "." + tmpName.replaceAll("\\.", "");


        if (!tmpName.equals("") && !tmpPath.equals("")){

            toolTreeMap.put(key, tool);
            SessionMgr.getSessionMgr().setModelProperty(key + ".Name", tmpName);
            SessionMgr.getSessionMgr().setModelProperty(key + ".Path", tmpPath);
            SessionMgr.getSessionMgr().setModelProperty(key + ".Icon", tool.getToolIcon());
        }
        else {
            JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Please make sure the tool name and path are correct.", "Tool Exception", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void removeTool(Tool tool){
        String tmpName = tool.getToolName();
        String key = "Tools." + tool.getToolUser() + "." + tmpName.replaceAll("\\.", "");
        if(tool.getToolUser().equals("SYSTEM")){
            JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Cannot remove a system tool.", "Tool Exception", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (null!=toolTreeMap.get(key)){

            toolTreeMap.remove(key);
            SessionMgr.getSessionMgr().removeModelProperty(key + ".Name");
            SessionMgr.getSessionMgr().removeModelProperty(key + ".Path");
            SessionMgr.getSessionMgr().removeModelProperty(key + ".Icon");
        }
        else {
            JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "The tool specified does not exist.", "Tool Exception", JOptionPane.ERROR_MESSAGE);
        }
    }

}
