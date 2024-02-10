import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;

public class PolygonObject {

	Polygon P;
	Color c;
	boolean
        draw    = true,
        visible = true,
        seeThrough;
	double lighting = 1;

	DPolygon parent;
	
	public PolygonObject(double[] x, double[] y, Color c, int n, boolean seeThrough, DPolygon parent) {
		P = new Polygon();
		for(int i = 0; i < x.length; i++) { P.addPoint((int)x[i], (int)y[i]); }
		this.c          = c;
		this.seeThrough = seeThrough;
		this.parent     = parent;
	}
	
	void UpdatePolygon(double[] x, double[] y) {
		P.reset();
		for (int i = 0; i<x.length; i++) {
			P.xpoints[i] = (int) x[i];
			P.ypoints[i] = (int) y[i];
			P.npoints = x.length;
		}
	}
	
	void Draw(Graphics g) {
		if (draw && visible) {
			g.setColor(new Color((int)(c.getRed() * lighting), (int)(c.getGreen() * lighting), (int)(c.getBlue() * lighting)));
			if (Screen.WireFrame) { g.drawPolygon(P); return; }
			
			g.fillPolygon(P);

			if (Screen.PolygonOver == this) {
				g.setColor(new Color(0, 0, 0));
				g.drawPolygon(P);
				g.setColor(new Color(255, 255, 255, 100));
				g.fillPolygon(P);
			}
		}
	}
	
	boolean MouseOver() {
		return P.contains(Main.ScreenSize.getWidth() / 2, Main.ScreenSize.getHeight() / 2);
	}

}