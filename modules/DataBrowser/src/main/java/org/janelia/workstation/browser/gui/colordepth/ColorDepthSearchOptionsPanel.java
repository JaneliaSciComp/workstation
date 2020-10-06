package org.janelia.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import org.janelia.model.domain.gui.cdmip.ColorDepthLibrary;
import org.janelia.model.domain.gui.cdmip.ColorDepthLibraryUtils;
import org.janelia.model.domain.gui.cdmip.ColorDepthParameters;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;
import org.janelia.workstation.browser.gui.editor.ConfigPanel;
import org.janelia.workstation.browser.gui.editor.SelectionButton;
import org.janelia.workstation.browser.gui.editor.SingleSelectionButton;
import org.janelia.workstation.common.gui.presets.PresetManager;
import org.janelia.workstation.common.gui.presets.PresetSelectionButton;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionEvent;

/**
 * User options panel for a color depth search. Reused between the color depth search editor and the mask dialog. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthSearchOptionsPanel extends ConfigPanel {
    
    // Constants
    private static final String PRESET_NAME = "ColorDepthSearch";
    private static final String THRESHOLD_LABEL_PREFIX = "Data Threshold: ";
    private static final int DEFAULT_THRESHOLD_VALUE = 100;

    private static final String PIX_COLOR_FLUCTUATION_TITLE = "Z Slice Range";
    private static final String XY_SHIFT_TITLE = "XY Shift";
    private static final String MAX_RESULTS_TITLE = "Max Results";

    private static final String THRESHOLD_TOOLTIP = "Everything below this value is not considered in the search images";
    private static final String PCT_PX_TOOLTIP = "Minimum percent pixel match to consider a match";
    private static final String XY_SHIFT_TOOLTIP = "Number of pixels to shift mask in XY plane";
    private static final String PIX_FLUC_TOOLTIP = "Tolerance for how many z slices to search for each pixel in the mask";
    private static final String MAX_RESULTS_TOOLTIP = "Maximum number of results to store";

    private static final LabeledValue defaultSliceRange = new LabeledValue("3", 2);
    private static final List<LabeledValue> rangeValues;
    private static final LabeledValue defaultShiftRange = new LabeledValue("0px", 0);
    private static final List<LabeledValue> shiftValues;
    private static final LabeledValue defaultMaxResults = new LabeledValue("200", 200);
    private static final List<LabeledValue> maxResultsValues;

    static {
        rangeValues = ImmutableList.of(new LabeledValue("1", 1), defaultSliceRange, new LabeledValue("5", 3));
        shiftValues = ImmutableList.of(defaultShiftRange, new LabeledValue("2px", 2), new LabeledValue("4px", 4));
        maxResultsValues = ImmutableList.of(
                new LabeledValue("100", 100),
                defaultMaxResults,
                new LabeledValue("500", 500),
                new LabeledValue("1000", 500));
    }

    // UI Components
    private final PresetSelectionButton presetSelectionButton;
    private final JLabel thresholdLabel;
    private final JPanel thresholdPanel;
    private final JSlider thresholdSlider;
    private final SingleSelectionButton<LabeledValue> xyShiftButton;
    private final SingleSelectionButton<LabeledValue> pixFlucButton;
    private final JCheckBox mirrorCheckbox;
    private final JCheckBox useSegmentationCheckbox;
    private final JCheckBox useGradScoresCheckbox;
    private final JCheckBox allMasks;
    private final SingleSelectionButton<LabeledValue> maxResultsButton;
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
        thresholdSlider.addChangeListener(e -> setThreshold(thresholdSlider.getValue()));
        thresholdPanel = new JPanel(new BorderLayout());
        thresholdPanel.add(thresholdLabel, BorderLayout.NORTH);
        thresholdPanel.add(thresholdSlider, BorderLayout.CENTER);
        thresholdPanel.setToolTipText(THRESHOLD_TOOLTIP);

        pixFlucButton = new LabeledValueButton(PIX_COLOR_FLUCTUATION_TITLE, PIX_FLUC_TOOLTIP) {
            @Override
            public Collection<LabeledValue> getValues() {
                return rangeValues;
            }
        };

        xyShiftButton = new LabeledValueButton(XY_SHIFT_TITLE, XY_SHIFT_TOOLTIP) {
            @Override
            public Collection<LabeledValue> getValues() {
                return shiftValues;
            }
        };

        maxResultsButton = new LabeledValueButton(MAX_RESULTS_TITLE, MAX_RESULTS_TOOLTIP) {
            @Override
            public Collection<LabeledValue> getValues() {
                return maxResultsValues;
            }
        };

        mirrorCheckbox = new JCheckBox("Mirror mask");

        useSegmentationCheckbox = new JCheckBox("Use segmentation");

        useGradScoresCheckbox = new JCheckBox("Use gradient scores");

        allMasks = new JCheckBox("Rerun all masks");
        allMasks.setToolTipText("Rerun all masks or just the currently selected mask?");

        libraryButton = new SelectionButton<ColorDepthLibrary>("Color Depth Libraries") {
            
            @Override
            public Collection<ColorDepthLibrary> getValues() {
                if (libraries == null) return Collections.emptySet();
                return libraries.stream()
                        .filter(library -> isShowLibrary(library))
                        .sorted(Comparator.comparing(ColorDepthLibrary::getIdentifier))
                        .collect(Collectors.toList());
            }

            @Override
            public Set<ColorDepthLibrary> getSelectedValues() {
                if (libraries == null) return Collections.emptySet();
                Map<String, ColorDepthLibrary> libraryLookup = libraries.stream().collect(Collectors.toMap(ColorDepthLibrary::getIdentifier, Function.identity()));
                return search.getCDSTargets().stream()
                        .map(libraryLookup::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
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
                if (libraries ==null) {
                    return;
                }
                List<String> all = libraries.stream()
                        .sorted(Comparator.comparing(ColorDepthLibrary::getIdentifier))
                        .map(ColorDepthLibrary::getIdentifier)
                        .collect(Collectors.toList());
                search.getParameters().setLibraries(all);
                dirty = true;
            }
            
            @Override
            protected void clearSelected() {
                search.clearAllCDSTargets();
                dirty = true;
            }

            @Override
            protected void updateSelection(ColorDepthLibrary library, boolean selected) {
                String cdsTarget = library.getIdentifier();
                if (selected) {
                    search.addCDSTarget(cdsTarget);
                } else {
                    search.removeCDSTarget(cdsTarget);
                }
                dirty = true;
            }
            
        };

        PresetManager<ColorDepthParameters> presetManager = new PresetManager<ColorDepthParameters>(PRESET_NAME) {

            private ObjectMapper mapper = new ObjectMapper();

            @Override
            protected ColorDepthParameters getCurrentSettings() {
                ColorDepthParameters preset = new ColorDepthParameters();
                populateParametersFromUI(preset);
                return preset;
            }

            @Override
            protected void loadSettings(ColorDepthParameters parameters) {
                setParameters(parameters);
            }

            @Override
            protected String serialize(ColorDepthParameters parameters) throws Exception {
                return mapper.writeValueAsString(parameters);
            }

            @Override
            protected ColorDepthParameters deserialize(String json) throws Exception  {
                return mapper.readValue(json, ColorDepthParameters.class);
            }
        };

        this.presetSelectionButton = new PresetSelectionButton(presetManager);
    }

    /**
     * Save changes to the search. Should be called from a background thread.
     * @return
     * @throws Exception
     */
    public ColorDepthSearch saveChanges() throws Exception {

        populateParametersFromUI(search.getParameters());
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

    private void populateParametersFromUI(ColorDepthParameters parameters) {

        Double pixFlucValue;
        try {
            pixFlucValue = new Double(pixFlucButton.getSelectedValue().getValue());
            if (pixFlucValue<1 || pixFlucValue>100) {
                throw new NumberFormatException();
            }
            parameters.setPixColorFluctuation(pixFlucValue);
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    PIX_COLOR_FLUCTUATION_TITLE +" must be a percentage between 1 and 100",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        try {
            int xyShiftValue = xyShiftButton.getSelectedValue().getValue();
            if (xyShiftValue<0) {
                throw new NumberFormatException();
            }
            parameters.setXyShift(xyShiftValue);
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    XY_SHIFT_TITLE+" must be a percentage between 1 and 100",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        parameters.setMirrorMask(mirrorCheckbox.isSelected());
        parameters.setDataThreshold(thresholdSlider.getValue());
        parameters.setUseSegmentation(useSegmentationCheckbox.isSelected());
        parameters.setUseGradientScores(useGradScoresCheckbox.isSelected());
        parameters.setMaxResultsPerMask(maxResultsButton.getSelectedValue().getValue());
    }
    
    public ColorDepthSearch getSearch() {
        return search;
    }

    public boolean isAllMasks() {
        return allMasks.isSelected();
    }

    public void setSearch(ColorDepthSearch colorDepthSearch) {
        this.search = colorDepthSearch;
        setTitle(colorDepthSearch.getName()+" ("+colorDepthSearch.getAlignmentSpace()+")");
        setParameters(colorDepthSearch.getParameters());
        this.dirty = false;
    }

    public void setParameters(ColorDepthParameters parameters) {

        if (parameters.getDataThreshold()!=null) {
            setThreshold(parameters.getDataThreshold());
        }
        else {
            setThreshold(DEFAULT_THRESHOLD_VALUE);
        }

        int currValue = parameters.getPixColorFluctuation() == null ? -1 : parameters.getPixColorFluctuation().intValue();
        LabeledValue value = rangeValues.stream().filter(lv -> lv.getValue()==currValue).findFirst().orElse(defaultSliceRange);
        pixFlucButton.setSelectedValue(value);

        int currShiftValue = parameters.getXyShift() == null ? 0 : parameters.getXyShift();
        LabeledValue value2 = shiftValues.stream().filter(lv -> lv.getValue()==currShiftValue).findFirst().orElse(defaultShiftRange);
        xyShiftButton.setSelectedValue(value2);

        Boolean mirrorMask = parameters.getMirrorMask();
        mirrorCheckbox.setSelected(mirrorMask != null && mirrorMask);

        Boolean useSegmentation = parameters.getUseSegmentation();
        useSegmentationCheckbox.setSelected(useSegmentation != null && useSegmentation);

        Boolean useGradientScores = parameters.getUseGradientScores();
        useGradScoresCheckbox.setSelected(useGradientScores != null && useGradientScores);

        int currMaxResults = parameters.getMaxResultsPerMask() == null ? -1 : parameters.getMaxResultsPerMask();
        LabeledValue value3 = maxResultsValues.stream().filter(lv -> lv.getValue()==currMaxResults).findFirst().orElse(defaultMaxResults);
        maxResultsButton.setSelectedValue(value3);
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
        xyShiftButton.update();
        maxResultsButton.update();
        libraryButton.update();
        removeAllConfigComponents();
        addConfigComponent(presetSelectionButton);
        addConfigComponent(thresholdPanel);
        addConfigComponent(pixFlucButton);
        addConfigComponent(xyShiftButton);
        addConfigComponent(mirrorCheckbox);
        addConfigComponent(useSegmentationCheckbox);
        addConfigComponent(useGradScoresCheckbox);
        addConfigComponent(allMasks);
        addConfigComponent(maxResultsButton);
        addConfigComponent(libraryButton);
    }

    @Override
    protected void titleClicked(MouseEvent e) {
        Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(this,
                Collections.singletonList(search), true, true, true));
    }

    private boolean isShowLibrary(ColorDepthLibrary library) {
        return ClientDomainUtils.hasReadAccess(library) && ColorDepthLibraryUtils.isSearchableVariant(library.getVariant());
    }
    
    public static final class LabeledValue {
        
        private String label;
        private int value;
        
        private LabeledValue(String label, int value) {
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

    private abstract class LabeledValueButton extends SingleSelectionButton<LabeledValue> {

        private LabeledValue currValue;

        public LabeledValueButton(String label, String tooltip) {
            super(label);
            setToolTipText(tooltip);
        }

        @Override
        public LabeledValue getSelectedValue() {
            return currValue;
        }

        @Override
        public String getLabel(LabeledValue sliceRange) {
            return sliceRange.getLabel();
        }

        @Override
        protected void updateSelection(LabeledValue value) {
            this.currValue = value;
        }
    };
}
