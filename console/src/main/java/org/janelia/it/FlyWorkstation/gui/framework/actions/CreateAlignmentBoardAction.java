package org.janelia.it.FlyWorkstation.gui.framework.actions;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.console.Perspective;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContextFactory;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;

import javax.swing.*;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 10/17/13
 * Time: 10:44 AM
 *
 * Create any "new alignment board" items (buttons, menu items, etc.) based upon this.
 */
public class CreateAlignmentBoardAction implements Action {
    private String name;

    public CreateAlignmentBoardAction( String name ) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    @Override
    public void doAction() {

        // Add button clicked
        final Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
        final String boardName = (String) JOptionPane.showInputDialog(browser, "Board Name:\n",
                "Create Alignment Board", JOptionPane.PLAIN_MESSAGE, null, null, null);
        if ((boardName == null) || (boardName.length() <= 0)) {
            return;
        }

        SimpleWorker worker = new SimpleWorker() {
            private RootedEntity newBoard;
            @Override
            protected void doStuff() throws Exception {
                // Pick an alignment context for the new board
                AlignmentContext[] values = new AlignmentContextFactory().getAllAlignmentContexts();
                final AlignmentContext alignmentContext = (AlignmentContext)JOptionPane.showInputDialog(browser, "Choose an alignment space for this alignment board",
                        "Choose alignment space", JOptionPane.QUESTION_MESSAGE, Icons.getIcon("folder_graphite_palette.png"),
                        values, values[0]);
                if (alignmentContext==null) return;

                // Update database
                newBoard = ModelMgr.getModelMgr().createAlignmentBoard(boardName, alignmentContext.getAlignmentSpaceName(), alignmentContext.getOpticalResolution(), alignmentContext.getPixelResolution());
            }
            @Override
            protected void hadSuccess() {
                // Update Tree UI
                final EntityOutline entityOutline = browser.getEntityOutline();
                entityOutline.totalRefresh(true, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        entityOutline.selectEntityByUniqueId(newBoard.getUniqueId());
                        SessionMgr.getBrowser().setPerspective(Perspective.AlignmentBoard);
                        SessionMgr.getBrowser().getLayersPanel().openAlignmentBoard(newBoard.getEntityId());
                        return null;
                    }
                });
            }
            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.execute();
    }

}
