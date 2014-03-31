package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.creation;

import java.awt.Component;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.alignment_board.Launcher;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContextFactory;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapperFactory;
import org.janelia.it.FlyWorkstation.model.domain.Sample;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.nb_action.EntityWrapperCreator;
import org.janelia.it.FlyWorkstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 * Use this with or without a known sample, to create a new Alignment Board.
 * 
 * @author fosterl
 */
@ServiceProvider(service=EntityWrapperCreator.class,path=EntityWrapperCreator.LOOKUP_PATH)
public class AlignmentBoardCreator implements EntityWrapperCreator {
    private RootedEntity sampleRootedEntity;
    
    public void execute() {

        final Component mainFrame = SessionMgr.getMainFrame();
        final Browser browser = SessionMgr.getBrowser();

        SimpleWorker worker = new SimpleWorker() {
            
            private Sample sample;
            private List<AlignmentContext> contexts;
            
            @Override
            protected void doStuff() throws Exception {
                if (getSampleRootedEntity()!=null) {
                    this.sample = (Sample)EntityWrapperFactory.wrap(getSampleRootedEntity());
                    this.contexts = sample.getAvailableAlignmentContexts();
                }
                else {
                    this.contexts = new AlignmentContextFactory().getAllAlignmentContexts();
                }
            }
            
            @Override
            protected void hadSuccess() {
                
                if (sample!=null && contexts.isEmpty()) {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Sample is not aligned to a compatible alignment space", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Pick an alignment context for the new board
                AlignmentContext values[] = new AlignmentContext[contexts.size()];
                contexts.toArray(values);
                final AlignmentContext alignmentContext = (AlignmentContext)JOptionPane.showInputDialog(mainFrame, "Choose an alignment space for this alignment board", 
                        "Choose alignment space", JOptionPane.QUESTION_MESSAGE, Icons.getIcon("folder_graphite_palette.png"), 
                        values, values[0]);
                if (alignmentContext==null) return;
                
                // Pick a name for the new board
                final String boardName = (String) JOptionPane.showInputDialog(mainFrame, "Board Name:\n",
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
                                Launcher launcher = new Launcher();
                                launcher.launch(newBoard.getEntityId());
                            }
                        });
                    }
                    
                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Preparing alignment board...", ""));
                worker.execute();
            }
            
            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Finding alignments...", ""));
        worker.execute();
    }

    /**
     * @return the sampleRootedEntity
     */
    public RootedEntity getSampleRootedEntity() {
        return sampleRootedEntity;
    }

    /**
     * @param sampleRootedEntity the sampleRootedEntity to set
     */
    public void setSampleRootedEntity(RootedEntity sampleRootedEntity) {
        this.sampleRootedEntity = sampleRootedEntity;
    }

    @Override
    public void wrapEntity(RootedEntity e) {
        this.sampleRootedEntity = (RootedEntity)e;
        execute();
    }

    @Override
    public boolean isCompatible(RootedEntity e) {
        final String entityTypeName = e.getEntity().getEntityTypeName();
        return entityTypeName.equals( EntityConstants.TYPE_SAMPLE )   ||
               entityTypeName.equals( EntityConstants.TYPE_NEURON_FRAGMENT );
    }

    @Override
    public String getActionLabel() {
        return "  Open In New Alignment Board Viewer";
    }

}
