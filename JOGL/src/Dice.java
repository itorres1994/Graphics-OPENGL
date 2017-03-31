
import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL3.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL3.GL_VERTEX_SHADER;
import static com.jogamp.common.nio.Buffers.newDirectFloatBuffer;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.nativewindow.ScalableSurface;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.Buffer;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.event.*;
import java.util.Random;
import javax.swing.JFrame;

/**
 * This defines the main class that implements the starter code for assignment 3
 * a few bits of the code originate from the JOGL tutorial here:
 * https://github.com/java-opengl-labs/helloTriangle/tree/master/HelloTriangle/src/gl3/helloTexture
 * (MIT open-source license: https://github.com/java-opengl-labs/helloTriangle/blob/master/LICENSE)
 */
public class Dice extends JFrame implements GLEventListener, KeyListener {

    public static FPSAnimator animator; // the animator attempts to call display() every 1/(target FPS)
    public static GLCanvas canvas; //  window component providing OpenGL rendering support
    private final String SHADERS_ROOT = "shaders/"; // please make sure you have the shaders folder in your source code directory
    private final String TEXTURE_ROOT = "assets/";  // please make sure you have the assets folder in your project root directory
    private final String TEXTURE_FILENAME = "texture.png"; // please do not change the filename of the texture in the assets/ directory

    // The vertices of our main object (cube representing the die). 
    // Three consecutive floats define a 3D vertex position; Three consecutive vertices give a triangle.
    // A cube has 6 faces with 2 triangles each, so this makes 6*2=12 triangles, and 12*3 vertices    
    // Mesh data could be read alternatively from a file - in this assignment, we simply specify them here
    private final float[] vertex_position_data = new float[]{
        -1.0f, -1.0f, -1.0f, // first triangle : first vertex 
        -1.0f, -1.0f, 1.0f, // first triangle : second vertex
        -1.0f, 1.0f, 1.0f, // first triangle : third vertex
        1.0f, 1.0f, -1.0f, // second triangle : first vertex 
        -1.0f, -1.0f, -1.0f, // second triangle : second vertex 
        -1.0f, 1.0f, -1.0f, // second triangle : third vertex         
        1.0f, -1.0f, 1.0f, // ...
        -1.0f, -1.0f, -1.0f,
        1.0f, -1.0f, -1.0f,
        1.0f, 1.0f, -1.0f,
        1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f, 1.0f, 1.0f,
        -1.0f, 1.0f, -1.0f,
        1.0f, -1.0f, 1.0f,
        -1.0f, -1.0f, 1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f, 1.0f, 1.0f,
        -1.0f, -1.0f, 1.0f,
        1.0f, -1.0f, 1.0f,
        1.0f, 1.0f, 1.0f,
        1.0f, -1.0f, -1.0f,
        1.0f, 1.0f, -1.0f,
        1.0f, -1.0f, -1.0f,
        1.0f, 1.0f, 1.0f,
        1.0f, -1.0f, 1.0f,
        1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, -1.0f,
        -1.0f, 1.0f, -1.0f,
        1.0f, 1.0f, 1.0f,
        -1.0f, 1.0f, -1.0f,
        -1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f,
        -1.0f, 1.0f, 1.0f,
        1.0f, -1.0f, 1.0f
    };

    // UV texture coordinates for each vertex. They were created with Blender
    // see https://www.youtube.com/watch?v=6gRUUeFteQg for a tutorial if you are interested
    private final float[] uv_data = new float[]{
        0.000059f, 1.0f - 0.000004f, // uv coordinates for first vertex
        0.000103f, 1.0f - 0.336048f, // uv coordinates for second vertex
        0.335973f, 1.0f - 0.335903f, // ...
        1.000023f, 1.0f - 0.000013f,
        0.667979f, 1.0f - 0.335851f,
        0.999958f, 1.0f - 0.336064f,
        0.667979f, 1.0f - 0.335851f,
        0.336024f, 1.0f - 0.671877f,
        0.667969f, 1.0f - 0.671889f,
        1.000023f, 1.0f - 0.000013f,
        0.668104f, 1.0f - 0.000013f,
        0.667979f, 1.0f - 0.335851f,
        0.000059f, 1.0f - 0.000004f,
        0.335973f, 1.0f - 0.335903f,
        0.336098f, 1.0f - 0.000071f,
        0.667979f, 1.0f - 0.335851f,
        0.335973f, 1.0f - 0.335903f,
        0.336024f, 1.0f - 0.671877f,
        1.000004f, 1.0f - 0.671847f,
        0.999958f, 1.0f - 0.336064f,
        0.667979f, 1.0f - 0.335851f,
        0.668104f, 1.0f - 0.000013f,
        0.335973f, 1.0f - 0.335903f,
        0.667979f, 1.0f - 0.335851f,
        0.335973f, 1.0f - 0.335903f,
        0.668104f, 1.0f - 0.000013f,
        0.336098f, 1.0f - 0.000071f,
        0.000103f, 1.0f - 0.336048f,
        0.000004f, 1.0f - 0.671870f,
        0.336024f, 1.0f - 0.671877f,
        0.000103f, 1.0f - 0.336048f,
        0.336024f, 1.0f - 0.671877f,
        0.335973f, 1.0f - 0.335903f,
        0.667969f, 1.0f - 0.671889f,
        1.000004f, 1.0f - 0.671847f,
        0.667979f, 1.0f - 0.335851f
    };

    // defines our camera (or eye) cordinate system (camera position & orientation)
    private final float[] camera_position = new float[]{0.0f, 2.0f, 8.0f};
    private final float[] camera_lookat = new float[]{0.0f, 0.0f, 0.0f};
    private final float[] camera_updirection = new float[]{0.0f, 1.0f, 0.0f};
    // defines our light position in world coordinate system
    private final float light0_pos[] = {0.0f, 0.0f, 8.0f, 1.0f}; // this needs to be converted into eye cooordinates
    private float light0_pos_eye[] = new float[4]; // will hold light position in eye coordinates

    // model transformation matrices to place the first die in the world coordinate system
    // this time we will populate these matrices ourselves! (no glTranslate, glRotate etc)
    private float[] translation_matrix1 = new float[16]; // translation matrix for first die
    private float[] rotation_matrix1 = new float[16];    // rotation matrix for first die
    private float[] axis_of_rotation1 = {1.0f, 0.0f, 0.0f};  // holds axis of rotation
    private float[] model_matrix1 = new float[16];  // model transformation for first die (combining translation & rotation)
    
    // transformation model matrices to place second die in the world coordinate system (you can use those!)
    private float[] translation_matrix2 = new float[16]; // translation matrix for second die
    private float[] rotation_matrix2 = new float[16];    // rotation matrix for second die
    private float[] axis_of_rotation2 = {0.0f, 1.0f, 0.0f}; // holds axis of rotation    
    private float[] model_matrix2 = new float[16]; // model transformation for second die (combining translation & rotation)

    // coordinate transformation matrices to convert our object coordinates to clip coordinates (see OpenGL pipeline)
    // this time we will populate these matrices ourselves! (no glOrtho, gluPerspective etc)
    // Each object should be transformed according to (projection_matrix * view_matrix * model_matrix)
    private float[] projection_matrix = new float[16];
    private float[] view_matrix = new float[16];
    private float[] model_view_matrix = new float[16];
    private float[] model_view_projection_matrix = new float[16];    
    private float[] inverse_model_view_matrix = new float[16]; 
    private float[] transpose_inverse_model_view_matrix = new float[16]; // remember we use this matrix for transforming normals!
    // temp matrices/vectors needed during transformation calculations
    private float[] tmp_matrix = new float[16];
    private float[] tmp_vec = new float[3];

    // variables used to create the animation of the die
    private Random rand = new Random();
    private int roll_state = 0; // state variable: 0 means that the die is still, 1 means that die is rolling, 2 means that die decelerates
    private int rotation_angle = 0; // tracks the total rotation angle wrt the axis of rotation for both dice (you will draw two dice)
    private int angle_change = 0; // stores the rate of change in the rotation angle (angular velocity) for both dice
    private long start, now; // store timestamps in milliseconds
    float timer = 0.0f; // holds a timer measuring time while dice are still

    // these variables serve as references to uniform variables or matrices defined in the shaders
    // e.g., timer_ref_shader is a reference to the variable 'timer' in the shader
    // this reference will be used to associate the 'timer' variable of our Java program
    // to the 'timer' variable in the shader. In this manner, we pass values from our Java program to the shaders
    // these references are set by 'glGetUniformLocation' (https://www.khronos.org/opengl/wiki/GLAPI/glGetUniformLocation)
    private int transpose_inverse_model_view_matrix_ref_shader, model_view_matrix_ref_shader;
    private int model_view_projection_matrix_ref_shader;
    private int light0_pos_ref_shader;
    private int timer_ref_shader;
    // these constants serve as explicit references to vertex attributes used as input in our vertex shader
    // these references are used  by 'glBindAttribLocation' (https://www.khronos.org/opengl/wiki/GLAPI/glBindAttribLocation)
    public static final int VERTEX_POSITION_REF_SHADER = 0;
    public static final int VERTEX_NORMAL_REF_SHADER = 1;
    public static final int VERTEX_TEXCOORD_REF_SHADER = 2;
   
    // to send data to the vertex shader, we need to allocate memory on the GPU where we store the vertex data
    // these vertex data are stored in 'buffers' - we manage this memory via so-called 'vertex buffer objects' (VBO) 
    // the following array stores references (think of them as unique IDs or pointers) to our buffer data
    // the first entry in our array stores a reference to the vertex position data of our die    
    // the second entry in our array stores a reference to the vertex normal data  of our die
    // the third entry in our array stores a reference to the vertex texture coordinate data  of our die
    private int[] vertex_buffer_object_refs = new int[3];
    public static final int VERTEX_POSITION_BUFFER_INDEX = 0;
    public static final int VERTEX_NORMAL_BUFFER_INDEX = 1;
    public static final int VERTEX_TEXCOORD_BUFFER_INDEX = 2;
    
    // the above buffers belong to a single geometric object (the die and its replicate second die)
    // every time we want to draw a die, we do not want to setup how all its different buffer data
    // are mapped to its positions, normals, texture coordinates over and over again (we want to do it once)
    // The following variable, called 'vertex array object' stores a reference to an object 
    // (or better, a state) that describes how the above buffers are mapped to positions, normals, 
    // texture coordinates of a geometric object
    private int[] vertex_array_object_refs = new int[1];
    
    // this variable stores a reference to our texture image that will be loaded in the GPU memory
    private int[] texture_object_refs = new int[1];

    // this interface contains all core, forward compatible, OpenGL methods starting from 3.1
    private GL3 gl3; 
    
    // this variable holds a reference (i.e., id or pointer) to our shader program
    private int shader_program_ref; 
    
    /**
     * Main function of our java shader_program_ref.
     */
    public static void main(String[] args) {
        new Dice();
    }

    /**
     * Constructor: creates the window, OpenGL rendering context, and starts the
     * animation.
     * You do not need to edit this function (but try to understand it).
     */
    public Dice() {
        super("Assignment 3 - Dice");
        GLProfile glProfile = GLProfile.get(GLProfile.GL3); // no fixed-functionality anymore, use OpenGL 3.1+ 
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        canvas = new GLCanvas(glCapabilities);
        canvas.setSurfaceScale(new float[]{ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE}); // potential fix for Retina Displays
        canvas.addGLEventListener(this);
        canvas.addKeyListener(this);

        getContentPane().add(canvas);
        setSize(1024, 768);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
        canvas.requestFocus();

        animator = new FPSAnimator(canvas, 30); // 30 FPS is enough for this application
        animator.start();
    }

    /**
     * Initializes our OpenGL program, specifically the rendering context, the buffers storing 
     * the vertex data (positions, normals, texture coordinates), the shaders, 
     * loads the texture, initializes the timer variable and rotation matrix of the dice.
     * You do not need to edit this function (but try to understand it).
     */    
    public void init(GLAutoDrawable drawable) {
        gl3 = drawable.getGL().getGL3(); // we will use GL 3.1+
        gl3.glEnable(GL3.GL_DEPTH_TEST); // the depth test (Z-test) remains (same with the fixed-functionality pipeline)
        gl3.glDepthFunc(GL3.GL_LESS);    // see visibility class
        initBuffers(gl3); // initialize buffer data, see below
        initTexture(gl3); // initialize texture data, see below
        initShader(gl3);  // initialize the shaders, see below
        
        // FloatUtil offers linear algebra ulility functions 
        // https://jogamp.org/deployment/v2.1.0/javadoc/jogl/javadoc/com/jogamp/opengl/math/FloatUtil.html
        FloatUtil.makeIdentity(rotation_matrix1); // our rotation for both dice is the idenity matrix (no rotation) initially
        FloatUtil.makeIdentity(rotation_matrix2);
        start = System.currentTimeMillis(); // we will keep a timer that we initialize here
    }

    /**
    * Initializes our buffers storing vertex attributes. To send data to the vertex shader, 
    * we need to allocate memory on the GPU where we store the vertex data.
    * These vertex data are stored in 'buffers' - we manage this memory via so-called 'vertex buffer objects' (VBO) 
    * In this function we initialize all the buffers (VBOs) for our die object, and associate them with a state 
    * describing how these buffers are used to draw our object. This state is called 'vertex array object' (VAO)
    * You do not need to edit this function (but try to understand it).
    */ 
    private void initBuffers(GL3 gl3) {
        // see glGenVertexArrays (https://www.khronos.org/opengl/wiki/GLAPI/glGenVertexArrays)
        // the description might be confusing in the beginning, read also: https://learnopengl.com/#!Getting-started/Hello-Triangle
        // the vertex array object stores a state describing how the following buffers are 
        // mapped to vertices, normals, texture coordinates etc that are used to draw an object 
        // glGenVertexArrays generates a new state corresponding to a new geometric object (and a reference to this state)
        // Since we need one state (one die object + its replicate), we ask OpenGL to generate
        // one reference for us. The third argument is specific to JOGL: because Java does not have pointers 
        // as in C++, we need to explain to OpenGL that vertex_array_object_refs[0] will store
        // the reference to the state (and not vertex_array_object_refs[1] or vertex_array_object_refs[2] etc)
        // see also http://download.java.net/media/jogl/jogl-2.x-docs/javax/media/opengl/GL3.html
        gl3.glGenVertexArrays(1, vertex_array_object_refs, 0); 
        // now we say that everything that will do from now on (all following buffers), will be associated with the new object state we just generated
        gl3.glBindVertexArray(vertex_array_object_refs[0]); 
        {
            // we now generate a new buffer that will store vertex position data
            // OpenGL will return a reference to this buffer
            // that will be stored in vertex_buffer_object_refs[VERTEX_POSITION_BUFFER_INDEX]
            // see https://www.khronos.org/opengl/wiki/GLAPI/glGenBuffers
            // the last arguement is JOGL-specific and simply means that the reference
            // should be stored in vertex_buffer_object_refs[VERTEX_POSITION_BUFFER_INDEX]
            gl3.glGenBuffers(1, vertex_buffer_object_refs, VERTEX_POSITION_BUFFER_INDEX);
            gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertex_buffer_object_refs[VERTEX_POSITION_BUFFER_INDEX]);
            {
                FloatBuffer vertex_position_buffer = GLBuffers.newDirectFloatBuffer(vertex_position_data);
                int size = vertex_position_data.length * (Float.SIZE / Byte.SIZE);
                gl3.glBufferData(GL3.GL_ARRAY_BUFFER, size, vertex_position_buffer, GL3.GL_STATIC_DRAW);                
                // We now sent the vertex data to the GPU memory. We are almost there...
                // OpenGL does not yet know how it should interpret the vertex data in memory and how it should 
                // map the vertex data to the vertex shader attributes. 
                // the following 2 lines explain that this buffer is (a) referenced in the shader (variable 'vertex_pos')
                // through the reference 'VERTEX_POSITION_REF_SHADER' (see also glBindAttribLocation below)
                // and (b) stores 3D coordinates (floating-point numbers) and the byte offset between consecutive 
                // values. For more details, see: https://www.khronos.org/opengl/wiki/GLAPI/glVertexAttribPointer
                gl3.glEnableVertexAttribArray(VERTEX_POSITION_REF_SHADER);
                gl3.glVertexAttribPointer(VERTEX_POSITION_REF_SHADER, 3, GL3.GL_FLOAT, false, 0, 0);

                // the following line is Java-specific. Since vertex_position_buffer is 
                // a direct buffer, this means it is not handled by the Java Garbage Collector job 
                // and it is up to us to remove it (this sucks!)
                destroyDirectBuffer(vertex_position_buffer);
            }

            // we repeat the same procedure to generate a buffer for the vertex normal data
            // and associate it with the reference 'VERTEX_NORMAL_REF_SHADER' to pass this data to our shader
            gl3.glGenBuffers(1, vertex_buffer_object_refs, VERTEX_NORMAL_BUFFER_INDEX);
            gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertex_buffer_object_refs[VERTEX_NORMAL_BUFFER_INDEX]);
            {
                FloatBuffer normal_buffer = GLBuffers.newDirectFloatBuffer(vertex_position_data); 
                int size = vertex_position_data.length * (Float.SIZE / Byte.SIZE);
                gl3.glBufferData(GL3.GL_ARRAY_BUFFER, size, normal_buffer, GL3.GL_STATIC_DRAW);
                gl3.glEnableVertexAttribArray(VERTEX_NORMAL_REF_SHADER);
                gl3.glVertexAttribPointer(VERTEX_NORMAL_REF_SHADER, 3, GL3.GL_FLOAT, false, 0, 0);
                /**
                 * Since vertex_position_buffer is a direct buffer, this means
                 * it is outside the Garbage Collector job and it is up to us to
                 * remove it.
                 */
                destroyDirectBuffer(normal_buffer);
            }

            // we repeat the same procedure to generate a buffer for the vertex uv coordinate data
            // and associate it with the reference 'VERTEX_TEXCOORD_REF_SHADER' to pass this data to our shader            
            gl3.glGenBuffers(1, vertex_buffer_object_refs, VERTEX_TEXCOORD_BUFFER_INDEX);
            gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertex_buffer_object_refs[VERTEX_TEXCOORD_BUFFER_INDEX]);
            {
                FloatBuffer uv_buffer = GLBuffers.newDirectFloatBuffer(uv_data);
                int size = uv_data.length * (Float.SIZE / Byte.SIZE);
                gl3.glBufferData(GL3.GL_ARRAY_BUFFER, size, uv_buffer, GL3.GL_STATIC_DRAW);
                gl3.glEnableVertexAttribArray(VERTEX_TEXCOORD_REF_SHADER);
                gl3.glVertexAttribPointer(VERTEX_TEXCOORD_REF_SHADER, 2, GL3.GL_FLOAT, false, 0, 0);
                /**
                 * Since vertex_position_buffer is a direct buffer, this means
                 * it is outside the Garbage Collector job and it is up to us to
                 * remove it.
                 */
                destroyDirectBuffer(uv_buffer);
            }
        }
        checkError(gl3, "error in the generation & binding of buffers");
    }

    /**
    * Loads a texture from a file, and configures it.
    * You do not need to edit this function (but try to understand it).
    */     
    private void initTexture(GL3 gl3) {
        try {
            // if the following line throws an error, it means that you have 
            // to put the texture file in <TEXTURE_ROOT>/<TEXTURE_FILENAME> 
            // (relative to your project directory - by now you should have understood
            // what your project directory is)
            File textureFile = new File(TEXTURE_ROOT + "/" + TEXTURE_FILENAME);

            // read http://download.java.net/media/jogl/jogl-2.x-docs/com/sun/opengl/util/texture/TextureIO.html
            // TextureIO is a JOGL class that provides functionality for loading a texture from disk
            TextureData textureData = TextureIO.newTextureData(gl3.getGLProfile(), textureFile, false, TextureIO.PNG);

            // glGenTextures generates a texture 'object' that will hold texture data 
            // it returns a reference (i.e., an id) to this object that we store in texture_object_refs[0]
            // see https://www.khronos.org/opengl/wiki/GLAPI/glGenTextures
            gl3.glGenTextures(1, texture_object_refs, 0);
            // this texture object will be treated as 2D texture - whatever 
            // we do in the following lines will be associated with this texture object
            gl3.glBindTexture(GL3.GL_TEXTURE_2D, texture_object_refs[0]);
            {
                // specifies the internal format of the target texture.
                // see https://www.khronos.org/opengl/wiki/GLAPI/glTexImage2D
                // the texture is in RGB format, 1 byte per channel, size is 512x512 (don't change the size)
                // there is no border, and we do not use mipmapping here (https://en.wikipedia.org/wiki/Mipmap, 
                // lectures notes for texturing)
                gl3.glTexImage2D(GL3.GL_TEXTURE_2D, 0, textureData.getInternalFormat(),
                        textureData.getWidth(), textureData.getHeight(), textureData.getBorder(),
                        textureData.getPixelFormat(), textureData.getPixelType(), textureData.getBuffer());
                
                // we set the base and max level for mipmapping (both are 0, since we use 
                // one texture resolution in this assigment)
                gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_BASE_LEVEL, 0);
                gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAX_LEVEL, 0);
                 // we set the swizzling (https://en.wikipedia.org/wiki/Swizzling_(computer_graphics))
                 // Since it is an RGB texture, we can choose to make the missing component alpha 
                 // equal to one (does not really matter, since we do not use transparency in our shader)
                int[] swizzle = new int[]{GL3.GL_RED, GL3.GL_GREEN, GL3.GL_BLUE, GL3.GL_ONE};
                gl3.glTexParameterIiv(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_SWIZZLE_RGBA, swizzle, 0);
            }
        } catch (IOException ex) {
            Logger.getLogger(Dice.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
    * Loads the shader programs from the disk, compiles them, links them into a 'shader program'
    * that will be executed during the OpenGL pipeline.
    * At a first glance, this might seem weird to you i.e., our Java program reads
    * some external source code in another language (GLSL), launches a special compiler for it
    * and associates variables in this external source code with variables defined in our Java program
    * Yet, this is how shaders work in modern OpenGL (and any other modern graphics API)
    * You do not need to edit this function (but try to understand it).
    */         
    private void initShader(GL3 gl3) {
        // The following lines are specific to JOGL. The ShaderCode class 
        // is a convenient class used to instantiate vertex or fragment programs. 
        // It essentially calls (a) glCreateShader, (b) glShaderSource, (c) glCompileShader
        // i.e. it loads shaders from disk and compiles them.
        // make sure that the shader files are placed inside the <SHADERS_ROOT> directory
        // relative to the directory storing your source code. If any of the two lines throw
        // an error, it means that you did not place the shaders in the above directory
        ShaderCode vertex_shader = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(),
                SHADERS_ROOT, null, "vs", "glsl", null, true);
        ShaderCode fragment_shader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(),
                SHADERS_ROOT, null, "fs", "glsl", null, true);

        // creates a shader program (involving a vertex and a fragment shader) that will be 
        // executed when the vertex data pass down  the OpenGL pipeline after a draw command (glDrawArrays)
        ShaderProgram shader_program = new ShaderProgram();
        shader_program.add(vertex_shader);
        shader_program.add(fragment_shader);
        shader_program.init(gl3);
        shader_program.link(gl3, System.out); // linking refers to the creation of a single program (translated into GPU-specific binary code) from multiple shader files                
        shader_program_ref = shader_program.program(); // we will refer to the shader program through the returned reference

        // Now we tell OpenGL that the variable name 
        // 'vertex_position' in our shader code is associated with the vertex buffer data referenced by 
        // the ids 'VERTEX_POSITION_REF_SHADER', 'VERTEX_NORMAL_REF_SHADER', 'VERTEX_TEXCOORD_REF_SHADER' defined in the beginning of the class
        gl3.glBindAttribLocation(shader_program_ref, VERTEX_POSITION_REF_SHADER, "vertex_pos");
        gl3.glBindAttribLocation(shader_program_ref, VERTEX_NORMAL_REF_SHADER, "vertex_normal");
        gl3.glBindAttribLocation(shader_program_ref, VERTEX_TEXCOORD_REF_SHADER, "uv_coordinates");
        gl3.glBindFragDataLocation(shader_program_ref, 0, "glFragColor");
        
        // the variable names in the shader code will be associated with references returned by 
        // glGetUniformLocation (https://www.khronos.org/opengl/wiki/GLAPI/glGetUniformLocation)
        // we will then associate these references with variables in our Java program (see display function below)
        // in this manner, we will pass down their values to the shader
        model_view_projection_matrix_ref_shader = gl3.glGetUniformLocation(shader_program_ref, "MVP");
        model_view_matrix_ref_shader = gl3.glGetUniformLocation(shader_program_ref, "MV");
        transpose_inverse_model_view_matrix_ref_shader = gl3.glGetUniformLocation(shader_program_ref, "TINVMV");
        light0_pos_ref_shader = gl3.glGetUniformLocation(shader_program_ref, "light0_pos_eye");
        timer_ref_shader = gl3.glGetUniformLocation(shader_program_ref, "timer");

        // once the shader source code is linked into a shader program (i.e., converted to binary code)
        // we do not need the source code anymore
        vertex_shader.destroy(gl3);
        fragment_shader.destroy(gl3);

        // checks for errors
        checkError(gl3, "error in the initialization of the shaders");
    }

    /**
    * Destroys / frees GPU memory associated with the shader program and buffers.
    * Will run when the program closes.
    * You do not need to edit this function (but try to understand it).
    */     
    public void dispose(GLAutoDrawable drawable) {
        System.out.println("dispose");
        gl3.glDeleteProgram(shader_program_ref);
        gl3.glDeleteVertexArrays(1, vertex_array_object_refs, 0);
        gl3.glDeleteBuffers(1, vertex_buffer_object_refs, VERTEX_POSITION_BUFFER_INDEX);
        gl3.glDeleteBuffers(1, vertex_buffer_object_refs, VERTEX_NORMAL_BUFFER_INDEX);
        gl3.glDeleteBuffers(1, vertex_buffer_object_refs, VERTEX_TEXCOORD_BUFFER_INDEX);
        gl3.glDeleteTextures(1, texture_object_refs, 0);
        System.exit(0);
    }

    /**
    * The display function is the core function of all OpenGL programs. 
    * The FPSAnimator and other window events call the display function as needed. 
    * In this assignment, the display function does the following:
    *  (a) update animation state variables (rotation angles for dice)
    *  (b) clears the framebuffer (per-pixel colors) and depth buffer (empty window in the beginning)
    *  (c) sets up the transformation matrices for our scene (model, view, projection trasformations)
    *  (d) sends the vertex attribute and state variables down the OpenGL pipeline,  
    *      which includes our vertex & fragment shader program (this is done when glDrawArrays is called)
    *  You need to edit this function! All you need is to add a second die,  
    *  which involves changing the modelview transformation matrix (after the first die
    *  is drawn), and calling glDrawArrays again to draw the second die with the 
    *  updated transformation.
    */         
    public void display(GLAutoDrawable drawable) {
        now = System.currentTimeMillis();
        timer = (float) (now - start);
        if (roll_state == 1) {  // if the dice are rolling
            timer = 0;          // we won't use the timer (no flashing effect while dice are rolling)
            rotation_angle += angle_change; // increment only their rotation angle (about a randomly picked axis, see keyPressed function)
        } else if (roll_state == 2) {  // if the dice are in 'decelerating state'
            timer = 0;                 // we won't use the timer (no flashing effect while dice are rolling)
            if (rotation_angle % 90 == 0) {  // we will stop the dice when their orientation is canonical (rotation angle is multiple of 90 degrees)
                angle_change = 0; 
                roll_state = 0; 
            } else {                            // if the dice orientation is not canonical,
                rotation_angle += angle_change; // keep rotating the dice with low angular velocity (see keyPressed function)
            }
        }

        // We clear the framebuffer and depthbuffer before we draw our dice
        gl3.glClearColor(0f, .33f, 0.66f, 1f);
        gl3.glClearDepthf(1.0f);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        
        // we now activate our shader program.
        // every shader and rendering call (glDrawArrays) after glUseProgram will now use this shader program
        // note: the reason for activating our program in the display function is that
        // we could have many different shader programs, each doing something different
        // (in this assignment, we only have one shader program)        
        gl3.glUseProgram(shader_program_ref);
        {
            // here we say that our timer variable value in our Java program should be 
            // used in corresponding variable in our shader 
            // (see also glGetUniformLocation above)
            // see https://www.khronos.org/opengl/wiki/GLAPI/glUniform
            gl3.glUniform1f(timer_ref_shader, timer);

            // FloatUtil is a utility function that offers the ability to create a perspective projection matrix for us
            // remember: no more 'gluPerspective' in modern OpenGL
            // see https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/opengl/math/FloatUtil.html
            FloatUtil.makePerspective(projection_matrix, 0, true, 0.7853f, 4.0f / 3.0f, 0.1f, 10.0f);
            // FloatUtil  can also generate a matrix that represents the camera coordinate system
            // see the 'Change of coordinate systems' lecture in the 3D Transformations lecture
            FloatUtil.makeLookAt(view_matrix, 0, camera_position, 0, camera_lookat, 0, camera_updirection, 0, tmp_matrix);
            // multMatrixVec multiplies the above matrix (representing the change of coordinate system world=>eye coordinates)
            // with the light position expressed in world coordinates - now the light position is expressed in eye coordinates
            // see https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/opengl/math/FloatUtil.html#multMatrixVec(float[],%20float[],%20float[])
            FloatUtil.multMatrixVec(view_matrix, light0_pos, light0_pos_eye);
            // the light position is now associated with the corresponding variable in our shader program
            gl3.glUniform4fv(light0_pos_ref_shader, 1, newDirectFloatBuffer(light0_pos_eye));

            // FloatUtil can create a rotation matrix (tmp_matrix) for us given the axis of rotation and rotation angle
            // see 3D transformation lecture
            FloatUtil.makeRotationAxis(tmp_matrix, 0, ((float) angle_change / 180.0f) * FloatUtil.PI, axis_of_rotation1[0], axis_of_rotation1[1], axis_of_rotation1[2], tmp_vec);
            // we compose all the rotations so far: R = tmp_matrix * R, tmp_matrix is the new applied rotation
            FloatUtil.multMatrix(tmp_matrix, rotation_matrix1, rotation_matrix1);
            // creates a translation matrix T
            FloatUtil.makeTranslation(translation_matrix1, true, -2.0f, 0.0f, 0.0f);
            // our model transformation matrix (object=>world coordinates) is M = T * R 
            // this means that we first rotate the die about the origin, then translate
            // the order matters! see the FloatUtil documentation for multMatrix 
            FloatUtil.multMatrix(translation_matrix1, rotation_matrix1, model_matrix1);
            // the view matrix transforms world coordinates to eye coordinates
            // thus we now have MV = V * M 
            FloatUtil.multMatrix(view_matrix, model_matrix1, model_view_matrix);
            // the perspective projection matrix transforms eye coordinates to clip coordinates inside our viewing frustum
            // thus we now have MVP = P * V * M 
            FloatUtil.multMatrix(projection_matrix, model_view_matrix, model_view_projection_matrix);
            // the next lines compute (MV)^(-T) to transform normals! Rememer how we properly transform normals!
            FloatUtil.invertMatrix(model_view_matrix, inverse_model_view_matrix); // first inverse
            FloatUtil.transposeMatrix(inverse_model_view_matrix, transpose_inverse_model_view_matrix); // then transpose
            
            // pass down all these matrices to our shader program
            gl3.glUniformMatrix4fv(model_view_projection_matrix_ref_shader, 1, false, model_view_projection_matrix, 0);
            gl3.glUniformMatrix4fv(model_view_matrix_ref_shader, 1, false, model_view_matrix, 0);
            gl3.glUniformMatrix4fv(transpose_inverse_model_view_matrix_ref_shader, 1, false, transpose_inverse_model_view_matrix, 0);

            // the next line means that all our vertex buffer data (positions, normals, texture coordinates)
            // associated with the die object will be passed down the OpenGL pipeline (including our shaders)
            // (see how set up our vertex array in 'initBuffers')
            // glBindVertexArray is very handy here - we do not need to call
            // glBindBuffer, glBufferData, glVertexAttribPointer inside our display function
            // to bind vertex, texture, normal data with our object over and over again
            // the object referenced by vertex_array_object_refs[0] stores all the necessary
            // references to buffers and attribute pointers
            gl3.glBindVertexArray(vertex_array_object_refs[0]);
            {
                // the next line means that our texture object referenced in texture_object_refs[0]
                // will be used i.e., use the texture loaded in 'initTexture'
                gl3.glBindTexture(GL3.GL_TEXTURE_2D, texture_object_refs[0]);
                {
                    // all our 'arrays' (vertex buffer, texture data, shader variable values) are ready-to-use
                    // pass them down then OpenGL pipeline
                    // vertex data will be treated as triangles (3 consecutive vertices define a triangle, total 12 triangles)
                    // see https://www.khronos.org/opengl/wiki/GLAPI/glDrawArrays
                    // there are alternative 'drawing' calls e.g., https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glDrawElements.xhtml
                    // if we used face indices to vertices (as in OBJs)
                    gl3.glDrawArrays(GL3.GL_TRIANGLES, 0, 12 * 3); // 12*3 indices starting at 0 -> 12 triangles
                }
            }            

            /* === YOUR WORK HERE === */
            // Can you draw the second die as required by the assignment? 
            // Hint: you need to change the model transformation matrix
            // and the other matrices that depend on it
            
            // Use the code from above and apply the same operations using the appropriate matrices and their corresponding operations
            
            // FloatUtil can create a rotation matrix (tmp_matrix) for us given the axis of rotation and rotation angle
            // see 3D transformation lecture
            FloatUtil.makeRotationAxis(tmp_matrix, 0, ((float) angle_change / 180.0f) * FloatUtil.PI, axis_of_rotation2[0], axis_of_rotation2[1], axis_of_rotation2[2], tmp_vec);
            
            // we compose all the rotations so far: R = tmp_matrix * R, tmp_matrix is the new applied rotation
            FloatUtil.multMatrix(tmp_matrix, rotation_matrix2, rotation_matrix2);
            // creates a translation matrix T
            FloatUtil.makeTranslation(translation_matrix2, true, 2.0f, 0.0f, 0.0f);
            // our model transformation matrix (object=>world coordinates) is M = T * R 
            // this means that we first rotate the die about the origin, then translate
            // the order matters! see the FloatUtil documentation for multMatrix 
            FloatUtil.multMatrix(translation_matrix2, rotation_matrix2, model_matrix2);
            // the view matrix transforms world coordinates to eye coordinates
            // thus we now have MV = V * M 
            FloatUtil.multMatrix(view_matrix, model_matrix2, model_view_matrix);
            // the perspective projection matrix transforms eye coordinates to clip coordinates inside our viewing frustum
            // thus we now have MVP = P * V * M 
            FloatUtil.multMatrix(projection_matrix, model_view_matrix, model_view_projection_matrix);
            // the next lines compute (MV)^(-T) to transform normals! Rememer how we properly transform normals!
            FloatUtil.invertMatrix(model_view_matrix, inverse_model_view_matrix); // first inverse
            FloatUtil.transposeMatrix(inverse_model_view_matrix, transpose_inverse_model_view_matrix); // then transpose
            
            // pass down all these matrices to our shader program
            gl3.glUniformMatrix4fv(model_view_projection_matrix_ref_shader, 1, false, model_view_projection_matrix, 0);
            gl3.glUniformMatrix4fv(model_view_matrix_ref_shader, 1, false, model_view_matrix, 0);
            gl3.glUniformMatrix4fv(transpose_inverse_model_view_matrix_ref_shader, 1, false, transpose_inverse_model_view_matrix, 0);
            
            // the next line means that all our vertex buffer data (positions, normals, texture coordinates)
            // associated with the die object will be passed down the OpenGL pipeline (including our shaders)
            // (see how set up our vertex array in 'initBuffers')
            // glBindVertexArray is very handy here - we do not need to call
            // glBindBuffer, glBufferData, glVertexAttribPointer inside our display function
            // to bind vertex, texture, normal data with our object over and over again
            // the object referenced by vertex_array_object_refs[0] stores all the necessary
            // references to buffers and attribute pointers
            gl3.glBindVertexArray(vertex_array_object_refs[0]);
            {
                // the next line means that our texture object referenced in texture_object_refs[0]
                // will be used i.e., use the texture loaded in 'initTexture'
                gl3.glBindTexture(GL3.GL_TEXTURE_2D, texture_object_refs[0]);
                {
                    // all our 'arrays' (vertex buffer, texture data, shader variable values) are ready-to-use
                    // pass them down then OpenGL pipeline
                    // vertex data will be treated as triangles (3 consecutive vertices define a triangle, total 12 triangles)
                    // see https://www.khronos.org/opengl/wiki/GLAPI/glDrawArrays
                    // there are alternative 'drawing' calls e.g., https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glDrawElements.xhtml
                    // if we used face indices to vertices (as in OBJs)
                    gl3.glDrawArrays(GL3.GL_TRIANGLES, 0, 12 * 3); // 12*3 indices starting at 0 -> 12 triangles
                }
            }           

            
        }

        checkError(gl3, "error during the display function"); // check for any error
    }

    /**
     * Returns true if an error happened during the initialization of our shaders, 
     * buffers, or during the display function.
     * It also prints a message that might help us understand the error. 
     * You do not need to edit this function (but try to understand it).
     */
    protected boolean checkError(GL3 gl, String title) {
        int error = gl.glGetError();
        if (error != GL_NO_ERROR) {
            String errorString;
            switch (error) {
                case GL_INVALID_ENUM:
                    errorString = "GL_INVALID_ENUM";
                    break;
                case GL_INVALID_VALUE:
                    errorString = "GL_INVALID_VALUE";
                    break;
                case GL_INVALID_OPERATION:
                    errorString = "GL_INVALID_OPERATION";
                    break;
                case GL_INVALID_FRAMEBUFFER_OPERATION:
                    errorString = "GL_INVALID_FRAMEBUFFER_OPERATION";
                    break;
                case GL_OUT_OF_MEMORY:
                    errorString = "GL_OUT_OF_MEMORY";
                    break;
                default:
                    errorString = "UNKNOWN";
                    break;
            }
            System.out.println("OpenGL Error(" + errorString + "): " + title);
            throw new Error();
        }
        return error == GL_NO_ERROR;
    }

    /**
     * Reshapes then OpenGL drawing area according to the size of the window.
     * see https://www.khronos.org/opengl/wiki/GLAPI/glViewport
     * and viewing transformations lecture 
     * You do not need to edit this function (but try to understand it).
     */    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        gl3.glViewport(x, y, width, height);
    }

    /**
     * Key pressed callback function.
     * If Escape or Q is pressed, our program terminates
     * If space is pressed, (a) the dice start to roll if they were previously still 
     * or (b) the dice start to decelerate if they were in a rolling state
     * You do not need to edit this function (but try to understand it).
     */  
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_Q:
                System.exit(0);
                break;
            case KeyEvent.VK_SPACE:
                if (roll_state == 0) { // were the dice still (non-rolling)? 
                    angle_change = 10; // set angular velocity to start rolling
                    roll_state = 1;   // state is 1 (dice are now rolling)
                    // pick a random axis of rotation for first die
                    int picked_random_axis = rand.nextInt(3); 
                    axis_of_rotation1[picked_random_axis] = 1.0f;
                    axis_of_rotation1[(picked_random_axis + 1) % 3] = 0.0f;
                    axis_of_rotation1[(picked_random_axis + 2) % 3] = 0.0f;
                    // pick an axis of rotation for the second die that will be orthogonal to the axis of rotation of the first die
                    axis_of_rotation2[picked_random_axis] = 0.0f; 
                    axis_of_rotation2[(picked_random_axis + 1) % 3] = 1.0f;
                    axis_of_rotation2[(picked_random_axis + 2) % 3] = 0.0f;
                } else if (roll_state == 1) { // were the dice rolling?
                    angle_change = 2; // decrease angular velocity
                    roll_state = 2; // 'deceleration' state -> it will switch to 0 in the display function 
                }
        }
        canvas.display();
    }

    /** Not Used. **/
    public void keyReleased(KeyEvent e) {
    }

    /** Not Used. **/
    public void keyTyped(KeyEvent e) {
    }

    /**
     * You do not need to edit (or understand) the following two functions - it is
     * just used to delete direct buffers we allocated in our program - they are specific to Java).
     * More details: Direct buffers are garbage collected by using a phantom
     * reference and a reference queue. Every once a while, the JVM checks the
     * reference queue and cleans the direct buffers. However, as this doesn't
     * happen immediately after discarding all references to a direct buffer,
     * it's easy to OutOfMemoryError yourself using direct buffers. This
     * function explicitly calls the Cleaner method of a direct buffer.
     *
     * @param toBeDestroyed The direct buffer that will be "cleaned". Utilizes
     * reflection.
     * @author Joshua Slack
     * @version $Id: BufferUtils.java,v 1.16 2007/10/29 16:56:18 nca Exp $ The
     * following two functions originate from jMonkeyEngine see
     * https://github.com/java-opengl-labs/helloTriangle/blob/master/LICENSE for
     * (open source) MIT license information
     */
    private static void destroyDirectBuffer(Buffer toBeDestroyed) {
        Method cleanerMethod = loadMethod("sun.nio.ch.DirectBuffer", "cleaner");
        Method cleanMethod = loadMethod("sun.misc.Cleaner", "clean");
        Method viewedBufferMethod = loadMethod("sun.nio.ch.DirectBuffer", "viewedBuffer");
        if (viewedBufferMethod == null) {
            // They changed the name in Java 7 (???)
            viewedBufferMethod = loadMethod("sun.nio.ch.DirectBuffer", "attachment");
        }

        try {
            Object cleaner = cleanerMethod.invoke(toBeDestroyed);
            if (cleaner != null) {
                cleanMethod.invoke(cleaner);
            } else {
                // Try the alternate approach of getting the viewed buffer first
                Object viewedBuffer = viewedBufferMethod.invoke(toBeDestroyed);
                if (viewedBuffer != null) {
                    destroyDirectBuffer((Buffer) viewedBuffer);
                } else {
                    System.err.println("Buffer cannot be destroyed");
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException ex) {
            System.err.println("Buffer cannot be destroyed");
        }
    }

    /* used by the destroyDirectBuffer function. */
    private static Method loadMethod(String className, String methodName) {
        try {
            Method method = Class.forName(className).getMethod(methodName);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException ex) {
            return null; // the method was not found
        }
    }
}
