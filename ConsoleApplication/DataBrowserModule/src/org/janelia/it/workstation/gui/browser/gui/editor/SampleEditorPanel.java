package org.janelia.it.workstation.gui.browser.gui.editor;

import static org.janelia.it.jacs.model.domain.enums.FileType.ReferenceMip;
import static org.janelia.it.jacs.model.domain.enums.FileType.SignalMip;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import org.janelia.it.jacs.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.selection.SampleResultSelectionEvent;
import org.janelia.it.workstation.gui.browser.gui.support.LoadedImagePanel;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.browser.gui.support.SelectablePanel;
import org.janelia.it.workstation.gui.browser.model.SampleResult;
import org.janelia.it.workstation.gui.util.MouseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleEditorPanel extends JPanel implements DomainObjectEditor<Sample> {

    private final static Logger log = LoggerFactory.getLogger(SampleEditorPanel.class);
    
    private final static String ALL_VALUE = "all";
    private final static DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd hh:mma");
    
    // UI Components
    private final SampleEditorToolbar toolbar;
    private final JPanel mainPanel;
    private final JScrollPane scrollPane;
    private final JPanel dataPanel;
    private final List<PipelineResultPanel> resultPanels = new ArrayList<>();
    private final Set<LoadedImagePanel> lips = new HashSet<>();
        
    // State
    private String currObjective = ALL_VALUE;
    private String currArea = ALL_VALUE;
    private Sample sample;
    
    // Listener for clicking on result panels
    protected MouseListener resultMouseListener = new MouseHandler() {

        @Override
        protected void popupTriggered(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            PipelineResultPanel resultPanel = getResultPanelAncestor(e.getComponent());
            // Select the button first
            resultPanelSelection(resultPanel, true);
            getButtonPopupMenu(resultPanel.getResult()).show(e.getComponent(), e.getX(), e.getY());
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
            resultPanelSelection(resultPanel, true);
        }
    };
    
    public SampleEditorPanel() {
        
        setLayout(new BorderLayout());
        
        // TODO: load filter from user prefs
        toolbar = new SampleEditorToolbar();
        toolbar.getObjectiveButton().setText("Objective: "+currObjective);
        toolbar.getAreaButton().setText("Area: "+currArea);
        add(toolbar, BorderLayout.NORTH);
        
        dataPanel = new JPanel();
        dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.PAGE_AXIS));

        mainPanel = new ScrollablePanel();
        mainPanel.add(dataPanel, BorderLayout.CENTER);

        scrollPane = new JScrollPane(); 
        scrollPane.setViewportView(mainPanel);
        add(scrollPane, BorderLayout.CENTER);
        
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                for(LoadedImagePanel image : lips) {
                    rescaleImage(image);
                    image.invalidate();
                }
            }
        });
    }
    
    private void resultPanelSelection(PipelineResultPanel resultPanel, boolean isUserDriven) {
        for(PipelineResultPanel otherResultPanel : resultPanels) {
            if (resultPanel != otherResultPanel) {
                otherResultPanel.setSelected(false);
            }
        }
        resultPanel.setSelected(true);
        SampleResult sampleResult = new SampleResult(sample, resultPanel.getResult());
        Events.getInstance().postOnEventBus(new SampleResultSelectionEvent(this, sampleResult, isUserDriven));
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
    
    private JPopupMenu getButtonPopupMenu(PipelineResult result) {
        SampleResultContextMenu popupMenu = new SampleResultContextMenu(sample, result);
        popupMenu.addMenuItems();
        return popupMenu;
    }
    
    @Override
    public String getName() {
        return "Sample Editor";
    }
    
    @Override
    public Object getEventBusListener() {
        return this;
    }
    
    
    @Override
    public void loadDomainObject(final Sample sample) {
                
        this.sample = sample;

        log.info("loadDomainObject({})",sample.getName());
        
        lips.clear();
        resultPanels.clear();
        dataPanel.removeAll();
        
        GridBagConstraints c = new GridBagConstraints();
        int y = 0;
                
        List<String> objectives = new ArrayList<>(sample.getOrderedObjectives());
        Set<String> areaSet = new LinkedHashSet<>();
        
        for(String objective : objectives) {
            
            boolean diplayObjective = true;
            
            if (!currObjective.equals(ALL_VALUE) && !currObjective.equals(objective)) {
                diplayObjective = false;
            }
            
            ObjectiveSample objSample = sample.getObjectiveSample(objective);
            if (objSample==null) continue;
            SamplePipelineRun run = objSample.getLatestRun();
            if (run==null || run.getResults()==null) continue;
            
            for(PipelineResult result : run.getResults()) {

                String area = null;
                if (result instanceof HasAnatomicalArea) {
                    area = ((HasAnatomicalArea)result).getAnatomicalArea();
                }
                
                if (area==null) area = "";
                areaSet.add(area);
                
                boolean display = diplayObjective;
                if (!currArea.equals(ALL_VALUE) && !areEqualOrEmpty(currArea, area)) {
                    display = false;
                }
                
                if (display) {
                    c.gridx = 0;
                    c.gridy = y++;
                    c.fill = GridBagConstraints.BOTH;
                    c.anchor = GridBagConstraints.PAGE_START;
                    c.weightx = 1;
                    c.weighty = 0.9;
                    PipelineResultPanel resultPanel = new PipelineResultPanel(objective, result);
                    resultPanels.add(resultPanel);
                    dataPanel.add(resultPanel);
                }
            }
        }
        
        objectives.add(0, ALL_VALUE);
        populateObjectiveButton(objectives);
        
        List<String> areas = new ArrayList<>(areaSet);
        areas.add(0, ALL_VALUE);
        populateAreaButton(areas);
        
        if (!resultPanels.isEmpty()) {
            resultPanelSelection(resultPanels.get(0), false);
        }
        
        // Force update
        updateUI();
    }

    private void populateObjectiveButton(List<String> objectives) {
        toolbar.getObjectiveButton().setText("Objective: "+currObjective);
        JPopupMenu popupMenu = toolbar.getObjectiveButton().getPopupMenu();
        popupMenu.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String objective : objectives) {
            JMenuItem menuItem = new JRadioButtonMenuItem(objective, objective.equals(currObjective));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setObjective(objective);
                }
            });
            group.add(menuItem);
            popupMenu.add(menuItem);
        }
    }
    
    private void setObjective(String objective) {
        this.currObjective = objective;
        loadDomainObject(sample);
    }
    
    private void populateAreaButton(List<String> areas) {
        toolbar.getAreaButton().setText("Area: "+currArea);
        JPopupMenu popupMenu = toolbar.getAreaButton().getPopupMenu();
        popupMenu.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String area : areas) {
            JMenuItem menuItem = new JRadioButtonMenuItem(area, area.equals(currArea));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setArea(area);
                }
            });
            group.add(menuItem);
            popupMenu.add(menuItem);
        }
    }
    
    private void setArea(String area) {
        this.currArea = area;
        loadDomainObject(sample);
    }
    
    private boolean areEqualOrEmpty(String value1, String value2) {
        if (value1==null || value1.equals("")) {
            return value2==null || value2.equals("");
        }
        if (value2==null || value2.equals("")) {
            return false;
        }
        return value1.equals(value2);
    }
    
    private void rescaleImage(LoadedImagePanel image) {
        double width = image.getParent()==null?0:image.getParent().getSize().getWidth();
        if (width==0) {
            width = scrollPane.getViewport().getSize().getWidth() - 20;
        }
        if (width==0) {
            log.warn("Could not get width from parent or viewport");
            return;
        }
        image.scaleImage((int)Math.ceil(width/2));
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

        private final PipelineResult result;
        private JLabel label = new JLabel();
        private JLabel subLabel = new JLabel();
        
        private PipelineResultPanel(PipelineResult result) {
            
            this.result = result;
            
            int b = SelectablePanel.BORDER_WIDTH;
            setBorder(BorderFactory.createEmptyBorder(b, b, b, b));
            setLayout(new BorderLayout());

            JPanel imagePanel = new JPanel();
            imagePanel.setLayout(new GridLayout(1, 2, 5, 0));

            if (result==null) return;

            String signalMip = DomainUtils.getFilepath(result, SignalMip);
            String refMip = DomainUtils.getFilepath(result, ReferenceMip);

            imagePanel.add(getImagePanel(signalMip));
            imagePanel.add(getImagePanel(refMip));

            JPanel titlePanel = new JPanel(new BorderLayout());
            titlePanel.add(label, BorderLayout.PAGE_START);
            titlePanel.add(subLabel, BorderLayout.PAGE_END);
            
            add(titlePanel, BorderLayout.NORTH);
            add(imagePanel, BorderLayout.CENTER);

            addMouseListener(resultMouseListener);
        }
        
        public PipelineResultPanel(String objective, PipelineResult result) {
            this(result);
            if (result instanceof SampleAlignmentResult) {
                SampleAlignmentResult sar = (SampleAlignmentResult)result;
                label.setText(objective+" "+result.getName()+" ("+sar.getAlignmentSpace()+")");
            }
            else {
                label.setText(objective+" "+result.getName());
            }
            subLabel.setText(dateFormatter.format(result.getCreationDate()).toLowerCase());
        }

        public PipelineResult getResult() {
            return result;
        }
    
        private JPanel getImagePanel(String filepath) {
            LoadedImagePanel lip = new LoadedImagePanel(filepath) {
                @Override
                protected void doneLoading() {
                    rescaleImage(this);
                    invalidate();
                }
            };
            rescaleImage(lip);
            lip.addMouseListener(new MouseForwarder(this, "LoadedImagePanel->PipelineResultPanel"));
            lips.add(lip);
            return lip;
        }
    }
}
