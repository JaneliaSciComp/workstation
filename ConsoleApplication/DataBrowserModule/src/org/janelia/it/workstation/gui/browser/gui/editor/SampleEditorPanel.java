package org.janelia.it.workstation.gui.browser.gui.editor;

import de.javasoft.swing.SimpleDropDownButton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.Scrollable;
import static org.janelia.it.jacs.model.domain.enums.FileType.ReferenceMip;
import static org.janelia.it.jacs.model.domain.enums.FileType.SignalMip;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.gui.support.LoadedImagePanel;
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
    
    public SampleEditorPanel() {
        
        filterPanel = new JPanel(new FlowLayout());
        
        objectiveButton = new SimpleDropDownButton("Objective");
        areaButton = new SimpleDropDownButton("Area");
        
        filterPanel.add(objectiveButton);
        filterPanel.add(areaButton);
        
        dataPanel = new JPanel();
        dataPanel.setLayout(new GridBagLayout());
        
//        mainPanel.setPreferredSize(new Dimension());
        
        mainPanel = new ScrollablePanel();
        mainPanel.add(filterPanel, BorderLayout.NORTH);
        mainPanel.add(dataPanel, BorderLayout.CENTER);
        
        
        setViewportView(mainPanel);
        
//        setLayout(new BorderLayout());
//        add(mainPanel, BorderLayout.CENTER);
        
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                for(LoadedImagePanel image : lips) {
                    rescaleImage(image);
                }
            }
        });
        
    }
    
    private void rescaleImage(LoadedImagePanel image) {
        double width = getViewport().getSize().getWidth();
        // If the images are scaled too large, then the GridBagLayout 
        // refuses to render them, so this needs to be large enough to avoid that.
        // This should probably just be equal to the size of the horizontal 
        // insets on the ResultPanels. 
        int fudgeFactor = 30; 
        image.scaleImage((int)Math.ceil(width/2)-fudgeFactor);
        invalidate();
    }
    
    @Override
    public void loadDomainObject(final Sample sample) {
                
        log.debug("loadDomainObject "+sample);
        
        dataPanel.removeAll();
        
        GridBagConstraints c = new GridBagConstraints();
        int y = 0;
                
//        dataPanel.add(new JLabel(sample.getName()));
        
        List<String> objectives = new ArrayList<>(sample.getObjectives().keySet());
        Collections.sort(objectives);
        
        for(String objective : objectives) {
            
            ObjectiveSample objSample = sample.getObjectiveSample(objective);
            
            SamplePipelineRun run = objSample.getLatestRun();
            
            SampleProcessingResult spr = run.getLatestProcessingResult();
            SampleAlignmentResult ar = run.getLatestAlignmentResult();
            
//            JPanel objectivePanel = new JPanel();
//            objectivePanel.setLayout(new BoxLayout(objectivePanel, BoxLayout.PAGE_AXIS));
//            objectivePanel.setLayout(new GridBagLayout());
            
//            GridBagConstraints c = new GridBagConstraints();
            
            c.gridx = 0;
            c.gridy = y++;
            c.insets = new Insets(0, 0, 0, 0);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.PAGE_START;
            c.weightx = 1;
            c.weighty = 1;
            dataPanel.add(getResultPanel(spr, objective+" "+spr.getName()), c);
            
            c.gridx = 0;
            c.gridy = y++;
            c.insets = new Insets(0, 0, 0, 0);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.PAGE_START;
            c.weightx = 1;
            c.weighty = 1;
            dataPanel.add(getResultPanel(ar, objective+" "+ar.getName()+" ("+ar.getAlignmentSpace()+")"), c);
            
//            mainPanel.add(objectivePanel);
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

        updateUI();
        
//        SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                updateUI();
//            }
//        });
    }
    
    private JPanel getResultPanel(HasFiles result, String label) {
        
        JPanel imagePanel = new JPanel();
        imagePanel.setLayout(new GridBagLayout());
        
        if (result==null) return imagePanel;
        
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
        imagePanel.add(getImagePanel(signalMip), c);
        
        c.gridx = 1;
        c.gridy = 0;
        c.insets = new Insets(10, 5, 10, 10);
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weightx = 0.5;
        c.weighty = 1;
        imagePanel.add(getImagePanel(refMip), c);
        
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.add(new JLabel(label), BorderLayout.NORTH);
        resultPanel.add(imagePanel, BorderLayout.CENTER);
        return resultPanel;
    }
    
    private JPanel getImagePanel(String filepath) {
        LoadedImagePanel lip = new LoadedImagePanel(filepath) {
            @Override
            protected void doneLoading() {
                rescaleImage(this);
            }
        };
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
}
