package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenuItem;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.MenuItemGenerator;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.MouseModalWidget;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Anchor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Skeleton;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.SkeletonActor;

public class TraceMode extends BasicMouseMode 
implements MouseMode, KeyListener
{
	private Skeleton skeleton;
	private SkeletonActor skeletonActor;
	private int currentHover = -1;
	// private Anchor hoverAnchor = null;
	private Vec3 popupXyz = null;
	// Sometimes users move an anchor with the mouse
	private Anchor dragAnchor = null;
	private Point dragStart = null;
	private Cursor grabHandCursor = BasicMouseMode.createCursor("grab_closed.png", 7, 7);
	private Cursor penCursor = BasicMouseMode.createCursor("nib.png", 7, 0);
	private Cursor crossCursor = BasicMouseMode.createCursor("crosshair.png", 7, 7);
	private Cursor penPlusCursor = BasicMouseMode.createCursor("nib_plus.png", 7, 0);
	private Point pressPoint;
	private Viewport viewport;
	private BoundingBox3d boundingBox;
	// private Anchor nextParent = null;
	
	public TraceMode(Skeleton skeleton) {
		this.skeleton = skeleton;
		setHoverCursor(penCursor);
		setDragCursor(crossCursor);
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
				+"Right-click for context menu" // TODO
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
            if (currentHover >= 0) {
                Anchor hoverAnchor = skeletonActor.getAnchorAtIndex(currentHover);
                camera.setFocus(hoverAnchor.getLocation());
            }
            else {
				// center on slice point
				camera.setFocus(worldFromPixel(event.getPoint()));
			}
		}
	}
	
	private void appendAnchor(Vec3 xyz) {
		Anchor anchor = skeleton.addAnchorAtXyz(xyz, skeletonActor.getNextParent());
		skeletonActor.setNextParent(anchor);
		camera.setFocus(anchor.getLocation());
	}
	
	private void seedAnchor(Vec3 xyz) {
		Anchor anchor = skeleton.addAnchorAtXyz(xyz, null);
		skeletonActor.setNextParent(anchor);
		camera.setFocus(anchor.getLocation());		
	}
	
	private void onMouseActuallyClicked(MouseEvent event) {
		// Only want left/near clicks with SHIFT down
		// BUTTON1 is near-click for both left and right handed mice (right?)
		if ((event.getButton() == MouseEvent.BUTTON1) && event.isShiftDown()) {
			// Place new anchor
			Vec3 xyz = worldFromPixel(event.getPoint());
			// System.out.println("Trace click "+xyz);
			appendAnchor(xyz);
		}
		else if (event.getButton() == MouseEvent.BUTTON1) {
			if (currentHover >= 0) {
				Anchor anchor = skeletonActor.getAnchorAtIndex(currentHover);
				skeletonActor.setNextParent(anchor);
				// System.out.println("select parent anchor "+currentHover);
			}
		}
	}
	
	@Override
	public void mouseDragged(MouseEvent event) {
		super.mouseDragged(event);
		// We might be moving an anchor
		if (dragAnchor != null) {
			Point p1 = getPreviousPoint();
			Point p2 = getPoint();
			Point dx = new Point(p2.x - p1.x, p2.y - p1.y);
			Vec3 dv = new Vec3(dx.x, dx.y, 0);
			dv = dv.times(1.0/camera.getPixelsPerSceneUnit()); // convert to scene units
			dv = viewerInGround.times(dv); // Rotate for viewer orientation
			skeletonActor.lightweightNudgeAnchor(dragAnchor, dv);
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
		Vec3 xyz = worldFromPixel(event.getPoint());
		int pixelRadius = 5;
		double worldRadius = pixelRadius / camera.getPixelsPerSceneUnit();
		// TODO - if this gets slow, use a more efficient search structure, like an octree
		// Find smallest squared distance
		double cutoff = worldRadius * worldRadius;
		double minDist2 = 10 * cutoff; // start too big
		Anchor closest = null;
		for (Anchor a : skeleton.getAnchors()) {
			double dz = xyz.getZ() - a.getLocation().getZ();
			if (Math.abs(2.0 * dz) >= 0.9 * viewport.getDepth())
				continue; // outside of Z (most of) range
			// Use X/Y (not Z) for distance comparison
			Vec3 dv = xyz.minus(a.getLocation());
			dv = viewerInGround.inverse().times(dv); // rotate into screen space
			double dx = dv.getX();
			double dy = dv.getY();
			double d2 = dx*dx + dy*dy;
			if (d2 > cutoff)
				continue;
			if (d2 >= minDist2)
				continue;
			minDist2 = d2;
			closest = a;
		}
		int ix = -1;
		if ((closest != null) && (skeletonActor != null))
			ix = skeletonActor.getIndexForAnchor(closest);
		if (ix != currentHover) {
			if (ix >= 0) {
				// System.out.println("Hover anchor "+ix);
			}
			currentHover = ix;
			skeletonActor.setHoverAnchorIndex(ix);
		}
		checkShiftPlusCursor(event);
	}

	@Override
	public void mousePressed(MouseEvent event) {
		super.mousePressed(event);
		// Might start dragging an anchor
		if (event.getButton() == MouseEvent.BUTTON1) 
		{
			// start dragging anchor position
			if (currentHover < 0) {
				dragAnchor = null;
				dragStart = null;
			}
			else {
				dragAnchor = skeletonActor.getAnchorAtIndex(currentHover);
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
					dragAnchor.setLocation(location);
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
        if (currentHover >= 0)
            return skeletonActor.getAnchorAtIndex(currentHover);
		return null;
	}
	
	@Override
    public MenuItemGenerator getMenuItemGenerator() {
        return new MenuItemGenerator() {
            @Override
            public List<JMenuItem> getMenus(MouseEvent event) 
            {
            	    List<JMenuItem> result = new Vector<JMenuItem>();
                    popupXyz = worldFromPixel(event.getPoint());
                    // Cancel 
                    result.add(new JMenuItem(new AbstractAction("Cancel [Escape]") {
                        private static final long serialVersionUID = 1L;
                        @Override
                        public void actionPerformed(ActionEvent e) {} // does nothing (closes context menu)
                    }));
                    // TODO - top level File, Edit, etc.
                    ///// Popup menu items that require an anchor under the mouse /////
                    if (getHoverAnchor() != null) {
                        result.add(null); // separator
                        // Center
                        result.add(new JMenuItem(new AbstractAction("Center on this anchor") {
                            private static final long serialVersionUID = 1L;
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                camera.setFocus(getHoverAnchor().getLocation());
                            }
                        }));                    
                        // Make Parent
                        result.add(new JMenuItem(new AbstractAction("Designate this anchor as parent [left click]") {
                            private static final long serialVersionUID = 1L;
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                skeletonActor.setNextParent(getHoverAnchor());
                            }
                        }));
                        // Connect to current parent
                        if ( (skeletonActor.getNextParent() != null) 
                                && (skeletonActor.getNextParent() != getHoverAnchor()) )
                        {
                            result.add(new JMenuItem(new AbstractAction("Connect parent anchor to this anchor") {
                                private static final long serialVersionUID = 1L;
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    skeleton.connect(getHoverAnchor(), skeletonActor.getNextParent());
                                }
                            }));                     
                        }
                        // Delete
                        result.add(new JMenuItem(new AbstractAction("Delete this anchor") {
                            private static final long serialVersionUID = 1L;
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                skeleton.delete(getHoverAnchor());
                            }
                        }));
                    }
                    ///// Popup menu items that do not require an anchor under the mouse /////
                    result.add(null); // separator
                    // Add branch to tree
                    if (skeletonActor.getNextParent() != null) {
                        result.add(new JMenuItem(new AbstractAction("Append new anchor here [Shift-click]") {
                            private static final long serialVersionUID = 1L;
                            @Override
                            public void actionPerformed(ActionEvent e) {
                            	appendAnchor(popupXyz);
                            }
                        }));                 
                        // Start new tree
                        result.add(new JMenuItem(new AbstractAction("Begin new neurite here") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                seedAnchor(popupXyz);
                            }
                        }));
                    }
                    else {
                        // Start new tree
                        result.add(new JMenuItem(new AbstractAction("Begin new neurite here [Shift-click]") {
                            private static final long serialVersionUID = 1L;
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                seedAnchor(popupXyz);
                            }
                        }));
                    }
            	    return result;
            	}
        };
    }
	
	@Override
	public void setComponent(MouseModalWidget widget) {
		boolean widgetChanged = (widget != this.widget);
		super.setComponent(widget);
		if (! widgetChanged)
			return;
		// Respond to shift up/down
		JComponent component = widget.getComponent();
		component.addKeyListener(this);
		// Use custom context menu
	}

	public Viewport getViewport() {
		return viewport;
	}

	public void setViewport(Viewport viewport) {
		this.viewport = viewport;
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent event) {
		checkShiftPlusCursor(event);
	}

	@Override
	public void keyReleased(KeyEvent event) {
		checkShiftPlusCursor(event);
	}
	
	private void checkShiftPlusCursor(InputEvent event) {
		if (currentHover >= 0) // no adding points while hovering over another point
			checkCursor(penCursor);
		else if (event.isShiftDown()) // display addable cursor
			checkCursor(penPlusCursor);
		else
			checkCursor(penCursor); 
	}
	
}
