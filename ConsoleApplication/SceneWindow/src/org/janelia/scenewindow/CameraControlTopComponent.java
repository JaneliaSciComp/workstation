/* 
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.scenewindow;

import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.janelia.geometry3d.CoordinateAxis;
import org.janelia.geometry3d.Rotation;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.ViewSlab;
import org.janelia.geometry3d.Vector3;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.scenewindow//CameraControl//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "CameraControlTopComponent",
        iconBase = "org/janelia/scenewindow/camera.gif",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "properties", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.scenewindow.CameraControlTopComponent")
@ActionReference(path = "Menu/Window/Horta" , position = 100 )
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_CameraControlAction",
        preferredID = "CameraControlTopComponent"
)
@Messages({
    "CTL_CameraControlAction=Camera Control",
    "CTL_CameraControlTopComponent=Camera Control",
    "HINT_CameraControlTopComponent=Adjust the current camera"
})
public final class CameraControlTopComponent extends TopComponent 
implements LookupListener
{
    private Lookup.Result<Vantage> vantageResult = null;
    private Lookup.Result<ViewSlab> viewSlabResult = null;
    
    private Observer vantageObserver;
    private Vantage selectedVantage = null;
    private Observer viewSlabObserver;
    private ViewSlab selectedViewSlab = null;
    
    private final float DEFAULT_ZOOM = 50.0f; // TODO something intelligent
    // Create custom spinner models, so we can adjust step size based on Zoom
    private final SpinnerNumberModel zoomSpinnerModel = new SpinnerNumberModel(
            Float.valueOf(DEFAULT_ZOOM), Float.MIN_NORMAL, null, Float.valueOf(0.1f));
    private final SpinnerNumberModel focusXSpinnerModel = new SpinnerNumberModel(
            Float.valueOf(0.0f), null, null, Float.valueOf(0.1f));
    private final SpinnerNumberModel focusYSpinnerModel = new SpinnerNumberModel(
            Float.valueOf(0.0f), null, null, Float.valueOf(0.1f));
    private final SpinnerNumberModel focusZSpinnerModel = new SpinnerNumberModel(
            Float.valueOf(0.0f), null, null, Float.valueOf(0.1f));
    private final SpinnerNumberModel localTranslationXSpinnerModel = new SpinnerNumberModel(
            Float.valueOf(0.0f), null, null, Float.valueOf(0.1f));
    private final SpinnerNumberModel localTranslationYSpinnerModel = new SpinnerNumberModel(
            Float.valueOf(0.0f), null, null, Float.valueOf(0.1f));
    private final SpinnerNumberModel localTranslationZSpinnerModel = new SpinnerNumberModel(
            Float.valueOf(0.0f), null, null, Float.valueOf(0.1f));    
    private final String micrometers = "\u00B5"+"m";

    public CameraControlTopComponent() {
        initComponents();
        setName(Bundle.CTL_CameraControlTopComponent());
        setToolTipText(Bundle.HINT_CameraControlTopComponent());

        // fixed precision output for spinners
        for (JSpinner s : new JSpinner[] {
            zoomSpinner, 
            focusXSpinner,
            focusYSpinner,
            focusZSpinner,
            rotXSpinner,
            rotYSpinner,
            rotZSpinner}) 
        {
            s.setEditor(new JSpinner.NumberEditor(s, "0.00"));
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        focusPanel = new javax.swing.JPanel();
        focusXSpinner = new javax.swing.JSpinner();
        focusXLabel = new javax.swing.JLabel();
        focusYLabel = new javax.swing.JLabel();
        focusYSpinner = new javax.swing.JSpinner();
        focusZLabel = new javax.swing.JLabel();
        focusZSpinner = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        resetFocusButton = new javax.swing.JButton();
        localTranslateXSpinner = new javax.swing.JSpinner();
        localTranslateYSpinner = new javax.swing.JSpinner();
        localTranslateZSpinner = new javax.swing.JSpinner();
        rotationPanel = new javax.swing.JPanel();
        rotXSpinner = new javax.swing.JSpinner();
        rotYSpinner = new javax.swing.JSpinner();
        rotZSpinner = new javax.swing.JSpinner();
        rotXLabel = new javax.swing.JLabel();
        rotYLabel = new javax.swing.JLabel();
        rotZLabel = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        resetRotationButton = new javax.swing.JButton();
        stayUpCheckBox = new javax.swing.JCheckBox();
        localRotXSpinner = new javax.swing.JSpinner();
        localRotYSpinner = new javax.swing.JSpinner();
        localRotZSpinner = new javax.swing.JSpinner();
        zoomPanl = new javax.swing.JPanel();
        resetZoomButton = new javax.swing.JButton();
        zoomSpinner = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        resetAllButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        nearSlabSlider = new javax.swing.JSlider();
        farSlabSlider = new javax.swing.JSlider();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();

        focusPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.focusPanel.border.title"))); // NOI18N

        focusXSpinner.setModel(focusXSpinnerModel);
        focusXSpinner.setMinimumSize(new java.awt.Dimension(6, 20));
        focusXSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                focusXSpinnerStateChanged(evt);
            }
        });

        focusXLabel.setLabelFor(focusXSpinner);
        org.openide.awt.Mnemonics.setLocalizedText(focusXLabel, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.focusXLabel.text")); // NOI18N

        focusYLabel.setLabelFor(focusYSpinner);
        org.openide.awt.Mnemonics.setLocalizedText(focusYLabel, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.focusYLabel.text")); // NOI18N

        focusYSpinner.setModel(focusYSpinnerModel);
        focusYSpinner.setMinimumSize(new java.awt.Dimension(6, 20));
        focusYSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                focusYSpinnerStateChanged(evt);
            }
        });

        focusZLabel.setLabelFor(focusZSpinner);
        org.openide.awt.Mnemonics.setLocalizedText(focusZLabel, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.focusZLabel.text")); // NOI18N

        focusZSpinner.setModel(focusZSpinnerModel);
        focusZSpinner.setMinimumSize(new java.awt.Dimension(6, 20));
        focusZSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                focusZSpinnerStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, micrometers);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, micrometers);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, micrometers);

        org.openide.awt.Mnemonics.setLocalizedText(resetFocusButton, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.resetFocusButton.text")); // NOI18N
        resetFocusButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetFocusButtonActionPerformed(evt);
            }
        });

        localTranslateXSpinner.setModel(localTranslationXSpinnerModel);
        localTranslateXSpinner.setToolTipText(org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.localTranslateXSpinner.toolTipText")); // NOI18N
        localTranslateXSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                localTranslateXSpinnerStateChanged(evt);
            }
        });

        localTranslateYSpinner.setModel(localTranslationYSpinnerModel);
        localTranslateYSpinner.setToolTipText(org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.localTranslateYSpinner.toolTipText")); // NOI18N
        localTranslateYSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                localTranslateYSpinnerStateChanged(evt);
            }
        });

        localTranslateZSpinner.setModel(localTranslationZSpinnerModel);
        localTranslateZSpinner.setToolTipText(org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.localTranslateZSpinner.toolTipText")); // NOI18N
        localTranslateZSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                localTranslateZSpinnerStateChanged(evt);
            }
        });

        javax.swing.GroupLayout focusPanelLayout = new javax.swing.GroupLayout(focusPanel);
        focusPanel.setLayout(focusPanelLayout);
        focusPanelLayout.setHorizontalGroup(
            focusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(focusPanelLayout.createSequentialGroup()
                .addGroup(focusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(focusPanelLayout.createSequentialGroup()
                        .addGroup(focusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(focusPanelLayout.createSequentialGroup()
                                .addComponent(localTranslateXSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(focusXLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(focusXSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(focusPanelLayout.createSequentialGroup()
                                .addComponent(localTranslateYSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(focusYLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(focusYSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(focusPanelLayout.createSequentialGroup()
                                .addComponent(localTranslateZSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(focusZLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(focusZSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(focusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel1)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, focusPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(resetFocusButton)))
                .addContainerGap())
        );
        focusPanelLayout.setVerticalGroup(
            focusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(focusPanelLayout.createSequentialGroup()
                .addGroup(focusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(focusXSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(focusXLabel)
                    .addComponent(jLabel1)
                    .addComponent(localTranslateXSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(focusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(focusYSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(focusYLabel)
                    .addComponent(jLabel2)
                    .addComponent(localTranslateYSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(focusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(focusZSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(focusZLabel)
                    .addComponent(jLabel3)
                    .addComponent(localTranslateZSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(resetFocusButton))
        );

        rotationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.rotationPanel.border.title"))); // NOI18N

        rotXSpinner.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(-360.0f), Float.valueOf(360.0f), Float.valueOf(1.0f)));
        rotXSpinner.setToolTipText(org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.rotXSpinner.toolTipText")); // NOI18N
        rotXSpinner.setMinimumSize(new java.awt.Dimension(6, 20));
        rotXSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rotXSpinnerStateChanged(evt);
            }
        });

        rotYSpinner.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(-360.0f), Float.valueOf(360.0f), Float.valueOf(1.0f)));
        rotYSpinner.setToolTipText(org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.rotYSpinner.toolTipText")); // NOI18N
        rotYSpinner.setMinimumSize(new java.awt.Dimension(6, 20));
        rotYSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rotYSpinnerStateChanged(evt);
            }
        });

        rotZSpinner.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(-360.0f), Float.valueOf(360.0f), Float.valueOf(1.0f)));
        rotZSpinner.setToolTipText(org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.rotZSpinner.toolTipText")); // NOI18N
        rotZSpinner.setMinimumSize(new java.awt.Dimension(6, 20));
        rotZSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rotZSpinnerStateChanged(evt);
            }
        });

        rotXLabel.setLabelFor(focusXSpinner);
        org.openide.awt.Mnemonics.setLocalizedText(rotXLabel, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.rotXLabel.text")); // NOI18N

        rotYLabel.setLabelFor(focusYSpinner);
        org.openide.awt.Mnemonics.setLocalizedText(rotYLabel, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.rotYLabel.text")); // NOI18N

        rotZLabel.setLabelFor(focusZSpinner);
        org.openide.awt.Mnemonics.setLocalizedText(rotZLabel, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.rotZLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.jLabel4.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.jLabel5.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.jLabel6.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(resetRotationButton, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.resetRotationButton.text")); // NOI18N
        resetRotationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetRotationButtonActionPerformed(evt);
            }
        });

        stayUpCheckBox.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(stayUpCheckBox, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.stayUpCheckBox.text")); // NOI18N
        stayUpCheckBox.setToolTipText(org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.stayUpCheckBox.toolTipText")); // NOI18N
        stayUpCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                stayUpCheckBoxStateChanged(evt);
            }
        });
        stayUpCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stayUpCheckBoxActionPerformed(evt);
            }
        });

        localRotXSpinner.setToolTipText(org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.localRotXSpinner.toolTipText")); // NOI18N
        localRotXSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                localRotXSpinnerStateChanged(evt);
            }
        });

        localRotYSpinner.setToolTipText(org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.localRotYSpinner.toolTipText")); // NOI18N
        localRotYSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                localRotYSpinnerStateChanged(evt);
            }
        });

        localRotZSpinner.setToolTipText(org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.localRotZSpinner.toolTipText")); // NOI18N
        localRotZSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                localRotZSpinnerStateChanged(evt);
            }
        });

        javax.swing.GroupLayout rotationPanelLayout = new javax.swing.GroupLayout(rotationPanel);
        rotationPanel.setLayout(rotationPanelLayout);
        rotationPanelLayout.setHorizontalGroup(
            rotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rotationPanelLayout.createSequentialGroup()
                .addGroup(rotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(rotationPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(rotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(rotationPanelLayout.createSequentialGroup()
                                .addComponent(stayUpCheckBox)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, rotationPanelLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(resetRotationButton))))
                    .addGroup(rotationPanelLayout.createSequentialGroup()
                        .addGroup(rotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(localRotYSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(localRotZSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(localRotXSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(rotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rotYLabel)
                            .addComponent(rotXLabel)
                            .addComponent(rotZLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(rotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rotYSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(rotXSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(rotZSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(rotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(rotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        rotationPanelLayout.setVerticalGroup(
            rotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rotationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(stayUpCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(rotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rotXSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rotXLabel)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(localRotXSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(7, 7, 7)
                .addGroup(rotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rotYSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rotYLabel)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(localRotYSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(rotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rotZSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rotZLabel)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(localRotZSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(resetRotationButton))
        );

        zoomPanl.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.zoomPanl.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(resetZoomButton, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.resetZoomButton.text")); // NOI18N
        resetZoomButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetZoomButtonActionPerformed(evt);
            }
        });

        zoomSpinner.setModel(zoomSpinnerModel);
        zoomSpinner.setMinimumSize(new java.awt.Dimension(6, 20));
        zoomSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                zoomSpinnerStateChanged(evt);
            }
        });

        jLabel7.setLabelFor(zoomSpinner);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel7, micrometers);

        javax.swing.GroupLayout zoomPanlLayout = new javax.swing.GroupLayout(zoomPanl);
        zoomPanl.setLayout(zoomPanlLayout);
        zoomPanlLayout.setHorizontalGroup(
            zoomPanlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(zoomPanlLayout.createSequentialGroup()
                .addGroup(zoomPanlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(zoomPanlLayout.createSequentialGroup()
                        .addComponent(zoomSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, zoomPanlLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(resetZoomButton)))
                .addContainerGap())
        );
        zoomPanlLayout.setVerticalGroup(
            zoomPanlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, zoomPanlLayout.createSequentialGroup()
                .addGroup(zoomPanlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(zoomSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(resetZoomButton))
        );

        org.openide.awt.Mnemonics.setLocalizedText(resetAllButton, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.resetAllButton.text")); // NOI18N
        resetAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetAllButtonActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.jPanel1.border.title"))); // NOI18N

        nearSlabSlider.setMinimum(1);
        nearSlabSlider.setMinorTickSpacing(25);
        nearSlabSlider.setValue(90);
        nearSlabSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                nearSlabSliderStateChanged(evt);
            }
        });

        farSlabSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                farSlabSliderStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel8, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.jLabel8.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel9, org.openide.util.NbBundle.getMessage(CameraControlTopComponent.class, "CameraControlTopComponent.jLabel9.text")); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(farSlabSlider, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(nearSlabSlider, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addComponent(jLabel9)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(nearSlabSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel8)
                        .addGap(12, 12, 12)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(farSlabSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(focusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(rotationPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(zoomPanl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(resetAllButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(focusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rotationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(zoomPanl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(resetAllButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void focusXSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_focusXSpinnerStateChanged
        updateVantageProperties();
    }//GEN-LAST:event_focusXSpinnerStateChanged

    private void focusYSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_focusYSpinnerStateChanged
        updateVantageProperties();
    }//GEN-LAST:event_focusYSpinnerStateChanged

    private void focusZSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_focusZSpinnerStateChanged
        updateVantageProperties();
    }//GEN-LAST:event_focusZSpinnerStateChanged

    private void zoomSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_zoomSpinnerStateChanged
        // Adjust step size, proportional to zoom value, for a logarithmic effect
        float zoom = ((Number)zoomSpinner.getValue()).floatValue();
        zoomSpinnerModel.setStepSize(0.02f * zoom);
        float translationStep = 0.008f * zoom;
        focusXSpinnerModel.setStepSize(translationStep);
        focusYSpinnerModel.setStepSize(translationStep);
        focusZSpinnerModel.setStepSize(translationStep);
        localTranslationXSpinnerModel.setStepSize(translationStep);
        localTranslationYSpinnerModel.setStepSize(translationStep);
        localTranslationZSpinnerModel.setStepSize(translationStep);
        // Change camera (maybe)
        updateVantageProperties();
    }//GEN-LAST:event_zoomSpinnerStateChanged

    private void resetRotationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetRotationButtonActionPerformed
        if (selectedVantage == null)
            return;
        selectedVantage.resetRotation();
        selectedVantage.notifyObservers();
    }//GEN-LAST:event_resetRotationButtonActionPerformed

    private void rotZSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rotZSpinnerStateChanged
        updateVantageProperties();
    }//GEN-LAST:event_rotZSpinnerStateChanged

    private void rotYSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rotYSpinnerStateChanged
        updateVantageProperties();
    }//GEN-LAST:event_rotYSpinnerStateChanged

    private void rotXSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rotXSpinnerStateChanged
        updateVantageProperties();
    }//GEN-LAST:event_rotXSpinnerStateChanged

    private void resetFocusButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetFocusButtonActionPerformed
        if (selectedVantage == null)
            return;
        selectedVantage.resetFocus();
        selectedVantage.notifyObservers();
    }//GEN-LAST:event_resetFocusButtonActionPerformed

    private void resetZoomButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetZoomButtonActionPerformed
        if (selectedVantage == null)
            return;
        // TODO - reset zoom based on scene contents
        selectedVantage.resetScale();
        selectedVantage.notifyObservers();
    }//GEN-LAST:event_resetZoomButtonActionPerformed

    private void resetAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetAllButtonActionPerformed
        if (selectedVantage != null) {
            selectedVantage.resetView();
            selectedVantage.notifyObservers();
        }
        nearSlabSlider.setValue(50);
        farSlabSlider.setValue(50);
    }//GEN-LAST:event_resetAllButtonActionPerformed

    private void stayUpCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stayUpCheckBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_stayUpCheckBoxActionPerformed

    private void stayUpCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_stayUpCheckBoxStateChanged
        if (selectedVantage == null) return;
        selectedVantage.setConstrainedToUpDirection(stayUpCheckBox.isSelected());
    }//GEN-LAST:event_stayUpCheckBoxStateChanged

    private void localRotXSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_localRotXSpinnerStateChanged
        incrementLocalRotation(localRotXSpinner, new Vector3(1, 0, 0));
    }//GEN-LAST:event_localRotXSpinnerStateChanged

    private void localRotYSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_localRotYSpinnerStateChanged
        incrementLocalRotation(localRotYSpinner, new Vector3(0, -1, 0));
    }//GEN-LAST:event_localRotYSpinnerStateChanged

    private void localRotZSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_localRotZSpinnerStateChanged
        incrementLocalRotation(localRotZSpinner, new Vector3(0, 0, 1));
    }//GEN-LAST:event_localRotZSpinnerStateChanged

    private void localTranslateXSpinnerStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_localTranslateXSpinnerStateChanged
    {//GEN-HEADEREND:event_localTranslateXSpinnerStateChanged
        incrementLocalTranslation(localTranslateXSpinner, new Vector3(1, 0, 0));
    }//GEN-LAST:event_localTranslateXSpinnerStateChanged

    private void localTranslateYSpinnerStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_localTranslateYSpinnerStateChanged
    {//GEN-HEADEREND:event_localTranslateYSpinnerStateChanged
        incrementLocalTranslation(localTranslateYSpinner, new Vector3(0, -1, 0));
    }//GEN-LAST:event_localTranslateYSpinnerStateChanged

    private void localTranslateZSpinnerStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_localTranslateZSpinnerStateChanged
    {//GEN-HEADEREND:event_localTranslateZSpinnerStateChanged
        incrementLocalTranslation(localTranslateZSpinner, new Vector3(0, 0, -1));
    }//GEN-LAST:event_localTranslateZSpinnerStateChanged

    private void nearSlabSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_nearSlabSliderStateChanged
        // TODO add your handling code here:
        if (selectedViewSlab == null)
            return;
        int sliderValue = nearSlabSlider.getValue();
        float zNearRelative = (float)sliderValue/100.1f; // (range 0.01 - 0.999)
        selectedViewSlab.setzNearRelative(zNearRelative);
        selectedViewSlab.getChangeObservable().notifyObservers();
    }//GEN-LAST:event_nearSlabSliderStateChanged

    private void farSlabSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_farSlabSliderStateChanged
        // TODO add your handling code here:
        if (selectedViewSlab == null)
            return;
        int sliderValue = farSlabSlider.getValue();
        double exponent = sliderValue / 25.0 - 1.0; // range [-1, 3]
        float zFarRelative = 0.901f + (float)Math.pow(10.0, exponent); // (range 1.001 - 1000.9)
        selectedViewSlab.setzFarRelative(zFarRelative);
        selectedViewSlab.getChangeObservable().notifyObservers();
    }//GEN-LAST:event_farSlabSliderStateChanged

    private void incrementLocalRotation(JSpinner spinner, Vector3 axis)
    {
        float angle = ((Number)spinner.getValue()).floatValue();
        if (angle == 0) return;
        spinner.setValue(new Integer(0));
        if (selectedVantage == null)
            return;
        // System.out.println("Rotating by "+angle+" degrees");
        Rotation newRot = new Rotation().setFromAxisAngle(axis, (float)(angle * Math.PI/180.0) );
        newRot = new Rotation(selectedVantage.getRotationInGround()).multiply(newRot);
        selectedVantage.setRotationInGround(newRot);
        selectedVantage.notifyObservers();        
    }
    
    private void incrementLocalTranslation(JSpinner spinner, Vector3 axis)
    {
        float distance = ((Number)spinner.getValue()).floatValue();
        if (distance == 0) return;
        spinner.setValue(new Integer(0));
        if (selectedVantage == null)
            return;
        // System.out.println("Rotating by "+angle+" degrees");
        Vector3 dFocus = new Vector3(axis).multiplyScalar(distance);
        dFocus = selectedVantage.getRotationInGround().multiply(dFocus);
        dFocus.add(selectedVantage.getFocusPosition());
        selectedVantage.setFocusPosition(dFocus);
        selectedVantage.notifyObservers();        
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider farSlabSlider;
    private javax.swing.JPanel focusPanel;
    private javax.swing.JLabel focusXLabel;
    private javax.swing.JSpinner focusXSpinner;
    private javax.swing.JLabel focusYLabel;
    private javax.swing.JSpinner focusYSpinner;
    private javax.swing.JLabel focusZLabel;
    private javax.swing.JSpinner focusZSpinner;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSpinner localRotXSpinner;
    private javax.swing.JSpinner localRotYSpinner;
    private javax.swing.JSpinner localRotZSpinner;
    private javax.swing.JSpinner localTranslateXSpinner;
    private javax.swing.JSpinner localTranslateYSpinner;
    private javax.swing.JSpinner localTranslateZSpinner;
    private javax.swing.JSlider nearSlabSlider;
    private javax.swing.JButton resetAllButton;
    private javax.swing.JButton resetFocusButton;
    private javax.swing.JButton resetRotationButton;
    private javax.swing.JButton resetZoomButton;
    private javax.swing.JLabel rotXLabel;
    private javax.swing.JSpinner rotXSpinner;
    private javax.swing.JLabel rotYLabel;
    private javax.swing.JSpinner rotYSpinner;
    private javax.swing.JLabel rotZLabel;
    private javax.swing.JSpinner rotZSpinner;
    private javax.swing.JPanel rotationPanel;
    private javax.swing.JCheckBox stayUpCheckBox;
    private javax.swing.JPanel zoomPanl;
    private javax.swing.JSpinner zoomSpinner;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        vantageResult = Utilities.actionsGlobalContext().lookupResult(Vantage.class);
        vantageResult.addLookupListener(this);
        Collection<? extends Vantage> allVantages = vantageResult.allInstances();
        if (allVantages.isEmpty()) {
            setVantage(null);
        }
        else {
            setVantage(allVantages.iterator().next());
        }
        
        viewSlabResult = Utilities.actionsGlobalContext().lookupResult(ViewSlab.class);
        viewSlabResult.addLookupListener(this);
        Collection<? extends ViewSlab> allViewSlabs = viewSlabResult.allInstances();
        if (allViewSlabs.isEmpty()) {
            setViewSlab(null);
        }
        else {
            setViewSlab(allViewSlabs.iterator().next());
        }        
        
        throttledUpdateControllerFields();
    }

    @Override
    public void componentClosed() {
        vantageResult.removeLookupListener(this);
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
    
    private final Vector3 newFocus = new Vector3(0, 0, 0); // scratch space
    
    private void updateViewSlabProperties() {
        if (! doUpdateViewSlab)
            return;
        if (selectedViewSlab == null)
            return;
        // TODO:
    }
    
    private void updateVantageProperties() {
        if (! doUpdateVantage)
            return;
        if (selectedVantage == null)
            return;
        // System.out.println("Updating vantage");
        // focus
        newFocus.copy(selectedVantage.getFocusPosition());
        newFocus.setX(((Number)focusXSpinner.getValue()).floatValue());
        newFocus.setY(((Number)focusYSpinner.getValue()).floatValue());
        newFocus.setZ(((Number)focusZSpinner.getValue()).floatValue());
        selectedVantage.setFocusPosition(newFocus);
        // zoom
        selectedVantage.setSceneUnitsPerViewportHeight(
                ((Number)zoomSpinner.getValue()).floatValue());
        // TODO - rotation, adjusting for tolerance...
        float [] eulerAngles = {
            ((Number)rotXSpinner.getValue()).floatValue() * (float)(Math.PI/180.0),
            ((Number)rotYSpinner.getValue()).floatValue() * (float)(Math.PI/180.0),
            ((Number)rotZSpinner.getValue()).floatValue() * (float)(Math.PI/180.0),
        };
        Rotation newRotation = new Rotation().setToBodyFixed123(
                eulerAngles[0], eulerAngles[1], eulerAngles[2]);
        Rotation diffRot = new Rotation(newRotation).transpose().multiply(
                selectedVantage.getRotationInGround());
        float diffAngle = diffRot.convertRotationToAxisAngle()[3];
        // Avoid precision recursion by ignoring tiny angle changes
        if (diffAngle > 5e-6)
            selectedVantage.setRotationInGround(newRotation);
        // rotation constraint
        selectedVantage.setConstrainedToUpDirection(stayUpCheckBox.isSelected());
        
        selectedVantage.notifyObservers();
    }

    @Override
    public void resultChanged(LookupEvent le) 
    {
        Collection<? extends Vantage> allVantages = vantageResult.allInstances();
        if (allVantages.isEmpty()) {
            setVantage(null);
            return;
        }
        setVantage(allVantages.iterator().next());
        
        Collection<? extends ViewSlab> allViewSlabs = viewSlabResult.allInstances();
        if (allViewSlabs.isEmpty()) {
            setViewSlab(null);
            return;
        }
        setViewSlab(allViewSlabs.iterator().next());
    }

    private void setViewSlab(ViewSlab viewSlab) {
        if (selectedViewSlab == viewSlab)
            return; // no change
        if (viewSlab == null)
            return; // Remember old viewSlab, even when view window focus is lost
        deregisterViewSlab(selectedViewSlab);
        registerViewSlab(viewSlab);
    }
    
    private void setVantage(Vantage vantage) {
        if (selectedVantage == vantage)
            return; // no change
        if (vantage == null)
            return; // Remember old vantage, even when view window focus is lost
        deregisterVantage(selectedVantage);
        registerVantage(vantage);
    }
    
    private void deregisterVantage(Vantage vantage) {
        if (vantage == null)
            return;
        vantage.deleteObserver(vantageObserver);
    }

    private void registerVantage(Vantage vantage) {
        selectedVantage = vantage;
        if (vantage == null)
            return;
        if (vantageObserver == null) {
            vantageObserver = new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    throttledUpdateControllerFields();
                }
            };
        }
        vantage.addObserver(vantageObserver);
        throttledUpdateControllerFields();
    }
    
    private void deregisterViewSlab(ViewSlab viewSlab) {
        if (viewSlab == null)
            return;
        viewSlab.getChangeObservable().deleteObserver(viewSlabObserver);
    }

    private void registerViewSlab(ViewSlab viewSlab) {
        selectedViewSlab = viewSlab;
        if (viewSlab == null)
            return;
        if (viewSlabObserver == null) {
            viewSlabObserver = new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    throttledUpdateControllerFields();
                }
            };
        }
        viewSlab.getChangeObservable().addObserver(viewSlabObserver);
        throttledUpdateControllerFields();
    }
    
    // Flag to temporarily withhold signalling
    private boolean doUpdateVantage = true;
    private boolean doUpdateViewSlab = true;
    private final JComponent[] widgets = new JComponent[] {
            focusXSpinner,
            focusYSpinner,
            focusZSpinner,
            rotXSpinner,
            rotYSpinner,
            rotZSpinner,
            zoomSpinner,
            resetFocusButton,
            resetRotationButton,
            resetZoomButton
    };
    
    // For better user interaction, throttle updates to GUI camera parameter fields
    private Timer fieldUpdateTimer;
    private long previousUpdateTime = System.nanoTime();
    private void throttledUpdateControllerFields()
    {
        // Make this number larger if the GL animation gets too jerky when 
        // the camera control widget is shown
        final int minMilliseconds = 100;
        
        // cancel any previous queued updates
        if (fieldUpdateTimer != null)
            fieldUpdateTimer.cancel();

        long now = System.nanoTime();

        long remaining = minMilliseconds - (now - previousUpdateTime)/1000000;
        
        // It's been a long time, so update now
        if ( remaining <= 0 ) {
            // System.out.println("throttled update");
            immediateUpdateControllerFields();
        }
        
        else {
            // System.out.println("throttle stop!");
            fieldUpdateTimer = new Timer();
            fieldUpdateTimer.schedule(new TimerTask() 
            {
                @Override
                public void run() {
                    immediateUpdateControllerFields();
                }
            }, remaining);
            return;
        }
        
        previousUpdateTime = now;
    }
    
    // TODO This method make GL updates lag...
    private void immediateUpdateControllerFields()
    // {} private void updateControllerFieldsNot() 
    {
        // turn off listener
        doUpdateVantage = false;
        doUpdateViewSlab = false;

        if (selectedVantage == null) {
            focusXSpinner.setValue(0.0);
            focusYSpinner.setValue(0.0);
            focusZSpinner.setValue(0.0);
            rotXSpinner.setValue(0.0);
            rotYSpinner.setValue(0.0);
            rotZSpinner.setValue(0.0);
            zoomSpinner.setValue(DEFAULT_ZOOM);
            stayUpCheckBox.setSelected(false);
            
            // Gray out tools not connected to a Vantage
            for (JComponent w : widgets)
                if (w != null) w.setEnabled(false);
        }
        else {
            // Ungray controls for live Vantage
            for (JComponent w : widgets)
                if (w != null) w.setEnabled(true);
            
            // TODO - these value updates slow the GL animation!
            // (But only the ones that actually change...)
            //
            // new Vector3 to avoid race condition
            // Actually, we instead use doUpdateVantage flag to avoid race condition.
            // focus
            newFocus.copy(selectedVantage.getFocusPosition());
            focusXSpinner.setValue(newFocus.getX());
            focusYSpinner.setValue(newFocus.getY());
            focusZSpinner.setValue(newFocus.getZ());
            Rotation rotation = selectedVantage.getRotationInGround();
            float [] eulerAngles = rotation.convertThreeAxesBodyFixedRotationToThreeAngles(
                    CoordinateAxis.X, 
                    CoordinateAxis.Y, 
                    CoordinateAxis.Z);
            rotXSpinner.setValue(eulerAngles[0] * (float)(180.0/Math.PI));
            rotYSpinner.setValue(eulerAngles[1] * (float)(180.0/Math.PI));
            rotZSpinner.setValue(eulerAngles[2] * (float)(180.0/Math.PI));
            // zoom
            zoomSpinner.setValue(selectedVantage.getSceneUnitsPerViewportHeight());
            // up constraint
            stayUpCheckBox.setSelected(selectedVantage.isConstrainedToUpDirection());
        }
        
        // Turn listeners back on
        doUpdateVantage = true;
        doUpdateViewSlab = true;
    }
}
