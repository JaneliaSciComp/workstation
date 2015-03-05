/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.JComponent;
import org.janelia.console.viewerapi.ViewerLocationAcceptor;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.ImageColorModel;
import org.janelia.it.workstation.gui.large_volume_viewer.LargeVolumeViewer;
import org.janelia.it.workstation.gui.large_volume_viewer.OrthogonalPanel;
import org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi;
import org.janelia.it.workstation.gui.large_volume_viewer.TileServer;
import org.janelia.it.workstation.gui.large_volume_viewer.action.GoToLocationAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.MouseMode;
import org.janelia.it.workstation.gui.large_volume_viewer.action.PanModeAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.RecentFileList;
import org.janelia.it.workstation.gui.large_volume_viewer.action.TraceMouseModeAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.WheelMode;
import org.janelia.it.workstation.gui.large_volume_viewer.action.ZScanScrollModeAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.ZoomMouseModeAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.ZoomScrollModeAction;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.tracing.PathTraceToParentRequest;

/**
 * External controller of the Quad View UI.  Distances it from incoming
 * directives.
 * 
 * @author fosterl
 */
public class QuadViewController implements ViewStateListener {
    private QuadViewUi ui;
    private final AnnotationManager annoMgr;
    private final LargeVolumeViewer lvv;
    private final QuadViewController.QvucMouseWheelModeListener qvucmwListener = new QuadViewController.QvucMouseWheelModeListener();
    private final QvucColorModelListener qvucColorModelListener = new QvucColorModelListener();
    private final Collection<MouseWheelModeListener> relayMwmListeners = new ArrayList<>();
    private final Collection<ColorModelListener> relayCMListeners = new ArrayList<>();
    private final Collection<JComponent> orthPanels = new ArrayList<>();
    private ViewerLocationAcceptor viewerLocationAcceptor = new QuadViewLocationAcceptor();
           
    public QuadViewController(QuadViewUi ui, AnnotationManager annoMgr, LargeVolumeViewer lvv) {
        this.ui = ui;
        this.annoMgr = annoMgr;
        this.lvv = lvv;
        lvv.setMessageListener(new QvucMessageListener());
        this.ui.setPathTraceListener(new QvucPathRequestListener());
        this.ui.setWsCloseListener(new QvucWsClosureListener());
    }
    
    @Override
    public void setCameraFocus(Vec3 focus) {
        ui.setCameraFocus(focus);
    }
    
    @Override
    public void loadColorModel(String colorModelString) {
        ui.imageColorModelFromString(colorModelString);
    }
    
    @Override
    public void pathTraceRequested(Long id) {
        ui.pathTraceRequested(id);
    }
    
    @Override
    public void centerNextParent() {
        ui.centerNextParentMicron();
    }
    
    public void registerForEvents(PanModeAction pma) {
        pma.setMwmListener(qvucmwListener);
    }
    
    public void registerForEvents(ZoomMouseModeAction zmma) {
        zmma.setMwmListener(qvucmwListener);
    }
    
    public void registerForEvents(TraceMouseModeAction tmma) {
        tmma.setMwmListener(qvucmwListener);
    }
    
    public void registerForEvents(ZoomScrollModeAction zsma) {
        zsma.setMwmListener(qvucmwListener);
    }
    
    public void registerForEvents(ZScanScrollModeAction zssma) {
        zssma.setMwmListener(qvucmwListener);
    }
    
    /** Repaint events on any component. */
    public void registerAsOrthPanelForRepaint(JComponent component) {
        orthPanels.add(component);
        relayCMListeners.add(new QvucRepaintColorModelListener(component));
    }
    
    public void registerForEvents(OrthogonalPanel op) {
        orthPanels.add(op);
        op.setMessageListener(new QvucMessageListener());
        relayMwmListeners.add(op);
        relayCMListeners.add(new QvucRepaintColorModelListener(op));
    }
    
    /** Since orthogonal panels are created on demand, they should all be unregistered before registering new ones. */
    public void unregisterOrthPanels() {
        for (JComponent op: orthPanels) {
            Collection<MouseWheelModeListener> tempListeners = new ArrayList<>(relayMwmListeners);
            for (MouseWheelModeListener l: relayMwmListeners) {
                if (l == op) {
                    tempListeners.remove(l);
                }
            }
            relayMwmListeners.clear();
            relayMwmListeners.addAll(tempListeners);
            tempListeners = null;
            Collection<ColorModelListener> tempListeners2 = new ArrayList<>(relayCMListeners);
            for (ColorModelListener l: relayCMListeners) {
                if (l instanceof QvucRepaintColorModelListener) {
                    QvucRepaintColorModelListener rl = (QvucRepaintColorModelListener)l;
                    if (rl.getComponent() == op) {
                        tempListeners2.remove(l);
                    }
                }
            }
            relayCMListeners.clear();
            relayCMListeners.addAll(tempListeners2);
            tempListeners2 = null;
        }
        orthPanels.clear();
    }
    
    public void registerForEvents(RecentFileList rfl) {
        rfl.setUrlLoadListener(new QvucUrlLoadListener());
    }
    
    public void registerForEvents(ImageColorModel icm) {
        icm.addColorModelListener(qvucColorModelListener);
    }
    
    public void registerForEvents(TileServer tileServer) {
        tileServer.setLoadStatusListener(new QvucLoadStatusListener());
    }
    
    public void registerForEvents(GoToLocationAction action) {
        action.setListener(new QvucGotoListener());
    }
    
    public ViewerLocationAcceptor getLocationAcceptor() {
        return viewerLocationAcceptor;
    }
    
    public void mouseModeChanged(MouseMode.Mode mode) {
        lvv.setMouseMode(mode);
        ui.setMouseMode(mode);
        for (MouseWheelModeListener l: relayMwmListeners) {
            l.setMode(mode);
        }
    }
    
    public void wheelModeChanged(WheelMode.Mode mode) {
        lvv.setWheelMode(mode);
        for (MouseWheelModeListener l: relayMwmListeners) {
            l.setMode(mode);
        }
    }
    
    private class QvucGotoListener implements CameraPanToListener {

        @Override
        public void cameraPanTo(Vec3 location) {
            ui.setCameraFocus( location );
        }
        
    }
    
    private class QvucMouseWheelModeListener implements MouseWheelModeListener {

        @Override
        public void setMode(MouseMode.Mode modeId) {
            mouseModeChanged(modeId);
        }

        @Override
        public void setMode(WheelMode.Mode modeId) {
            wheelModeChanged(modeId);
        }
        
    }
    
    private class QvucUrlLoadListener implements UrlLoadListener {

        @Override
        public void loadUrl(URL url) {
            ui.loadRender(url);
        }
        
    }
    
    private class QvucMessageListener implements MessageListener {

        @Override
        public void message(String msg) {
            ui.setStatusLabelText(msg);
        }
        
    }
    
    private class QvucColorModelListener implements ColorModelListener {

        @Override
        public void colorModelChanged() {
            for (ColorModelListener l: relayCMListeners) {
                l.colorModelChanged();
            }
            ui.updateSliderLockButtons();
        }
        
    }
    
    private class QvucRepaintColorModelListener implements ColorModelListener {
        private JComponent component;
        
        public QvucRepaintColorModelListener(JComponent component) {
            this.component = component;
        }
        
        @Override
        public void colorModelChanged() {
            if (component != null)
                component.repaint();
        }
        
        public JComponent getComponent() {
            return component;
        }
        
    }
    
    private class QvucLoadStatusListener implements LoadStatusListener {

        @Override
        public void updateLoadStatus(TileServer.LoadStatus loadStatus) {
            ui.setLoadStatus(loadStatus);
        }
        
    }
    
    private class QvucPathRequestListener implements PathTraceRequestListener {

        @Override
        public void pathTrace(PathTraceToParentRequest request) {
            annoMgr.tracePathToParent(request);
        }
        
    }
    
    private class QvucWsClosureListener implements WorkspaceClosureListener {

        @Override
        public void closeWorkspace() {
            annoMgr.setInitialEntity(null);
        }
        
    }
    
    private class QuadViewLocationAcceptor implements ViewerLocationAcceptor {

        @Override
        public void acceptLocation(URL url, double[] coords) throws Exception {            
            Vec3 newFocus = new Vec3( coords[0], coords[1], coords[2] );
            ui.loadRender(url);
            ui.focusChanged(newFocus);
        }
        
    }
        
}
