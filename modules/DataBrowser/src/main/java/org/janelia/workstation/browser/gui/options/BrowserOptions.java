package org.janelia.workstation.browser.gui.options;

import java.beans.PropertyChangeSupport;

import org.janelia.workstation.browser.gui.listview.icongrid.ImagesPanel;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BrowserOptions {

    private static final Logger log = LoggerFactory.getLogger(BrowserOptions.class);

    public static final String UNLOAD_IMAGES_PROPERTY = "SessionMgr.UnloadImagesProperty";
    public static final String DISABLE_IMAGE_DRAG_PROPERTY = "SessionMgr.DisableImageDragProperty";
    public static final String DUPLICATE_ANNOTATIONS_PROPERTY = "SessionMgr.AllowDuplicateAnnotationsProperty";
    public static final String SHOW_ANNOTATION_TABLES_PROPERTY = "SessionMgr.ShowAnnotationTablesProperty";
    public static final String ANNOTATION_TABLES_HEIGHT_PROPERTY = "SessionMgr.AnnotationTablesHeightProperty";
    public static final String SHOW_SEARCH_HERE = "SessionMgr.ShowSearchHereProperty";

    private static BrowserOptions instance;

    public static synchronized BrowserOptions getInstance() {
        if (null == instance) {
            instance = new BrowserOptions();
        }
        return instance;
    }

    private PropertyChangeSupport propSupport;

    private BrowserOptions() {
    }

    public boolean isUnloadImages() {
        return FrameworkAccess.getModelProperty(UNLOAD_IMAGES_PROPERTY, Boolean.TRUE);
    }

    public void setUnloadImages(boolean value) {
        boolean oldVal = isUnloadImages();
        if (oldVal == value) return;
        FrameworkAccess.setModelProperty(UNLOAD_IMAGES_PROPERTY, value);
        log.info("Set unload images = {}", value);
    }

    public boolean isDragAndDropDisabled() {
        return FrameworkAccess.getModelProperty(DISABLE_IMAGE_DRAG_PROPERTY, Boolean.FALSE);
    }

    public void setDragAndDropDisabled(boolean value) {
        boolean oldVal = isDragAndDropDisabled();
        if (oldVal == value) return;
        FrameworkAccess.setModelProperty(DISABLE_IMAGE_DRAG_PROPERTY, value);
        log.info("Set disable image drag = {}", value);
    }

    public boolean isDuplicateAnnotationAllowed() {
        return FrameworkAccess.getModelProperty(DUPLICATE_ANNOTATIONS_PROPERTY, Boolean.FALSE);
    }

    public void setDuplicateAnnotationAllowed(boolean value) {
        boolean oldVal = isDuplicateAnnotationAllowed();
        if (oldVal == value) return;
        FrameworkAccess.setModelProperty(DUPLICATE_ANNOTATIONS_PROPERTY, value);
        log.info("Set duplicate annotations = {}", value);
    }

    public boolean isShowAnnotationTables() {
        return FrameworkAccess.getModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY, Boolean.FALSE);
    }

    public void setShowAnnotationTables(boolean value) {
        boolean oldVal = isShowAnnotationTables();
        if (oldVal == value) return;
        FrameworkAccess.setModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY, value);
        log.info("Set annotation tables = {}", value);
    }

    public int getAnnotationTableHeight() {
        return FrameworkAccess.getModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY, ImagesPanel.DEFAULT_TABLE_HEIGHT);
    }

    public void setAnnotationTableHeight(int value) {
        int oldVal = getAnnotationTableHeight();
        if (oldVal == value) return;
        FrameworkAccess.setModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY, value);
        log.info("Set annotation tables = {}", value);
    }

    public boolean isShowSearchHere() {
        return FrameworkAccess.getModelProperty(SHOW_SEARCH_HERE, Boolean.FALSE);
    }

    public void setShowSearchHere(boolean value) {
        boolean oldVal = isShowAnnotationTables();
        if (oldVal == value) return;
        FrameworkAccess.setModelProperty(SHOW_SEARCH_HERE, value);
        log.info("Set search here = {}", value);
    }

}
