package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Anchor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Skeleton;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.SkeletonActor;

public class TraceMode extends BasicMouseMode implements MouseMode 
{
	private Skeleton skeleton;
	private SkeletonActor skeletonActor;
	private int currentHover = -1;
	// Sometimes users move an anchor with the mouse
	private Anchor dragAnchor = null;
	private Point dragStart = null;
	private Cursor grabHandCursor = BasicMouseMode.createCursor("grab_closed.png", 7, 7);
	private Cursor penCursor = BasicMouseMode.createCursor("nib.png", 7, 0);
	private Cursor crossCursor = BasicMouseMode.createCursor("crosshair.png", 7, 7);
	
	public TraceMode(Skeleton skeleton) {
		this.skeleton = skeleton;
		setHoverCursor(penCursor);
		setDragCursor(crossCursor);
	}

	@Override 
	public String getToolTipText() {
		// Use html to get multiline tooltip
		return  "<html>"
				+"SHIFT-click to place a new anchor<br>" // done
				+"Click and drag to move anchor<br>" // done
				+"Click to designate parent anchor<br>" // done
				+"Middle-button drag to Pan XY<br>" // done
				+"Scroll wheel to scan Z<br>" // done
				+"SHIFT-scroll wheel to zoom<br>" // done
				+"Right-click for context menu" // TODO
				+"</html>";
	}
	
	@Override
	public void mouseClicked(MouseEvent event) {
		super.mouseClicked(event);
		// Only want left/near clicks with SHIFT down
		// BUTTON1 is near-click for both left and right handed mice (right?)
		if ((event.getButton() == MouseEvent.BUTTON1) && event.isShiftDown()) {
			// Place new anchor
			Vec3 xyz = worldFromPixel(event.getPoint());
			// System.out.println("Trace click "+xyz);
			skeleton.addAnchorAtXyz(xyz);
		}
		else if (event.getButton() == MouseEvent.BUTTON1) {
			if (currentHover >= 0) {
				Anchor anchor = skeletonActor.getAnchorAtIndex(currentHover);
				if (skeleton.setNextParent(anchor))
					skeletonActor.skeletonActorChangedSignal.emit(); // marker changes
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
			skeletonActor.lightweightNudgeAnchor(dragAnchor, dv);
		}
		// Middle button drag to pan
		if ((event.getModifiers() & InputEvent.BUTTON2_MASK) != 0) {
			checkCursor(grabHandCursor);
			Point p1 = getPreviousPoint();
			Point p2 = getPoint();
			Point dx = new Point(p2.x - p1.x, p2.y - p1.y);
			getCamera().incrementFocusPixels(-dx.x, -dx.y, 0);
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
			double d2 = (a.getLocation().minus(xyz)).normSqr();
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
	}
	
	public void setActor(SkeletonActor actor) {
		this.skeletonActor = actor;
	}

	public SkeletonActor getActor() {
		return skeletonActor;
	}

}
