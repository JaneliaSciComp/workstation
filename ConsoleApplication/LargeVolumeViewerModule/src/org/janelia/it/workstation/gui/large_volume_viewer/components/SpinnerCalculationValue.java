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
    
    /**
     * Call this to retrieve how the number stored in the spinner
     * _should_ look to external API-level callers.
     * 
     * @return value as it should look to everything but spinner.
     */
    public Integer getValue() {
        return getExternalValue();
    }
    
    /**
     * Call this to store some value from the external-world's coordinate
     * system, and have it stashed in the spinner in the spinner's own
     * coordinate system.
     * 
     * @param value some external-world value.
     */
    public void setValue(Integer value) {
        Integer newValue = getInternalValue(value);
        spinner.setValue(newValue);
    }
    
    /**
     * This returns the value that should be seen by API callers of the
     * wrapped spinner.
     * 
     * @return in external coord system.
     * @see #getInternalValue(java.lang.Integer) 
     */
    public Integer getExternalValue() {
        return offsetFromZero
                + new Double(rangeMultiplier * (Integer) spinner.getValue()).intValue();
    }
    
    /**
     * This returns the internal representation of the value given, which is
     * expected to be in the external coordinate system.
     * 
     * @param externalValue as seen without
     * @return as seen within
     * @see #getExternalValue() 
     */
    public int getInternalValue(Integer externalValue) {
        Integer internalValue = new Double(externalValue / rangeMultiplier).intValue()
                - offsetFromZero;
        return internalValue;
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
