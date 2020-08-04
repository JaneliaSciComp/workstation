package org.janelia.workstation.browser.gui.colordepth;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.janelia.filecacheutils.FileProxy;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.FileMgr;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
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

    private final static DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd hh:mma");
    
    public NewMaskActionListener() {
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        ActivityLogHelper.logUserAction("NewMaskActionListener.actionPerformed");

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Mask Files", "png"));
        
        int returnVal = fileChooser.showOpenDialog(FrameworkAccess.getMainFrame());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File localFile = fileChooser.getSelectedFile();
            
            SimpleWorker worker = new SimpleWorker()     {

                private String uploadPath;
                private BufferedImage image;
                private List<String> alignmentSpaces;
                
                @Override
                protected void doStuff() throws Exception {
                    uploadPath = MaskUtils.uploadMask(localFile);
                    try (InputStream imageStream = FileMgr.getFileMgr().openFileInputStream(uploadPath, false)) {
                        this.image = Utils.readImageFromInputStream(imageStream, FilenameUtils.getExtension(uploadPath));
                    }
                    alignmentSpaces = DomainMgr.getDomainMgr().getModel().getAlignmentSpaces();
                }

                @Override
                protected void hadSuccess() {
                    String now = dateFormatter.format(new Date());
                    String maskName = "Mask uploaded on "+now;
                    MaskCreationDialog maskCreationDialog = new MaskCreationDialog( 
                            image, uploadPath, alignmentSpaces, null, maskName, null, null, true);
                    maskCreationDialog.showForMask();
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };

            worker.setProgressMonitor(new IndeterminateProgressMonitor(FrameworkAccess.getMainFrame(), "Uploading mask", ""));
            worker.execute();
        }
    }
}
