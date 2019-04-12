package org.janelia.workstation.browser.flavors;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

import org.janelia.workstation.browser.nodes.AbstractDomainObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drop flavors for domain object nodes.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectNodeFlavor extends DataFlavor {
    
    private final static Logger log = LoggerFactory.getLogger(DomainObjectNodeFlavor.class);
    
    static private DataFlavor createConstant(String mt, String prn) {
        try {
            return new DomainObjectNodeFlavor(mt, prn);
        } catch (Exception e) {
            log.error("Error creating data flavor "+mt,e);
            return null;
        }
    }
    
    public static final DataFlavor SINGLE_FLAVOR = createConstant("application/x-domain-object;class=org.janelia.workstation.browser.nodes.DomainObjectNode","Domain Object Node");
    public static final DataFlavor LIST_FLAVOR = createConstant("application/x-domain-object-list;class=java.util.List","Domain Object Node List");

    private DomainObjectNodeFlavor(String mt, String prn) {
        super(mt, prn);
    }

    public static AbstractDomainObjectNode<?> getDomainObjectNode(Transferable t) {
        AbstractDomainObjectNode<?> node = null;
        try {
            node = (AbstractDomainObjectNode<?>)t.getTransferData(SINGLE_FLAVOR);
        }
        catch (UnsupportedFlavorException | IOException e) {
            log.error("Error getting transfer data", e);
        }
        return node;
    }
    
    @SuppressWarnings("unchecked")
    public static List<AbstractDomainObjectNode<?>> getDomainObjectNodeList(Transferable t) {
        List<AbstractDomainObjectNode<?>> node = null;
        try {
            node = (List<AbstractDomainObjectNode<?>>)t.getTransferData(LIST_FLAVOR);
        }
        catch (UnsupportedFlavorException | IOException e) {
            log.error("Error getting transfer data", e);
        }
        return node;
    }
} 