package org.janelia.workstation.common.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.janelia.workstation.core.actions.PopupMenuGenerator;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.spi.actions.AdminActionBuilder;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 * Admin menu item for testing the processing of various types of exceptions.
 */
@ServiceProvider(service = AdminActionBuilder.class, position=50)
public final class TestExceptionsActionBuilder implements AdminActionBuilder {

    private static final TestExceptionActions ACTION = new TestExceptionActions();

    @Override
    public Action getAction() {
        return ACTION;
    }

    private static class TestExceptionActions extends AbstractAction implements PopupMenuGenerator {

        private JMenu subMenu;

        @Override
        public void actionPerformed(ActionEvent e) {
            // Do nothing. Action is performed by menu presenter.
        }

        @Override
        public JMenuItem getPopupPresenter() {
            if (subMenu == null) {
                subMenu = new JMenu("Test Exceptions");

                JMenuItem edtItem = new JMenuItem("Unhandled EDT Exception");
                edtItem.addActionListener(e -> {
                    throw new IllegalStateException("Test Unhandled EDT Exception");
                });
                subMenu.add(edtItem);

                JMenuItem edtBarrageItem = new JMenuItem("Unhandled EDT Exception (x10)");
                edtBarrageItem.addActionListener(e -> {
                    for (int i = 0; i < 11; i++) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                throw new IllegalStateException("Test Unhandled EDT Exception");
                            }
                        });
                    }
                });
                subMenu.add(edtBarrageItem);

                JMenuItem unexpectedItem = new JMenuItem("Unexpected Exception");
                unexpectedItem.addActionListener(e -> {
                    try {
                        try {
                            exceptionTest("Test Unexpected Exception"); // Generate some causes to test the "Caused:" logging
                        } catch (Exception ex2) {
                            throw new Exception("Exception wrapper", ex2);
                        }
                    } catch (Exception ex) {
                        FrameworkAccess.handleException("Testing Unexpected Exception", ex);
                    }
                });
                subMenu.add(unexpectedItem);

                JMenuItem unexpectedItem2 = new JMenuItem("Unexpected Exception (no message)");
                unexpectedItem2.addActionListener(e -> {
                    try {
                        exceptionTest(null); // Generate some causes to test the "Caused:" logging
                    } catch (Exception ex) {
                        FrameworkAccess.handleException(ex);
                    }
                });
                subMenu.add(unexpectedItem2);

                JMenuItem oomTestItem = new JMenuItem("Out of Memory Exception");
                oomTestItem.addActionListener(e -> {
                    throw new OutOfMemoryError("Test");
                });
                subMenu.add(oomTestItem);

                JMenuItem noSpaceItem = new JMenuItem("No Space Left Exception");
                noSpaceItem.addActionListener(e -> {
                    throw new RuntimeException("No space left on device");
                });
                subMenu.add(noSpaceItem);

                JMenuItem noSpaceItem2 = new JMenuItem("No Space Left Exception (on worker thread)");
                noSpaceItem2.addActionListener(e -> {
                    SimpleWorker.runInBackground(() -> {
                        throw new RuntimeException("No space left on device");
                    });
                });
                subMenu.add(noSpaceItem2);

                JMenuItem nbExceptionItem = new JMenuItem("NetBeans-style Exceptions.printStackTrace");
                nbExceptionItem.addActionListener(e -> Exceptions.printStackTrace(new Exception("Test Exception")));
                subMenu.add(nbExceptionItem);
            }
            subMenu.setEnabled(isEnabled());
            return subMenu;
        }

        private void exceptionTest(String message) {
            exceptionTestInner(message);
        }

        private void exceptionTestInner(String message) {
            throw new IllegalStateException(message);
        }
    }
}
