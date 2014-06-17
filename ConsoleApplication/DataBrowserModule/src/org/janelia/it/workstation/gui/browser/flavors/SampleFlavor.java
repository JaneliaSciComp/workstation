package org.janelia.it.workstation.gui.browser.flavors;

import java.awt.datatransfer.DataFlavor;
import org.janelia.it.workstation.gui.browser.nodes.SampleNode;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleFlavor extends DataFlavor {

    public static final DataFlavor CUSTOMER_FLAVOR = new SampleFlavor();

    public SampleFlavor() {
         super(SampleNode.class, "Sample");
    }

} 