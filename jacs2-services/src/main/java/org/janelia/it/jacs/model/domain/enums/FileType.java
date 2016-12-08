package org.janelia.it.jacs.model.domain.enums;

public enum FileType {
    FirstAvailable2d("First Available", true, false),
    FirstAvailable3d("First Available Stack", false, false),

    // Stacks
    LosslessStack("Lossless Stack", false, false),
    VisuallyLosslessStack("Visually Lossless Stack", false, false),
    FastStack("Fast-loading Stack", false, false),

    // Metadata files
    LsmMetadata("LSM Metadata", false, true),

    // Maximum Intensity Projections (MIPs)
    SignalMip("Signal MIP", true, false),
    Signal1Mip("Signal 1 MIP", true, false),
    Signal2Mip("Signal 2 MIP", true, false),
    Signal3Mip("Signal 3 MIP", true, false),
    ReferenceMip("Reference MIP", true, false),
    AllMip("Reference+Signal MIP", true, false),
    RefSignal1Mip("Reference+Signal 1 MIP", true, false),
    RefSignal2Mip("Reference+Signal 2 MIP", true, false),
    RefSignal3Mip("Reference+Signal 3 MIP", true, false),

    // Movies
    SignalMovie("Signal Movie", false, false),
    ReferenceMovie("Reference Movie", false, false),
    AllMovie("Reference+Signal Movie", false, false),

    // Alignment outputs
    AlignmentVerificationMovie("Alignment Verification Movie", false, false),
    AlignedCondolidatedLabel("Aligned Consolidated Label", false, false),

    // Heatmaps for pattern data
    HeatmapStack("Heatmap Stack", false, false),
    HeatmapMip("Heatmap MIP", true, false),

    // Neuron separation
    NeuronSeparatorResult("Neuron Separator Result", false, false),
    MaskFile("Mask File", false, false),
    ChanFile("Chan File", false, false),

    // Cell Counting Results
    CellCountPlan("Cell Counting Plan", false, true),
    CellCountReport("Cell Counting Report", false, true),
    CellCountStack("Cell Counting Stack", false, false),
    CellCountStackMip("Cell Counting Stack MIP", true, false),
    CellCountImage("Cell Counting Image", false, false),
    CellCountImageMip("Cell Counting Image MIP", true, false),

    // Legacy files
    Unclassified2d("2D Image", true, false),
    Unclassified3d("3D Image", false, false),
    UnclassifiedAscii("Text File", false, true);

    private final String label;
    private final boolean is2dImage;
    private final boolean isAscii;

    private FileType(String label, boolean is2dImage, boolean isAscii) {
        this.label = label;
        this.is2dImage = is2dImage;
        this.isAscii = isAscii;
    }

    public String getLabel() {
        return label;
    }

    public static FileType getByLabel(String label) {
        FileType[] values = FileType.values();
        for (int i=0; i<values.length; i++) {
            if (values[i].getLabel().equals(label)) {
                return values[i];
            }
        }
        return null;
    }

    public boolean is2dImage() {
        return is2dImage;
    }

    public boolean isAscii() {
        return isAscii;
    }

}
