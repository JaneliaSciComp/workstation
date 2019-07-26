package org.janelia.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;

import com.google.common.collect.ImmutableSet;
import org.janelia.model.domain.gui.cdmip.ColorDepthLibrary;
import org.janelia.workstation.browser.gui.editor.ConfigPanel;
import org.janelia.workstation.browser.gui.editor.SelectionButton;
import org.janelia.workstation.browser.gui.editor.SingleSelectionButton;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionEvent;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;

import com.google.common.collect.ImmutableList;

/**
 * User options panel for a color depth search. Reused between the color depth search editor and the mask dialog. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthSearchOptionsPanel extends ConfigPanel {
    
    // Constants
    private static final String THRESHOLD_LABEL_PREFIX = "Data Threshold: ";
    private static final int DEFAULT_THRESHOLD_VALUE = 100;
    private static final NumberFormat PX_FORMATTER = new DecimalFormat("#0.00");
    private static final String PCT_POSITIVE_THRESHOLD = "Min match %";
    private static final String PIX_COLOR_FLUCTUATION = "Z Slice Range";
    private static final String DEFAULT_PCT_PC = "10.00";

    private static final String THRESHOLD_TOOLTIP = "Everything below this value is not considered in the search images";
    private static final String PCT_PX_TOOLTIP = "Minimum percent pixel match to consider a match";
    private static final String PIX_FLUC_TOOLTIP = "Tolerance for how many z slices to search for each pixel in the mask";
    
    private static final ZSliceRange defaultSliceRange = new ZSliceRange("3", 2);
    private static final List<ZSliceRange> rangeValues;

    static {
        rangeValues = ImmutableList.of(new ZSliceRange("1", 1), defaultSliceRange, new ZSliceRange("5", 3));
    }

    // UI Components
    private final JPanel pctPxPanel;
    private final SingleSelectionButton<ZSliceRange> pixFlucButton;
    private final JPanel thresholdPanel;
    private final JSlider thresholdSlider;
    private final JTextField pctPxField;
    private final JLabel thresholdLabel;
    private final SelectionButton<ColorDepthLibrary> libraryButton;
    
    // State
    private boolean dirty = false;
    private ColorDepthSearch search;
    private List<ColorDepthLibrary> libraries; // all possible libraries in the current alignment space
    
    public ColorDepthSearchOptionsPanel() {

        super(false, true, 15, 10);
        
        thresholdLabel = new JLabel();
        thresholdSlider = new JSlider(1, 255);
        thresholdSlider.putClientProperty("Slider.paintThumbArrowShape", Boolean.TRUE);
        thresholdSlider.addChangeListener((ChangeEvent e) -> {
            setThreshold(thresholdSlider.getValue());
        });
        thresholdPanel = new JPanel(new BorderLayout());
        thresholdPanel.add(thresholdLabel, BorderLayout.NORTH);
        thresholdPanel.add(thresholdSlider, BorderLayout.CENTER);
        thresholdPanel.setToolTipText(THRESHOLD_TOOLTIP);
        
        pctPxField = new JTextField(DEFAULT_PCT_PC);
        pctPxField.setHorizontalAlignment(JTextField.RIGHT);
        pctPxField.setColumns(5);
        pctPxPanel = new JPanel(new BorderLayout());
        pctPxPanel.add(new JLabel(PCT_POSITIVE_THRESHOLD), BorderLayout.CENTER);
        pctPxPanel.add(pctPxField, BorderLayout.SOUTH);
        pctPxPanel.setToolTipText(PCT_PX_TOOLTIP);
        
        
        pixFlucButton = new SingleSelectionButton<ZSliceRange>(PIX_COLOR_FLUCTUATION) {
            
            private ZSliceRange currSliceRange;
            
            @Override
            public Collection<ZSliceRange> getValues() {
                return rangeValues;
            }

            @Override
            public ZSliceRange getSelectedValue() {
                return currSliceRange;
            }
            
            @Override
            public String getLabel(ZSliceRange sliceRange) {
                return sliceRange.getLabel();
            }
            
            @Override
            protected void updateSelection(ZSliceRange value) {
                this.currSliceRange = value;
            }
        };
        pixFlucButton.setToolTipText(PIX_FLUC_TOOLTIP);
        
        libraryButton = new SelectionButton<ColorDepthLibrary>("Color Depth Libraries") {
            
            @Override
            public Collection<ColorDepthLibrary> getValues() {
                if (libraries ==null) return ImmutableSet.of();
                return libraries.stream()
                        .filter(library -> isShowLibrary(library))
                        .sorted(Comparator.comparing(ColorDepthLibrary::getIdentifier))
                        .collect(Collectors.toList());
            }

            @Override
            public Set<ColorDepthLibrary> getSelectedValues() {
                if (libraries ==null) return ImmutableSet.of();
                Map<String, ColorDepthLibrary> libraryLookup = libraries.stream().collect(Collectors.toMap(ColorDepthLibrary::getIdentifier, Function.identity()));
                return search.getLibraries().stream().map(library -> libraryLookup.get(library)).filter(Objects::nonNull).collect(Collectors.toSet());
            }

            @Override
            public String getName(ColorDepthLibrary value) {
                return value.getIdentifier();
            }
            
            @Override
            public String getLabel(ColorDepthLibrary value) {
                Integer count = value.getColorDepthCounts().get(search.getAlignmentSpace());
                if (count==null) return value.getIdentifier();
                return String.format("%s (%d images)", value.getIdentifier(), count);
            }
            
            @Override
            protected void selectAll() {
                if (libraries ==null) return;
                List<String> all = libraries.stream()
                        .sorted(Comparator.comparing(ColorDepthLibrary::getIdentifier))
                        .map(ColorDepthLibrary::getIdentifier)
                        .collect(Collectors.toList());
                search.getParameters().setLibraries(all);
                dirty = true;
            }
            
            @Override
            protected void clearSelected() {
                search.getLibraries().clear();
                dirty = true;
            }

            @Override
            protected void updateSelection(ColorDepthLibrary library, boolean selected) {
                if (selected) {
                    search.getLibraries().add(library.getIdentifier());
                }
                else {
                    search.getLibraries().remove(library.getIdentifier());
                }
                dirty = true;
            }
            
        };
    }

    /**
     * Save changes to the search. Should be called from a background thread.
     * @return
     * @throws Exception
     */
    public ColorDepthSearch saveChanges() throws Exception {

        Double pctPositivePixels;
        try {
            pctPositivePixels = new Double(pctPxField.getText());
            if (pctPositivePixels<1 || pctPositivePixels>100) {
                throw new NumberFormatException();
            }
            search.getParameters().setPctPositivePixels(pctPositivePixels);
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    PCT_POSITIVE_THRESHOLD+" must be a percentage between 1 and 100",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        Double pixFlucValue;
        try {
            pixFlucValue = new Double(pixFlucButton.getSelectedValue().getValue());
            if (pixFlucValue<1 || pixFlucValue>100) {
                throw new NumberFormatException();
            }
            search.getParameters().setPixColorFluctuation(pixFlucValue);
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    PIX_COLOR_FLUCTUATION+" must be a percentage between 1 and 100",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        
        search.getParameters().setDataThreshold(thresholdSlider.getValue());
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        
        if (search.getId() == null) {
            // new search
            search = model.createColorDepthSearch(search);
        }
        else {
            search = model.save(search);    
        }
        
        return search;
    }
    
    
    public ColorDepthSearch getSearch() {
        return search;
    }

    public void setSearch(ColorDepthSearch colorDepthSearch) {
        this.search = colorDepthSearch;

        setTitle(colorDepthSearch.getName()+" ("+colorDepthSearch.getAlignmentSpace()+")");
        
        if (colorDepthSearch.getDataThreshold()!=null) {
            setThreshold(colorDepthSearch.getDataThreshold());
        }
        else {
            setThreshold(DEFAULT_THRESHOLD_VALUE);
        }
        
        if (colorDepthSearch.getPctPositivePixels()!=null) {
            pctPxField.setText(PX_FORMATTER.format(colorDepthSearch.getPctPositivePixels()));
        }
        else {
            pctPxField.setText(DEFAULT_PCT_PC);
        }

        int currValue = colorDepthSearch.getPixColorFluctuation() == null ? -1 : colorDepthSearch.getPixColorFluctuation().intValue();
        ZSliceRange value = rangeValues.stream().filter(sliceRange -> sliceRange.getValue()==currValue).findFirst().orElseGet(() -> defaultSliceRange);
        pixFlucButton.setSelectedValue(value);

        this.dirty = false;
    }

    public void setLibraries(List<ColorDepthLibrary> libraries) {
        this.libraries = libraries;
    }

    public void setThreshold(int threshold) {
        thresholdSlider.setValue(threshold);
        thresholdLabel.setText(THRESHOLD_LABEL_PREFIX+threshold);
    }
    
    public void refresh() {
        pixFlucButton.update();
        libraryButton.update();
        removeAllConfigComponents();
        addConfigComponent(thresholdPanel);
        addConfigComponent(pctPxPanel);
        addConfigComponent(pixFlucButton);
        addConfigComponent(libraryButton);
    }

    @Override
    protected void titleClicked(MouseEvent e) {
        Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(this, Arrays.asList(search), true, true, true));
    }

    protected boolean isShowLibrary(ColorDepthLibrary library) {
        return ClientDomainUtils.hasReadAccess(library);
    }
    
    public static final class ZSliceRange {
        
        private String label;
        private int value;
        
        private ZSliceRange(String label, int value) {
            this.label = label;
            this.value = value;
        }
        
        public String getLabel() {
            return label;
        }
        
        public int getValue() {
            return value;
        }
    }
}
