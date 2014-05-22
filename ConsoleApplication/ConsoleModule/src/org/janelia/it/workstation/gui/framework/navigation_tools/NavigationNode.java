package org.janelia.it.workstation.gui.framework.navigation_tools;

public class NavigationNode implements java.io.Serializable {

  private Long     id;
  private int     nodeType;
  private String  displayName;

  public NavigationNode(Long oid, int nodeType, String displayName)
  {
    this.id             = id;
    this.nodeType       = nodeType;
    this.displayName    = displayName;
  }

  public Long    getID()            { return id; }
  public int    getNodeType()       { return nodeType; }
  public String getDisplayname()    { return displayName; }

  public String toString()
  {
    StringBuffer retVal = new StringBuffer(64);
    retVal.append("(");
    retVal.append(nodeType);
    retVal.append("),");
    retVal.append(id.toString());
    retVal.append(",");
    return retVal.toString();
  }
}