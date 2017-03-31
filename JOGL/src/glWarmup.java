// Ian Torres - 27621588
// CICS 373 - Introduction to Computer Graphics
// Assignment 1

import java.awt.event.*;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.nativewindow.ScalableSurface;

import java.awt.event.KeyListener;
import javax.swing.JFrame;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class glWarmup extends JFrame implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, ActionListener {
    // window variables
    private final GLCanvas canvas;
    private int winW = 512, winH = 512;
    
    // mouse control variables
    private int mouseX, mouseY;
    private int mouseButton;
    private boolean mouseClick = false;
    private boolean clickedOnShape = false;

    // object transformation variables
    private float differenceTranslationX = 0.0f, differenceTranslationY = 0.0f, differenceTranslationZ = 0.0f;
    private float currentTranslationX = 0.0f, currentTranslationY = 0.0f, currentTranslationZ = -10.0f;
    private float differenceScale = 0.0f;
    private float currentScale = 1.0f;  
    private float differenceAngleX = 0.0f, differenceAngleY = 0.0f, differenceAngleZ = 0.0f;
    FloatBuffer currentRotation = FloatBuffer.allocate(16);

    // gl shading variables
    private boolean drawWireframe = false;
    private float lightPos[] = {-5.0f, 10.0f, 5.0f, 1.0f};

    // a set of shapes (your TODO item)
    // I chose to include: Cube, Cone, and Octahedron shapes
    private static final int Triangle = 0, Torus = 1, Sphere = 2, Icosahedron = 3, Teapot = 4, Cube = 5, Cone = 6, Octahedron = 7;
    // Changed from 5 to 8. If this is not changed the new shapes cannot be drawn (i.e. Line 290: (shape = (shape + 1) % NumShapes;)
    private static final int NumShapes = 8;
    // initial shape is a triangle
    private int shape = Triangle;

    // gl context/variables
    private GL2 gl;
    private final GLU glu = new GLU();
    private final GLUT glut = new GLUT();

    public static void main(String args[]) {
        new glWarmup();
    }

    // constructor, setup window with OpenGL capability
    public glWarmup() {
        super("Introduction to Computer Graphics - Assignment 1");
        final GLProfile glprofile = GLProfile.getMaxFixedFunc(true);
        GLCapabilities glcapabilities = new GLCapabilities(glprofile);
        canvas = new GLCanvas(glcapabilities);
        canvas.setSurfaceScale(new float[] { ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE }); // potential fix for Retina Displays
        canvas.addGLEventListener(this);
        canvas.addKeyListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        getContentPane().add(canvas);
        setSize(winW, winH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
        canvas.requestFocus();
    }

    // OpenGL display function
    public void display(GLAutoDrawable drawable) {
        // if mouse is clicked, we need to detect whether it's clicked on the shape
        if (mouseClick) {
            ByteBuffer pixel = ByteBuffer.allocateDirect(1);

            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
            gl.glColor3f(1.0f, 1.0f, 1.0f);
            gl.glDisable(GL2.GL_LIGHTING);
            drawShape();
            gl.glReadPixels(mouseX, (winH - 1 - mouseY), 1, 1, GL2.GL_RED, GL2.GL_UNSIGNED_BYTE, pixel);

            if (pixel.get(0) == (byte) 255) {
                // mouse clicked on the shape, set clickedOnShape to true
                clickedOnShape = true;
            }
            // set mouseClick to false to avoid detecting again
            mouseClick = false;
        }

        // shade the current shape [don't worry about the details here - we'll cover them later in the course]
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, drawWireframe ? GL2.GL_LINE : GL2.GL_FILL);
        gl.glColor3f(1.0f, 0.3f, 0.1f);
        drawShape();
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
    }


    // draw the current shape
    public void drawShape() {
        // calculate the new rotation based on input angle changes
        gl.glLoadIdentity(); // initialize rotation to identity matrix
        gl.glRotatef(differenceAngleX, 1.0f, 0.0f, 0.0f);        
        // your TODO item  (rotations in two other axes), hint: use glRotatef       
        gl.glRotatef(differenceAngleY, 0.0f, 1.0f, 0.0f); // Rotation about the Y-Axis
        gl.glRotatef(differenceAngleZ, 0.0f, 0.0f, 1.0f); // Rotation about the Z-Axis
        // the above instructions calcuated a new rotation (matrix)
        // which now needs to be combined (multiplied) with the current rotation (matrix)        
        // OpenGL does the combination (matrix multiplication) for us in the next line
        // and stores the result internally (MODELVIEW matrix)        
        gl.glMultMatrixf(currentRotation);
        // save the current rotation
        gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, currentRotation);

        // update current translation and scale
        currentTranslationX += differenceTranslationX;
        currentTranslationY += differenceTranslationY;
        currentTranslationZ += differenceTranslationZ; // this will be always zero in this assignment (it's ok)
        currentScale += differenceScale;
        
        // now that we calculated the current rotation, translation, scaling...
        // we need to apply them to a drawn object so that it is transformed
        // on the screen.
        // Transformations are applied in the following order [the order is important!]
        // 1. Initialize the internal OpenGL matrix to identity matrix 
        gl.glLoadIdentity();
        // 2. Apply the current translation
        gl.glTranslatef(currentTranslationX, currentTranslationY, currentTranslationZ);
        // 3. Apply the current scaling
        gl.glScalef(currentScale, currentScale, currentScale);
        // 4. Apply the current rotation
        gl.glMultMatrixf(currentRotation);

        // draw the shape
        switch (shape) {
            case Triangle:
                gl.glBegin(GL2.GL_TRIANGLES);
                gl.glVertex3f(0.0f, 1.0f, 0.0f);
                gl.glVertex3f(-1.0f, -0.5f, 0.0f);
                gl.glVertex3f(1.0f, -0.5f, 0.0f);
                gl.glEnd();
                break;
            case Torus:
                glut.glutSolidTorus(0.5f, 1.0f, 32, 32);
                break;
            case Sphere:
                glut.glutSolidSphere(1.0f, 32, 32);
                break;
            case Icosahedron:
                glut.glutSolidIcosahedron();
                break;
            case Teapot:
                gl.glFrontFace(GL2.GL_CW);
                glut.glutSolidTeapot(1.0f);
                gl.glFrontFace(GL2.GL_CCW);
                break;
            // more TODO items
            case Cube:
            	//Arbitrary choice for side lengths
            	glut.glutSolidCube(1.0f);
            	break;
            case Cone:
            	//The choice of numbers for these parameters are completely arbitrary 
            	//(first = reasonable radius, second = reasonable height, third = smoothed base, 
            	//fourth = good amount of subdivisions)
            	glut.glutSolidCone(1.0, 2.5, 50, 5);
            	break;
            case Octahedron:
            	glut.glutSolidOctahedron();  //One of my favorite polyhedrons
            	break;
        }

        // position the light
        gl.glLoadIdentity();
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);
        
        // everything is drawn, re-initializing all updates to zero
        differenceAngleX = 0.0f;
        differenceAngleY = 0.0f;
        differenceAngleZ = 0.0f;
        differenceTranslationX = 0.0f;
        differenceTranslationY = 0.0f;
        differenceTranslationZ = 0.0f;
        differenceScale = 0.0f;   
    }

    // initialization of OpenGL window / variables
    public void init(GLAutoDrawable drawable) {
        // will discuss these later in the course
        gl = drawable.getGL().getGL2();
        gl.setSwapInterval(1);

        gl.glColorMaterial(GL2.GL_FRONT, GL2.GL_DIFFUSE);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glEnable(GL2.GL_NORMALIZE);
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LESS);
        gl.glCullFace(GL2.GL_BACK);
        gl.glEnable(GL2.GL_CULL_FACE);

        // set clear color: this determines the background color (which is dark gray)
        gl.glClearColor(.3f, .3f, .3f, 1f);
        gl.glClearDepth(1.0f);

        // initialize current rotation to identity matrix
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, currentRotation);
    }

    // reshape callback function: called when the size of the window changes
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        winW = width;
        winH = height;

        // places camera correctly (will discuss later in the course)
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(30.0f, (float) width / (float) height, 0.01f, 100.0f);
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    // mouse pressed callback function
    // gets mouse position / button state
    public void mousePressed(MouseEvent e) {
        mouseClick = true;
        mouseX = e.getX();
        mouseY = e.getY();
        mouseButton = e.getButton();
        canvas.display();
    }

    // mouse released callback function
    public void mouseReleased(MouseEvent e) {
        clickedOnShape = false;
        canvas.display();
    }

    // mouse dragged callback function    
    public void mouseDragged(MouseEvent e) {
        if (!clickedOnShape) {
            return;
        }

        int x = e.getX();
        int y = e.getY();
        if (mouseButton == MouseEvent.BUTTON3) {
            // right button scales
            differenceScale = (y - mouseY) * 0.01f;
        } else if (mouseButton == MouseEvent.BUTTON2) {
            // middle button translates
            differenceTranslationX = (x - mouseX) * 0.01f;
            differenceTranslationY = -(y - mouseY) * 0.01f;
        } else if (mouseButton == MouseEvent.BUTTON1) {
            // left button + shift button rotates about z
            if (e.isShiftDown()) {
                // your TODO item
            	//Movements about the Z-Axis depends on the position of the mouse in both the X & Y coordinates
            	//This supports moving the mouse either in the vertical or horizontal directions.
            	//The equation is set up like so in order to "realistically" spin in the right direction.
            	differenceAngleZ = -(y - mouseY) - (x - mouseX);
            } else {
                    differenceAngleX = (y - mouseY);
                    // ... more TODO items ...
                    differenceAngleY = (x - mouseX);  // We are only missing the difference Angle for Y!
            }
        }
        mouseX = x;
        mouseY = y;
        canvas.display();
    }

    // key pressed callback function
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_Q:
                System.exit(0);
                break;
            case KeyEvent.VK_W:
                drawWireframe = !drawWireframe;
                break;
            case KeyEvent.VK_SPACE:
                shape = (shape + 1) % NumShapes;
                break;
        }
        canvas.display();
    }

    
    // these functions are not used for this assignment
    // but may be useful in the future
    public void dispose(GLAutoDrawable glautodrawable) {
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }
    
    public void keyTyped(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void actionPerformed(ActionEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

}
