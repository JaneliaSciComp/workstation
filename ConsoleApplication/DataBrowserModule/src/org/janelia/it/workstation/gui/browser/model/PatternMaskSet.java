package org.janelia.it.workstation.gui.browser.model;

import java.util.List;
import org.janelia.it.jacs.model.domain.screen.PatternMask;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PatternMaskSet {
    
    private String name;
    private List<PatternMask> masks;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<PatternMask> getMasks() {
        return masks;
    }
    public void setMasks(List<PatternMask> masks) {
        this.masks = masks;
    }
}
