/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.full_skeleton_view.viewer;

import org.janelia.it.workstation.gui.full_skeleton_view.data_source.AnnotationSkeletonDataSourceI;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
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
    
    public UniqueColorSelector(AnnotationSkeletonDataSourceI dataSource, IdCoderProvider idCoderProvider, AnnotationSkeletonPanel redrawComponent) {
        this.dataSource = dataSource;
        this.idCoderProvider = idCoderProvider;
        this.annoSkeletonPanel = redrawComponent;
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
        System.out.println(String.format("ID or Row=%d", row));
        final AnnotationModel annoMdl = dataSource.getAnnotationModel();
        final FilteredAnnotationModel filteredModel = annoMdl.getFilteredAnnotationModel();
        if (row < filteredModel.getRowCount()  &&  row >= 0) {
            InterestingAnnotation annotation = filteredModel.getAnnotationAtRow(row);
            if (annotation != null) {
                long annoId = annotation.getAnnotationID();
                annoSkeletonPanel.positionForSelection(annoId);
            }
        }
        redraw();
	}
    
    private void redraw() {
        annoSkeletonPanel.validate();
        annoSkeletonPanel.repaint();
    }
}
