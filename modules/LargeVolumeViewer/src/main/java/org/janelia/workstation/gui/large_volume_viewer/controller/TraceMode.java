package org.janelia.workstation.gui.large_volume_viewer.controller;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import Jama.Matrix;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.action.*;
import org.janelia.workstation.controller.dialog.NeuronGroupsDialog;
import org.janelia.workstation.controller.eventbus.NeuronUpdateEvent;
import org.janelia.workstation.controller.eventbus.SelectionAnnotationEvent;
import org.janelia.workstation.controller.eventbus.ViewEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmViewState;
import org.janelia.workstation.geom.BoundingBox3d;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.gui.camera.Camera3d;
import org.janelia.workstation.gui.large_volume_viewer.MenuItemGenerator;
import org.janelia.workstation.gui.large_volume_viewer.action.BasicMouseMode;
import org.janelia.workstation.gui.large_volume_viewer.action.MouseMode;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.SkeletonController;
import org.janelia.workstation.gui.large_volume_viewer.options.ApplicationPanel;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.SkeletonActorModel;
import org.janelia.workstation.gui.large_volume_viewer.LargeVolumeViewerTopComponent;
import org.janelia.workstation.controller.task_workflow.TaskWorkflowViewTopComponent;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.keybind.KeymapUtil;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.gui.large_volume_viewer.MouseModalWidget;
import org.janelia.workstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.model.domain.tiledMicroscope.AnnotationNavigationDirection;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceMode extends BasicMouseMode
implements MouseMode, KeyListener
{
    private Logger logger = LoggerFactory.getLogger(TraceMode.class);

    // Radius of an anchor
    private static final int pixelRadius = 5;
    
	private Skeleton skeleton;
	private SkeletonActor skeletonActor;
    private SkeletonController controller = SkeletonController.getInstance();
	private Anchor hoverAnchor = null;
	private Vec3 popupXyz = null;
	// Sometimes users move an anchor with the mouse
	private Anchor dragAnchor = null;
	private Point dragStart = null;
	private Cursor grabHandCursor = createCursor("grab_closed.png", 7, 7);
	private Cursor penCursor = createCursor("nib.png", 7, 0);
	private Cursor crossCursor = createCursor("crosshair.png", 7, 7);
	private Cursor penPlusCursor = createCursor("nib_plus.png", 7, 0);
	private Point pressPoint;
	private Viewport viewport;
	private BoundingBox3d boundingBox;
	private NeuronManager neuronManager;
	// private Anchor nextParent = null;
	private boolean autoFocusNextAnchor = false;
	private static long time1;
	public static void startTimer() { time1=new Date().getTime();}
	public static String getTimerMs() { return new Long(new Date().getTime() - time1).toString(); }
	
	public TraceMode(Skeleton skeleton) {
	    neuronManager = NeuronManager.getInstance();
		this.skeleton = skeleton;
		setHoverCursor(penCursor);
		setDragCursor(crossCursor);
		// Center on new anchors, and mark them with a "P"
		skeleton.skeletonChanged();
	}

	@Override 
	public String getToolTipText() {
		// But tooltip during tracing is annoying
		return null;
		/*
		// Use html to get multiline tooltip
		return  "<html>"
				+"SHIFT-click to place a new anchor<br>" // done
				+"Click and drag to move anchor<br>" // done
				+"Click to designate parent anchor<br>" // done
				+"Middle-button drag to Pan XY<br>" // done
				+"Scroll wheel to scan Z<br>" // done
				+"SHIFT-scroll wheel to zoom<br>" // done
				+"Double-click to recenter on a point<br>"
				+"Right-click for context menu" // done
				+"</html>";
				*/
	}
	
	@Override
	public void mouseClicked(MouseEvent event) {
        super.mouseClicked(event);
        // Java swing mouseClicked() requires zero motion and is therefore stupid.
        // onMouseActuallyClicked(event);
        //
        // But this will have to do for double clicking...
        // Double click to center; on anchor or on slice point
        if (event.getClickCount() == 2) {
            if (hoverAnchor != null) {
                camera.setFocus(hoverAnchor.getLocation());
//                skeleton.getHistory().push(hoverAnchor);
            } else {
                // center on slice point
                camera.setFocus(worldFromPixel(event.getPoint()));
            }
        }
    }
	
	private void appendAnchor(Vec3 xyz) {
		autoFocusNextAnchor = true; // center on new position
		skeleton.addAnchorAtXyz(xyz, skeletonActor.getModel().getNextParent());
		camera.setFocus(xyz);
	}
	
	private void seedAnchor(Vec3 xyz) {
		autoFocusNextAnchor = true; // center on new position
		skeleton.addAnchorAtXyz(xyz, null);
	}
	
	private void onMouseActuallyClicked(MouseEvent event) {
        // we don't use other button or middle button for tracing
        if (event.getButton() == MouseEvent.BUTTON1) {
            // regardless of how you shift-click or click, or your settings,
            //  clicking an anchor always sets next parent (old behavior: could put
            //  a point on top of another, which we don't want)
            if (hoverAnchor != null) {
                NeuronManager.getInstance().updateFragsByAnnotation(hoverAnchor.getNeuronID(), hoverAnchor.getGuid());
                controller.setNextParent(hoverAnchor);
                TmNeuronMetadata neuron = neuronManager.getNeuronFromNeuronID(hoverAnchor.getNeuronID());
                TmGeoAnnotation annotation = neuronManager.getGeoAnnotationFromID(neuron.getId(),
                        hoverAnchor.getGuid());
                TmModelManager.getInstance().getCurrentSelections().setCurrentNeuron(neuron);
                TmModelManager.getInstance().getCurrentSelections().setCurrentVertex(annotation);
                SelectionAnnotationEvent selectionEvent = new SelectionAnnotationEvent(
                        Arrays.asList(new TmGeoAnnotation[]{annotation}), true, false);
                ViewerEventBus.postEvent(selectionEvent);
            } else {
                // original behavior: shift-click to annotate; new behavior (2018):
                //  shift not required to annotate; check preference for which:
                if (ApplicationPanel.getAnnotationClickMode().equals(ApplicationPanel.CLICK_MODE_SHIFT_LEFT_CLICK)
                        && !event.isShiftDown()) {
                    // require shift but don't have shift = no annotation for you
                    return;
                }
                // finally we're cleared to annotate
                Vec3 xyz = worldFromPixel(event.getPoint());

                TraceMode.startTimer();
                appendAnchor(xyz);
            }
        }
	}
	
	@Override
	public void mouseDragged(MouseEvent event) {
		super.mouseDragged(event);
		// We might be moving an anchor
		if (dragAnchor != null && controller.checkOwnership(dragAnchor.getNeuronID()) &&
            !skeletonActor.getModel().anchorIsNonInteractable(dragAnchor)) {
                    
			Vec3 loc = worldFromPixel(event.getPoint());
			skeletonActor.getModel().lightweightPlaceAnchor(dragAnchor, loc);
		}
		// Middle button drag to pan
		if ((event.getModifiers() & InputEvent.BUTTON2_MASK) != 0) {
			// TODO reuse pan code
			checkCursor(grabHandCursor);
			Point p1 = getPreviousPoint();
			Point p2 = getPoint();
			Vec3 dx = new Vec3(p1.x - p2.x, p1.y - p2.y, 0);
			dx = viewerInGround.times(dx);
			getCamera().incrementFocusPixels(dx);
			if (boundingBox != null)
				getCamera().setFocus(boundingBox.clip(getCamera().getFocus()));
		}
	}
    
	// Amplify size of anchor when hovered
	@Override
	public void mouseMoved(MouseEvent event) {
		super.mouseMoved(event);
		final Vec3 xyz = worldFromPixel(event.getPoint());
        SkeletonActorModel skeletonActorModel = skeletonActor.getModel();

        final double worldRadius = pixelRadius / camera.getPixelsPerSceneUnit();
        // Find smallest squared distance
        final double cutoff = worldRadius * worldRadius;

        StopWatch stopwatch = new StopWatch();
        stopwatch.start();

        Anchor spatial = null;
        List<TmGeoAnnotation> closestAnchors = TmModelManager.getInstance().getSpatialIndexManager().getAnchorClosestToMicronLocation(new double[]{xyz.x(), xyz.y(), xyz.z()}, 20);
        if (closestAnchors != null) {
            logger.trace("Got {} closest neurons for mouse position {}", closestAnchors.size(), xyz);
            List<Anchor> anchors = new ArrayList<>();
            for (TmGeoAnnotation vertex : closestAnchors) {
                Anchor anchor = skeleton.getAnchorByID(vertex.getId());
                if (anchor!=null) {
                    anchors.add(anchor);
                }
            }
            spatial = findBestAnchor(anchors, xyz, cutoff);
        }

        stopwatch.stop();

        if (spatial != null) {
            logger.trace("Found closest anchor in spatial index: {} (elapsed = {} ms)", spatial.getGuid(), stopwatch.getElapsedTime());
        }

        Anchor closest = spatial;

        // closest == null means you're not on an anchor anymore
		if (skeletonActor != null && closest != hoverAnchor) {
			// test for closest == null because null will come back invisible,
			//	and we need hover-->null to unhover
			if ((closest == null || skeletonActorModel.anchorIsVisible(closest)) && !skeletonActorModel.anchorIsNonInteractable(closest)) {
				hoverAnchor = closest;
				skeletonActor.getModel().setHoverAnchor(hoverAnchor);
			}
			if (skeletonActorModel.anchorIsNonInteractable(closest)) {
			    // be sure anchor is not hovered if it's non-interactable; doesn't solve all issues,
                //  but does improve things
                skeletonActor.getModel().setHoverAnchor(null);
            }
		}

		checkShiftPlusCursor(event);
	}
	
	private Anchor findBestAnchor(Collection<Anchor> anchors, Vec3 xyz, double cutoff) {
	    
	    SkeletonActorModel skeletonActorModel = skeletonActor.getModel();
        double minDist2 = 10 * cutoff; // start too big
        Anchor best = null;
        
        for (Anchor anchor : anchors) {
            //Vec3 du2 = xyz.minus(anchor.getLocation());
            //double dist = Math.sqrt(du2.dot(du2));
            //logger.trace("   Inspecting {} at {} (dist={})", anchor.getGuid(), anchor.getLocation(), dist);
            
            // we don't interact with invisible anchors, and since hovering
            //  is the key to all interactions, we can elegantly prevent that
            // interaction here
            if (!skeletonActorModel.anchorIsVisible(anchor) && !skeletonActorModel.anchorIsNonInteractable(anchor)) {
                continue;
            }
            double dz = Math.abs(2.0 * (xyz.getZ() - anchor.getLocation().getZ()) * camera.getPixelsPerSceneUnit());
            if (dz >= 0.95 * viewport.getDepth()) {
                continue; // outside of Z (most of) range
            }
            // Use X/Y (not Z) for distance comparison
            Vec3 dv = xyz.minus(anchor.getLocation());
            dv = viewerInGround.inverse().times(dv); // rotate into screen space
            double dx = dv.getX();
            double dy = dv.getY();
            double d2 = dx*dx + dy*dy;
            if (d2 > cutoff) {
                continue;
            }
            if (d2 >= minDist2) {
                continue;
            }
            minDist2 = d2;
            best = anchor;
        }
        
        return best;
	}

	@Override
	public void mousePressed(MouseEvent event) {
		super.mousePressed(event);
		// Might start dragging an anchor
		if (event.getButton() == MouseEvent.BUTTON1) 
		{
			// start dragging anchor position
			if (hoverAnchor == null) {
				dragAnchor = null;
				dragStart = null;
			}
			else {
				dragAnchor = hoverAnchor;
				dragStart = event.getPoint();
			}
		}
		// middle button to Pan
		else if (event.getButton() == MouseEvent.BUTTON2) {
			checkCursor(grabHandCursor);
		}
		pressPoint = event.getPoint();		
	}
	
	@Override
	public void mouseReleased(MouseEvent event) {
		super.mouseReleased(event);
		// Might stop dragging an anchor
		if (event.getButton() == MouseEvent.BUTTON1) {
			if (dragAnchor != null) {
				Point finalPos = event.getPoint();
				if (dragStart.distance(event.getPoint()) > 2.0) {
					Vec3 location = worldFromPixel(finalPos);
					Vec3 oldLoc = dragAnchor.getLocation();
					Vec3 dLoc = location.minus(oldLoc);
					// relaxed restriction on move; used to disallow movement in z to
                    //  prevent changing plane of annotations dragged while not actually
                    //  on their plane (since they are visible and draggable in nearby
                    //  planes)
					// Vec3 viewPlane = viewerInGround.inverse().times(new Vec3(1,1,0));
					Vec3 viewPlane = viewerInGround.inverse().times(new Vec3(1,1,1));
					for (int i = 0; i < 3; ++i) {
						dLoc.set(i, dLoc.get(i) * viewPlane.get(i));
					}
					Vec3 newLoc = oldLoc.plus(dLoc);
					dragAnchor.setLocation(newLoc);
//					skeleton.getHistory().push(dragAnchor);
				}
			}
			dragAnchor = null;
			dragStart = null;
		}
        // Check for click because Swing mouseClicked() event is stupid.
        if (pressPoint != null) {
            double clickDistance = event.getPoint().distance(pressPoint);
            if (clickDistance < 3) {
                onMouseActuallyClicked(event);
            }
        }
	}
	
	public void setActor(SkeletonActor actor) {
		this.skeletonActor = actor;
	}

	public SkeletonActor getActor() {
		return skeletonActor;
	}
	
    public BoundingBox3d getBoundingBox() {
		return boundingBox;
	}

	public void setBoundingBox(BoundingBox3d boundingBox) {
		this.boundingBox = boundingBox;
	}

	private Anchor getHoverAnchor() {
        return hoverAnchor;
	}
	
	@Override
    public MenuItemGenerator getMenuItemGenerator() {
        return new MenuItemGenerator() {
            @SuppressWarnings("serial")
			@Override
            public List<JMenuItem> getMenus(MouseEvent event) 
            {
                List<JMenuItem> result = new Vector<JMenuItem>();
                popupXyz = worldFromPixel(event.getPoint());
                // note: it's important that we use this hover anchor, the one that's
                //  under the mouse when the menu is opened, because the mouse pointer
                //  can move off the anchor and de-hover it while the menu is open; we
                //  don't want that, we want the anchor that was hovered
                final Anchor hover = getHoverAnchor();
                Anchor parent = skeletonActor.getModel().getNextParent();
                result.add(null); // separator

                AbstractAction syncViewsAction = new AbstractAction("Synchronize Views At This Location") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // send out a view event to synchronize
                        Point cursorPos = event.getPoint();
                        Vec3 location = worldFromPixel(cursorPos);
                        ViewEvent syncViewEvent = new ViewEvent(camera.getFocus().getX(),
                                camera.getFocus().getY(),
                                camera.getFocus().getZ(),
                                500, null, false);
                        Camera3d camera = getCamera();

                        Matrix m2v = TmModelManager.getInstance().getMicronToVoxMatrix();
                        Jama.Matrix micLoc = new Jama.Matrix(new double[][]{
                                {location.getX(),},
                                {location.getY(),},
                                {location.getZ(),},
                                {1.0,},});
                        // NeuronVertex API requires coordinates in micrometers
                        Jama.Matrix voxLoc = m2v.times(micLoc);
                        Vec3 voxelXyz = new Vec3(
                                (float) voxLoc.get(0, 0),
                                (float) voxLoc.get(1, 0),
                                (float) voxLoc.get(2, 0));
                        TmViewState currView = TmModelManager.getInstance().getCurrentView();
                        currView.setCameraFocusX(voxelXyz.getX());
                        currView.setCameraFocusY(voxelXyz.getY());
                        currView.setCameraFocusZ(voxelXyz.getZ());
                        currView.setZoomLevel(camera.getPixelsPerSceneUnit());
                        ViewerEventBus.postEvent(syncViewEvent);
                    }
                };
                syncViewsAction.setEnabled(controller.editsAllowed());
                result.add(new JMenuItem(syncViewsAction));

                // always available:
                AbstractAction scrollBrainAction = new AbstractAction("Scroll through Sample(Z)") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                         SimpleWorker scrollWorker = new SimpleWorker() {
                            @Override
                            protected void doStuff() throws Exception {
                                // grab current camera position and zoom and loop in z from there
                                Vec3 cameraPos = getCamera().getFocus();
                                cameraPos.setZ(getBoundingBox().getMinZ());
                                Thread.sleep(2000);
                                controller.setLVVFocus(cameraPos);
                                float step = 40;
                                while (cameraPos.getZ() < getBoundingBox().getMaxZ()) {
                                    Thread.sleep(100);
                                    cameraPos = cameraPos.plus(new Vec3(0, 0, step));
                                    controller.setLVVFocus(cameraPos);
                                }
                            }

                            @Override
                            protected void hadSuccess() {

                            }

                            @Override
                            protected void hadError(Throwable error) {
                            }
                        };
                        scrollWorker.execute();                                                               //
                    }
                };
                scrollBrainAction.setEnabled(true);
                result.add(new JMenuItem(scrollBrainAction));
                ///// Popup menu items that do not require an anchor under the mouse /////
                if (hover == null) {
                    // No anchor under mouse? Maybe user wants to create a new anchor.
                    if (parent == null) {
                        // Start new tree
                        AbstractAction beginNeuriteHereAction = new AbstractAction("Begin new neurite here [Shift-click]") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                seedAnchor(popupXyz);
                            }
                        };
                        beginNeuriteHereAction.setEnabled(controller.editsAllowed());
                        result.add(new JMenuItem(beginNeuriteHereAction));
                    }
                    else {
                        // Add branch to tree
                        AbstractAction appendNewAnchorAction = new AbstractAction("Append new anchor here [Shift-click]") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                appendAnchor(popupXyz);
                            }
                        };
                        appendNewAnchorAction.setEnabled(controller.editsAllowed());
                        result.add(new JMenuItem(appendNewAnchorAction));
                        
                        
                                                
                        // Start new tree
                        AbstractAction beginNeuriteHereAction = new AbstractAction("Begin new neurite here") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                seedAnchor(popupXyz);
                            }
                        };
                        beginNeuriteHereAction.setEnabled(controller.editsAllowed());
                        result.add(new JMenuItem(beginNeuriteHereAction));
                    }
                }
                else {
                    // There is an anchor under the mouse; for all of these, we can assume
                    //  that hover != null
                    // Center
                    result.add(new JMenuItem(new AbstractAction("Center on this anchor") {
                        private static final long serialVersionUID = 1L;
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            camera.setFocus(hover.getLocation());
                        }
                    }));
                    // Trace connection to parent
                    final boolean showTraceMenu = true; // just for initial debugging?
                    if (showTraceMenu && (hover.getNeighbors().size() > 0)) {
                        AbstractAction tracePathToParentAction = new AbstractAction("Trace path to parent") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                skeleton.traceAnchorConnection(hover);
                            }
                        };
                        tracePathToParentAction.setEnabled(controller.editsAllowed());
                        result.add(new JMenuItem(tracePathToParentAction));
                    }
                    // Make Parent
                    if (hover != parent) {
                        AbstractAction makeParentAction = new AbstractAction("Designate this anchor as parent [left click]") {
                            private static final long serialVersionUID = 1L;
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                controller.setNextParent(hover);
                            }
                        };
                        makeParentAction.setEnabled(controller.editsAllowed());
                        result.add(new JMenuItem(makeParentAction));
                    }
                    // Delete
                    AbstractAction deleteSubtreeAction = new AbstractAction("Delete subtree rooted at this anchor") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            DeleteNeuronSubtreeAction action = new DeleteNeuronSubtreeAction();
                            action.execute(hover.getNeuronID(), hover.getGuid());
                        }
                    };
                    deleteSubtreeAction.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(deleteSubtreeAction));

                    AbstractAction deleteLinkAction = new AbstractAction("Delete link") {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            DeleteVertexLinkAction action = new DeleteVertexLinkAction();
                            action.execute(hover.getNeuronID(), hover.getGuid());
                        }
                    };
                    deleteLinkAction.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(deleteLinkAction));

                    AbstractAction smartMergeNeuriteAction = new AbstractAction("Merge chosen neurite to selected neurite") {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            skeleton.smartMergeNeuriteRequest(hover);
                        }
                    };
                    smartMergeNeuriteAction.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(smartMergeNeuriteAction));

                    AbstractAction transferNeuriteAction = new AbstractAction("Transfer neurite...") {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            // used to be called "Move neurite", and that's
                            //  still the internal name for the command
                            skeleton.moveNeuriteRequest(hover);
                        }
                    };
                    transferNeuriteAction.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(transferNeuriteAction));

                    AbstractAction splitAnchorAction = new AbstractAction("Split anchor") {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            SplitNeuronBetweenVerticesAction action = new SplitNeuronBetweenVerticesAction();
                            action.execute(hover.getNeuronID(), hover.getGuid());
                        }
                    };
                    splitAnchorAction.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(splitAnchorAction));

                    AbstractAction splitNeuriteAction = new AbstractAction("Split neurite") {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            SplitNeuronAtVertexAction action = new SplitNeuronAtVertexAction();
                            action.execute(hover.getNeuronID(), hover.getGuid());
                        }
                    };
                    splitNeuriteAction.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(splitNeuriteAction));

                    AbstractAction setAnchorAsRootAction = new AbstractAction("Set anchor as root") {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            RerootNeuronAction action = new RerootNeuronAction();
                            action.execute(hover.getNeuronID(), hover.getGuid());
                        }
                    };
                    setAnchorAsRootAction.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(setAnchorAsRootAction));

                    result.add(null); // separator

                    AbstractAction addNoteAction = new AbstractAction("Add, edit, delete note...") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            AddEditNoteAction action = new AddEditNoteAction();
                            action.execute(hover.getNeuronID(), hover.getGuid());
                        }
                    };
                    addNoteAction.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(addNoteAction));

                    AbstractAction editNeuronTagsAction = new AbstractAction("Edit neuron tags...") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            neuronManager.editNeuronTags(neuronManager.getNeuronFromNeuronID(hover.getNeuronID()));
                        }
                    };
                    editNeuronTagsAction.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(editNeuronTagsAction));
                    
                    AbstractAction setNeuronGroupsAction = new AbstractAction("Edit neuron group properties...") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().editNeuronGroups();
                        }
                    };
                    setNeuronGroupsAction.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(setNeuronGroupsAction));

                    AbstractAction generateReviewPointList = new GenerateTaskReviewAction();
                    generateReviewPointList.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(generateReviewPointList));
                    
                    AbstractAction selectTaskPoint = new AbstractAction("Select point in Task View ...") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            TaskWorkflowViewTopComponent.getInstance().selectPoint(hover.getGuid());
                        }
                    };
                    selectTaskPoint.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(selectTaskPoint));


                    AbstractAction setNeuronRadiusAction = new AbstractAction("Set neuron radius...") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NeuronSetRadiusAction action = new NeuronSetRadiusAction();
                            action.execute(hover.getNeuronID());
                        }
                    };
                    setNeuronRadiusAction.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(setNeuronRadiusAction));

                    result.add(null); // separator

                    AbstractAction changeNeuronStyleAction = new AbstractAction("Change neuron color...") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NeuronChooseColorAction action = new NeuronChooseColorAction();
                            action.chooseNeuronColor(NeuronManager.getInstance().getNeuronFromNeuronID(hover.getNeuronID()));
                        }
                    };
                    changeNeuronStyleAction.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(changeNeuronStyleAction));

                    AbstractAction hideNeuronAction = new AbstractAction("Hide neuron") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            TmModelManager.getInstance().getCurrentView().toggleHidden(hover.getNeuronID());
                            TmNeuronMetadata neuron = neuronManager.getNeuronFromNeuronID(hover.getNeuronID());
                            NeuronUpdateEvent updateEvent = new NeuronUpdateEvent(
                                    Arrays.asList(new TmNeuronMetadata[]{neuron}));
                            ViewerEventBus.postEvent(updateEvent);
                        }
                    };
                    hideNeuronAction.setEnabled(controller.editsAllowed());
                    result.add(new JMenuItem(hideNeuronAction));

                    result.add(null); // separator
                }
                
              /** commenting out for now since it might be affecting merge performance and nobody uses
               * AbstractAction neuronHistoryAction = new AbstractAction("View neuron history...") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().showNeuronHistory();
                    }
                };
                neuronHistoryAction.setEnabled(true);
                result.add(new JMenuItem(neuronHistoryAction));                
                */
                                
                if (parent != null) {
                    if (parent != hover) {
                        result.add(new JMenuItem(new AbstractAction("Center on current parent anchor") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                camera.setFocus(skeletonActor.getModel().getNextParent().getLocation());
                            }
                        }));
                    }
                    result.add(new JMenuItem(new AbstractAction("Clear current parent anchor") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            controller.setNextParent((Long)null);
                        }
                    }));
                }
                return result;
            }
        };
    }
	
	@Override
	public void setWidget(MouseModalWidget widget, boolean updateCursor) {
		super.setWidget(widget, updateCursor);
	}

	public Viewport getViewport() {
		return viewport;
	}

	public void setViewport(Viewport viewport) {
		this.viewport = viewport;
	}

	@Override
	public void keyPressed(KeyEvent event) {          
		checkShiftPlusCursor(event);
		int keyCode = event.getKeyCode();
		Anchor historyAnchor = null;
		Anchor nextParent = skeletonActor.getModel().getNextParent();
                
        // ***** NOTE *****
        // if you hard-code key combinations, be sure to exclude the combinations of
        //  modifiers that you aren't handling!

		switch(keyCode) {
		case KeyEvent.VK_BACK_SPACE:
		case KeyEvent.VK_DELETE:
			if (nextParent != null) {
			    DeleteVertexLinkAction action = new DeleteVertexLinkAction();
			    action.execute(nextParent.getNeuronID(), nextParent.getGuid());
			}
			break;
		case KeyEvent.VK_LEFT:
			if (nextParent != null) {
				if (event.isAltDown()) {
				controller.navigationRelative(nextParent.getNeuronID(), nextParent.getGuid(),
						AnnotationNavigationDirection.ROOTWARD_STEP);
				} else {
					controller.navigationRelative(nextParent.getNeuronID(), nextParent.getGuid(),
							AnnotationNavigationDirection.ROOTWARD_JUMP);
				}
			}
			break;
		case KeyEvent.VK_RIGHT:
			if (nextParent != null) {
				if (event.isAltDown()) {
					controller.navigationRelative(nextParent.getNeuronID(), nextParent.getGuid(),
							AnnotationNavigationDirection.ENDWARD_STEP);
				} else {
					controller.navigationRelative(nextParent.getNeuronID(), nextParent.getGuid(),
							AnnotationNavigationDirection.ENDWARD_JUMP);
				}
			}
			break;
		case KeyEvent.VK_UP:
			if (nextParent != null) {
				controller.navigationRelative(nextParent.getNeuronID(), nextParent.getGuid(),
						AnnotationNavigationDirection.PREV_PARALLEL);
			}
			break;
		case KeyEvent.VK_DOWN:
			if (nextParent != null) {
				controller.navigationRelative(nextParent.getNeuronID(), nextParent.getGuid(),
						AnnotationNavigationDirection.NEXT_PARALLEL);
			}
			break;
		}
                
        // if not normal key event, check our group toggle events
        NeuronManager annModel = NeuronManager.getInstance();
        Map<String, Map<String,Object>> groupMappings = annModel.getTagGroupMappings();
        Iterator<String> groups = groupMappings.keySet().iterator();
        while (groups.hasNext()) {
            String groupName = groups.next();
            Map<String,Object> fooMap = groupMappings.get(groupName);
            String keyMap = (String)fooMap.get("keymap");
            if (keyMap!=null && keyMap.equals(KeymapUtil.getTextByKeyStroke(KeyStroke.getKeyStrokeForEvent(event)))) {
                // toggle property
                Boolean toggled = (Boolean)fooMap.get("toggled");
                if (toggled==null)
                    toggled = Boolean.FALSE;
                toggled = !toggled;
                fooMap.put("toggled", toggled);

                // get all neurons in group
                Set<TmNeuronMetadata> neurons = annModel.getNeuronsForTag(groupName);
                List<TmNeuronMetadata> neuronList = new ArrayList<TmNeuronMetadata>(neurons);
                // set toggle state
                String property =(String)fooMap.get("toggleprop");
                if (property!=null) {
                    try {
                        Iterator<TmNeuronMetadata> neuronsIter = neurons.iterator();
                        if (property.equals(NeuronGroupsDialog.PROPERTY_RADIUS)) {
                            if (toggled) {
                                for (TmNeuronMetadata neuron: neuronList) {
                                    TmModelManager.getInstance().getCurrentView().addAnnotationToNeuronRadiusToggle(neuron.getId());
                                }
                            } else {
                                for (TmNeuronMetadata neuron: neuronList) {
                                    TmModelManager.getInstance().getCurrentView().removeAnnotationFromNeuronRadiusToggle(neuron.getId());
                                }
                            }
                            NeuronManager.getInstance().saveUserPreferences();
                        } else if (property.equals(NeuronGroupsDialog.PROPERTY_VISIBILITY)) {
                            if (toggled) {
                                for (TmNeuronMetadata neuron: neuronList) {
                                    TmModelManager.getInstance().getCurrentView().addAnnotationToHidden(neuron.getId());
                                }
                            } else {
                                for (TmNeuronMetadata neuron: neuronList) {
                                    TmModelManager.getInstance().getCurrentView().removeAnnotationFromHidden(neuron.getId());
                                }
                            }
                            NeuronManager.getInstance().saveUserPreferences();
                        } else if (property.equals(NeuronGroupsDialog.PROPERTY_READONLY)) {
                            if (toggled) {
                                for (TmNeuronMetadata neuron: neuronList) {
                                    TmModelManager.getInstance().getCurrentView().addAnnotationToNonInteractable(neuron.getId());
                                }
                            } else {
                                for (TmNeuronMetadata neuron: neuronList) {
                                    TmModelManager.getInstance().getCurrentView().removeAnnotationFromNonInteractable(neuron.getId());
                                }
                            }
                            NeuronManager.getInstance().saveUserPreferences();
                        } else if (property.equals(NeuronGroupsDialog.PROPERTY_CROSSCHECK)) {
                            if (toggled) {
                                for (TmNeuronMetadata neuron: neuronList) {
                                    TmModelManager.getInstance().getCurrentView().addAnnotationToNeuronRadiusToggle(neuron.getId());
                                    TmModelManager.getInstance().getCurrentView().addAnnotationToNonInteractable(neuron.getId());
                                }
                            } else {
                                for (TmNeuronMetadata neuron: neuronList) {
                                    TmModelManager.getInstance().getCurrentView().removeAnnotationFromNeuronRadiusToggle(neuron.getId());
                                    TmModelManager.getInstance().getCurrentView().removeAnnotationFromNonInteractable(neuron.getId());
                                }
                            }
                            NeuronManager.getInstance().saveUserPreferences();
                        }
                    } catch (Exception error) {

                        FrameworkAccess.handleException(error);
                    }
                }

            }
        }
        if (historyAnchor != null)
            camera.setFocus(historyAnchor.getLocation());
	}

	private void checkShiftPlusCursor(InputEvent event) {
		if (hoverAnchor != null) // no adding points while hovering over another point
			checkCursor(penCursor);
		else if (event.isShiftDown()) // display addable cursor
			checkCursor(penPlusCursor);
		else
			checkCursor(penCursor); 
	}
	
}
