package org.janelia.it.workstation.gui.browser.gui.editor;

import de.javasoft.swing.SimpleDropDownButton;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import static org.janelia.it.jacs.model.domain.enums.FileType.ReferenceMip;
import static org.janelia.it.jacs.model.domain.enums.FileType.SignalMip;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.gui.support.LoadedImagePanel;
import org.janelia.it.workstation.gui.browser.gui.support.SelectablePanel;
import org.janelia.it.workstation.gui.util.MouseForwarder;
import org.janelia.it.workstation.gui.util.MouseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleEditorPanel extends JScrollPane implements DomainObjectEditor<Sample> {

    private final static Logger log = LoggerFactory.getLogger(SampleEditorPanel.class);
    
    private final JPanel mainPanel;
    private final JPanel filterPanel;
    private final JPanel dataPanel;
    private final SimpleDropDownButton objectiveButton;
    private final SimpleDropDownButton areaButton;
        
    private Set<LoadedImagePanel> lips = new HashSet<>();
    private Set<PipelineResultPanel> resultPanels = new HashSet<>();
    
    
    // Listener for clicking on buttons
    protected MouseListener buttonMouseListener = new MouseHandler() {

        @Override
        protected void popupTriggered(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            PipelineResultPanel resultPanel = getResultPanelAncestor(e.getComponent());
            // Select the button first
            resultPanelSelection(resultPanel);
//            T imageObject = button.getImageObject();
//            if (!button.isSelected()) {
//                selectImageObject(imageObject, true);
//            }
//            getButtonPopupMenu().show(e.getComponent(), e.getX(), e.getY());
            e.consume();
        }

        @Override
        protected void doubleLeftClicked(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            PipelineResultPanel resultPanel = getResultPanelAncestor(e.getComponent());
            resultPanelSelection(resultPanel);
//            buttonDrillDown(button);
            // Double-clicking an image in gallery view triggers an outline selection
            e.consume();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
            if (e.isConsumed()) {
                return;
            }
            PipelineResultPanel resultPanel = getResultPanelAncestor(e.getComponent());
            if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 0) {
                return;
            }
//            hud.setKeyListener(keyListener);
            
            resultPanelSelection(resultPanel);
//            buttonSelection(button, (SystemInfo.isMac && e.isMetaDown()) || e.isControlDown(), e.isShiftDown());
        }
    };
    
    private void resultPanelSelection(PipelineResultPanel resultPanel) {
        for(PipelineResultPanel otherResultPanel : resultPanels) {
            if (resultPanel != otherResultPanel) {
                otherResultPanel.setSelected(false);
//                otherResultPanel.revalidate();
//                otherResultPanel.repaint();
            }
        }
        resultPanel.setSelected(true);
        resultPanel.requestFocus();
    }
    
    private PipelineResultPanel getResultPanelAncestor(Component component) {
        Component c = component;
        while (c!=null) {
            if (c instanceof PipelineResultPanel) {
                return (PipelineResultPanel)c;
            }
            c = c.getParent();
        }
        return null;
    }
    
    public SampleEditorPanel() {
        
        objectiveButton = new SimpleDropDownButton("Objective");
        areaButton = new SimpleDropDownButton("Area");
        
        filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.LINE_AXIS));
        filterPanel.add(objectiveButton);
        filterPanel.add(areaButton);
        filterPanel.add(Box.createHorizontalGlue());
        
        dataPanel = new JPanel();
        dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.PAGE_AXIS));
        
        mainPanel = new ScrollablePanel();
        mainPanel.add(filterPanel, BorderLayout.NORTH);
        mainPanel.add(dataPanel, BorderLayout.CENTER);
        
        setViewportView(mainPanel);
        
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
//                log.info("getSize().getWidth()={}", getSize().getWidth());
//                log.info("getViewport().getSize().getWidth()={}", getViewport().getSize().getWidth());
//                for(PipelineResultPanel resultPanel : resultPanels) {
//                    log.info("resultPanel.getSize().getWidth()={}", resultPanel.getSize().getWidth());
//                    break;
//                }
//                for(LoadedImagePanel image : lips) {
//                    log.info("image.getParent().getSize().getWidth()={}", image.getParent().getSize().getWidth());
//                    break;
//                }
                for(LoadedImagePanel image : lips) {
                    rescaleImage(image);
                    image.invalidate();
                }
            }
        });
        
    }
    
    private void rescaleImage(LoadedImagePanel image) {
        double width = image.getParent()==null?0:image.getParent().getSize().getWidth();
        if (width==0) {
            width = getViewport().getSize().getWidth() - 20;
//            log.warn("Have to get width from viewport");
        }
        if (width==0) {
            log.warn("Could not get width from parent or viewport");
            return;
        }
//        double width = getViewport().getSize().getWidth();
        // If the images are scaled too large, then the GridBagLayout 
        // refuses to render them, so this needs to be large enough to avoid that.
        // This should probably just be equal to the size of the horizontal 
        // insets on the ResultPanels. 
        int fudgeFactor = 30; 
        image.scaleImage((int)Math.ceil(width/2)-fudgeFactor);
    }
    
    @Override
    public void loadDomainObject(final Sample sample) {
                
        log.debug("loadDomainObject "+sample);
        
        lips.clear();
        resultPanels.clear();
        dataPanel.removeAll();
        
        GridBagConstraints c = new GridBagConstraints();
        int y = 0;
                
        List<String> objectives = new ArrayList<>(sample.getObjectives().keySet());
        Collections.sort(objectives);
        
        for(String objective : objectives) {
            
            ObjectiveSample objSample = sample.getObjectiveSample(objective);
            SamplePipelineRun run = objSample.getLatestRun();
            
            if (run==null) continue;
            
            SampleProcessingResult spr = run.getLatestProcessingResult();
            if (spr!=null) {
                c.gridwidth = 1;
                c.gridheight = 1;
                c.gridx = 0;
                c.gridy = y++;
                c.insets = new Insets(0, 0, 0, 0);
                c.fill = GridBagConstraints.BOTH;
                c.anchor = GridBagConstraints.PAGE_START;
                c.weightx = 1;
                c.weighty = 0.9;
                dataPanel.add(getResultPanel(spr, objective+" "+spr.getName()));
            }
            
            SampleAlignmentResult ar = run.getLatestAlignmentResult();
            if (ar!=null) {
                c.gridwidth = 1;
                c.gridheight = 1;
                c.gridx = 0;
                c.gridy = y++;
                c.insets = new Insets(0, 0, 0, 0);
                c.fill = GridBagConstraints.BOTH;
                c.anchor = GridBagConstraints.PAGE_START;
                c.weightx = 1;
                c.weighty = 0.9;
                dataPanel.add(getResultPanel(ar, objective+" "+ar.getName()+" ("+ar.getAlignmentSpace()+")"));
            }
        }
        
        
//        SimpleWorker childLoadingWorker = new SimpleWorker() {
//
//            @Override
//            protected void doStuff() throws Exception {
//                // TODO: get neurons, etc
//            }
//
//            @Override
//            protected void hadSuccess() {
//                
//            }
//
//            @Override
//            protected void hadError(Throwable error) {
//                SessionMgr.getSessionMgr().handleException(error);
//            }
//        };
//
//        childLoadingWorker.execute();

//        updateUI();
    }
    
    private JPanel getResultPanel(PipelineResult result, String label) {
        
        PipelineResultPanel resultPanel = new PipelineResultPanel();
                
        JPanel imagePanel = new JPanel();
        imagePanel.setLayout(new GridLayout(1, 2));
            
        if (result==null) return resultPanel;
        
        String signalMip = DomainUtils.get2dImageFilepath(result, SignalMip);
        String refMip = DomainUtils.get2dImageFilepath(result, ReferenceMip);

        GridBagConstraints c = new GridBagConstraints();
        
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(10, 10, 10, 5);
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weightx = 0.5;
        c.weighty = 1;
        imagePanel.add(getImagePanel(signalMip, resultPanel));
        
        c.gridx = 1;
        c.gridy = 0;
        c.insets = new Insets(10, 5, 10, 10);
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weightx = 0.5;
        c.weighty = 1;
        imagePanel.add(getImagePanel(refMip, resultPanel));
        
        resultPanel.add(new JLabel(label), BorderLayout.NORTH);
        resultPanel.add(imagePanel, BorderLayout.CENTER);
        resultPanels.add(resultPanel);
        return resultPanel;
    }
    
    private JPanel getImagePanel(String filepath, PipelineResultPanel resultPanel) {
        LoadedImagePanel lip = new LoadedImagePanel(filepath) {
            @Override
            protected void doneLoading() {
                rescaleImage(this);
                invalidate();
            }
        };
        rescaleImage(lip);
        lip.addMouseListener(new MouseForwarder(resultPanel, "LoadedImagePanel->PipelineResultPanel"));
        lips.add(lip);
        return lip;
    }
    
    @Override
    public String getName() {
        return "Sample Editor";
    }
    
    @Override
    public Object getEventBusListener() {
        return this;
    }
    
    private class ScrollablePanel extends JPanel implements Scrollable {

        public ScrollablePanel() {
            setLayout(new BorderLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 30;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 300;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private class PipelineResultPanel extends SelectablePanel {

        public PipelineResultPanel() {
            setLayout(new BorderLayout());
            int b = SelectablePanel.BORDER_WIDTH;
            setBorder(BorderFactory.createEmptyBorder(b, b, b, b));
            addMouseListener(buttonMouseListener);
        }

    }
}
