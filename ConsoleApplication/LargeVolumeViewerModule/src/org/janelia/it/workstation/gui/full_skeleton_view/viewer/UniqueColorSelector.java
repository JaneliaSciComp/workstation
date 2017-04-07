package org.janelia.it.workstation.gui.full_skeleton_view.viewer;

import org.janelia.it.workstation.gui.full_skeleton_view.data_source.AnnotationSkeletonDataSourceI;
import org.janelia.it.workstation.gui.large_volume_viewer.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.FilteredAnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.InterestingAnnotation;
import org.janelia.it.workstation.gui.viewer3d.picking.IdCoder;
import org.janelia.it.workstation.gui.viewer3d.picking.IdCoderProvider;
import org.janelia.it.workstation.gui.viewer3d.picking.RenderedIdPicker;

/**
 * Using color-under-click to find what was selected.
 * @author fosterl
 */
public class UniqueColorSelector implements RenderedIdPicker.PixelListener {
    private final AnnotationSkeletonDataSourceI dataSource;
    private IdCoderProvider idCoderProvider;
    private AnnotationSkeletonPanel annoSkeletonPanel;
    private ActivityLogHelper activityLog;
    
    public UniqueColorSelector(AnnotationSkeletonDataSourceI dataSource, IdCoderProvider idCoderProvider, AnnotationSkeletonPanel redrawComponent) {
        this.dataSource = dataSource;
        this.idCoderProvider = idCoderProvider;
        this.annoSkeletonPanel = redrawComponent;
        this.activityLog = ActivityLogHelper.getInstance();
    }
    
    public UniqueColorSelector(AnnotationSkeletonDataSourceI dataSource) {
        this.dataSource = dataSource;
    }
    
    public void setIdCoderProvider(IdCoderProvider provider) {
        this.idCoderProvider = provider;
    }

    public long select(int x, int y) {
        return 0;
    }

    /**
     * What to do when the pixel has been received.
     *
     * @param pixel int value buried in a pixel.
     */
	@Override
	public void setPixel(int pixel) {
        final IdCoder idCoder = idCoderProvider.getIdCoder();
        final int row = idCoder.decode(pixel / IdCoder.RAW_RANGE_DIVISOR);
        //DEBUG System.out.println(String.format("ID or Row=%d", row));
        final AnnotationManager annotationMgr = dataSource.getAnnotationManager();
        final FilteredAnnotationModel filteredModel = annotationMgr.getFilteredAnnotationModel();
        if (row < filteredModel.getRowCount()  &&  row >= 0) {
            InterestingAnnotation annotation = filteredModel.getAnnotationAtRow(row);
            if (annotation != null) {
                long annoId = annotation.getAnnotationID();
                annoSkeletonPanel.positionForSelection(annoId);
                activityLog.logLandmarkViewPick(annotationMgr, annoId);
            }
        }
        redraw();
	}
    
    private void redraw() {
        annoSkeletonPanel.validate();
        annoSkeletonPanel.repaint();
    }
}
