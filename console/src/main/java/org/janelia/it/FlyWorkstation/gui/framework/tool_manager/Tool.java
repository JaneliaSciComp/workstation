package org.janelia.it.FlyWorkstation.gui.framework.tool_manager;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 6/1/12
 * Time: 10:14 AM
 */
public class Tool {
    private String toolName;
    private String toolPath;
    private String toolIcon;
    private String toolUser;

    public Tool(String toolName, String toolPath, String toolIcon, String toolUser){
        this.toolName = toolName;
        this.toolPath = toolPath;
        this.toolIcon = toolIcon;
        this.toolUser = toolUser;
    }

    public String getToolName(){
        return toolName;
    }

    public String getToolPath(){
        return toolPath;
    }

    public String getToolIcon(){
        return toolIcon;
    }

    public String getToolUser(){
        return toolUser;
    }

    public void setToolName(String name){
        toolName = name;
    }

    public void setToolPath(String path){
        toolPath = path;
    }

    public void setToolIcon(String icon){
        toolIcon = icon;
    }

    public void setToolUser(String user){
        toolUser = user;
    }

    public  boolean isUserSystem(){
        return toolUser.equals("SYSTEM");
    }

}
