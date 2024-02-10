import java.awt.Color;

public class DPolygon {
	Color c;
	double[] x, y, z;
	boolean
		draw       = true,
		occluded   = false,
		seeThrough = false;
	double[] CalcPos, newX, newY;
	PolygonObject DrawablePolygon;
	double AvgDist, zBuffer;

	Cube parent;
	
	public DPolygon(double[] x, double[] y,  double[] z, Color c, boolean seeThrough, Cube parent) {
		this.x          = x;
		this.y          = y;
		this.z          = z;
		this.c          = c;
		this.seeThrough = seeThrough; 
		this.parent     = parent;
		CreatePolygon();
	}
	
	void CreatePolygon() {
		DrawablePolygon = new PolygonObject(new double[x.length], new double[x.length], c, Screen.DPolygons.size(), seeThrough, this);
	}
	
	void UpdatePolygon() {

		newX = new double[x.length];
		newY = new double[x.length];
		if (!occluded && !BackFaceCulled()) {
			draw = true;
			for (int i = 0; i < x.length; i++) {
				CalcPos = Calculator.CalculatePositionP(Screen.ViewFrom, Screen.ViewTo, x[i], y[i], z[i]);
				newX[i] = (Main.ScreenSize.getWidth()  / 2 - Calculator.CalcFocusPos[0]) + CalcPos[0] * Screen.zoom;
				newY[i] = (Main.ScreenSize.getHeight() / 2 - Calculator.CalcFocusPos[1]) + CalcPos[1] * Screen.zoom;			
				if (Calculator.t < 0) { draw = false; }
			}
		}
		
		CalcLighting();
		
		DrawablePolygon.draw = draw;
		DrawablePolygon.UpdatePolygon(newX, newY);
		AvgDist = GetDist();
	}
	
	void CalcLighting() {
		Plane lightingPlane = new Plane(this);

		double angle = Math.acos(
            (
                (lightingPlane.NV.x * Screen.LightDir[0]) +
                (lightingPlane.NV.y * Screen.LightDir[1]) +
                (lightingPlane.NV.z * Screen.LightDir[2])
            ) / (
                Math.sqrt(
                    Screen.LightDir[0] * Screen.LightDir[0] +
                    Screen.LightDir[1] * Screen.LightDir[1] +
                    Screen.LightDir[2] * Screen.LightDir[2]
                )
            )
        );
		
		DrawablePolygon.lighting = 0.2 + 1 - Math.sqrt(Math.toDegrees(angle) / 180);

		if (DrawablePolygon.lighting > 1) { DrawablePolygon.lighting = 1; }
		if (DrawablePolygon.lighting < 0) { DrawablePolygon.lighting = 0; }
	}
		
	double GetDist() {
		double total = 0;
		for (int i = 0; i < x.length; i++) { total += GetDistanceToP(i); }
		return total / x.length;
	}
	
	double GetDistanceToP(int i) {
		return Math.sqrt(
            (Screen.ViewFrom[0] - x[i]) * (Screen.ViewFrom[0] - x[i]) + 
            (Screen.ViewFrom[1] - y[i]) * (Screen.ViewFrom[1] - y[i]) +
            (Screen.ViewFrom[2] - z[i]) * (Screen.ViewFrom[2] - z[i])
        );
	}
	Vector GetVectorToFace() {
		double
			x_val = 0,
			y_val = 0,
			z_val = 0;
		
		for (int i = 0; i < x.length; i++) {
			x_val += x[i];
			y_val += y[i];
			z_val += z[i];
		}
		
		Vector V = new Vector(
			(x.length * Screen.ViewFrom[0] - x_val) / x.length,
			(x.length * Screen.ViewFrom[1] - y_val) / x.length,
			(x.length * Screen.ViewFrom[2] - z_val) / x.length
		);
		return V;
	}

	boolean BackFaceCulled() {
		Vector ViewVector = GetVectorToFace();
		Vector Side2      = new Vector(x[1] - x[0], y[1] - y[0], z[1] - z[0]);
		Vector Side1      = new Vector(x[2] - x[1], y[2] - y[1], z[2] - z[1]);
		Vector Normal     = Side1.CrossWith(Side2);
		return ViewVector.DotWith(Normal) <= 0;
	}

}