package org.janelia.it.workstation.model.viewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An aligned item an alignment board context. Always has an entity, and may have other AlignedItems as children. 
 * Also provides contextual properties for displaying the alignment board, such as visibility and color. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignedItem {

    private static final Logger log = LoggerFactory.getLogger(AlignedItem.class);
    public enum InclusionStatus {
        In, ExcludedForSize;

        private static final String EXCLUDED_FOR_SIZE = "Excluded for Size";
        private static final String IN = "In";

        public static InclusionStatus get( String strVal ) {
            // Not set at all --> keep it in.
            if ( strVal == null ) {
                return In;
            }

            if ( strVal.equals( EXCLUDED_FOR_SIZE ) ) {
                return ExcludedForSize;
            }
            else {
                return valueOf( strVal );
            }
        }

        public String toString() {
            return this.equals( In ) ? IN : EXCLUDED_FOR_SIZE;
        }
    };

}
