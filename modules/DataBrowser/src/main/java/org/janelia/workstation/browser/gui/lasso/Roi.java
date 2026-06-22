package org.janelia.workstation.browser.gui.lasso;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Vector;

import org.janelia.workstation.core.util.SystemInfo;

/**
 * Code copy and pasted from the ImageJA project. It's not possible to reuse their code as-is because of dependencies on AWT.
 */
public class Roi extends Object implements Cloneable, java.io.Serializable, Iterable<Point> {

	public static final int CONSTRUCTING=0, MOVING=1, RESIZING=2, NORMAL=3, MOVING_HANDLE=4; // States
	public static final int RECTANGLE=0, OVAL=1, POLYGON=2, FREEROI=3, TRACED_ROI=4, LINE=5, 
		POLYLINE=6, FREELINE=7, ANGLE=8, COMPOSITE=9, POINT=10; // Types
	public static final int HANDLE_SIZE = 5; 
	public static final int NOT_PASTING = -1; 
	
	static final int NO_MODS=0, ADD_TO_ROI=1, SUBTRACT_FROM_ROI=2; // modification states
		
	int startX, startY, x, y, width, height;
	double startXD, startYD;
	Rectangle2D.Double bounds;
	int activeHandle;
	int state;
	int modState = NO_MODS;
	int cornerDiameter;
	
	public static Roi previousRoi;
	public static final BasicStroke onePixelWide = new BasicStroke(1);
	protected static Color ROIColor = Color.yellow;
	protected static int lineWidth = 1;
	protected static Color defaultFillColor;
	private static Vector listeners = new Vector();
	
	protected int type;
	protected int xMax, yMax;
	protected ImagePlus imp;
	private int imageID;
	protected ImageCanvas ic;
	protected int oldX, oldY, oldWidth, oldHeight;
	protected int clipX, clipY, clipWidth, clipHeight;
	protected ImagePlus clipboard;
	protected boolean constrain; // to be square
	protected boolean center;
	protected boolean aspect;
	protected boolean updateFullWindow;
	protected double mag = 1.0;
	protected double asp_bk; //saves aspect ratio if resizing takes roi very small
	protected ImageProcessor cachedMask;
	protected Color handleColor = Color.white;
	protected Color	 strokeColor;
	protected Color instanceColor; //obsolete; replaced by	strokeColor
	protected Color fillColor;
	protected BasicStroke stroke;
	protected boolean nonScalable;
	protected boolean overlay;
	protected boolean wideLine;
	protected boolean ignoreClipRect;
	protected double flattenScale = 1.0;

	private String name;
	private int position;
	private int channel, slice, frame;
	private boolean hyperstackPosition;
	private boolean subPixel;
	private boolean activeOverlayRoi;
	private Properties props;
	private boolean isCursor;
	private double xcenter = Double.NaN;
	private double ycenter;
	private boolean listenersNotified;

	/** Creates a rectangular ROI. */
	public Roi(int x, int y, int width, int height) {
		this(x, y, width, height, 0);
	}

	/** Creates a new rounded rectangular ROI. */
	public Roi(int x, int y, int width, int height, int cornerDiameter) {
		setImage(null);
		if (width<1) width = 1;
		if (height<1) height = 1;
		if (width>xMax) width = xMax;
		if (height>yMax) height = yMax;
		this.cornerDiameter = cornerDiameter;
		this.x = x;
		this.y = y;
		startX = x; startY = y;
		oldX = x; oldY = y; oldWidth=0; oldHeight=0;
		this.width = width;
		this.height = height;
		oldWidth=width;
		oldHeight=height;
		clipX = x;
		clipY = y;
		clipWidth = width;
		clipHeight = height;
		state = NORMAL;
		type = RECTANGLE;
		fillColor = defaultFillColor;
	}

	/** Starts the process of creating a user-defined rectangular Roi,
		where sx and sy are the starting screen coordinates. */
	public Roi(int sx, int sy, ImagePlus imp) {
		this(sx, sy, imp, 0);
	}
	
	/** Starts the process of creating a user-defined rectangular Roi,
		where sx and sy are the starting screen coordinates. */
	public Roi(int sx, int sy, ImagePlus imp, int cornerDiameter) {
		setImage(imp);
		int ox=sx, oy=sy;
		if (ic!=null) {
			ox = ic.offScreenX(sx);
			oy = ic.offScreenY(sy);
		}
		setLocation(ox, oy);
		this.cornerDiameter = cornerDiameter;
		width = 0;
		height = 0;
		state = CONSTRUCTING;
		type = RECTANGLE;
		fillColor = defaultFillColor;
	}

	/** Set the location of the ROI in image coordinates. */
	public void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
		startX = x; startY = y;
		oldX = x; oldY = y; oldWidth=0; oldHeight=0;
		if (bounds!=null) {
			bounds.x = x;
			bounds.y = y;
		}
	}

	public void setImage(ImagePlus imp) {
		this.imp = imp;
		cachedMask = null;
		if (imp==null) {
			ic = null;
			clipboard = null;
			xMax = yMax = Integer.MAX_VALUE;
		} else {
			ic = imp.getCanvas();
			xMax = imp.getWidth();
			yMax = imp.getHeight();
		}
	}
	
	/** Returns the ImagePlus associated with this ROI, or null. */
	public ImagePlus getImage() {
		return imp;
	}
	
	/** Returns the ID of the image associated with this ROI. */
	public int getImageID() {
		return imp!=null?imp.getID():imageID;
	}

	public int getType() {
		return type;
	}
	
	public int getState() {
		return state;
	}
	
	/** Returns the perimeter length. */
	public double getLength() {
		double pw=1.0, ph=1.0;
		return 2.0*width*pw+2.0*height*ph;
	}

	/** Return this selection's bounding rectangle. */
	public Rectangle getBounds() {
		return new Rectangle(x, y, width, height);
	}

	/** Returns a copy of this roi. See Thinking is Java by Bruce Eckel
		(www.eckelobjects.com) for a good description of object cloning. */
	public synchronized Object clone() {
		try { 
			Roi r = (Roi)super.clone();
			r.setImage(null);
			r.setStroke(getStroke());
			r.setFillColor(getFillColor());
			r.imageID = getImageID();
			if (bounds!=null)
				r.bounds = (Rectangle2D.Double)bounds.clone();
			return r;
		}
		catch (CloneNotSupportedException e) {return null;}
	}

	protected void grow(int sx, int sy) {
		if (clipboard!=null) return;
		int xNew = ic.offScreenX(sx);
		int yNew = ic.offScreenY(sy);
		if (type==RECTANGLE) {
			if (xNew < 0) xNew = 0;
			if (yNew < 0) yNew = 0;
		}
		if (constrain) {
			// constrain selection to be square
			if (!center)
				{growConstrained(xNew, yNew); return;}
			int dx, dy, d;
			dx = xNew - x;
			dy = yNew - y;
			if (dx<dy)
				d = dx;
			else
				d = dy;
			xNew = x + d;
			yNew = y + d;
		}
		if (center) {
			width = Math.abs(xNew - startX)*2;
			height = Math.abs(yNew - startY)*2;
			x = startX - width/2;
			y = startY - height/2;
		} else {
			width = Math.abs(xNew - startX);
			height = Math.abs(yNew - startY);
			x = (xNew>=startX)?startX:startX - width;
			y = (yNew>=startY)?startY:startY - height;
			if (type==RECTANGLE) {
				if ((x+width) > xMax) width = xMax-x;
				if ((y+height) > yMax) height = yMax-y;
			}
		}
		updateClipRect();
		imp.draw(clipX, clipY, clipWidth, clipHeight);
		oldX = x;
		oldY = y;
		oldWidth = width;
		oldHeight = height;
		bounds = null;
	}

	private void growConstrained(int xNew, int yNew) {
		int dx = xNew - startX;
		int dy = yNew - startY;
		width = height = (int)Math.round(Math.sqrt(dx*dx + dy*dy));
		if (type==RECTANGLE) {
			x = (xNew>=startX)?startX:startX - width;
			y = (yNew>=startY)?startY:startY - height;
			if (x<0) x = 0;
			if (y<0) y = 0;
			if ((x+width) > xMax) width = xMax-x;
			if ((y+height) > yMax) height = yMax-y;
		} else {
			x = startX + dx/2 - width/2;
			y = startY + dy/2 - height/2;
		}
		updateClipRect();
		imp.draw(clipX, clipY, clipWidth, clipHeight);
		oldX = x;
		oldY = y;
		oldWidth = width;
		oldHeight = height;
	}

	void move(int sx, int sy) {
		int xNew = ic.offScreenX(sx);
		int yNew = ic.offScreenY(sy);
		int dx = xNew - startX;
		int dy = yNew - startY;
		if (dx==0 && dy==0)
			return;
		x += dx;
		y += dy;
		if (bounds!=null) {
			bounds.x += dx;
			bounds.y += dy;
		}
		startX = xNew;
		startY = yNew;
		updateClipRect();
		imp.draw(clipX, clipY, clipWidth, clipHeight);
		oldX = x;
		oldY = y;
		oldWidth = width;
		oldHeight=height;
		if (bounds!=null) {
			bounds.x = x;
			bounds.y = y;
		}
	}

	// Finds the union of current and previous roi
	protected void updateClipRect() {
		clipX = (x<=oldX)?x:oldX;
		clipY = (y<=oldY)?y:oldY;
		clipWidth = ((x+width>=oldX+oldWidth)?x+width:oldX+oldWidth) - clipX + 1;
		clipHeight = ((y+height>=oldY+oldHeight)?y+height:oldY+oldHeight) - clipY + 1;
		int m = 3;
		m += clipRectMargin();
		m = (int)(m+getStrokeWidth()*2);
		clipX-=m; clipY-=m;
		clipWidth+=m*2; clipHeight+=m*2;
	 }
	 
	protected int clipRectMargin() {
		return 0;
	}
		
	protected void handleMouseDrag(int sx, int sy, int flags) {
		if (ic==null) return;
		constrain = (flags&Event.SHIFT_MASK)!=0;
		center = (flags&Event.CTRL_MASK)!=0 || (SystemInfo.isMac&&(flags&Event.META_MASK)!=0);
		aspect = (flags&Event.ALT_MASK)!=0;
		switch(state) {
			case CONSTRUCTING:
				grow(sx, sy);
				break;
			case MOVING:
				move(sx, sy);
				break;
			default:
				break;
		}
	}

	public void draw(Graphics g) {
		Color color =  strokeColor!=null? strokeColor:ROIColor;
		if (fillColor!=null) color = fillColor;
		g.setColor(color);
		mag = getMagnification();
		int sw = (int)(width*mag);
		int sh = (int)(height*mag);
		int sx1 = screenX(x);
		int sy1 = screenY(y);
		int sx2 = sx1+sw/2;
		int sy2 = sy1+sh/2;
		int sx3 = sx1+sw;
		int sy3 = sy1+sh;
		Graphics2D g2d = (Graphics2D)g;
		if (stroke!=null)
			g2d.setStroke(getScaledStroke());
		if (cornerDiameter>0) {
			int sArcSize = (int)Math.round(cornerDiameter*mag);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			if (fillColor!=null)
				g.fillRoundRect(sx1, sy1, sw, sh, sArcSize, sArcSize);
			else
				g.drawRoundRect(sx1, sy1, sw, sh, sArcSize, sArcSize);
		} else {
			if (fillColor!=null) {
				if (!overlay && isActiveOverlayRoi()) {
					g.setColor(Color.cyan);
					g.drawRect(sx1, sy1, sw, sh);
				} else {
					g.drawRect(sx1, sy1, sw, sh);
				}
			} else
				g.drawRect(sx1, sy1, sw, sh);
		}
		if (state!=CONSTRUCTING && clipboard==null && !overlay) {
			int size2 = HANDLE_SIZE/2;
			drawHandle(g, sx1-size2, sy1-size2);
			drawHandle(g, sx2-size2, sy1-size2);
			drawHandle(g, sx3-size2, sy1-size2);
			drawHandle(g, sx3-size2, sy2-size2);
			drawHandle(g, sx3-size2, sy3-size2);
			drawHandle(g, sx2-size2, sy3-size2);
			drawHandle(g, sx1-size2, sy3-size2);
			drawHandle(g, sx1-size2, sy2-size2);
		}
		drawPreviousRoi(g);
		if (state!=NORMAL)
			showStatus();
		if (updateFullWindow)
			{updateFullWindow = false; imp.draw();}
	}

	void drawPreviousRoi(Graphics g) {
		if (previousRoi!=null && previousRoi!=this && previousRoi.modState!=NO_MODS) {
			previousRoi.setImage(imp);
			previousRoi.draw(g);
		}		
	}
	
	void drawHandle(Graphics g, int x, int y) {
		double size = (width*height)*mag*mag;
		if (type==LINE) {
			size = Math.sqrt(width*width+height*height);
			size *= size*mag*mag;
		}
		if (size>4000.0) {
			g.setColor(Color.black);
			g.fillRect(x,y,5,5);
			g.setColor(handleColor);
			g.fillRect(x+1,y+1,3,3);
		} else if (size>1000.0) {
			g.setColor(Color.black);
			g.fillRect(x+1,y+1,4,4);
			g.setColor(handleColor);
			g.fillRect(x+2,y+2,2,2);
		} else {
			g.setColor(Color.black);
			g.fillRect(x+1,y+1,3,3);
			g.setColor(handleColor);
			g.fillRect(x+2,y+2,1,1);
		}
	}

	public boolean contains(int x, int y) {
		Rectangle r = new Rectangle(this.x, this.y, width, height);
		boolean contains = r.contains(x, y);
		if (cornerDiameter==0 || contains==false)
			return contains;
		RoundRectangle2D rr = new RoundRectangle2D.Float(this.x, this.y, width, height, cornerDiameter, cornerDiameter);
		return rr.contains(x, y);
	}
		
	/** Returns a handle number if the specified screen coordinates are
		inside or near a handle, otherwise returns -1. */
	public int isHandle(int sx, int sy) {
		return -1;
	}

	protected void mouseDownInHandle(int handle, int sx, int sy) {
		state = MOVING_HANDLE;
		activeHandle = handle;
	}

	protected void handleMouseDown(int sx, int sy) {
		if (state==NORMAL && ic!=null) {
			state = MOVING;
			startX = ic.offScreenX(sx);
			startY = ic.offScreenY(sy);
			startXD = ic.offScreenXD(sx);
			startYD = ic.offScreenYD(sy);
		}
	}
		
	protected void handleMouseUp(int screenX, int screenY) {
		state = NORMAL;
		if (imp==null) return;
		imp.draw(clipX-5, clipY-5, clipWidth+10, clipHeight+10);
		modifyRoi();
	}

	void modifyRoi() {
		if (previousRoi==null || previousRoi.modState==NO_MODS || imp==null)
			return;
		Roi previous = (Roi)previousRoi.clone();
		previous.modState = NO_MODS;
		ShapeRoi s1	 = null;
		ShapeRoi s2 = null;
		if (previousRoi instanceof ShapeRoi)
			s1 = (ShapeRoi)previousRoi;
		else
			s1 = new ShapeRoi(previousRoi);
		if (this instanceof ShapeRoi)
			s2 = (ShapeRoi)this;
		else
			s2 = new ShapeRoi(this);
		if (previousRoi.modState==ADD_TO_ROI)
			s1.or(s2);
		else
			s1.not(s2);
		previousRoi.modState = NO_MODS;
		Roi[] rois = s1.getRois();
		if (rois.length==0) return;
		int type2 = rois[0].getType();
		Roi roi2 = null;
		if (rois.length==1 && (type2==POLYGON||type2==FREEROI))
			roi2 = rois[0];
		else
			roi2 = s1;
		if (roi2!=null)
			roi2.copyAttributes(previousRoi);
		imp.setRoi(roi2);
		previousRoi = previous;
	}

	protected void showStatus() {
		// do nothing
	}
		
	/** Always returns null for rectangular Roi's */
	public ImageProcessor getMask() {
		return null;
	}

	/** Returns the angle in degrees between the specified line and a horizontal line. */
	public double getAngle(int x1, int y1, int x2, int y2) {
		return getFloatAngle(x1, y1, x2, y2);
	}

	/** Returns the angle in degrees between the specified line and a horizontal line. */
	public double getFloatAngle(double x1, double y1, double x2, double y2) {
		double dx = x2-x1;
		double dy = y1-y2;
		return (180.0/Math.PI)*Math.atan2(dy, dx);
	}

	/** Sets the default (global) color used for ROI outlines.
	 * @see #getColor()
	 * @see #setStrokeColor(Color)
	 */
	public static void setColor(Color c) {
		ROIColor = c;
	}

	/** Returns the default (global) color used for drawing ROI outlines.
	 * @see #setColor(Color)
	 * @see #getStrokeColor()
	 */
	public static Color getColor() {
		return ROIColor;
	}

	/** Sets the color used by this ROI to draw its outline. This color, if not null, 
	 * overrides the global color set by the static setColor() method.
	 * @see #getStrokeColor
	 * @see #setStrokeWidth
	 */
	public void setStrokeColor(Color c) {
		 strokeColor = c;
	}

	/** Returns the the color used to draw the ROI outline or null if the default color is being used.
	 * @see #setStrokeColor(Color)
	 */
	public Color getStrokeColor() {
		return	strokeColor;
	}

	/** Sets the fill color used to display this ROI, or set to null to display it transparently.
	 * @see #getFillColor
	 * @see #setStrokeColor
	 */
	public void setFillColor(Color color) {
		fillColor = color;
	}

	/** Returns the fill color used to display this ROI, or null if it is displayed transparently.
	 * @see #setFillColor
	 * @see #getStrokeColor
	 */
	public Color getFillColor() {
		return fillColor;
	}
	
	public static void setDefaultFillColor(Color color) {
		defaultFillColor = color;
	}
	
	public static Color getDefaultFillColor() {
		return defaultFillColor;
	}
	
	/** Copy the attributes (outline color, fill color, outline width) 
		of	'roi2' to the this selection. */
	public void copyAttributes(Roi roi2) {
		this. strokeColor = roi2. strokeColor;
		this.fillColor = roi2.fillColor;
		this.stroke = roi2.stroke;
	}

	/**
	* @deprecated
	* replaced by setStrokeColor()
	*/
	public void setInstanceColor(Color c) {
		 strokeColor = c;
	}

	/**
	* @deprecated
	* replaced by setStrokeWidth(int)
	*/
	public void setLineWidth(int width) {
		setStrokeWidth(width) ;
	}
		
	public void updateWideLine(float width) {
		if (isLine()) {
			wideLine = true;
			setStrokeWidth(width);
			if (getStrokeColor()==null) {
				Color c = getColor();
				setStrokeColor(new Color(c.getRed(),c.getGreen(),c.getBlue(), 77));
			}
		}
	}

	/** Set 'nonScalable' true to have TextRois in a display 
		list drawn at a fixed location and size. */
	public void setNonScalable(boolean nonScalable) {
		this.nonScalable = nonScalable;
	}
	
	/** Sets the width of the line used to draw this ROI. Set
	 * the width to 0.0 and the ROI will be drawn using a
	 * a 1 pixel stroke width regardless of the magnification.
	 * @see #setStrokeColor(Color)
	 */
	public void setStrokeWidth(float width) {
		if (width<0f)
			width = 0f;
		if (width==0)
			stroke = null;
		else if (wideLine)
			this.stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
		else
			this.stroke = new BasicStroke(width);
		if (width>1f)
			fillColor = null;
	}

	/** This is a version of setStrokeWidth() that accepts a double argument. */
	public void setStrokeWidth(double width) {
		setStrokeWidth((float)width);
	}

	/** Returns the lineWidth. */
	public float getStrokeWidth() {
		return stroke!=null?stroke.getLineWidth():0f;
	}

	/** Sets the Stroke used to draw this ROI. */
	public void setStroke(BasicStroke stroke) {
		this.stroke = stroke;
	}
	
	/** Returns the Stroke used to draw this ROI, or null if no Stroke is used. */
	public BasicStroke getStroke() {
		return stroke;
	}
	
	protected BasicStroke getScaledStroke() {
		return stroke;
	}

	/** Returns the name of this ROI, or null. */
	public String getName() {
		return name;
	}

	/** Sets the name of this ROI. */
	public void setName(String name) {
		this.name = name;
	}

	/** Returns 'true' if this is an area selection. */
	public boolean isArea() {
		return (type>=RECTANGLE && type<=TRACED_ROI) || type==COMPOSITE;
	}

	/** Returns 'true' if this is a line selection. */
	public boolean isLine() {
		return type>=LINE && type<=FREELINE;
	}

	protected double getMagnification() {
		return 1.0;
	}

	/** Returns true if this ROI is currently displayed on an image. */
	public boolean isVisible() {
		return ic!=null;
	}

	/** Returns 'true' if this ROI is displayed and is also in an overlay. */
	public final boolean isActiveOverlayRoi() {
		return false;
	}

	protected int screenX(int ox) {return ic!=null?ic.screenX(ox):ox;}
	protected int screenY(int oy) {return ic!=null?ic.screenY(oy):oy;}
	protected int screenXD(double ox) {return ic!=null?ic.screenXD(ox):(int)ox;}
	protected int screenYD(double oy) {return ic!=null?ic.screenYD(oy):(int)oy;}

	/** Converts a float array to an int array using rounding. */
	public static int[] toIntR(float[] arr) {
		int n = arr.length;
		int[] temp = new int[n];
		for (int i=0; i<n; i++)
			temp[i] = (int)Math.floor(arr[i]+0.5);
		return temp;
	}

	/** Converts an int array to a float array. */
	public static float[] toFloat(int[] arr) {
		int n = arr.length;
		float[] temp = new float[n];
		for (int i=0; i<n; i++)
			temp[i] = arr[i];
		return temp;
	}

	public void mouseDragged(MouseEvent e) {
		handleMouseDrag(e.getX(), e.getY(), e.getModifiers());
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
		handleMouseUp(e.getX(), e.getY());
	}

	public double getXBase() {
		if (bounds!=null)
			return bounds.x;
		else
			return x;
	}

	public double getYBase() {
		if (bounds!=null)
			return bounds.y;
		else
			return y;
	}

	public void setIsCursor(boolean isCursor) {
		this.isCursor = isCursor;
	}

	public boolean isCursor() {
		return isCursor;
	}

	/**
	 * Required by the {@link Iterable} interface.
	 * Use to iterate over the contained coordinates. Usage example:
	 * <pre>
	 * for (Point p : roi) {
	 *   // process p
	 * }
	 * </pre>
	 * Author: Wilhelm Burger
	*/
	public Iterator<Point> iterator() {
		// Returns the default (mask-based) point iterator. Note that 'Line' overrides the
		// iterator() method and returns a specific point iterator.
		return new RoiPointsIteratorMask();
	}

	/**
	 * Default iterator over points contained in a mask-backed {@link Roi}.
	 * Author: W. Burger
	*/
	private class RoiPointsIteratorMask implements Iterator<Point> {
		private final ImageProcessor mask;
		private final Rectangle bounds;
		private final int xbase, ybase;
		private final int n;
		private int next;

		RoiPointsIteratorMask() {
				mask = getMask();
				bounds = getBounds();
				xbase = Roi.this.x;
				ybase = Roi.this.y;
			n = bounds.width * bounds.height;
			findNext(0);	// sets next
		}

		@Override
		public boolean hasNext() {
			return next < n;
		}

		@Override
		public Point next() {
			if (next >= n)
				throw new NoSuchElementException();
			int x = next % bounds.width;
			int y = next / bounds.width;
			findNext(next+1);
			return new Point(xbase+x, ybase+y);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		// finds the next element (from start), sets next
		private void findNext(int start) {
            throw new UnsupportedOperationException();
		}
	}

}
