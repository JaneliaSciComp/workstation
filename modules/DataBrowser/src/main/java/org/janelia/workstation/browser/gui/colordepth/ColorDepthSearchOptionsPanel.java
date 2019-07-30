package org.janelia.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
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

    private static final String PCT_POSITIVE_THRESHOLD_TITLE = "Min match %";
    private static final String PIX_COLOR_FLUCTUATION_TITLE = "Z Slice Range";
    private static final String XY_SHIFT_TITLE = "XY Shift";

    private static final String DEFAULT_PCT_PC = "10.00";

    private static final String THRESHOLD_TOOLTIP = "Everything below this value is not considered in the search images";
    private static final String PCT_PX_TOOLTIP = "Minimum percent pixel match to consider a match";
    private static final String XY_SHIFT_TOOLTIP = "Number of pixels to shift mask in XY plane";
    private static final String PIX_FLUC_TOOLTIP = "Tolerance for how many z slices to search for each pixel in the mask";
    
    private static final LabeledValue defaultSliceRange = new LabeledValue("3", 2);
    private static final List<LabeledValue> rangeValues;
    private static final LabeledValue defaultShiftRange = new LabeledValue("0px", 0);
    private static final List<LabeledValue> shiftValues;

    static {
        rangeValues = ImmutableList.of(new LabeledValue("1", 1), defaultSliceRange, new LabeledValue("5", 3));
        shiftValues = ImmutableList.of(defaultShiftRange, new LabeledValue("2px", 2), new LabeledValue("4px", 4));
    }

    // UI Components
    private final JLabel thresholdLabel;
    private final JPanel thresholdPanel;
    private final JSlider thresholdSlider;
    private final JPanel pctPxPanel;
    private final JTextField pctPxField;
    private final SingleSelectionButton<LabeledValue> xyShiftButton;
    private final SingleSelectionButton<LabeledValue> pixFlucButton;
    private final JCheckBox mirrorCheckbox;
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
        pctPxPanel.add(new JLabel(PCT_POSITIVE_THRESHOLD_TITLE), BorderLayout.CENTER);
        pctPxPanel.add(pctPxField, BorderLayout.SOUTH);
        pctPxPanel.setToolTipText(PCT_PX_TOOLTIP);

        pixFlucButton = new SingleSelectionButton<LabeledValue>(PIX_COLOR_FLUCTUATION_TITLE) {
            
            private LabeledValue currSliceRange;
            
            @Override
            public Collection<LabeledValue> getValues() {
                return rangeValues;
            }

            @Override
            public LabeledValue getSelectedValue() {
                return currSliceRange;
            }
            
            @Override
            public String getLabel(LabeledValue sliceRange) {
                return sliceRange.getLabel();
            }
            
            @Override
            protected void updateSelection(LabeledValue value) {
                this.currSliceRange = value;
            }
        };
        pixFlucButton.setToolTipText(PIX_FLUC_TOOLTIP);

        xyShiftButton = new SingleSelectionButton<LabeledValue>(XY_SHIFT_TITLE) {

            private LabeledValue currShiftValue;

            @Override
            public Collection<LabeledValue> getValues() {
                return shiftValues;
            }

            @Override
            public LabeledValue getSelectedValue() {
                return currShiftValue;
            }

            @Override
            public String getLabel(LabeledValue sliceRange) {
                return sliceRange.getLabel();
            }

            @Override
            protected void updateSelection(LabeledValue value) {
                this.currShiftValue = value;
            }
        };
        xyShiftButton.setToolTipText(XY_SHIFT_TOOLTIP);

        mirrorCheckbox = new JCheckBox("Mirror mask");

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
                    PCT_POSITIVE_THRESHOLD_TITLE +" must be a percentage between 1 and 100",
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
                    PIX_COLOR_FLUCTUATION_TITLE +" must be a percentage between 1 and 100",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        try {
            int xyShiftValue = xyShiftButton.getSelectedValue().getValue();
            if (xyShiftValue<0) {
                throw new NumberFormatException();
            }
            search.getParameters().setXyShift(xyShiftValue);
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    XY_SHIFT_TITLE+" must be a percentage between 1 and 100",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        search.getParameters().setMirrorMask(mirrorCheckbox.isSelected());
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
        LabeledValue value = rangeValues.stream().filter(lv -> lv.getValue()==currValue).findFirst().orElseGet(() -> defaultSliceRange);
        pixFlucButton.setSelectedValue(value);

        int currShiftValue = colorDepthSearch.getXyShift() == null ? 0 : colorDepthSearch.getXyShift();
        LabeledValue value2 = shiftValues.stream().filter(lv -> lv.getValue()==currShiftValue).findFirst().orElseGet(() -> defaultShiftRange);
        xyShiftButton.setSelectedValue(value2);

        Boolean mirrorMask = colorDepthSearch.getMirrorMask();
        mirrorCheckbox.setSelected(mirrorMask != null && mirrorMask);

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
        xyShiftButton.update();
        libraryButton.update();
        removeAllConfigComponents();
        addConfigComponent(thresholdPanel);
        addConfigComponent(pctPxPanel);
        addConfigComponent(pixFlucButton);
        addConfigComponent(xyShiftButton);
        addConfigComponent(mirrorCheckbox);
        addConfigComponent(libraryButton);
    }

    @Override
    protected void titleClicked(MouseEvent e) {
        Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(this,
                Collections.singletonList(search), true, true, true));
    }

    private boolean isShowLibrary(ColorDepthLibrary library) {
        return ClientDomainUtils.hasReadAccess(library);
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
}
