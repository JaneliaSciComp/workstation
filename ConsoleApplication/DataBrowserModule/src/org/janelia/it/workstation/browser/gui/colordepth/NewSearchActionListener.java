package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;

/**
 * Prompts the user to select an alignment space and creates a new, empty color depth search.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class NewSearchActionListener implements ActionListener {

    public NewSearchActionListener() {
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        ActivityLogHelper.logUserAction("NewSearchActionListener.actionPerformed");
  
        SimpleWorker worker = new SimpleWorker() {

            private List<String> alignmentSpaces;
            private List<ColorDepthSearch> allSearches;
            
            @Override
            protected void doStuff() throws Exception {
                alignmentSpaces = DomainMgr.getDomainMgr().getModel().getAlignmentSpaces();
                allSearches = DomainMgr.getDomainMgr().getModel().getAllDomainObjectsByClass(ColorDepthSearch.class);
            }

            @Override
            protected void hadSuccess() {

                String[] values = alignmentSpaces.toArray(new String[alignmentSpaces.size()]);
                
                String alignmentSpace = (String)JOptionPane.showInputDialog(
                        FrameworkImplProvider.getMainFrame(),
                        "Choose an alignment space to search within",
                        "Choose alignment space", JOptionPane.QUESTION_MESSAGE,
                        null,
                        values, values[0]);
                if (alignmentSpace==null) return;

                List<ColorDepthSearch> searches = new ArrayList<>(); 
                for(ColorDepthSearch search : allSearches) {
                    if (alignmentSpace==null || search.getAlignmentSpace().equals(alignmentSpace)) {
                        searches.add(search);
                    }
                }

                String defaultName = ClientDomainUtils.getNextNumberedName(searches, alignmentSpace+" Search", true);
                
                String name = (String) JOptionPane.showInputDialog(ConsoleApp.getMainFrame(), "Search Name:\n", 
                        "", JOptionPane.PLAIN_MESSAGE, null, null, defaultName);
                if (StringUtils.isEmpty(name)) return;
                        
                SimpleWorker worker = new SimpleWorker() {
                    
                    ColorDepthSearch colorDepthSearch;
                    
                    @Override
                    protected void doStuff() throws Exception {
                        colorDepthSearch = new ColorDepthSearch();
                        colorDepthSearch.setName(name);
                        colorDepthSearch.setAlignmentSpace(alignmentSpace);
                        colorDepthSearch = DomainMgr.getDomainMgr().getModel().createColorDepthSearch(colorDepthSearch);
                    }

                    @Override
                    protected void hadSuccess() {
                        DomainExplorerTopComponent.getInstance().selectAndNavigateNodeById(colorDepthSearch.getId());
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        FrameworkImplProvider.handleException(error);
                    }
                };

                worker.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), "Creating search", ""));
                worker.execute();
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkImplProvider.handleException(error);
            }
        };

        worker.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), "Fetching alignment spaces", ""));
        worker.execute();
    }
}
