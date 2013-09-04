package org.janelia.it.FlyWorkstation.geom;

public enum CoordinateAxis {
	X(0),
	Y(1),
	Z(2);
	
	private final int index;
	private CoordinateAxis(int indexParam) {
		this.index = indexParam;
	}
	
	public CoordinateAxis fromIndex(int index) {
		index = index % 3;
		if (index == 0) return X;
		if (index == 1) return Y;
		if (index == 2) return Z;
		return X; // never happens; shut up compiler
	}

	public int index() {return index;}
	
	public CoordinateAxis next() {
		return fromIndex(index() + 1);
	}
	
	public CoordinateAxis previous() {
		return fromIndex(index() + 2);
	}

	public String getName() {
		if (this == CoordinateAxis.X) 
			return "X";
		else if (this == CoordinateAxis.Y) 
			return "Y";
		return "Z";
	}
}
