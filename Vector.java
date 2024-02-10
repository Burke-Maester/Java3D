public class Vector {

	double x, y, z;

	public Vector(double x, double y, double z) {
		double length = Math.sqrt(x*x + y*y + z*z);

		if (length > 0) {
			this.x = x / length;
			this.y = y / length;
			this.z = z / length;
		}

	}
	
	Vector CrossWith(Vector V) {
		Vector CrossVector = new Vector(
				y * V.z - z * V.y,
				z * V.x - x * V.z,
				x * V.y - y * V.x);
		return CrossVector;		
	}
    
	double DotWith(Vector V) {
		return (x * V.x + y * V.y + z * V.z);
	}

}