import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.nativewindow.ScalableSurface;

import javax.swing.JFrame;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import java.util.ArrayList;

class Hierarchical extends JFrame implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, ActionListener {

    /* This defines the objModel class, which takes care
	 * of loading a triangular mesh from an obj file,
	 * estimating per vertex average normal,
	 * and displaying the mesh.
     */
	
	private final GLUT glut = new GLUT();  //Used to draw GLUT objects
	
    class objModel {

        public FloatBuffer vertexBuffer;
        public IntBuffer faceBuffer;
        public FloatBuffer normalBuffer;
        public Point3f center;
        public int num_verts;		// number of vertices
        public int num_faces;		// number of triangle faces

        public void Draw() {
            vertexBuffer.rewind();
            normalBuffer.rewind();
            faceBuffer.rewind();
            gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);

            gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vertexBuffer);
            gl.glNormalPointer(GL2.GL_FLOAT, 0, normalBuffer);

            gl.glDrawElements(GL2.GL_TRIANGLES, num_faces * 3, GL2.GL_UNSIGNED_INT, faceBuffer);

            gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
        }

        public objModel(String filename) {
            /* load a triangular mesh model from a .obj file */
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(filename));
            } catch (IOException e) {
                System.out.println("Error reading from file " + filename);
                System.exit(0);
            }

            center = new Point3f();
            float x, y, z;
            int v1, v2, v3;
            float minx, miny, minz;
            float maxx, maxy, maxz;
            float bbx, bby, bbz;
            minx = miny = minz = 10000.f;
            maxx = maxy = maxz = -10000.f;

            String line;
            String[] tokens;
            ArrayList<Point3f> input_verts = new ArrayList<Point3f>();
            ArrayList<Integer> input_faces = new ArrayList<Integer>();
            ArrayList<Vector3f> input_norms = new ArrayList<Vector3f>();
            try {
                while ((line = in.readLine()) != null) {
                    if (line.length() == 0) {
                        continue;
                    }
                    switch (line.charAt(0)) {
                        case 'v':
                            tokens = line.split("[ ]+");
                            x = Float.valueOf(tokens[1]);
                            y = Float.valueOf(tokens[2]);
                            z = Float.valueOf(tokens[3]);
                            minx = Math.min(minx, x);
                            miny = Math.min(miny, y);
                            minz = Math.min(minz, z);
                            maxx = Math.max(maxx, x);
                            maxy = Math.max(maxy, y);
                            maxz = Math.max(maxz, z);
                            input_verts.add(new Point3f(x, y, z));
                            center.add(new Point3f(x, y, z));
                            break;
                        case 'f':
                            tokens = line.split("[ ]+");
                            v1 = Integer.valueOf(tokens[1]) - 1;
                            v2 = Integer.valueOf(tokens[2]) - 1;
                            v3 = Integer.valueOf(tokens[3]) - 1;
                            input_faces.add(v1);
                            input_faces.add(v2);
                            input_faces.add(v3);
                            break;
                        default:
                            continue;
                    }
                }
                in.close();
            } catch (IOException e) {
                System.out.println("Unhandled error while reading input file.");
            }

            System.out.println("Read " + input_verts.size()
                    + " vertices and " + input_faces.size() + " faces.");

            center.scale(1.f / (float) input_verts.size());

            bbx = maxx - minx;
            bby = maxy - miny;
            bbz = maxz - minz;
            float bbmax = Math.max(bbx, Math.max(bby, bbz));

            for (Point3f p : input_verts) {

                p.x = (p.x - center.x) / bbmax;
                p.y = (p.y - center.y) / bbmax;
                p.z = (p.z - center.z) / bbmax;
            }
            center.x = center.y = center.z = 0.f;

            /* estimate per vertex average normal */
            int i;
            for (i = 0; i < input_verts.size(); i++) {
                input_norms.add(new Vector3f());
            }

            Vector3f e1 = new Vector3f();
            Vector3f e2 = new Vector3f();
            Vector3f tn = new Vector3f();
            for (i = 0; i < input_faces.size(); i += 3) {
                v1 = input_faces.get(i + 0);
                v2 = input_faces.get(i + 1);
                v3 = input_faces.get(i + 2);

                e1.sub(input_verts.get(v2), input_verts.get(v1));
                e2.sub(input_verts.get(v3), input_verts.get(v1));
                tn.cross(e1, e2);
                input_norms.get(v1).add(tn);

                e1.sub(input_verts.get(v3), input_verts.get(v2));
                e2.sub(input_verts.get(v1), input_verts.get(v2));
                tn.cross(e1, e2);
                input_norms.get(v2).add(tn);

                e1.sub(input_verts.get(v1), input_verts.get(v3));
                e2.sub(input_verts.get(v2), input_verts.get(v3));
                tn.cross(e1, e2);
                input_norms.get(v3).add(tn);
            }

            /* convert to buffers to improve display speed */
            for (i = 0; i < input_verts.size(); i++) {
                input_norms.get(i).normalize();
            }

            vertexBuffer = Buffers.newDirectFloatBuffer(input_verts.size() * 3);
            normalBuffer = Buffers.newDirectFloatBuffer(input_verts.size() * 3);
            faceBuffer = Buffers.newDirectIntBuffer(input_faces.size());

            for (i = 0; i < input_verts.size(); i++) {
                vertexBuffer.put(input_verts.get(i).x);
                vertexBuffer.put(input_verts.get(i).y);
                vertexBuffer.put(input_verts.get(i).z);
                normalBuffer.put(input_norms.get(i).x);
                normalBuffer.put(input_norms.get(i).y);
                normalBuffer.put(input_norms.get(i).z);
            }

            for (i = 0; i < input_faces.size(); i++) {
                faceBuffer.put(input_faces.get(i));
            }
            num_verts = input_verts.size();
            num_faces = input_faces.size() / 3;
        }
    }

    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_Q:
                System.exit(0);
                break;
            case 'r':
            case 'R':
                initViewParameters();
                break;
            case 'w':
            case 'W':
                wireframe = !wireframe;
                break;
            case 'b':
            case 'B':
                cullface = !cullface;
                break;
            case 'f':
            case 'F':
                flatshade = !flatshade;
                break;
            case 'a':
            case 'A':
                if (animator.isAnimating()) {
                    animator.stop();
                } else {
                    animator.start();
                }
                break;
            case '+':
            case '=':
                animation_speed *= 1.2f;
                break;
            case '-':
            case '_':
                animation_speed /= 1.2;
                break;
            default:
                break;
        }
        canvas.display();
    }

    /* GL, display, model transformation, and mouse control variables */
    private final GLCanvas canvas;
    private GL2 gl;
    private final GLU glu = new GLU();
    private FPSAnimator animator;

    private int winW = 800, winH = 800;
    private boolean wireframe = false;
    private boolean cullface = true;
    private boolean flatshade = false;

    private float xpos = 0, ypos = 0, zpos = 0;
    private float centerx, centery, centerz;
    private float roth = 0, rotv = 0;
    private float znear, zfar;
    private int mouseX, mouseY, mouseButton;
    private float motionSpeed, rotateSpeed;
    private float animation_speed = 1.0f;

    /* === YOUR WORK HERE === */
 /* Define more models you need for constructing your scene */
    //private objModel example_model = new objModel("bunny.obj");    //Example Object
    //private objModel example_model2 = new objModel("cactus.obj");  //Example Object

    private float example_rotateT = 0.f;
    
    private double theta1 = -25.0f;
    private double theta2 = -25.0f;
    private double theta3 = -25.0f;
    private double theta4 = -25.0f;
    private double theta5 = -25.0f;
    private double theta6 = -25.0f;
    private double theta7 = -25.0f;
    
    private float trans_ellipsoid = 0.0f;
    
    private float trans_arm2 = 0.0f;
    
    private float translate_box1 = 0.0f;
    private float translate_box2 = 0.0f;
    private float translate_box3 = 0.0f;
    private float translate_box4 = 0.0f;
    private float translate_box5 = 0.0f;
    private float translate_box6 = 0.0f;
    
    private float rotate_ellipsoid = 0.0f;
    private float rotate_ellipsoid1 = 0.0f;
    private float rotate_ellipsoid2 = 0.0f;
    private float rotate_ellipsoid3 = 0.0f;
    private float rotate_ellipsoid4 = 0.0f;
    private float rotate_ellipsoid5 = 0.0f;
    private float rotate_ellipsoid6 = 0.0f;
    
    
    /* Here you should give a conservative estimate of the scene's bounding box
	 * so that the initViewParameters function can calculate proper
	 * transformation parameters to display the initial scene.
	 * If these are not set correctly, the objects may disappear on start.
     */
    private float xmin = -1f, ymin = -1f, zmin = -1f;
    private float xmax = 1f, ymax = 1f, zmax = 1f;

    public void display(GLAutoDrawable drawable) {
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl.glEnable(gl.GL_COLOR_MATERIAL);
        gl.glEnable(gl.GL_LIGHTING);
        
        float rgba[]  = {0.2f,0.4f,0.5f,1.0f};
        float rgba2[] = {0.1f,0.3f,0.6f,1.0f};
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, wireframe ? GL2.GL_LINE : GL2.GL_FILL);
        gl.glShadeModel(flatshade ? GL2.GL_FLAT : GL2.GL_SMOOTH);
        if (cullface) {
            gl.glEnable(GL2.GL_CULL_FACE);
        } else {
            gl.glDisable(GL2.GL_CULL_FACE);
        }

        gl.glLoadIdentity();

        /* this is the transformation of the entire scene */
        gl.glTranslatef(-xpos, -ypos, -zpos);
        gl.glTranslatef(centerx, centery, centerz);
        gl.glRotatef(360.f - roth, 0, 1.0f, 0);
        gl.glRotatef(rotv, 1.0f, 0, 0);
        gl.glTranslatef(-centerx, -centery, -centerz);

        /* === YOUR WORK HERE === */
        /* Below is an example of a rotating bunny
		 * It rotates the bunny with example_rotateT degrees around the bunny's gravity center  
         */
        gl.glPushMatrix();	// push the current matrix to stack
	        gl.glRotatef(-example_rotateT, 0, 1, 0);
	        gl.glTranslated(0.5f, 0.0f, 0.0f);
	        gl.glRotatef(90, 0, 1, 0);
        	gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_AMBIENT, rgba, 0);  // My attempt at shading :(
        	gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_SPECULAR, rgba2, 0);
        	// First Box
        	gl.glPushMatrix();
	        	gl.glColor3f(1.0f, 0.0f, 0.0f);
		        gl.glTranslatef(-0.6f, translate_box1, 0.0f);
		        glut.glutSolidCube(0.1f);
	        gl.glPopMatrix();
	        
	        // First Wings
	        gl.glPushMatrix();
	        	gl.glColor3f(0.0f, 1.0f, 0.0f);
	        	gl.glRotatef(rotate_ellipsoid, 1, 0, 0);
        		gl.glScalef(0.5f, 0.25f, 8.0f);
        		gl.glTranslatef(-1.25f, 0.0f, 0.04f);
        		glut.glutSolidSphere(0.04f, 25, 25);
        	gl.glPopMatrix();
        	
        	// First Wings
        	gl.glPushMatrix();
        		gl.glColor3f(0.0f, 1.0f, 0.0f);
	        	gl.glRotatef(-rotate_ellipsoid, 1, 0, 0);
	    		gl.glScalef(0.5f, 0.25f, 8.0f);
	    		gl.glTranslatef(-1.25f, 0.0f, -0.04f);
	    		glut.glutSolidSphere(0.04f, 25, 25);
        	gl.glPopMatrix();
        	
	        // Second Wings
	        gl.glPushMatrix();
	        	gl.glColor3f(0.0f, 1.0f, 0.0f);
	        	gl.glRotatef(rotate_ellipsoid1, 1, 0, 0);
	    		gl.glScalef(0.5f, 0.25f, 8.0f);
	    		gl.glTranslatef(-1.15f, 0.0f, 0.04f);
	    		glut.glutSolidSphere(0.04f, 25, 25);
    		gl.glPopMatrix();
    	
    		// Second Wings
    		gl.glPushMatrix();
    			gl.glColor3f(0.0f, 1.0f, 0.0f);
	        	gl.glRotatef(-rotate_ellipsoid1, 1, 0, 0);
	    		gl.glScalef(0.5f, 0.25f, 8.0f);
	    		gl.glTranslatef(-1.15f, 0.0f, -0.04f);
	    		glut.glutSolidSphere(0.04f, 25, 25);
    		gl.glPopMatrix();
	        
    		// Second Box
	        gl.glPushMatrix();
	        	gl.glColor3f(1.0f, 0.0f, 0.0f);
	        	gl.glTranslatef(-0.48f, translate_box2, 0.0f);
	        	glut.glutSolidCube(0.1f);
	        gl.glPopMatrix();
	        
	        // Third Wings
        	gl.glPushMatrix();
        		gl.glColor3f(0.0f, 1.0f, 0.0f);
	        	gl.glRotatef(rotate_ellipsoid2, 1, 0, 0);
	    		gl.glScalef(0.5f, 0.25f, 8.0f);
	    		gl.glTranslatef(-1.05f, trans_ellipsoid, 0.04f);
	    		glut.glutSolidSphere(0.04f, 25, 25);
        	gl.glPopMatrix();
        	
        	// Third Wings
        	gl.glPushMatrix();
        		gl.glColor3f(0.0f, 1.0f, 0.0f);
	        	gl.glRotatef(-rotate_ellipsoid2, 1, 0, 0);
	    		gl.glScalef(0.5f, 0.25f, 8.0f);
	    		gl.glTranslatef(-1.05f, trans_ellipsoid, -0.04f);
	    		glut.glutSolidSphere(0.04f, 25, 25);
        	gl.glPopMatrix();
        	
        	// Fourth Wings
        	gl.glPushMatrix();
        		gl.glColor3f(0.0f, 1.0f, 0.0f);
	        	gl.glRotatef(rotate_ellipsoid3, 1, 0, 0);
	    		gl.glScalef(0.5f, 0.25f, 8.0f);
	    		gl.glTranslatef(-0.95f, 0.0f, 0.04f);
	    		glut.glutSolidSphere(0.04f, 25, 25);
        	gl.glPopMatrix();
        	
        	// Fourth Wings
        	gl.glPushMatrix();
        		gl.glColor3f(0.0f, 1.0f, 0.0f);
	        	gl.glRotatef(-rotate_ellipsoid3, 1, 0, 0);
	    		gl.glScalef(0.5f, 0.25f, 8.0f);
	    		gl.glTranslatef(-0.95f, 0.0f, -0.04f);
	    		glut.glutSolidSphere(0.04f, 25, 25);
        	gl.glPopMatrix();
	        
        	// Third Box
	        gl.glPushMatrix();
	        	gl.glColor3f(1.0f, 0.0f, 0.0f);
	        	gl.glTranslatef(-0.36f, translate_box3, 0.0f);
	        	glut.glutSolidCube(0.1f);
        	gl.glPopMatrix();
        	
        	// Fifth Wings
        	gl.glPushMatrix();
        		gl.glColor3f(0.0f, 1.0f, 0.0f);
	        	gl.glRotatef(rotate_ellipsoid4, 1, 0, 0);
	    		gl.glScalef(0.5f, 0.25f, 8.0f);
	    		gl.glTranslatef(-0.80f, 0.0f, 0.04f);
	    		glut.glutSolidSphere(0.04f, 25, 25);
        	gl.glPopMatrix();
        	
        	// Fifth Wings
        	gl.glPushMatrix();
        		gl.glColor3f(0.0f, 1.0f, 0.0f);
	        	gl.glRotatef(-rotate_ellipsoid4, 1, 0, 0);
	    		gl.glScalef(0.5f, 0.25f, 8.0f);
	    		gl.glTranslatef(-0.80f, 0.0f, -0.04f);
	    		glut.glutSolidSphere(0.04f, 25, 25);
        	gl.glPopMatrix();
        	
        	// Sixth Wings
        	gl.glPushMatrix();
        		gl.glColor3f(0.0f, 1.0f, 0.0f);
	        	gl.glRotatef(rotate_ellipsoid5, 1, 0, 0);
	    		gl.glScalef(0.5f, 0.25f, 8.0f);
	    		gl.glTranslatef(-0.65f, 0.0f, 0.04f);
	    		glut.glutSolidSphere(0.04f, 25, 25);
        	gl.glPopMatrix();
        	
        	// Sixth Wings
        	gl.glPushMatrix();
        		gl.glColor3f(0.0f, 1.0f, 0.0f);
	        	gl.glRotatef(-rotate_ellipsoid5, 1, 0, 0);
	    		gl.glScalef(0.5f, 0.25f, 8.0f);
	    		gl.glTranslatef(-0.65f, 0.0f, -0.04f);
	    		glut.glutSolidSphere(0.04f, 25, 25);
        	gl.glPopMatrix();
        	
        	// Fourth Box
        	gl.glPushMatrix();
        		gl.glColor3f(1.0f, 0.0f, 0.0f);
        		gl.glTranslatef(-0.24f, translate_box4, 0.0f);
        		glut.glutSolidCube(0.1f);
        	gl.glPopMatrix();
	        
        	// Fifth Box
	        gl.glPushMatrix();
	        	gl.glColor3f(1.0f, 0.0f, 0.0f);
		        gl.glTranslatef(-0.12f, translate_box5, 0.0f);
	        	glut.glutSolidCube(0.1f);
	        gl.glPopMatrix();
	        
	        // Sixth box
	        gl.glPushMatrix();
	        	gl.glColor3f(1.0f, 0.0f, 0.0f);
		        gl.glTranslatef(0.0f, translate_box6, 0.0f);
	        	glut.glutSolidCube(0.1f);
        	gl.glPopMatrix();
        	
        	// First Sphere
        	gl.glPushMatrix();
        		gl.glColor3f(0.0f, 0.0f, 1.0f);
        		gl.glTranslatef(0.12f, translate_box6, 0.0f);
        		glut.glutSolidSphere(0.05f, 25, 25);
        	gl.glPopMatrix();
        	
        	// Second Sphere
        	gl.glPushMatrix();
        		gl.glColor3f(0.0f, 0.0f, 1.0f);
        		gl.glTranslatef(0.22f, translate_box5, 0.0f);
        		glut.glutSolidSphere(0.04f, 25, 25);
        	gl.glPopMatrix();
        	
        	// Third Sphere
        	gl.glPushMatrix();
        		gl.glColor3f(0.0f, 0.0f, 1.0f);
        		gl.glTranslatef(0.3f, translate_box4, 0.0f);
        		glut.glutSolidSphere(0.03f, 25, 25);
        	gl.glPopMatrix();
        	
        	// Fourth Sphere
        	gl.glPushMatrix();
        		gl.glColor3f(0.0f, 0.0f, 1.0f);
    			gl.glTranslatef(0.36f, translate_box3, 0.0f);
    			glut.glutSolidSphere(0.02f, 25, 25);
    		gl.glPopMatrix();
        	
        gl.glPopMatrix();
        
        /* call objModel::Draw function to draw the model */
        //example_model.Draw();
//        example_model2.Draw();

        /* increment example_rotateT */
        if (animator.isAnimating()) {
        	theta1 += 1.07 * animation_speed;
        	theta2 += 1.06 * animation_speed;
        	theta3 += 1.05 * animation_speed;
        	theta4 += 1.04 * animation_speed;
        	theta5 += 1.03 * animation_speed;
        	theta6 += 1.02 * animation_speed;
        	theta7 += 1.01 * animation_speed;
        	rotate_ellipsoid  = (float) (60 * Math.sin(Math.toRadians(theta1)));
        	rotate_ellipsoid1 = (float) (65 * Math.sin(Math.toRadians(theta2)));
        	rotate_ellipsoid2 = (float) (70 * Math.sin(Math.toRadians(theta3)));
        	rotate_ellipsoid3 = (float) (75 * Math.sin(Math.toRadians(theta4)));
        	rotate_ellipsoid4 = (float) (80 * Math.sin(Math.toRadians(theta5)));
        	rotate_ellipsoid5 = (float) (85 * Math.sin(Math.toRadians(theta6)));
        	rotate_ellipsoid6 = (float) (90 * Math.sin(Math.toRadians(theta7)));
        	
        	trans_ellipsoid = (float)(-0.11 * Math.sin(Math.toRadians(theta1)));
        	
        	translate_box1 = (float)(0.01 * Math.sin(Math.toRadians(theta1)));
        	translate_box2 = (float)(0.02 * Math.sin(Math.toRadians(theta1)));
        	translate_box3 = (float)(0.03 * Math.sin(Math.toRadians(theta1)));
        	translate_box4 = (float)(0.04 * Math.sin(Math.toRadians(theta1)));
        	translate_box5 = (float)(0.05 * Math.sin(Math.toRadians(theta1)));
        	translate_box6 = (float)(0.06 * Math.sin(Math.toRadians(theta1)));
        	
            example_rotateT += 1.0f * animation_speed;
        }
    }

    public Hierarchical() {
        super("Assignment 2 -- Hierarchical Modeling -- first");
        final GLProfile glprofile = GLProfile.getMaxFixedFunc(true);
        GLCapabilities glcapabilities = new GLCapabilities(glprofile);
        canvas = new GLCanvas(glcapabilities);        
        canvas.setSurfaceScale(new float[] { ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE }); // potential fix for Retina Displays
        canvas.addGLEventListener(this);
        canvas.addKeyListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        animator = new FPSAnimator(canvas, 30);	// create a 30 fps animator
        getContentPane().add(canvas);
        setSize(winW, winH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
        animator.start();
        canvas.requestFocus();
    }

    public static void main(String[] args) {

        new Hierarchical();
    }

    public void init(GLAutoDrawable drawable) {
        gl = drawable.getGL().getGL2();

        initViewParameters();
        gl.glClearColor(.1f, .1f, .1f, 1f);
        gl.glClearDepth(1.0f);

        // white light at the eye
        float light0_position[] = {0, 0, 1, 0};
        float light0_diffuse[] = {1, 1, 1, 1};
        float light0_specular[] = {1, 1, 1, 1};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, light0_position, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, light0_diffuse, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, light0_specular, 0);

        //red light
        float light1_position[] = {-.1f, .1f, 0, 0};
        float light1_diffuse[] = {.6f, .05f, .05f, 1};
        float light1_specular[] = {.6f, .05f, .05f, 1};
        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, light1_position, 0);
        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, light1_diffuse, 0);
        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPECULAR, light1_specular, 0);

        //blue light
        float light2_position[] = {.1f, .1f, 0, 0};
        float light2_diffuse[] = {.05f, .05f, .6f, 1};
        float light2_specular[] = {.05f, .05f, .6f, 1};
        gl.glLightfv(GL2.GL_LIGHT2, GL2.GL_POSITION, light2_position, 0);
        gl.glLightfv(GL2.GL_LIGHT2, GL2.GL_DIFFUSE, light2_diffuse, 0);
        gl.glLightfv(GL2.GL_LIGHT2, GL2.GL_SPECULAR, light2_specular, 0);

        float lmodel_ambient[] = {1.0f, 1.0f, 1.0f, 1.0f};
        gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, lmodel_ambient, 0);
        gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, 1);

        gl.glEnable(GL2.GL_NORMALIZE);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glEnable(GL2.GL_LIGHT1);
        gl.glEnable(GL2.GL_LIGHT2);

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LESS);
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
        gl.glCullFace(GL2.GL_BACK);
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glShadeModel(GL2.GL_SMOOTH);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        winW = width;
        winH = height;

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.f, (float) width / (float) height, znear, zfar);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    public void mousePressed(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        mouseButton = e.getButton();
        canvas.display();
    }

    public void mouseReleased(MouseEvent e) {
        mouseButton = MouseEvent.NOBUTTON;
        canvas.display();
    }

    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if (mouseButton == MouseEvent.BUTTON3) {
            zpos -= (y - mouseY) * motionSpeed;
            mouseX = x;
            mouseY = y;
            canvas.display();
        } else if (mouseButton == MouseEvent.BUTTON2) {
            xpos -= (x - mouseX) * motionSpeed;
            ypos += (y - mouseY) * motionSpeed;
            mouseX = x;
            mouseY = y;
            canvas.display();
        } else if (mouseButton == MouseEvent.BUTTON1) {
            roth -= (x - mouseX) * rotateSpeed;
            rotv += (y - mouseY) * rotateSpeed;
            mouseX = x;
            mouseY = y;
            canvas.display();
        }
    }

    /* computes optimal transformation parameters for OpenGL rendering.
	 * this is based on an estimate of the scene's bounding box
     */
    void initViewParameters() {
        roth = rotv = 0;

        float ball_r = (float) Math.sqrt((xmax - xmin) * (xmax - xmin)
                + (ymax - ymin) * (ymax - ymin)
                + (zmax - zmin) * (zmax - zmin)) * 0.707f;

        centerx = (xmax + xmin) / 2.f;
        centery = (ymax + ymin) / 2.f;
        centerz = (zmax + zmin) / 2.f;
        xpos = centerx;
        ypos = centery;
        zpos = ball_r / (float) Math.sin(45.f * Math.PI / 180.f) + centerz;

        znear = 0.01f;
        zfar = 1000.f;

        motionSpeed = 0.002f * ball_r;
        rotateSpeed = 0.1f;

    }

    // these event functions are not used for this assignment
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