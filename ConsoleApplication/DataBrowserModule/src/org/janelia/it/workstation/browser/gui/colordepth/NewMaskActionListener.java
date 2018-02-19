package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.FileMgr;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prompts the user to select a file on local disk, uploads it, and then creates 
 * a color depth mask from it.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class NewMaskActionListener implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(NewMaskActionListener.class);
    
    public NewMaskActionListener() {
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        ActivityLogHelper.logUserAction("NewMaskActionListener.actionPerformed");

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return "PNG mask files (*.png)";
            }

            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".png");
            }
        });
        
        int returnVal = fileChooser.showOpenDialog(FrameworkImplProvider.getMainFrame());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File localFile = fileChooser.getSelectedFile();  

            SimpleWorker worker = new SimpleWorker()     {

                private String uploadPath;
                private BufferedImage image;
                private List<String> alignmentSpaces;
                
                @Override
                protected void doStuff() throws Exception {
                    uploadPath = MaskUtils.uploadMask(localFile);
                    File file = FileMgr.getFileMgr().getFile(uploadPath, false);
                    if (file==null) {
                        // Cache is probably disabled, just fetch remotely
                        URL imageFileURL = FileMgr.getFileMgr().getURL(uploadPath, false);
                        this.image = Utils.readImage(imageFileURL);
                    }
                    else {
                        this.image = Utils.readImage(file.getAbsolutePath());
                    }
                    
                    alignmentSpaces = DomainMgr.getDomainMgr().getModel().getAlignmentSpaces();
                }

                @Override
                protected void hadSuccess() {
                    
                    MaskCreationDialog maskCreationDialog = new MaskCreationDialog(image, alignmentSpaces, null, true);
                    if (!maskCreationDialog.showForMask()) {
                        return; // User cancelled operation
                    }
                     
                    BufferedImage mask = maskCreationDialog.getMask();
                    if (!mask.equals(image)) {

                        SimpleWorker worker = new SimpleWorker()     {

                            private String uploadPath;
                            
                            @Override
                            protected void doStuff() throws Exception {
                                File tempFile = File.createTempFile("mask", ".png");
                                tempFile.deleteOnExit();
                                ImageIO.write(mask, "png", tempFile);
                                log.info("Wrote mask to temporary file: "+tempFile);
                                uploadPath = MaskUtils.uploadMask(tempFile);
                            }

                            @Override
                            protected void hadSuccess() {
                                String alignmentSpace = maskCreationDialog.getAlignmentSpace();
                                int maskThreshold = maskCreationDialog.getThreshold();
                                new AddMaskDialog().showForMask(uploadPath, alignmentSpace, maskThreshold, null);
                            }

                            @Override
                            protected void hadError(Throwable error) {
                                ConsoleApp.handleException(error);
                            }
                        };

                        worker.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), "Uploading mask", ""));
                        worker.execute();
                    }
                    else {
                        String alignmentSpace = maskCreationDialog.getAlignmentSpace();
                        int maskThreshold = maskCreationDialog.getThreshold();
                        new AddMaskDialog().showForMask(uploadPath, alignmentSpace, maskThreshold, null);
                    }
                }

                @Override
                protected void hadError(Throwable error) {
                    ConsoleApp.handleException(error);
                }
            };

            worker.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), "Uploading mask", ""));
            worker.execute();
        }
    }
}
