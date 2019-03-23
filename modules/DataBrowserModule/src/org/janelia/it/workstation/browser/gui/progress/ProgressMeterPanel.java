package org.janelia.it.workstation.browser.gui.progress;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicProgressBarUI;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.workers.WorkerChangedEvent;
import org.janelia.it.workstation.browser.events.workers.WorkerEndedEvent;
import org.janelia.it.workstation.browser.events.workers.WorkerStartedEvent;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.workers.BackgroundWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;

/**
 * A progress meter for all background worker tasks.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ProgressMeterPanel extends JPanel {
    
    private static final Logger log = LoggerFactory.getLogger(ProgressMeterPanel.class);

    private static final Font STATUS_FONT = new Font("Sans Serif", Font.PLAIN, 10);
    
    private static ProgressMeterPanel instance;
    
    private final JPanel mainPanel;
    private final JButton clearButton;
    private final JButton clearSuccessButton;

    private final Component glue = Box.createVerticalGlue();
    
    private ProgressMeterPanel() {

        setLayout(new BorderLayout());
        
        this.mainPanel = new JPanel();

        JPanel scrollLayer = new JPanel();
        scrollLayer.setLayout(new BorderLayout());
        scrollLayer.add(mainPanel, BorderLayout.CENTER);
        scrollLayer.add(Box.createVerticalStrut(20), BorderLayout.SOUTH);

        mainPanel.setLayout(new MigLayout(
                "ins 0, flowy, fillx",
                "[grow 1, growprio 1, fill]"
        ));

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(scrollLayer);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        reglueMainPanel();

        add(scrollPane, BorderLayout.CENTER);

        this.clearButton = new JButton("Clear Completed");
        clearButton.setToolTipText("Remove all operations");
        clearButton.setEnabled(false);
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearCompleted(false);
            }
        });

        this.clearSuccessButton = new JButton("Clear Successful");
        clearSuccessButton.setToolTipText("Remove all successful operations");
        clearSuccessButton.setEnabled(false);
        clearSuccessButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearCompleted(true);
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(clearButton);
        buttonPane.add(clearSuccessButton);

        add(buttonPane, BorderLayout.SOUTH);
    }
    
    private void reglueMainPanel() {
        mainPanel.remove(glue);
        mainPanel.add(glue, "growy 100, growprioy 100");
    }

    public static ProgressMeterPanel getSingletonInstance() {
        if (instance == null) {
            instance = new ProgressMeterPanel();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }    
    
    public List<BackgroundWorker> getWorkersInProgress() {
        List<BackgroundWorker> workers = new ArrayList<>();
        for(Component child : mainPanel.getComponents()) {
            if (child instanceof MonitoredWorkerPanel) {
                MonitoredWorkerPanel workerPanel = (MonitoredWorkerPanel)child;
                BackgroundWorker worker = workerPanel.getWorker();
                if (!worker.isDone()) {
                    workers.add(worker);
                }
            }
        }
        return workers;
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
        mainPanel.add(new MonitoredWorkerPanel(worker), "growy 0, growprioy 0");
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
    
    private void clearCompleted(boolean onlySuccessful) {
        Set<MonitoredWorkerPanel> toRemove = new HashSet<>();
        for(Component child : mainPanel.getComponents()) {
            if (child instanceof MonitoredWorkerPanel) {
                MonitoredWorkerPanel workerPanel = (MonitoredWorkerPanel)child;
                if (workerPanel.getEndedAt()!=null) {
                    if (!onlySuccessful || !workerPanel.hasError()) {
                        toRemove.add(workerPanel);
                    }
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
                clearSuccessButton.setEnabled(hasWorkersCompleted());
                mainPanel.revalidate();
                mainPanel.repaint();
            }
        });
    }
        
    @Subscribe
    public void processEvent(WorkerStartedEvent e) {
        log.info("Worker started: {}",e.getWorker().getName());
        addWorker(e.getWorker());
    }

    @Subscribe
    public void processEvent(WorkerChangedEvent e) {
        MonitoredWorkerPanel workerPanel = getWorkerPanel(e.getWorker());
        if (workerPanel!=null) {
            log.debug("Worker changed: {}, Status:{}",e.getWorker().getName(),e.getWorker().getStatus());
            workerPanel.update();
        }
    }

    @Subscribe
    public void processEvent(WorkerEndedEvent e) {
        MonitoredWorkerPanel workerPanel = getWorkerPanel(e.getWorker());
        if (workerPanel!=null) {
            log.debug("Worker ended: {}",e.getWorker().getName());
            workerPanel.update();
            Throwable error = e.getWorker().getError();
            if (error!=null) {
                if (error instanceof CancellationException) {
                    log.info("Worker was cancelled: {}, Status:{}",e.getWorker().getName(),e.getWorker().getStatus());
                }
                else {
                    log.error("Error occurred while running task", error);
                }
            }
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

            setLayout(new MigLayout(
                    "ins 0, wrap 4, fillx",
                    "[grow 10, growprio 10, fill][grow 1, growprio 5][grow 1, growprio 1][grow 1, growprio 1]"
            ));

            this.nameLabel = new JLabel(worker.getName());
            add(nameLabel, "width 10:100:1000");

            this.progressBar = new JProgressBar(1, 100);
            progressBar.setIndeterminate(false);
            progressBar.setUI(new BasicProgressBarUI());
            add(progressBar, "width 10:80:300, gapx 5 5, spany 2");
            
            this.nextButton = new JButton();
            nextButton.setIcon(Icons.getIcon("arrow_forward.gif"));
            nextButton.setFocusable(false);
            nextButton.setBorderPainted(false);
            nextButton.setToolTipText("View results");
            nextButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (worker.getError()!=null) {
                        FrameworkImplProvider.handleException("User viewing background task error", worker.getError());
                    }
                    else {
                        worker.runSuccessCallback();    
                    }
                }
            });
            nextButton.setEnabled(false);
            add(nextButton, "width 32, height 32, gap 0 0 0 0, pad 0 0 0 0, spany 2");
            
            this.closeButton = new JButton();
            closeButton.setIcon(Icons.getIcon("close_red.png"));
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
            add(closeButton, "width 32, height 32, gap 0 0 0 0, pad 0 0 0 0, spany 2");

            this.statusLabel = new JLabel(worker.getStatus());
            statusLabel.setFont(STATUS_FONT);
            add(statusLabel, "width 10:100:1000");
        }
        
        public BackgroundWorker getWorker() {
            return worker;
        }
        
        public Long getEndedAt() {
            return endedAt;
        }

        public boolean hasError() {
            return worker.getError()!=null;
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
                statusLabel.setText("ERROR: "+error.getMessage());
                nextButton.setEnabled(true);
                nextButton.setIcon(Icons.getIcon("bullet_error.png"));
                nextButton.setToolTipText("View error details");
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
            clearSuccessButton.setEnabled(hasWorkersCompleted());
            
            revalidate();
            repaint();
        }
    }

}
