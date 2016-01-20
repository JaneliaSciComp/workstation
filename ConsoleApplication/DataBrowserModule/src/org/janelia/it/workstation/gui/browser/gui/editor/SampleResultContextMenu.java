package org.janelia.it.workstation.gui.browser.gui.editor;

import static org.janelia.it.jacs.model.domain.enums.FileType.LosslessStack;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.actions.OpenInFinderAction;
import org.janelia.it.workstation.gui.browser.actions.OpenWithDefaultAppAction;
import org.janelia.it.workstation.gui.browser.components.SampleResultViewerManager;
import org.janelia.it.workstation.gui.browser.components.SampleResultViewerTopComponent;
import org.janelia.it.workstation.gui.browser.components.ViewerUtils;
import org.janelia.it.workstation.gui.browser.gui.support.PopupContextMenu;
import org.janelia.it.workstation.gui.browser.model.SampleResult;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolMgr;
import org.janelia.it.workstation.ws.ExternalClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleResultContextMenu extends PopupContextMenu {

    private static final Logger log = LoggerFactory.getLogger(SampleResultContextMenu.class);
    
    private final Sample sample;
    private final PipelineResult result;

    public SampleResultContextMenu(Sample sample, PipelineResult result) {
        this.sample = sample;
        this.result = result;
    }
    
    public void addMenuItems() {
        
        if (result==null) {
            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
            titleMenuItem.setEnabled(false);
            add(titleMenuItem);
            return;
        }
        
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        
        setNextAddRequiresSeparator(true);
        add(getOpenInSeparationNewViewer());
        add(getOpenInFinderItem());
        add(getOpenWithAppItem());
        add(getNeuronAnnotatorItem());
        add(getVaa3dTriViewItem());
        add(getVaa3d3dViewItem());
        add(getFijiViewerItem());
        add(getDownloadMenu());
        
    }
    
    protected JMenuItem getTitleItem() {
        String name = result.getName();
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }
    
    protected JMenuItem getCopyNameToClipboardItem() {
        JMenuItem copyMenuItem = new JMenuItem("  Copy Name To Clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Transferable t = new StringSelection(result.getName());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getOpenInSeparationNewViewer() {
        JMenuItem copyMenuItem = new JMenuItem("  View Neuron Fragments");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SampleResult sampleResult = new SampleResult(sample, result);
                SampleResultViewerTopComponent targetViewer = ViewerUtils.provisionViewer(SampleResultViewerManager.getInstance(), "editor3"); 
                targetViewer.loadSampleResult(sampleResult, true);
            }
        });
        return copyMenuItem;
    }
    
    protected JMenuItem getOpenInFinderItem() {
        if (!OpenInFinderAction.isSupported()) return null;
        final String path = DomainUtils.getFilepath(result, LosslessStack);
        if (path==null) return null;
        
        JMenuItem menuItem = getNamedActionItem(new OpenInFinderAction(path));
        return menuItem;
    }

    protected JMenuItem getOpenWithAppItem() {
        if (!OpenWithDefaultAppAction.isSupported()) return null;
        final String path = DomainUtils.getFilepath(result, LosslessStack);
        if (path==null) return null;
        return getNamedActionItem(new OpenWithDefaultAppAction(path));
    }

    protected JMenuItem getNeuronAnnotatorItem() {

        final NeuronSeparation separation = result.getLatestSeparationResult();
        if (separation==null) return null;
        
        JMenuItem vaa3dMenuItem = new JMenuItem("  View In Neuron Annotator");
        vaa3dMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    // Check that there is a valid NA instance running
                    List<ExternalClient> clients = SessionMgr.getSessionMgr().getExternalClientsByName(ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME);
                    // If no NA client then try to start one
                    if (clients.isEmpty()) {
                        startNA();
                    }
                    // If NA clients "exist", make sure they are up
                    else {
                        ArrayList<ExternalClient> finalList = new ArrayList<>();
                        for (ExternalClient client : clients) {
                            boolean connected = client.isConnected();
                            if (!connected) {
                                log.debug("Removing client "+client.getName()+" as the heartbeat came back negative.");
                                SessionMgr.getSessionMgr().removeExternalClientByPort(client.getClientPort());
                            }
                            else {
                                finalList.add(client);
                            }
                        }
                        // If none are up then start one
                        if (finalList.isEmpty()) {
                            startNA();
                        }
                    }

                    if (SessionMgr.getSessionMgr()
                            .getExternalClientsByName(ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME).isEmpty()) {
                        JOptionPane.showMessageDialog(mainFrame,
                                "Could not get Neuron Annotator to launch and connect. "
                                        + "Please contact support.", "Launch ERROR", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // TODO: neuron annotator integration
//                    log.debug("Requesting entity view in Neuron Annotator: " + result.getId());
//                    ModelMgr.getModelMgr().notifyEntityViewRequestedInNeuronAnnotator(result.getId());
                } catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
        return vaa3dMenuItem;
    }

    private void startNA() throws Exception {
        log.debug("Client {} is not running. Starting a new instance.",
                ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME);
        ToolMgr.runTool(ToolMgr.TOOL_NA);
        boolean notRunning = true;
        int killCount = 0;
        while (notRunning && killCount < 2) {
            if (SessionMgr.getSessionMgr()
                    .getExternalClientsByName(ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME).isEmpty()) {
                log.debug("Waiting for {} to start.", ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME);
                Thread.sleep(3000);
                killCount++;
            }
            else {
                notRunning = false;
            }
        }
    }
        
    protected JMenuItem getVaa3dTriViewItem() {
        final String path = DomainUtils.getFilepath(result, LosslessStack);
        if (path==null) return null;
        
        JMenuItem vaa3dMenuItem = new JMenuItem("  View In Vaa3D Tri-View");
        vaa3dMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    ToolMgr.openFile(ToolMgr.TOOL_VAA3D, path, null);
                } 
                catch (Exception e) {
                	log.error("Error launching tool", e);
                    JOptionPane.showMessageDialog(mainFrame, "Could not launch this tool. "
                            + "Please choose the appropriate file path from the Tools->Configure Tools area",
                            "ToolInfo Launch ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return vaa3dMenuItem;
    }

    protected JMenuItem getVaa3d3dViewItem() {
        final String path = DomainUtils.getFilepath(result, LosslessStack);
        if (path==null) return null;
        
        JMenuItem vaa3dMenuItem = new JMenuItem("  View In Vaa3D 3D View");
        vaa3dMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    ToolMgr.openFile(ToolMgr.TOOL_VAA3D, path, ToolMgr.MODE_3D);
                } 
                catch (Exception e) {
                	log.error("Error launching tool", e);
                    JOptionPane.showMessageDialog(mainFrame, "Could not launch this tool. "
                            + "Please choose the appropriate file path from the Tools->Configure Tools area",
                            "ToolInfo Launch ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return vaa3dMenuItem;
    }

    protected JMenuItem getFijiViewerItem() {
        final String path = DomainUtils.getFilepath(result, LosslessStack);
        if (path==null) return null;
        
        JMenuItem fijiMenuItem = new JMenuItem("  View In Fiji");
        fijiMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    ToolMgr.openFile(ToolMgr.TOOL_FIJI, path, null);
                } 
                catch (Exception e) {
                	log.error("Error launching tool", e);
                    JOptionPane.showMessageDialog(mainFrame, "Could not launch this tool. "
                                    + "Please choose the appropriate file path from the Tools->Configure Tools area",
                            "Tool Launch ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return fijiMenuItem;
    }
    
    protected JMenuItem getDownloadMenu() {

        final String path = DomainUtils.getFilepath(result, LosslessStack);
        if (path==null) return null;

        String[] DOWNLOAD_EXTENSIONS = {"tif", "v3draw", "v3dpbd", "mp4"};
        String itemTitle = "  Download 3D Image As...";
        
        JMenu downloadMenu = new JMenu(itemTitle);
        for(String extension : DOWNLOAD_EXTENSIONS) {
            add(downloadMenu, getDownloadItem(path, false, extension));
        }
        for(String extension : DOWNLOAD_EXTENSIONS) {
            add(downloadMenu, getDownloadItem(path, true, extension));
        }
        return downloadMenu;
    }
    
    protected JMenuItem getDownloadItem(final String filepath,
                                        final boolean splitChannels,
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
                    // TODO: adapt for domain objects
//                        org.janelia.it.workstation.shared.workers.SampleDownloadWorker sampleDownloadWorker =
//                                new org.janelia.it.workstation.shared.workers.SampleDownloadWorker(entity, extension, splitChannels, copyFileLock);
//                        sampleDownloadWorker.execute();
                } catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
        
        return downloadItem;
    }
}
