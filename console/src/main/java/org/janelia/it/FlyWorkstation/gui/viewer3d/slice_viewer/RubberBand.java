package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;

public class RubberBand
{
	// private static final Logger log = LoggerFactory.getLogger(RubberBand.class);

	protected Color startColor = new Color(0.85f, 0.85f, 0.85f, 0.15f);
	protected Color endColor = new Color(1.0f, 1.0f, 1.0f, 0.15f);
	protected Color outlineColor = new Color(0.9f, 0.9f, 0.7f, 0.9f);
	protected float dash1[] = {6.0f};
	protected BasicStroke dashed = new BasicStroke(2.0f, 
			BasicStroke.CAP_BUTT,
			BasicStroke.JOIN_ROUND,
			4.0f, dash1, 0.0f);
	protected Point startPoint = new Point(10, 10);
	protected Point endPoint = new Point(30, 30);
	boolean visible = false;

	public QtSignal changed = new QtSignal();
	
	public Point getStartPoint() {
		return startPoint;
	}

	public void setStartPoint(Point startPoint) {
		if (this.startPoint == startPoint)
			return;
		this.startPoint = startPoint;
		if (this.visible)
			changed.emit();
	}

	public Point getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(Point endPoint) {
		//log.info("setEndPoint");
		if (this.endPoint == endPoint)
			return;
		this.endPoint = endPoint;
		if (this.isVisible())
			changed.emit();
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		//log.info("setVisible "+visible);
		if (this.visible == visible)
			return;
		this.visible = visible;
		changed.emit();
	}

	public void paint(Graphics2D g) 
	{
		//log.info("paint");
		if (! visible)
			return;
		int x = startPoint.x;
		int y = startPoint.y;
		int w = endPoint.x - startPoint.x;
		int h = endPoint.y - startPoint.y;
		if (w == 0) return;
		if (h == 0) return;
		if (w < 0) {
			w = -w;
			x = endPoint.x;
		}
		if (h < 0) {
			h = -h;
			y = endPoint.y;
		}
		// Transparent gray rectangle with a subtle gradient
		GradientPaint gradient = new GradientPaint(
				x, y, startColor,
				x+w, y+h, endColor);
		g.setPaint(gradient);
		g.fillRect(x, y, w, h);
		// Dashed yellow outline
		g.setColor(outlineColor);
		g.setStroke(dashed);
		g.drawRect(x, y, w, h);
	}
}
