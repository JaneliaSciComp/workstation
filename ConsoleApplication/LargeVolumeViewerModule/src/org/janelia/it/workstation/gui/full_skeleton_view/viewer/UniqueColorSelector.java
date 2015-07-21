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
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh.PixelReadActor;
import org.janelia.it.workstation.gui.viewer3d.picking.IdCoder;
import org.janelia.it.workstation.gui.viewer3d.picking.IdCoderProvider;
import org.janelia.it.workstation.gui.viewer3d.picking.RenderedIdPicker;

/**
 * Using color-under-click to find what was selected.
 * @author fosterl
 */
public class UniqueColorSelector implements PixelReadActor.PixelListener, RenderedIdPicker.PixelListener {
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
     * What to do when the pixel has been received by the actor.
     * @param pixel 
     */
    @Override
    public void setPixel(float[] pixel) {
        IdCoder idCoder = idCoderProvider.getIdCoder();
        if (idCoder != null) {
            int id = idCoder.decode(pixel[0]);
            System.out.println(String.format("Color: r=%f / g=%f / b=%f.  ID=%d", pixel[0], pixel[1], pixel[2], id));
        }
        redraw();
    }
	
	@Override
	public void setPixel(int pixel) {
        final int row = idCoderProvider.getIdCoder().decode(pixel / 255.5f);
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
