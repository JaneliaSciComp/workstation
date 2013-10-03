package org.janelia.it.FlyWorkstation.model.domain;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 10/3/13
 * Time: 1:46 PM
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
    public AlignmentContext[] getAllAlignmentContexts() {
        // TODO get this information from the database.
        return new AlignmentContext[] {
                new AlignmentContext("Unified 20x Alignment Space", "0.62x0.62x0.62", "1024x512x218"),
                new AlignmentContext("Unified 20x Alignment Space", "0.38x0.38x0.38", "1712x1370x492")
        };
    }
}
