package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import org.janelia.workstation.controller.color_slider.SliderPanel;
import org.janelia.workstation.integration.util.FrameworkAccess;

/**
 * posts a filechooser which funnels into JSON import/export of the color models in LVV/Horta
 *
 * @author schauderd
 */
public class ImportExportColorModelAction extends AbstractAction {
    public enum MODE { IMPORT, EXPORT, SAVE_WORKSPACE, SAVE_USER};

    private MODE mode;
    private SliderPanel sliderMgr;
    private JPanel panel;

    public ImportExportColorModelAction(MODE mode, SliderPanel mgr, JPanel panel) {
        this.panel = panel;
        this.sliderMgr = mgr;
        this.mode = mode;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            // check which top component is live
            JFileChooser chooser = new JFileChooser(sliderMgr.getDefaultColorModelDirectory());
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            switch (mode) {
                case IMPORT:
                    chooser.setDialogTitle("Choose color model file to import");
                    chooser.setApproveButtonText("Load");

                    int returnValue = chooser.showOpenDialog(panel);
                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        File modelFile = chooser.getSelectedFile();
                        sliderMgr.importCompleteColorModel(modelFile);
                    }
                    break;
                case EXPORT:
                    chooser.setDialogTitle("Choose color model file to export");
                    chooser.setSelectedFile(new File(this.getValue("top") + "_colormodel.json"));
                    chooser.setApproveButtonText("Save");
                    returnValue = chooser.showOpenDialog(panel);
                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        File modelFile = chooser.getSelectedFile();
                        sliderMgr.exportCompleteColorModel(modelFile);
                    }
                    break;
                case SAVE_USER:
                    sliderMgr.saveColorModel(MODE.SAVE_USER);
                    break;
                case SAVE_WORKSPACE:
                    sliderMgr.saveColorModel(MODE.SAVE_WORKSPACE);
                    break;
            }
        }
        catch (Exception ex) {
            FrameworkAccess.handleException("Problem handing color model import/export", ex);
        }
    }

    class ColorModelFileFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            return f.getAbsolutePath().endsWith("json");
        }

        @Override
        public String getDescription() {
            return "*.json";
        }
    }
}
