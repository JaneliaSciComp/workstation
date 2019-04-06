package org.janelia.it.workstation.browser.gui.model;

import javax.swing.ImageIcon;

import org.janelia.it.workstation.browser.gui.support.Icons;

/**
 * Canned decorators on images.  
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum ImageDecorator {

    PURGED("Purged", Icons.getIcon("decorator_trash.png")),
    SYNC("SAGE Sync", Icons.getIcon("decorator_connect.png")),
    DESYNC("SAGE Desync", Icons.getIcon("decorator_disconnect.png")),
    DISCONNECTED("Disconnected", Icons.getIcon("decorator_disconnect.png")),
    AD("AD line is available", Icons.getIcon("split_ad.png")),
    DBD("DBD line is available", Icons.getIcon("split_dbd.png"));
    
    private final String label;
    private final ImageIcon icon;
    
    private ImageDecorator(String label, ImageIcon icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public ImageIcon getIcon() {
        return icon;
    }
    
}
