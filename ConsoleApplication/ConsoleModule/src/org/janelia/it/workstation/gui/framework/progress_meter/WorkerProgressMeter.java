package org.janelia.it.workstation.gui.framework.progress_meter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;

import org.janelia.it.workstation.api.entity_model.events.WorkerChangedEvent;
import org.janelia.it.workstation.api.entity_model.events.WorkerEndedEvent;
import org.janelia.it.workstation.api.entity_model.events.WorkerStartedEvent;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.workers.BackgroundWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * A progress meter for all background worker tasks.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkerProgressMeter extends JDialog {
    
    public static final Dimension PREFERRED_DIMENSION = new Dimension(800, 600);

    private static final Logger log = LoggerFactory.getLogger(WorkerProgressMeter.class);
    
    private static final int LABEL_COLUMN_WIDTH = 400;
    private static final int PROGRESS_COLUMN_WIDTH = 150;
    private static final int PROGRESS_BAR_HEIGHT = 12;
    
    private static final Font statusFont = new Font("Sans Serif", Font.PLAIN, 10);
    
    private static WorkerProgressMeter progressMeter;
    
    private JPanel wholeMeterPanel = new JPanel();
    private JPanel mainPanel = new JPanel();
    private JButton clearButton;
    private JButton okButton;
    
    private ImageIcon animatedIcon = Icons.getIcon("cog_small_anim_orange.gif");
    private ImageIcon staticIcon = Icons.getIcon("cog_small.gif");
    private JLabel menuLabel;
    
    static {
        WorkerProgressMeter.getProgressMeter();
    }

    private WorkerProgressMeter(Frame frame, String title, boolean modal) {
        super(frame, title, modal);

        wholeMeterPanel.setPreferredSize(PREFERRED_DIMENSION);
        wholeMeterPanel.setLayout(new BorderLayout());
        setPreferredSize(PREFERRED_DIMENSION);
        
        JPanel scrollLayer = new JPanel();
        scrollLayer.setLayout(new BorderLayout());
        scrollLayer.add(mainPanel, BorderLayout.CENTER);
        scrollLayer.add(Box.createVerticalStrut(20), BorderLayout.SOUTH);
        
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(scrollLayer);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        setLayout(new BorderLayout());
        wholeMeterPanel.add(scrollPane, BorderLayout.CENTER);

        this.clearButton = new JButton("Clear Completed");
        clearButton.setToolTipText("Remove all finished operations");
        clearButton.setEnabled(false);
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearCompleted();
            }
        });

        this.okButton = new JButton("OK");
        okButton.setToolTipText("Hide this window");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(clearButton);
        buttonPane.add(okButton);
        
        wholeMeterPanel.add(buttonPane, BorderLayout.SOUTH);
        add(wholeMeterPanel);
        pack();
    }
    
    public JPanel getMeterPanel() { return wholeMeterPanel; }

    private WorkerProgressMeter() {
        this(SessionMgr.getMainFrame(), "Progress Monitor", false);

        // Exported UI element for use in the top level menu
        menuLabel = new JLabel(staticIcon);
        menuLabel.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                resetPosition();
                setVisible(true);
            }
        });
    }
    
    protected void resetPosition() {
        Point bp = SessionMgr.getMainFrame().getLocation();
        Dimension bs = SessionMgr.getMainFrame().getSize();
        Point tp = menuLabel.getLocation();
        // Fudge the title bar height, since it's probably he same as the menu height
        int titleBarHeight = SessionMgr.getMainFrame().getJMenuBar().getSize().height;
        setLocation(new Point(bp.x + bs.width - getWidth(), bp.y + titleBarHeight + tp.y));
    }

    public static WorkerProgressMeter getProgressMeter() {
        if (progressMeter == null) {
            progressMeter = new WorkerProgressMeter();
            ModelMgr.getModelMgr().registerOnEventBus(progressMeter);
        }
        return progressMeter;
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
        Set<MonitoredWorkerPanel> toRemove = new HashSet<MonitoredWorkerPanel>();
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
                resetPosition();
                clearButton.setEnabled(hasWorkersCompleted());
                mainPanel.revalidate();
                mainPanel.repaint();
            }
        });
    }
    
    public JLabel getMenuLabel() {
        return menuLabel;
    }
    
    private void updateMenuLabel() {
        if (hasWorkersInProgress()) {
            menuLabel.setIcon(animatedIcon);
        }
        else {
            menuLabel.setIcon(staticIcon);
        }
        menuLabel.revalidate();
        menuLabel.repaint();
    }
    
    @Subscribe
    public void processEvent(WorkerStartedEvent e) {
        log.debug("Worker started: {}",e.getWorker().getName());
        addWorker(e.getWorker());
        updateMenuLabel();
        setVisible(true);
    }
    
    @Subscribe
    public void processEvent(WorkerChangedEvent e) {
        MonitoredWorkerPanel workerPanel = getWorkerPanel(e.getWorker());
        if (workerPanel!=null) {
            log.debug("Worker changed: {}, Status:{}",e.getWorker().getName(),e.getWorker().getStatus());
            workerPanel.update();
            updateMenuLabel();
        }
    }
    
    @Subscribe
    public void processEvent(WorkerEndedEvent e) {
        MonitoredWorkerPanel workerPanel = getWorkerPanel(e.getWorker());
        if (workerPanel!=null) {
            log.debug("Worker ended: {}",e.getWorker().getName());
            workerPanel.update();
            updateMenuLabel();
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

        private JLabel nameLabel;
        private JLabel statusLabel;
        private JProgressBar progressBar;
        private JButton nextButton;
        private JButton closeButton;
        
        private Long endedAt;
        private boolean cancelled = false;
        private BackgroundWorker worker;
        
        public MonitoredWorkerPanel(BackgroundWorker backgroundWorker) {
            this.worker = backgroundWorker;
            
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            
            JPanel textPanel = new JPanel();
            textPanel.setPreferredSize(new Dimension(LABEL_COLUMN_WIDTH, 20));
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 10));
            add(textPanel);
            
            this.nameLabel = new JLabel(worker.getName());
            textPanel.add(nameLabel);
            
            this.statusLabel = new JLabel(worker.getStatus());
            statusLabel.setFont(statusFont);
            textPanel.add(statusLabel);
            
            this.progressBar = new JProgressBar(1, 100);
            progressBar.setPreferredSize(new Dimension(PROGRESS_COLUMN_WIDTH, PROGRESS_BAR_HEIGHT));
            progressBar.setIndeterminate(true);
            progressBar.setUI(new BasicProgressBarUI());
            add(progressBar);
            
            this.nextButton = new JButton();
            nextButton.setIcon(Icons.getIcon("arrow_forward.gif"));
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
