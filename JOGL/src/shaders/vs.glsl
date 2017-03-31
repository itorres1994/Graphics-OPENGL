/* === YOUR WORK HERE ===
 * Vertex shader: you need to edit the code here!
 * First, you need to understand the Java program,
 * mainly understand how the variables in this shader program
 * are associated with buffers and variables defined 
 * in your Java program. 
 * The main changes you need to do here is to use the 'TINVMV' matrix
 * to properly transform vertex normals, compute light vectors 
 * that you will use for diffuse shading, pass the uv coordinates down the pipeline.
 */
#version 330

layout(location = 0) in vec4 vertex_pos;    // https://www.khronos.org/opengl/wiki/Layout_Qualifier_(GLSL)
layout(location = 1) in vec3 vertex_normal;
layout(location = 2) in vec2 uv_coordinates;

uniform mat4 MVP;                 // Model view projection matrix
uniform mat4 MV;                  // Model view matrix
uniform mat4 TINVMV;              // tinv model view matrix
uniform vec4 light0_pos_eye;      // eye coordinates

out vec2 uv_coordinates_raster;
out vec3 normals_raster;
out vec3 light0_vector_raster;

void main() 
{   
    gl_Position = MVP * vertex_pos;
    
    // you need to change the following lines!
    vec4 vertex_pos_eye = MV * vertex_pos;
    uv_coordinates_raster = uv_coordinates;
    normals_raster = normalize(vec3(TINVMV * vec4(vertex_normal,0)));
    light0_vector_raster = normalize(vec3(light0_pos_eye - vertex_pos_eye));
    
}
