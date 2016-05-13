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

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JComponent;
import org.janelia.geometry3d.CubeGeometry;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.material.BasicMaterial;
import org.janelia.gltools.material.DiffuseMaterial;
import org.janelia.gltools.material.Material;
import org.janelia.gltools.MeshActor;
import org.janelia.scenewindow.SceneRenderer.CameraType;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.scenewindow//SceneWindow//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "SceneWindowTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.scenewindow.SceneWindowTopComponent")
// @ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_SceneWindowAction",
        preferredID = "SceneWindowTopComponent"
)
@Messages({
    "CTL_SceneWindowAction=SceneWindow",
    "CTL_SceneWindowTopComponent=3D",
    "HINT_SceneWindowTopComponent=Middle-drag to rotate"
})
public final class SceneWindowTopComponent extends TopComponent {
    private final SceneInteractor interactor;
    private final SceneWindow sceneWindow;

    public SceneWindowTopComponent() {
        initComponents();
        setName(Bundle.CTL_SceneWindowTopComponent());
        setToolTipText(Bundle.HINT_SceneWindowTopComponent());
        //
        Scene scene = new BasicScene();
        Vantage vantage = new Vantage(scene); // TODO: shared camera viewpoint
        scene.add(vantage);
        // Public primary camera changes to other modules (e.g. Camera Control)
        associateLookup(Lookups.singleton(vantage));
        //
        this.setLayout(new BorderLayout());
        sceneWindow = new SceneWindow(vantage, CameraType.PERSPECTIVE);
        this.add(sceneWindow.getOuterComponent(), BorderLayout.CENTER);
        //
        BasicMaterial material = 
                new DiffuseMaterial();
                // new IBLDiffuseMaterial();
                // new NormalMaterial();
                // new FlatMeshMaterial(0.3f, 0.5f, 0.3f);
        material.setShadingStyle(
                Material.Shading.SMOOTH);
                // Material.Shading.FLAT);
        
        GL3Actor cubeMesh = new MeshActor(
                new CubeGeometry(new Vector3(0, 0, 0), new Vector3(1, 1, 1)),
                material, null);
        
        sceneWindow.getRenderer().addActor(cubeMesh);
        // Canvas->Camera information flow
        interactor = new OrbitPanZoomInteractor(sceneWindow.getCamera(), 
                sceneWindow.getInnerComponent());
        setToolTipText(interactor.getToolTipText());
        Component c = sceneWindow.getOuterComponent();
        if (c instanceof JComponent) {
            ((JComponent)c).setInheritsPopupMenu(true);
            // I think we simply cannot get Swing tooltips over a GLCanvas...
            // ((JComponent)c).setToolTipText("Outer component"); // crashes on Windows at tool tip time
        }
        c = sceneWindow.getInnerComponent();
        if (c instanceof JComponent)
            ((JComponent)c).setToolTipText(interactor.getToolTipText());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        setToolTipText(org.openide.util.NbBundle.getMessage(SceneWindowTopComponent.class, "SceneWindowTopComponent.toolTipText")); // NOI18N

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
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
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
}
