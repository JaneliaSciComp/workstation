package org.janelia.it.workstation.gui.framework.navigation_tools;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class NavigationPath implements Serializable, Comparable
{
  private String displayName;
  private List  nodeArray;

  public NavigationPath(String displayName,
                        NavigationNode[] nodeArray) {
    this.displayName    = displayName;
    this.nodeArray      = new LinkedList(Arrays.asList(nodeArray));
  }

  public String           getDisplayName()          { return displayName; }
  public NavigationNode[] getNavigationNodeArray()  { return
    (NavigationNode[]) nodeArray.toArray(new NavigationNode[nodeArray.size()]); }
  public List             getNavigationNodeList()     { return Collections.unmodifiableList(nodeArray); }

  public void setDisplayName(String newName) { this.displayName = newName; }

  public void appendNewNode(NavigationNode node) {
    nodeArray.add(node);
  }

  public void prependNewNode(NavigationNode node) {
    nodeArray.add(0,node);
  }

  /** @return a URL-like String that displays the path */
  public String toString() {
    StringBuffer retVal = new StringBuffer(128);
    retVal.append(displayName);
    retVal.append(":");
    for(int i=0; i<nodeArray.size(); ++i) {
      retVal.append("/");
      retVal.append(nodeArray.get(i).toString());
    }
    return retVal.toString();
  }

  public int compareTo(Object o2) {
//    NavigationPath nav2 = (NavigationPath)o2;
//    if (nav2 == null) return 1;
//    Integer gV1 = new Integer(getNavigationNodeArray()[0].getID().getGenomeVersionId());
//    Integer gV2 = new Integer(nav2.getNavigationNodeArray()[0].getID().getGenomeVersionId());
//    int genomeVersionComparison = gV1.compareTo(gV2);
      int comparison = getNavigationNodeArray()[0].getID().compareTo(((NavigationPath)o2).getNavigationNodeArray()[0].getID());
    return comparison;
  }

}
