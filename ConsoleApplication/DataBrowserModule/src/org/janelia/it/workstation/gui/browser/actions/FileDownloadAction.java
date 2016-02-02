package org.janelia.it.workstation.gui.browser.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.browser.gui.support.SampleDownloadWorker;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * Menu system for downloading files.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileDownloadAction implements NamedAction {

    private static final Lock copyFileLock = new ReentrantLock();
    
    private Sample sample;
    private HasFiles fileProvider;
    
    public FileDownloadAction(Sample sample, HasFiles fileProvider) {
        this.sample = sample;
        this.fileProvider = fileProvider;
    }

    @Override
    public String getName() {
        return "Download 3D Image As...";
    }

    @Override
    public void doAction() {
        throw new IllegalStateException("This action must be executed via its popup presenter");
    }
    
    public JMenuItem getPopupPresenter() {

        String[] DOWNLOAD_EXTENSIONS = {"tif", "v3draw", "v3dpbd", "mp4"};
        String itemTitle = "  Download 3D Image As...";
        
        JMenu downloadMenu = new JMenu(itemTitle);
        for(String extension : DOWNLOAD_EXTENSIONS) {
            downloadMenu.add(getDownloadItem(false, extension));
        }
        for(String extension : DOWNLOAD_EXTENSIONS) {
            downloadMenu.add(getDownloadItem(true, extension));
        }
        return downloadMenu;
    }

    protected JMenuItem getDownloadItem(final boolean splitChannels,
                                        final String extension) {
        String itemTitle;
        if (splitChannels) {
            itemTitle = "Split Channel "+extension+" File (Background Task)";
        }
        else {
            itemTitle = extension+" File (Background Task)";
        }
        
        JMenuItem downloadItem = new JMenuItem(itemTitle);

        downloadItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    SampleDownloadWorker worker = new SampleDownloadWorker(sample, fileProvider, extension, splitChannels, copyFileLock);
                    worker.execute();
                } 
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
        
        return downloadItem;
    }
}
