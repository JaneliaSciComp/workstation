package org.janelia.it.workstation.browser.model;

import java.util.List;
import java.util.stream.Collectors;

import org.janelia.model.domain.enums.SplitHalfType;

/**
 * For a given fly frag name, what are the split halves which are available.
 */
public class SplitTypeInfo {

    private String fragName;
    private List<SplitHalf> splitHalves;
    
    public SplitTypeInfo(String fragName, List<SplitHalf> splitHalves) {
        this.fragName = fragName;
        this.splitHalves = splitHalves;
    }
        
    public String getFragName() {
        return fragName;
    }
    
    public boolean hasAD() {
        return !getADSplitHalves().isEmpty();
    }
    
    public boolean hasDBD() {
        return !getDBDSplitHalves().isEmpty();
    }

    public List<SplitHalf> getSplitHalves() {
        return splitHalves;
    }

    public List<SplitHalf> getADSplitHalves() {
        return splitHalves.stream().filter(s -> s.getType()==SplitHalfType.AD).collect(Collectors.toList());
    }

    public List<SplitHalf> getDBDSplitHalves() {
        return splitHalves.stream().filter(s -> s.getType()==SplitHalfType.DBD).collect(Collectors.toList());
    }
}
