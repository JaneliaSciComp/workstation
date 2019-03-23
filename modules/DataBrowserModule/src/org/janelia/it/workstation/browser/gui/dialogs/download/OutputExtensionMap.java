package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.util.HashMap;

/**
 * A little hack to get around type erasure, as described in "Known Issues" here:
 * http://wiki.fasterxml.com/JacksonPolymorphicDeserialization
 */
public class OutputExtensionMap extends HashMap<String,String> {
    
    public OutputExtensionMap() {
    }
    
}