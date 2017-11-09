/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.task_workflow;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.janelia.it.workstation.browser.gui.support.WindowLocator;
import org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;


public class TopComponentPopulator {
    private TaskWorkflowPanel taskPanel;
    
    public void populate(JPanel panel) {
        depopulate(panel);
        if (taskPanel == null) {
            taskPanel = new TaskWorkflowPanel(new TaskDataSource());
        }
        panel.add(taskPanel, BorderLayout.CENTER);
    }
    
    public void depopulate(JPanel panel) {
        System.out.println("Depopulating the task panel.");
        if (panel != null && taskPanel != null) {
            panel.remove(taskPanel);
            taskPanel.close();
            taskPanel = null;
        }
    }
    
    private static class TaskDataSource implements TaskDataSourceI {

        private AnnotationModel annotationModel;

        public TaskDataSource() {
        }
        
        public AnnotationModel getAnnotationModel() {
            if (annotationModel == null) {
                cacheValues();
            }
            return annotationModel;
        }
        
        private void cacheValues() {
            // Strategy: get the Large Volume Viewer View.
            LargeVolumeViewerTopComponent tc = 
                    (LargeVolumeViewerTopComponent) WindowLocator.getByName(
                            LargeVolumeViewerTopComponent.LVV_PREFERRED_ID
                    );
            if (tc != null) {
                QuadViewUi ui = tc.getLvvv().getQuadViewUi();
                if (ui != null) {
                    annotationModel = ui.getAnnotationModel();
                }
            }
        }

    }
}
