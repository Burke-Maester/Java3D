import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.JPanel;

public class Screen extends JPanel implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
	
	// ArrayList of all the 3D polygons - each 3D polygon has a 2D 'PolygonObject' inside called 'DrawablePolygon'
	static ArrayList<DPolygon> DPolygons = new ArrayList<DPolygon>();
	
	static ArrayList<Cube> Cubes    = new ArrayList<Cube>();
	       ArrayList<Cube> TopCubes = new ArrayList<Cube>();
	
	// The polygon that the mouse is currently over
	static PolygonObject PolygonOver  = null;
	static Cube          CubeOver     = null;

	// Used for keeping mouse in center
	Robot r;

	static double[]
        ViewFrom = new double[] { 15, 5, 10 },
		ViewTo   = new double[] { 8,  8, 0  },
		LightDir = new double[] { 1,  1, 1  };

	
	// The smaller the zoom the more zoomed out you are and visa versa, although altering too far from 1000 will make it look pretty weird
	static double
        zoom          = 1000,
        MinZoom       = 500,
        MaxZoom       = 2500,
        MouseX        = 0,
        MouseY        = 0,
        MovementSpeed = 0.5;
	
	// FPS is a bit primitive, you can set the MaxFPS as high as you want
	double
        drawFPS      = 0,
        MaxFPS       = 120,
        SleepTime    = 1000.0 / MaxFPS,
        LastRefresh  = 0,
        StartTime    = System.currentTimeMillis(),
        LastFPSCheck = 0,
        Checks       = 0;

	// VertLook goes from 0.999 to -0.999, minus being looking down and + looking up, HorLook takes any number and goes round in radians
	// AimSight changes the size of the center-cross. The lower HorRotSpeed or VertRotSpeed, the faster the camera will rotate in those directions
	double
        VertLook     = -0.9,
        HorLook      = 0,
        aimSight     = 4,
        HorRotSpeed  = 900,
        VertRotSpeed = 2200,
        SunPos       = 0,
		nearPlane    = 100,
		farPlane     = 0.001;

	// Will hold the order that the polygons in the ArrayList DPolygon should be drawn meaning DPolygon.get(NewOrder[0]) gets drawn first
	int[] NewOrder, zOrder;

	static boolean WireFrame = false;
	boolean[] Keys           = new boolean[6];
	
	long repaintTime = 0;
	
	public Screen() {
		this.addKeyListener(this);
		setFocusable(true);		
		
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addMouseWheelListener(this);
		
		InvisibleMouse();
		new GenerateTerrain();
		// Cubes.add    (new Cube    (0,  -5, 0, 2, 2, 2, Color.red  ));
		// Prisms.add   (new Prism   (6,  -5, 0, 2, 2, 2, Color.green));
		// Pyramids.add (new Pyramid (12, -5, 0, 2, 2, 2, Color.blue ));
		// Cubes.add    (new Cube    (18, -5, 0, 2, 2, 2, Color.red  ));
		// Cubes.add    (new Cube    (20, -5, 0, 2, 2, 2, Color.red  ));
		// Cubes.add    (new Cube    (22, -5, 0, 2, 2, 2, Color.red  ));
		// Cubes.add    (new Cube    (20, -5, 2, 2, 2, 2, Color.red  ));
		// Prisms.add   (new Prism   (18, -5, 2, 2, 2, 2, Color.green));
		// Prisms.add   (new Prism   (22, -5, 2, 2, 2, 2, Color.green));
		// Pyramids.add (new Pyramid (20, -5, 4, 2, 2, 2, Color.blue ));
	}	
	
	public void paintComponent(Graphics g) {
		// Clear screen and draw background color
		g.setColor(new Color(140, 180, 180));
		g.fillRect(0, 0, (int)Main.ScreenSize.getWidth(), (int)Main.ScreenSize.getHeight());

		CameraMovement();
		
		// Calculated all that is general for this camera position
		Calculator.SetPrederterminedInfo();

		ControlSunAndLight();

		// Updates each polygon for this camera position
		for (int i = 0; i < DPolygons.size(); i++) { DPolygons.get(i).UpdatePolygon(); }
		
		// Rotate and update shape examples
		// Cubes.get(0).rotation += 0.01;
		// Cubes.get(0).UpdatePoly(); 

		// Prisms.get(0).rotation += 0.01;
		// Prisms.get(0).UpdatePoly();
		
		// Pyramids.get(0).rotation += 0.01;
		// Pyramids.get(0).UpdatePoly();

		// Set drawing order so closest polygons gets drawn last
		Sort();
		ZSort();
			
		// Set the polygon that the mouse is currently over
		SetPolygonOver();
		SetVisiblePolygons();
			
		// Draw polygons in the Order that is set by the 'setOrder' function
		for (int i = 0; i < NewOrder.length; i++) { DPolygons.get(NewOrder[i]).DrawablePolygon.Draw(g); }
			
		// Draw the cross in the center of the screen
		DrawMouseAim(g);			
		
		// FPS display
		g.drawString("FPS: " + (int)drawFPS + " (Benchmark)", 40, 40);
		
//		repaintTime = System.currentTimeMillis() - repaintTime; 
//		System.out.println(repaintTime);
		SleepAndRefresh();
	}
	
	void Sort() {
		double[] k = new double[DPolygons.size()];
		NewOrder   = new int[DPolygons.size()];
		
		for (int i = 0; i < DPolygons.size(); i++) {
			k[i]        = DPolygons.get(i).AvgDist;
			NewOrder[i] = i;
		}
		
	    double temp;
	    int tempr;
		for (int a = 0; a < k.length - 1; a++) {
			for (int b = 0; b < k.length - 1; b++) {
				if (k[b] < k[b + 1]) {
					temp        = k[b];
					tempr       = NewOrder[b];
					NewOrder[b] = NewOrder[b + 1];
					k[b]        = k[b + 1];
					   
					NewOrder[b + 1] = tempr;
					k[b + 1]        = temp;
				}
            }
        }
	}

	void ZSort() {
		// z' = (far + near) / (far - near) + (1 / z)((-2 • far • near) / (far - near))
		// I can calcualate "(far + near) / (far - near)" and "((-2 • far • near) / (far - near))" beforehand
		// https://en.wikipedia.org/wiki/Z-buffering#Mathematics
		double 
			preCalc1 = ((farPlane + nearPlane) / (farPlane - nearPlane)),
			preCalc2 = ((-2 * farPlane * nearPlane) / (farPlane - nearPlane));
		
		for (int i = 0; i < Cubes.size(); i++) {
			for (int j = 0; j < Cubes.get(i).Polys.length; j++) {
				Cubes.get(i).Polys[j].zBuffer = preCalc1 + (1 / Cubes.get(i).Polys[j].zBuffer) * preCalc2;
			}
		}
		
		double[] k = new double[DPolygons.size()];
		zOrder     = new int[DPolygons.size()];
		
		for (int i = 0; i < DPolygons.size(); i++) {
			k[i]      = DPolygons.get(i).zBuffer;
			zOrder[i] = i;
		}
		
	    double temp;
	    int tempr;
		for (int a = 0; a < k.length - 1; a++) {
			for (int b = 0; b < k.length - 1; b++) {
				if (k[b] < k[b + 1]) {
					temp      = k[b];
					tempr     = zOrder[b];
					zOrder[b] = zOrder[b + 1];
					k[b]      = k[b + 1];
					   
					zOrder[b + 1] = tempr;
					k[b + 1]      = temp;
				}
            }
        }
	}
		
	void InvisibleMouse() {
		 Toolkit toolkit           = Toolkit.getDefaultToolkit();
		 BufferedImage cursorImage = new BufferedImage(1, 1, BufferedImage.TRANSLUCENT); 
		 Cursor invisibleCursor    = toolkit.createCustomCursor(cursorImage, new Point(0,0), "InvisibleCursor");        
		 setCursor(invisibleCursor);
	}
	
	void DrawMouseAim(Graphics g) {
		g.setColor(Color.black);
		g.drawLine((int)(Main.ScreenSize.getWidth() / 2 - aimSight), (int)(Main.ScreenSize.getHeight() / 2),            (int)(Main.ScreenSize.getWidth() / 2 + aimSight), (int)(Main.ScreenSize.getHeight() / 2));
		g.drawLine((int)(Main.ScreenSize.getWidth() / 2),            (int)(Main.ScreenSize.getHeight() / 2 - aimSight), (int)(Main.ScreenSize.getWidth() / 2),            (int)(Main.ScreenSize.getHeight() / 2 + aimSight));			
	}

	void SleepAndRefresh() {
		long timeSLU = (long)(System.currentTimeMillis() - LastRefresh);

		Checks ++;			
		if (Checks >= 15) {
			drawFPS      = Checks / ((System.currentTimeMillis() - LastFPSCheck) / 1000.0);
			LastFPSCheck = System.currentTimeMillis();
			Checks       = 0;
		}
		
		if (timeSLU < 1000.0/MaxFPS) {
			try {
				Thread.sleep((long) (1000.0 / MaxFPS - timeSLU));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
				
		LastRefresh = System.currentTimeMillis();
		
		repaint();
	}
	
	void ControlSunAndLight() {
		SunPos        += 0.005;
		double mapSize = GenerateTerrain.mapSize * GenerateTerrain.Size;
		LightDir[0]    = mapSize / 2 - (mapSize / 2 + Math.cos(SunPos) * mapSize * 10);
		LightDir[1]    = mapSize / 2 - (mapSize / 2 + Math.sin(SunPos) * mapSize * 10);
		LightDir[2]    = -200;
	}
	
	void CameraMovement() {
		Vector ViewVector = new Vector(ViewTo[0] - ViewFrom[0], ViewTo[1] - ViewFrom[1], ViewTo[2] - ViewFrom[2]);
		double
            xMove = 0,
            yMove = 0,
            zMove = 0;
        
		Vector VerticalVector = new Vector (0, 0, 1);
		Vector SideViewVector = ViewVector.CrossWith(VerticalVector);
		
		if (Keys[0]) {
			xMove += ViewVector.x;
			yMove += ViewVector.y;
		}

		if (Keys[2]) {
			xMove -= ViewVector.x;
			yMove -= ViewVector.y;
		}
			
		if (Keys[1]) {
			xMove += SideViewVector.x;
			yMove += SideViewVector.y;
		}

		if (Keys[3]) {
			xMove -= SideViewVector.x;
			yMove -= SideViewVector.y;
		}

        if (Keys[4]) {
            zMove += VerticalVector.z;
        }

        if (Keys[5]) {
            zMove -= VerticalVector.z;
        }
		
		Vector MoveVector = new Vector(xMove, yMove, zMove);
		MoveTo(ViewFrom[0] + MoveVector.x * MovementSpeed, ViewFrom[1] + MoveVector.y * MovementSpeed, ViewFrom[2] + MoveVector.z * MovementSpeed);
	}

	void MoveTo(double x, double y, double z) {
		ViewFrom[0] = x;
		ViewFrom[1] = y;
		ViewFrom[2] = z;
		UpdateView();
	}

	void SetVisiblePolygons() {

	}

	void SetPolygonOver() {
		PolygonOver = null;
		for (int i = NewOrder.length - 1; i >= 0; i--) {
			if (
				DPolygons.get(NewOrder[i]).draw                        &&
                DPolygons.get(NewOrder[i]).DrawablePolygon.visible     &&
				DPolygons.get(NewOrder[i]).DrawablePolygon.MouseOver() &&
				DPolygons.get(NewOrder[i]).GetDist() < 6
			) {
				PolygonOver = DPolygons.get(NewOrder[i]).DrawablePolygon;
				CubeOver    = PolygonOver.parent.parent;
				break;
			}
        }
	}

	void MouseMovement(double NewMouseX, double NewMouseY) {
        double difX = (NewMouseX - Main.ScreenSize.getWidth()  / 2);
        double difY = (NewMouseY - Main.ScreenSize.getHeight() / 2);
        difY       *= 6 - Math.abs(VertLook) * 5;
        VertLook   -= difY / VertRotSpeed;
        HorLook    += difX / HorRotSpeed;

        if (VertLook >  0.999) { VertLook =  0.999; }
        if (VertLook < -0.999) { VertLook = -0.999; }
        
        UpdateView();
	}
	
	void UpdateView() {
		double r  = Math.sqrt(1 - (VertLook * VertLook)); // get radius
		ViewTo[0] = ViewFrom[0] + r * Math.cos(HorLook);
		ViewTo[1] = ViewFrom[1] + r * Math.sin(HorLook);
		ViewTo[2] = ViewFrom[2] + VertLook;
	}
	
	void CenterMouse() {
        try {
            r = new Robot();
			do {
				r.mouseMove((int)Main.ScreenSize.getWidth() / 2, (int)Main.ScreenSize.getHeight() / 2);
			} while (MouseInfo.getPointerInfo().getLocation().getY() != (int)Main.ScreenSize.getHeight() / 2);
        } catch (AWTException e) {
            e.printStackTrace();
        }
	}
	
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_W)      { Keys[0]   = true;       }
		if (e.getKeyCode() == KeyEvent.VK_A)      { Keys[1]   = true;       }
		if (e.getKeyCode() == KeyEvent.VK_S)      { Keys[2]   = true;       }
		if (e.getKeyCode() == KeyEvent.VK_D)      { Keys[3]   = true;       }
        if (e.getKeyCode() == KeyEvent.VK_SPACE)  { Keys[4]   = true;       }
        if (e.getKeyCode() == KeyEvent.VK_SHIFT)  { Keys[5]   = true;       }
		if (e.getKeyCode() == KeyEvent.VK_O)      { WireFrame = !WireFrame; }
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { System.exit(0);  }
	}

	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_W)     { Keys[0] = false; }
		if (e.getKeyCode() == KeyEvent.VK_A)     { Keys[1] = false; }
		if (e.getKeyCode() == KeyEvent.VK_S)     { Keys[2] = false; }
		if (e.getKeyCode() == KeyEvent.VK_D)     { Keys[3] = false; }
        if (e.getKeyCode() == KeyEvent.VK_SPACE) { Keys[4] = false; }
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) { Keys[5] = false; }
	}

	public void keyTyped(KeyEvent e) {}

	public void mouseDragged(MouseEvent arg0) {
		MouseMovement(MouseInfo.getPointerInfo().getLocation().getX(), MouseInfo.getPointerInfo().getLocation().getY());
		MouseX = MouseInfo.getPointerInfo().getLocation().getX();
		MouseY = MouseInfo.getPointerInfo().getLocation().getY();
		CenterMouse();
	}
	
	public void mouseMoved(MouseEvent arg0) {
		MouseMovement(MouseInfo.getPointerInfo().getLocation().getX(), MouseInfo.getPointerInfo().getLocation().getY());
		MouseX = MouseInfo.getPointerInfo().getLocation().getX();
		MouseY = MouseInfo.getPointerInfo().getLocation().getY();
		CenterMouse();
	}
	
	public void mouseClicked (MouseEvent arg0) {}
	public void mouseEntered (MouseEvent arg0) {}
    public void mouseExited  (MouseEvent arg0) {}
    public void mouseReleased(MouseEvent arg0) {}

	public void mousePressed(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON1 && PolygonOver != null) {
			PolygonOver.parent.parent.Remove();
        }

		if (arg0.getButton() == MouseEvent.BUTTON3 && PolygonOver != null) {
			/*
			 * 0 => bottom
			 * 1 => top
			 * 2 => left
			 * 3 => back
			 * 4 => right
			 * 5 => forward
			 */
			if (PolygonOver.parent == CubeOver.Polys[0]) {
				Cubes.add(new Cube(CubeOver.x, CubeOver.y, CubeOver.z - 1, 1, 1, 1, GenerateTerrain.G));
			} else if (PolygonOver.parent == CubeOver.Polys[1]) {
				Cubes.add(new Cube(CubeOver.x, CubeOver.y, CubeOver.z + 1, 1, 1, 1, GenerateTerrain.G));
			} else if (PolygonOver.parent == CubeOver.Polys[2]) {
				Cubes.add(new Cube(CubeOver.x, CubeOver.y + 1, CubeOver.z, 1, 1, 1, GenerateTerrain.G));
			} else if (PolygonOver.parent == CubeOver.Polys[3]) {
				Cubes.add(new Cube(CubeOver.x - 1, CubeOver.y, CubeOver.z, 1, 1, 1, GenerateTerrain.G));
			} else if (PolygonOver.parent == CubeOver.Polys[4]) {
				Cubes.add(new Cube(CubeOver.x, CubeOver.y - 1, CubeOver.z, 1, 1, 1, GenerateTerrain.G));
			} else if (PolygonOver.parent == CubeOver.Polys[5]) {
				Cubes.add(new Cube(CubeOver.x + 1, CubeOver.y, CubeOver.z, 1, 1, 1, GenerateTerrain.G));
			}
        }
    }

	public void mouseWheelMoved(MouseWheelEvent arg0) {
		if (arg0.getUnitsToScroll() > 0) {
			if (zoom > MinZoom) {
				zoom -= 25 * arg0.getUnitsToScroll();
            }
		} else {
			if (zoom < MaxZoom) {
				zoom -= 25 * arg0.getUnitsToScroll();
            }
		}	
	}

}