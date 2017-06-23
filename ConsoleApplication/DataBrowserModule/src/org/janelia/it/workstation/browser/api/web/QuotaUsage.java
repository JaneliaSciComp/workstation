package org.janelia.it.workstation.browser.api.web;

import java.math.BigDecimal;

/**
 * Usage information for a group's (i.e. lab's) filestore quota. 
 *  *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class QuotaUsage {

	private String lab;
	private BigDecimal spaceUsedTB;
	private BigDecimal totalSpaceTB;
	private Long totalFiles;
	private Double percentUsage;
	
    public String getLab() {
        return lab;
    }
    public void setLab(String lab) {
        this.lab = lab;
    }
    public BigDecimal getSpaceUsedTB() {
        return spaceUsedTB;
    }
    public void setSpaceUsedTB(BigDecimal spaceUsedTB) {
        this.spaceUsedTB = spaceUsedTB;
    }
    public BigDecimal getTotalSpaceTB() {
        return totalSpaceTB;
    }
    public void setTotalSpaceTB(BigDecimal totalSpaceTB) {
        this.totalSpaceTB = totalSpaceTB;
    }
    public Long getTotalFiles() {
        return totalFiles;
    }
    public void setTotalFiles(Long totalFiles) {
        this.totalFiles = totalFiles;
    }
    public Double getPercentUsage() {
        return percentUsage;
    }
    public void setPercentUsage(Double percentUsage) {
        this.percentUsage = percentUsage;
    }
    
}
