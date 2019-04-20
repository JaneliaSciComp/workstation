package org.janelia.workstation.common.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.model.domain.enums.SubjectRole;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

@ActionID(
        category = "Tools",
        id = "TestExceptionActions"
)
@ActionRegistration(
        displayName = "#CTL_TestExceptionActions",
        lazy = false
)
@ActionReferences({
    @ActionReference(path = "Menu/Tools", position = 5000, separatorBefore=4999)
})
@Messages("CTL_TestExceptionActions=Test Exceptions")
public final class TestExceptionActions extends AbstractAction implements Presenter.Menu {

    private JMenu subMenu;
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Do nothing. Action is performed by menu presenter.
    }

    private JMenuItem getSubMenu() {
        if (subMenu==null) {
            subMenu = new JMenu("Test Exceptions");

            JMenuItem edtItem = new JMenuItem("Unhandled EDT Exception");
            edtItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    throw new IllegalStateException("Test Unhandled EDT Exception");
                }
            });
            subMenu.add(edtItem);

            JMenuItem edtBarrageItem = new JMenuItem("Unhandled EDT Exception (10)");
            edtBarrageItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for(int i=0; i<11; i++) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                throw new IllegalStateException("Test Unhandled EDT Exception");
                            }
                        });
                    }
                }
            });
            subMenu.add(edtBarrageItem);
            
            JMenuItem unexpectedItem = new JMenuItem("Unexpected Exception");
            unexpectedItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        try {
                            exceptionTest("Test Unexpected Exception"); // Generate some causes to test the "Caused:" logging
                        }
                        catch (Exception ex2) {
                            throw new Exception("Exception wrapper", ex2);
                        }
                    }
                    catch (Exception ex) {
                        FrameworkAccess.handleException("Testing Unexpected Exception", ex);
                    }
                }
            });
            subMenu.add(unexpectedItem);

            JMenuItem unexpectedItem2 = new JMenuItem("Unexpected Exception (no message)");
            unexpectedItem2.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        exceptionTest(null); // Generate some causes to test the "Caused:" logging
                    }
                    catch (Exception ex) {
                        FrameworkAccess.handleException(ex);
                    }
                }
            });
            subMenu.add(unexpectedItem2);

            JMenuItem oomTestItem = new JMenuItem("Out of Memory Exception");
            oomTestItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    throw new OutOfMemoryError("Test");
                }
            });
            subMenu.add(oomTestItem);
            
            JMenuItem noSpaceItem = new JMenuItem("No Space Left Exception");
            noSpaceItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    throw new RuntimeException("No space left on device");
                }
            });
            subMenu.add(noSpaceItem);

            JMenuItem nbExceptionItem = new JMenuItem("NetBeans-style Exceptions.printStackTrace");
            nbExceptionItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Exceptions.printStackTrace(new Exception("Test Exception"));
                }
            });
            subMenu.add(nbExceptionItem);
        }
        return subMenu;
    }

    private void exceptionTest(String message) {
        exceptionTestInner(message);
    }
    
    private void exceptionTestInner(String message) {
        throw new IllegalStateException(message);
    }
    
    @Override
    public JMenuItem getMenuPresenter() {
        if (isAccessible()) {
            return getSubMenu();
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        return isAccessible();
    }

    public static boolean isAccessible() {
        return AccessManager.authenticatedSubjectIsInGroup(SubjectRole.Admin);
    }
}
