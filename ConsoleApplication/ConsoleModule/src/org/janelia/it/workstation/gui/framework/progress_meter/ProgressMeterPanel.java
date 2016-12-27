package org.janelia.it.workstation.gui.framework.progress_meter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;

import org.janelia.it.workstation.api.entity_model.events.WorkerChangedEvent;
import org.janelia.it.workstation.api.entity_model.events.WorkerEndedEvent;
import org.janelia.it.workstation.api.entity_model.events.WorkerStartedEvent;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.workers.BackgroundWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.openide.windows.TopComponent;

/**
 * A progress meter for all background worker tasks.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ProgressMeterPanel extends JPanel {
    
    private static final Logger log = LoggerFactory.getLogger(ProgressMeterPanel.class);
    
    private static final int PROGRESS_BAR_WIDTH = 40;
    private static final int PROGRESS_BAR_HEIGHT = 16;
    private static final Font STATUS_FONT = new Font("Sans Serif", Font.PLAIN, 10);
    
    private static ProgressMeterPanel instance;
    
    private final JPanel mainPanel;
    private final JButton clearButton;
    
    private final Component glue = Box.createVerticalGlue();
    
    private ProgressMeterPanel() {
        
        setLayout(new BorderLayout());
        
        this.mainPanel = new JPanel();
                
        JPanel scrollLayer = new JPanel();
        scrollLayer.setLayout(new BorderLayout());
        scrollLayer.add(mainPanel, BorderLayout.CENTER);
        scrollLayer.add(Box.createVerticalStrut(20), BorderLayout.SOUTH);
        
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(scrollLayer);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        reglueMainPanel();
        
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        this.clearButton = new JButton("Clear Completed");
        clearButton.setToolTipText("Remove all finished operations");
        clearButton.setEnabled(false);
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearCompleted();
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(clearButton);

        add(buttonPane, BorderLayout.SOUTH);
    }
    
    private void reglueMainPanel() {
        mainPanel.remove(glue);
        mainPanel.add(glue);
    }

    public static ProgressMeterPanel getSingletonInstance() {
        if (instance == null) {
            instance = new ProgressMeterPanel();
            ModelMgr.getModelMgr().registerOnEventBus(instance);
        }
        return instance;
    }    
    
    public boolean hasWorkersInProgress() {
        for(Component child : mainPanel.getComponents()) {
            if (child instanceof MonitoredWorkerPanel) {
                MonitoredWorkerPanel workerPanel = (MonitoredWorkerPanel)child;
                if (!workerPanel.getWorker().isDone()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasWorkersCompleted() {
        for(Component child : mainPanel.getComponents()) {
            if (child instanceof MonitoredWorkerPanel) {
                MonitoredWorkerPanel workerPanel = (MonitoredWorkerPanel)child;
                if (workerPanel.getWorker().isDone()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public void addWorker(BackgroundWorker worker) {
        mainPanel.add(new MonitoredWorkerPanel(worker));
        reglueMainPanel();
        refresh();
    }
    
    private void stopWorker(MonitoredWorkerPanel workerPanel) {
        workerPanel.getWorker().cancel(true);
        refresh();
    }

    private void removeWorker(MonitoredWorkerPanel workerPanel) {
        mainPanel.remove(workerPanel);
        refresh();
    }
    
    private void clearCompleted() {
        Set<MonitoredWorkerPanel> toRemove = new HashSet<>();
        for(Component child : mainPanel.getComponents()) {
            if (child instanceof MonitoredWorkerPanel) {
                MonitoredWorkerPanel workerPanel = (MonitoredWorkerPanel)child;
                if (workerPanel.getEndedAt()!=null) {
                    toRemove.add(workerPanel);
                }
            }
        }
        for(MonitoredWorkerPanel workerPanel : toRemove) {
            mainPanel.remove(workerPanel);
        }
        refresh();
    }
    
    private void refresh() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                clearButton.setEnabled(hasWorkersCompleted());
                mainPanel.revalidate();
                mainPanel.repaint();
            }
        });
    }
        
    private void updateMenuLabel(boolean showProgress) {
        TopComponent tc = WindowLocator.getByName(ProgressTopComponent.PREFERRED_ID);
        if (tc!=null) {
            tc.open();
            if (showProgress) {
                tc.requestVisible();
            }
        }
    }
        
    @SuppressWarnings("UnusedDeclaration") // event bus usage not detected by IDE
    @Subscribe
    public void processEvent(WorkerStartedEvent e) {
        log.debug("Worker started: {}",e.getWorker().getName());
        addWorker(e.getWorker());
        updateMenuLabel(true);
    }

    @SuppressWarnings("UnusedDeclaration") // event bus usage not detected by IDE
    @Subscribe
    public void processEvent(WorkerChangedEvent e) {
        MonitoredWorkerPanel workerPanel = getWorkerPanel(e.getWorker());
        if (workerPanel!=null) {
            log.debug("Worker changed: {}, Status:{}",e.getWorker().getName(),e.getWorker().getStatus());
            workerPanel.update();
            updateMenuLabel(false);
        }
    }

    @SuppressWarnings("UnusedDeclaration") // event bus usage not detected by IDE
    @Subscribe
    public void processEvent(WorkerEndedEvent e) {
        MonitoredWorkerPanel workerPanel = getWorkerPanel(e.getWorker());
        if (workerPanel!=null) {
            log.debug("Worker ended: {}",e.getWorker().getName());
            workerPanel.update();
            updateMenuLabel(false);
        }
    }

    private MonitoredWorkerPanel getWorkerPanel(BackgroundWorker worker) {
        for(Component child : mainPanel.getComponents()) {
            if (child instanceof MonitoredWorkerPanel) {
                MonitoredWorkerPanel workerPanel = (MonitoredWorkerPanel)child;
                if (workerPanel.getWorker().equals(worker)) {
                    return workerPanel;
                }
            }
        }
        return null;
    }
    
    private class MonitoredWorkerPanel extends JPanel {

        private final JLabel nameLabel;
        private final JLabel statusLabel;
        private final JProgressBar progressBar;
        private final JButton nextButton;
        private final JButton closeButton;
        
        private Long endedAt;
        private boolean cancelled = false;
        private BackgroundWorker worker;
        
        public MonitoredWorkerPanel(BackgroundWorker backgroundWorker) {
            this.worker = backgroundWorker;
            
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            
            JPanel textPanel = new JPanel();
            textPanel.setPreferredSize(new Dimension(300, 20));
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 10));
            add(textPanel);
            
            this.nameLabel = new JLabel(worker.getName());
            textPanel.add(nameLabel);
            
            this.statusLabel = new JLabel(worker.getStatus());
            statusLabel.setFont(STATUS_FONT);
            textPanel.add(statusLabel);
            
            this.progressBar = new JProgressBar(1, 100);
            progressBar.setPreferredSize(new Dimension(PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT));
            progressBar.setIndeterminate(true);
            progressBar.setUI(new BasicProgressBarUI());
            add(progressBar);
            
            this.nextButton = new JButton();
            nextButton.setIcon(Icons.getIcon("arrow_forward.gif"));
            nextButton.setPreferredSize(new Dimension(32, 32));
            nextButton.setFocusable(false);
            nextButton.setBorderPainted(false);
            nextButton.setToolTipText("View results");
            nextButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    worker.runSuccessCallback();
                }
            });
            nextButton.setEnabled(false);
            add(nextButton);
            
            this.closeButton = new JButton();
            closeButton.setIcon(Icons.getIcon("close_red.png"));
            closeButton.setPreferredSize(new Dimension(32, 32));
            closeButton.setFocusable(false);
            closeButton.setBorderPainted(false);
            closeButton.setToolTipText("Cancel");
            closeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (endedAt!=null || cancelled) {
                        removeWorker(MonitoredWorkerPanel.this);
                    }
                    else {
                        cancelled = true;
                        stopWorker(MonitoredWorkerPanel.this);  
                    }
                }
            });
            add(closeButton);
        }
        
        public BackgroundWorker getWorker() {
            return worker;
        }
        
        public Long getEndedAt() {
            return endedAt;
        }

        public void update() {
            nameLabel.setText(worker.getName());
            statusLabel.setText(worker.getStatus());
            
            if (worker.isDone()) {
                endedAt = System.currentTimeMillis();
                progressBar.setValue(100);
                progressBar.setIndeterminate(false);
                closeButton.setToolTipText("Remove from this view");
            }
            
            Throwable error = worker.getError();
            if (cancelled) {
                statusLabel.setText("Cancelled");
            }
            else if (error!=null) {
                log.error("Error running task",error);
                statusLabel.setText("ERROR: "+error.getMessage());
            }
            else {
                if (worker.isDone()) {
                    nextButton.setEnabled(true);
                }
                else {
                    boolean hasProgress = worker.getProgress() > 0;
                    progressBar.setIndeterminate(!hasProgress);
                }
            }
            progressBar.setValue(worker.getProgress());
            
            // Set tooltips to show the whole value, in case it gets truncated on display
            nameLabel.setToolTipText(nameLabel.getText());
            statusLabel.setToolTipText(statusLabel.getText());
            
            clearButton.setEnabled(hasWorkersCompleted());
            
            revalidate();
            repaint();
        }
        
        public JProgressBar getProgressBar() {
            return progressBar;
        }
    }

}
