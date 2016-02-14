package org.janelia.it.workstation.gui.browser.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.browser.gui.support.FileDownloadWorker;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;

/**
 * Menu system for downloading files.
 * 
 * @deprecated Use FileExportDialog instead
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileDownloadAction implements NamedAction {

    private static final Lock copyFileLock = new ReentrantLock();
    
    private static final String[] DOWNLOAD_EXTENSIONS = {"tif", "v3draw", "v3dpbd", "mp4", "h5j"};
    
    private List<? extends DomainObject> domainObjectList;
    private ResultDescriptor resultDescriptor;
    
    public FileDownloadAction(List<? extends DomainObject> domainObjectList, ResultDescriptor resultDescriptor) {
        this.domainObjectList = domainObjectList;
        this.resultDescriptor = resultDescriptor;
    }

    @Override
    public String getName() {
        int n = domainObjectList.size();
        return n>1 ? "Download "+n+" 3D Images As..." : "Download 3D Image As...";
    }

    @Override
    public void doAction() {
        throw new IllegalStateException("This action must be executed via its popup presenter");
    }
    
    public JMenuItem getPopupPresenter() {

        if (!domainObjectList.isEmpty()) {
            DomainObject first = domainObjectList.get(0);
            
            if (first instanceof Sample || first instanceof HasFiles) {
                JMenu downloadMenu = new JMenu("  "+getName());
                // LSM download
                if (first instanceof LSMImage) {
                    downloadMenu.add(getDownloadItem(false, Utils.EXTENSION_LSM));
                    downloadMenu.add(getDownloadItem(false, Utils.EXTENSION_LSM_BZ2));
                }
                // Regular file download
                for(String extension : DOWNLOAD_EXTENSIONS) {
                    downloadMenu.add(getDownloadItem(false, extension));
                }
                // Split channel download
                for(String extension : DOWNLOAD_EXTENSIONS) {
                    downloadMenu.add(getDownloadItem(true, extension));
                }
                return downloadMenu;
            }
        }
        
        return null;
    }

    protected JMenuItem getDownloadItem(final boolean splitChannels, final String extension) {
        String itemTitle = extension+" File";
        if (splitChannels) {
            itemTitle = "Split Channel "+itemTitle;
        }
        
        JMenuItem downloadItem = new JMenuItem(itemTitle);

        downloadItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    for(DomainObject domainObject : domainObjectList) {
//                        FileDownloadWorker worker = new FileDownloadWorker(null, domainObject, resultDescriptor, extension, splitChannels, "[Name]/[File Name]", copyFileLock);
//                        worker.execute();
                    }
                } 
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
        
        return downloadItem;
    }
}
