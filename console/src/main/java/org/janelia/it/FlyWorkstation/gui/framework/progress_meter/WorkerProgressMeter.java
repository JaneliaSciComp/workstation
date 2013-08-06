package org.janelia.it.FlyWorkstation.gui.framework.progress_meter;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.janelia.it.FlyWorkstation.api.entity_model.events.WorkerChangedEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.events.WorkerEndedEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.events.WorkerStartedEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.shared.workers.BackgroundWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * A progress meter for all background worker tasks.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkerProgressMeter extends JDialog {
    
    private static final Logger log = LoggerFactory.getLogger(WorkerProgressMeter.class);
    
    private static final int WIDTH = 400;
    private static final long MAX_DISPLAY_TIME_AFTER_END_MS = 1000 * 60 * 10;
    private static final Font statusFont = new Font("Sans Serif", Font.PLAIN, 10);
    
    private static WorkerProgressMeter progressMeter;
    
    private JPanel mainPanel = new JPanel();
    
    static {
        WorkerProgressMeter.getProgressMeter();
    }

    private WorkerProgressMeter() {
        this(SessionMgr.getBrowser(), "Progress Monitor", false);

//        addWindowFocusListener(new WindowFocusListener() {
//            @Override
//            public void windowLostFocus(WindowEvent e) {
//                setVisible(false);
//            }
//            
//            @Override
//            public void windowGainedFocus(WindowEvent e) {
//            }
//        });
        
//        JComponent glassPane = new GlassPane(null, getContentPane());
//        setGlassPane(glassPane);
        
    }

    public static WorkerProgressMeter getProgressMeter() {
        if (progressMeter == null) {
            progressMeter = new WorkerProgressMeter();
            ModelMgr.getModelMgr().registerOnEventBus(progressMeter);
        }
        return progressMeter;
    }

    private WorkerProgressMeter(Frame frame, String title, boolean modal) {
        super(frame, title, modal);
        setMinimumSize(new Dimension(WIDTH, 100));
        setMaximumSize(new Dimension(WIDTH, 500));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(mainPanel);
        getContentPane().add(scrollPane);
        pack();
    }
    
    public void addWorker(BackgroundWorker worker) {
        mainPanel.add(new MonitoredWorkerPanel(worker));
        update();
        pack();
    }
    
    private void stopWorker(MonitoredWorkerPanel workerPanel) {
        workerPanel.getWorker().cancel(true);
    }

    private void removeWorker(MonitoredWorkerPanel workerPanel) {
        mainPanel.remove(workerPanel);
        repaint();
    }
    
    private void update() {
        Set<MonitoredWorkerPanel> toRemove = new HashSet<MonitoredWorkerPanel>();
        for(Component child : mainPanel.getComponents()) {
            if (child instanceof MonitoredWorkerPanel) {
                MonitoredWorkerPanel workerPanel = (MonitoredWorkerPanel)child;
                workerPanel.update();
                Long endedAt = workerPanel.getEndedAt();
                if (endedAt!=null && (endedAt+MAX_DISPLAY_TIME_AFTER_END_MS > System.currentTimeMillis())) {
                    toRemove.add(workerPanel);
                }
            }
        }
        for(MonitoredWorkerPanel workerPanel : toRemove) {
            mainPanel.remove(workerPanel);
        }
        repaint();
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
    
    @Subscribe
    public void processEvent(WorkerStartedEvent e) {
        log.info("Worker started: {}",e.getWorker().getName());
        addWorker(e.getWorker());
    }
    
    @Subscribe
    public void processEvent(WorkerChangedEvent e) {
        MonitoredWorkerPanel workerPanel = getWorkerPanel(e.getWorker());
        if (workerPanel!=null) {
            log.info("Worker changed: {}, Status:{}",e.getWorker().getName(),e.getWorker().getStatus());
            workerPanel.update();    
        }
    }
    
    @Subscribe
    public void processEvent(WorkerEndedEvent e) {
        MonitoredWorkerPanel workerPanel = getWorkerPanel(e.getWorker());
        if (workerPanel!=null) {
            log.info("Worker ended: {}",e.getWorker().getName());
            workerPanel.update();    
        }
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
            
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
            add(textPanel);
            
            this.nameLabel = new JLabel(worker.getName());
            textPanel.add(nameLabel);
            
            this.statusLabel = new JLabel(worker.getStatus());
            statusLabel.setFont(statusFont);
            textPanel.add(statusLabel);
            
            this.progressBar = new JProgressBar(1, 100);
            progressBar.setMinimumSize(new Dimension(200, 10));
            progressBar.setMaximumSize(new Dimension(200, 10));
            progressBar.setIndeterminate(true);
            add(progressBar);
            
            this.nextButton = new JButton();
            nextButton.setIcon(Icons.getIcon("arrow_forward.gif"));
            nextButton.setFocusable(false);
            nextButton.setBorderPainted(false);
            nextButton.setToolTipText("View results");
            nextButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    worker.executeSuccessCallback();
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
                        WorkerProgressMeter.this.update();
                    }
                    else {
                        stopWorker(MonitoredWorkerPanel.this);  
                        cancelled = true;
                        MonitoredWorkerPanel.this.update();
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
            
            Throwable error = worker.getError();
            if (error!=null) {
                statusLabel.setText("ERROR: "+error.getMessage());
                progressBar.setIndeterminate(false);
            }
            else if (cancelled) {
                statusLabel.setText("Cancelled");
            }
            else {
                if (worker.isDone()) {
                    endedAt = System.currentTimeMillis();
                    progressBar.setValue(100);
                    progressBar.setIndeterminate(false);
                    closeButton.setToolTipText("Remove from this view");
                    nextButton.setEnabled(true);
                }
                boolean hasProgress = worker.getProgress() > 0;
                progressBar.setIndeterminate(!hasProgress);
                statusLabel.setText(worker.getStatus());
            }
            progressBar.setValue(worker.getProgress());
            log.info("Updated worker, progress={}",worker.getProgress());
            revalidate();
            repaint();
        }
    }

//    private class GlassPane extends JComponent {
//        
//        private Point point;
//
//        public GlassPane(JMenuBar menuBar, Container contentPane) {
//            CBListener listener = new CBListener(menuBar, this, contentPane);
//            addMouseListener(listener);
//            addMouseMotionListener(listener);
//            setVisible(true);
//            System.out.println("ADDED");
//        }
//        
//        protected void paintComponent(Graphics g) {
//            g.setColor(Color.red);
//            g.fillOval(5,5,20,20);
//            if (point != null) {
//                g.setColor(Color.red);
//                g.fillOval(point.x - 10, point.y - 10, 20, 20);
//            }
//        }
//        
//        public void setPoint(Point p) {
//            point = p;
//        }
//    }
//
//    private class CBListener extends MouseInputAdapter {
//        private JMenuBar menuBar;
//        private GlassPane glassPane;
//        private Container contentPane;
//
//        public CBListener(JMenuBar menuBar, GlassPane glassPane, Container contentPane) {
//            this.menuBar = menuBar;
//            this.glassPane = glassPane;
//            this.contentPane = contentPane;
//        }
//
//        public void mouseMoved(MouseEvent e) {
//            redispatchMouseEvent(e, false);
//        }
//
//        public void mouseDragged(MouseEvent e) {
//            redispatchMouseEvent(e, false);
//        }
//
//        public void mouseClicked(MouseEvent e) {
//            System.out.println("CLICK");
//            redispatchMouseEvent(e, false);
//        }
//
//        public void mouseEntered(MouseEvent e) {
//            redispatchMouseEvent(e, false);
//        }
//
//        public void mouseExited(MouseEvent e) {
//            System.out.println("EXIT");
//            WorkerProgressMeter.getProgressMeter().setVisible(false);
//            redispatchMouseEvent(e, false);            
//        }
//
//        public void mousePressed(MouseEvent e) {
//            redispatchMouseEvent(e, false);
//        }
//
//        public void mouseReleased(MouseEvent e) {
//            redispatchMouseEvent(e, true);
//        }
//
//        // A basic implementation of redispatching events.
//        private void redispatchMouseEvent(MouseEvent e, boolean repaint) {
//            System.out.println("redispatchMouseEvent "+e);
//            Point glassPanePoint = e.getPoint();
//            Container container = contentPane;
//            Point containerPoint = SwingUtilities.convertPoint(glassPane, glassPanePoint, contentPane);
//            if (containerPoint.y < 0) { // we're not in the content pane
//                if (containerPoint.y + menuBar.getHeight() >= 0) {
//                    // The mouse event is over the menu bar.
//                    // Could handle specially.
//                } else {
//                    // The mouse event is over non-system window
//                    // decorations, such as the ones provided by
//                    // the Java look and feel.
//                    // Could handle specially.
//                }
//            } else {
//                // The mouse event is probably over the content pane.
//                // Find out exactly which component it's over.
//                Component component = SwingUtilities.getDeepestComponentAt(container, containerPoint.x,
//                        containerPoint.y);
//
//                if (component != null) {
//                    System.out.println("FORWARD");
//                    // Forward events over the check box.
//                    Point componentPoint = SwingUtilities.convertPoint(glassPane, glassPanePoint, component);
//                    component.dispatchEvent(new MouseEvent(component, e.getID(), e.getWhen(), e.getModifiers(),
//                            componentPoint.x, componentPoint.y, e.getClickCount(), e.isPopupTrigger()));
//                }
//            }
//
//            // Update the glass pane if requested.
//            if (repaint) {
//                glassPane.setPoint(glassPanePoint);
//                glassPane.repaint();
//            }
//        }
//    }

}
