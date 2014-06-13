package org.janelia.it.workstation.gui.framework.tool_manager;

import org.janelia.it.workstation.shared.preferences.InfoObject;
import org.janelia.it.workstation.shared.preferences.PreferenceManager;

import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 6/1/12
 * Time: 10:14 AM
 */
public class ToolInfo extends InfoObject {
    public static final String TOOL_PREFIX  = "Tool.";
    public static final String TOOL_NAME    = "Name";
    public static final String TOOL_PATH    = "Path";
    public static final String TOOL_ICON    = "Icon";
    public static final String USER         = "USER";
    public static final String SYSTEM       = "SYSTEM";

    private String name;
    private String path;
    private String iconPath;

    public ToolInfo(String keyBase, Properties inputProperties, String sourceFile) {
        this.keyBase=keyBase;
        this.sourceFile=sourceFile;
        String tmpString = inputProperties.getProperty(keyBase+"."+TOOL_NAME);
        if (tmpString!=null) name=tmpString;
        else name="Unknown";

        tmpString = inputProperties.getProperty(keyBase+"."+TOOL_PATH);
        if (tmpString!=null) path=tmpString;
        else path="Unknown";

        tmpString = inputProperties.getProperty(keyBase+"."+TOOL_ICON);
        if (tmpString!=null) iconPath=tmpString;
        else iconPath="Unknown";

    }

    public ToolInfo(String name, String pathText, String iconPath) {
        this.name = name;
        this.path = pathText;
        this.iconPath = iconPath;
        this.keyBase = PreferenceManager.getKeyForName(name, true);
    }

    // This constructor should only be used for the clone.
    private ToolInfo(String keyBase, String name, String sourceFile, String path, String iconPath){
        this.keyBase = keyBase;
        this.name = name;
        this.sourceFile = sourceFile;
        this.path= path;
        this.iconPath = iconPath;
    }


    @Override
    public String getKeyName() {
        return TOOL_PREFIX+keyBase;
    }

    public Properties getPropertyOutput() {
        Properties outputProperties=new Properties();
        String key = getKeyName()+".";

        outputProperties.put(key+TOOL_NAME,name);
        outputProperties.put(key+TOOL_PATH,path);
        outputProperties.put(key+TOOL_ICON,iconPath);

        return outputProperties;
    }
    public String getName(){
        return name;
    }

    public String getPath(){
        return path;
    }

    public String getIconPath(){
        return iconPath;
    }

    public void setName(String name){
        this.name = name;
        this.isDirty=true;
    }

    public void setPath(String path){
        this.path = path;
        this.isDirty=true;
    }

    public void setIconPath(String icon){
        iconPath = icon;
        this.isDirty=true;
    }

    @Override
    public Object clone() {
        return new ToolInfo(this.keyBase, this.name, this.sourceFile,
                this.path, this.iconPath);
    }

}
