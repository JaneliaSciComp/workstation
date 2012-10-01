package org.janelia.it.FlyWorkstation.gui.viewer3d;

public class Quaternion {
	private double[] q = new double[4];
	
	public Quaternion() {
		setElements(1,0,0,0);
	}
	
	public Quaternion(double angle, UnitVec3 axis) {
		double ca2 = Math.cos(angle/2.0);
		double sa2 = Math.sin(angle/2.0);
		if (ca2 < 0.0) {
			ca2 = -ca2;
			sa2 = -sa2;
		}
		setElements(ca2, sa2*axis.x(), sa2*axis.y(), sa2*axis.z());
	}
	
	private void setElements(double w, double x, double y, double z) {
		q[0] = w;
		q[1] = x;
		q[2] = y;
		q[3] = z;
	}
	
	public double w() {return q[0];}
	public double x() {return q[1];}
	public double y() {return q[2];}
	public double z() {return q[3];}
}
