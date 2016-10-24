package org.janelia.it.workstation.model.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA. User: fosterl Date: 10/3/13 Time: 1:46 PM
 * 
 * Gets me all the alignment spaces.
 */
public class AlignmentContextFactory {
    
    public AlignmentContextFactory() {
    }

    /**
     * Returns all alignment contexts known.
     * 
     * @return anything for which an alignemnt board (etc.) may be created.
     */
    public List<AlignmentContext> getAllAlignmentContexts() {
        // TODO get this information from the database.
        List<AlignmentContext> contexts = new ArrayList<AlignmentContext>();
        contexts.add(new AlignmentContext("Unified 20x Alignment Space", "0.62x0.62x0.62", "1024x512x218"));
        contexts.add(new AlignmentContext("Unified 20x Alignment Space", "0.38x0.38x0.38", "1712x1370x492"));
        contexts.add(new AlignmentContext("Yoshi 20x Alignment Space", "0.46x0.46x0.46", "1184x592x218"));
        contexts.add(new AlignmentContext("Yoshi 63x Subsampled Alignment Space", "0.38x0.38x0.38", "1450x725x436"));
        return contexts;
    }
}
