package org.janelia.workstation.common.gui.model;

import javax.swing.ImageIcon;

import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.model.Decorator;

/**
 * Canned decorators on images.  
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum SampleDecorator implements Decorator {

    PURGED("Purged", Icons.getIcon("decorator_trash.png")),
    SYNC("SAGE Sync", Icons.getIcon("decorator_connect.png")),
    DESYNC("SAGE Desync", Icons.getIcon("decorator_disconnect.png")),
    DISCONNECTED("Disconnected", Icons.getIcon("decorator_disconnect.png")),
    AD("AD line is available", Icons.getIcon("split_ad.png")),
    DBD("DBD line is available", Icons.getIcon("split_dbd.png"));
    
    private final String label;
    private final ImageIcon icon;
    
    SampleDecorator(String label, ImageIcon icon) {
        this.label = label;
        this.icon = icon;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public ImageIcon getIcon() {
        return icon;
    }
    
}
