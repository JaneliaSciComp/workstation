package org.janelia.horta;

import java.awt.BorderLayout;

import org.janelia.workstation.controller.color_slider.SliderPanel;
import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.janelia.workstation.controller.model.TmModelManager;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.horta//ColorSliders//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "ColorSlidersTopComponent",
        iconBase = "org/janelia/horta/ColorSliderIcon16.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.horta.ColorSlidersTopComponent")
@ActionReference(path = "Menu/Window/Horta", position = 150 /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ColorSlidersAction",
        preferredID = "ColorSlidersTopComponent"
)
@Messages({
    "CTL_ColorSlidersAction=Color Sliders",
    "CTL_ColorSlidersTopComponent=ColorSliders",
    "HINT_ColorSlidersTopComponent=Controls for adjusting brightness, contrast, color"
})
public final class ColorSlidersTopComponent extends TopComponent
{
    private ImageColorModel colorMap = null;
    private ImageColorModel selectedColorMap = null;
    private final SliderPanel sliderPanel = new SliderPanel(SliderPanel.ModelType.COLORMODEL_3D);

    public ColorSlidersTopComponent() {
        initComponents();
        sliderPanel.setTop(SliderPanel.VIEW.Horta);
        setName(Bundle.CTL_ColorSlidersTopComponent());
        setToolTipText(Bundle.HINT_ColorSlidersTopComponent());
        
        this.setLayout(new BorderLayout());
        this.add(sliderPanel, BorderLayout.CENTER);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        colorMap = TmModelManager.getInstance().getCurrentView().getColorMode("default");
        setColorMap(colorMap);
    }

    @Override
    public void componentClosed() {

    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    private void setColorMap(ImageColorModel colorMap) {
        if (selectedColorMap == colorMap)
            return; // no change
        if (colorMap == null)
            return; // Remember old colorMap, even when view window focus is lost
        deregisterColorMap(selectedColorMap);
        registerColorMap(colorMap);
    }
    
    private void deregisterColorMap(ImageColorModel colorMap) {
    }

    private void registerColorMap(ImageColorModel colorMap) {
        selectedColorMap = colorMap;
        if (colorMap == null)
            return;
        sliderPanel.setImageColorModel(colorMap);
    }
}
