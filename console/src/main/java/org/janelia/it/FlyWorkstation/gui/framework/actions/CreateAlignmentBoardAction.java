package org.janelia.it.FlyWorkstation.gui.framework.actions;

import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.console.Perspective;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContextFactory;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapperFactory;
import org.janelia.it.FlyWorkstation.model.domain.Sample;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Create any "new alignment board" items (buttons, menu items, etc.) based upon this.
 * 
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateAlignmentBoardAction implements Action {
    
    private String name;
    private RootedEntity sampleRootedEntity;
    
    public CreateAlignmentBoardAction(String name) {
        this.name = name;
    }
    
    public CreateAlignmentBoardAction(String name, RootedEntity sampleRootedEntity) {
        this.name = name;
        this.sampleRootedEntity = sampleRootedEntity;
    }

    public String getName() {
        return name;
    }
    
    @Override
    public void doAction() {

        final Browser browser = SessionMgr.getBrowser();

        SimpleWorker worker = new SimpleWorker() {
            
            private Sample sample;
            private List<AlignmentContext> contexts;
            
            @Override
            protected void doStuff() throws Exception {
                if (sampleRootedEntity!=null) {
                    this.sample = (Sample)EntityWrapperFactory.wrap(sampleRootedEntity);
                    this.contexts = sample.getAvailableAlignmentContexts();
                }
                else {
                    this.contexts = new AlignmentContextFactory().getAllAlignmentContexts();
                }
            }
            
            @Override
            protected void hadSuccess() {
                
                if (sample!=null && contexts.isEmpty()) {
                    JOptionPane.showMessageDialog(browser,
                            "Sample is not aligned to a compatible alignment space", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Pick an alignment context for the new board
                AlignmentContext values[] = new AlignmentContext[contexts.size()];
                contexts.toArray(values);
                final AlignmentContext alignmentContext = (AlignmentContext)JOptionPane.showInputDialog(browser, "Choose an alignment space for this alignment board", 
                        "Choose alignment space", JOptionPane.QUESTION_MESSAGE, Icons.getIcon("folder_graphite_palette.png"), 
                        values, values[0]);
                if (alignmentContext==null) return;
                
                // Pick a name for the new board
                final String boardName = (String) JOptionPane.showInputDialog(browser, "Board Name:\n",
                        "Create Alignment Board", JOptionPane.PLAIN_MESSAGE, null, null, null);
                if (StringUtils.isEmpty(boardName)) return;
                
                SimpleWorker worker = new SimpleWorker() {
                    
                    private RootedEntity newBoard;
                    
                    @Override
                    protected void doStuff() throws Exception {
                        newBoard = ModelMgr.getModelMgr().createAlignmentBoard(boardName, 
                                alignmentContext.getAlignmentSpaceName(), alignmentContext.getOpticalResolution(), alignmentContext.getPixelResolution());
                        AlignmentBoardContext alignmentBoardContext = new AlignmentBoardContext(newBoard);
                        if (sample!=null) {
                            alignmentBoardContext.addNewAlignedEntity(sample);
                        }
                    }
                    
                    @Override
                    protected void hadSuccess() {
                        // Update Tree UI
                        final EntityOutline entityOutline = browser.getEntityOutline();
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                entityOutline.selectEntityByUniqueId(newBoard.getUniqueId());
                                SessionMgr.getBrowser().setPerspective(Perspective.AlignmentBoard);
                                SessionMgr.getBrowser().getLayersPanel().openAlignmentBoard(newBoard.getEntityId());
                            }
                        });
                    }
                    
                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.setProgressMonitor(new IndeterminateProgressMonitor(browser, "Preparing alignment board...", ""));
                worker.execute();
            }
            
            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(browser, "Finding alignments...", ""));
        worker.execute();
    }
}
