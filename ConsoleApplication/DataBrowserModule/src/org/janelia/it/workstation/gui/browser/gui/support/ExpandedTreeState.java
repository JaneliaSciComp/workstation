package org.janelia.it.workstation.gui.browser.gui.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExpandedTreeState {

    private List<List<Long>> expandedPaths;

    public ExpandedTreeState() {
    }

    public ExpandedTreeState(List<List<Long>> expandedPaths) {
        this.expandedPaths = expandedPaths;
    }
    
    public void setExpandedPaths(List<List<Long>> expandedPaths) {
        this.expandedPaths = expandedPaths;
    }
    
    public List<List<Long>> getExpandedPaths() {
        return expandedPaths;
    }
    
    public static ExpandedTreeState createState(List<Long[]> paths) {
        ExpandedTreeState state = new ExpandedTreeState();
        state.setExpandedPaths(new ArrayList<List<Long>>());
        for (Long[] path : paths) {
            state.getExpandedPaths().add(Arrays.asList(path));
        }
        return state;
    }

    public List<Long[]> getExpandedArrayPaths() {
        List<Long[]> paths = new ArrayList<>();
        for (List<Long> path : expandedPaths) {
            paths.add(path.toArray(new Long[path.size()]));
        }
        return paths;
    }
}
