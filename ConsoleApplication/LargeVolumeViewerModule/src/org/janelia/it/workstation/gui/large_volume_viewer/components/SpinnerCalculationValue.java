/**
 * 
 */
package org.janelia.it.workstation.gui.large_volume_viewer.components;

import javax.swing.JSpinner;

/**
 * Encapsulate the spinner, so that its value may be adjusted for 
 * practical use, externally to the spinner itself.
 *
 * @author fosterl
 */
public class SpinnerCalculationValue {
    private final JSpinner spinner;
    private Integer offsetFromZero = 0;
    private Double rangeMultiplier = 1.0;
    
    public SpinnerCalculationValue(JSpinner spinner) {
        this.spinner = spinner;
    }
    
    public Integer getValue() {
        return offsetFromZero +
               new Double(rangeMultiplier * (Integer)spinner.getValue()).intValue();
    }
    
    public void setValue(Integer value) {
        Integer newValue = new Double(value / rangeMultiplier).intValue()
                - offsetFromZero;
        spinner.setValue(newValue);
    }

    /**
     * Offset-from-zero is a bias to be added to the internal value, to make
     * the value the rest of the world should see.
     * @return the offsetFromZero
     */
    public Integer getOffsetFromZero() {
        return offsetFromZero;
    }

    /**
     * @param offsetFromZero the offsetFromZero to set
     */
    public void setOffsetFromZero(Integer offsetFromZero) {
        this.offsetFromZero = offsetFromZero;
    }

    /**
     * Range multiplier is to be multiplied by the internal value, to make
     * its intervals compatible with what the rest of the world should see.
     * 
     * @return the rangeMultiplier
     */
    public Double getRangeMultiplier() {
        return rangeMultiplier;
    }

    /**
     * @param rangeMultiplier the rangeMultiplier to set
     */
    public void setRangeMultiplier(Double rangeMultiplier) {
        this.rangeMultiplier = rangeMultiplier;
    }
}
