/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.full_skeleton_view.viewer;

import org.janelia.it.workstation.gui.full_skeleton_view.data_source.AnnotationSkeletonDataSourceI;

/**
 * Using color-under-click to find what was selected.
 * @author fosterl
 */
public class UniqueColorSelector {
    private AnnotationSkeletonDataSourceI dataSource;
    public UniqueColorSelector(AnnotationSkeletonDataSourceI dataSource) {
        this.dataSource = dataSource;
    }
    
    private void compareColor() {
        //
    }
}
