package org.janelia.it.workstation.gui.browser.flavors;

import java.awt.datatransfer.DataFlavor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drop flavors for ontology terms.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyTermFlavor extends DataFlavor {
    
    private final static Logger log = LoggerFactory.getLogger(OntologyTermFlavor.class);
    
    static private DataFlavor createConstant(String mt, String prn) {
        try {
            return new OntologyTermFlavor(mt, prn);
        } catch (Exception e) {
            log.error("Error creating data flavor "+mt,e);
            return null;
        }
    }
    
    public static final DataFlavor SINGLE_FLAVOR = createConstant("application/x-domain-object;class=org.janelia.it.jacs.model.domain.ontology.OntologyTerm","Ontology Term");
    public static final DataFlavor LIST_FLAVOR = createConstant("application/x-domain-object-list;class=java.util.List","Ontology Term List");

    private OntologyTermFlavor(String mt, String prn) {
        super(mt, prn);
    }
} 