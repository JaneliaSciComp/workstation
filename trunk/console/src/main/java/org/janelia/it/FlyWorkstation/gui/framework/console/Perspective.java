package org.janelia.it.FlyWorkstation.gui.framework.console;

/**
 * A perspective configures the console in a specific way for some specific task.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum Perspective {
    
    ImageBrowser("Image Browsing"),
    AlignmentBoard("Alignment Board"),
    SliceViewer("Slice Viewer"),
    SplitPicker("Split Picking"),
    AnnotationSession("Annotation Session"),
    TaskMonitoring("Task Monitoring");
    
    private String name;
    
    private Perspective(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}