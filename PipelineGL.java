/*
 * Renderer 1. The MIT License.
 * Copyright (c) 2022 rlkraft@pnw.edu
 * See LICENSE for details.
*/

package renderer.pipelineGL;

import java.awt.Color;
import java.nio.*;
import java.util.Arrays;

import renderer.scene.*;
import renderer.scene.primitives.*;
import renderer.framebuffer.*;
import renderer.pipelineGL.PipelineChecker;

import com.jogamp.opengl.*;
import com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.common.nio.Buffers;

/**
   This renderer takes as its input a {@link Scene} data structure
   and a {@link FrameBuffer.Viewport} within a {@link FrameBuffer}
   data structure. This renderer mutates the {@link FrameBuffer.Viewport}
   so that it is filled in with the rendered image of the geometric
   scene represented by the {@link Scene} object.
*/
public final class PipelineGL
{
      private static GLCapabilities glCap;             // the capabilities of the gl profile
      private static GLProfile      glProf;            // the gl profile being used , gl4
      private static GL4            gl;                // the gl4 object

      private static GLDrawableFactory       glFact;         // used to create the pbuffer for offscreen rendering
      private static GLOffscreenAutoDrawable glPixelBuffer;  // the pbuffer for offscreen rendering

      private static int[] vao = new int[1]; // the main buffer id that all vertex info gets bound to
      private static int[] vbo = new int[1]; // the buffer id's for the vertex
      private static int vertexVBOID;    // the buffer id for the vertex info
      private static int transUniformID;     // the uniform id for the translation info

      private static final String[] vertexShaderSourceCode1 =
      {
         "#version 450 \n",
         "layout (location=0) in vec3 vertex; \n",
         "uniform vec3 translationVector; \n"
      };

      private static final String[] vertexShaderSourceCode2 =
      {
         "void main(void) \n",
         "{ \n",
         //"gl_Position = model2Camera(); \n",
         " \tgl_Position = vec4(vertex, 1); \n",
         //"gl_Position = vec4(model2Camera(), 1); \n",
         //"gl_Position = projection(); \n",
         "} \n"
      };

      private static final int verCode1Size = vertexShaderSourceCode1.length;
      private static final int verCode2Size = vertexShaderSourceCode2.length;
      private static final int mod2CamSize  = Model2Camera.model2Camera.length;

      private static String[] vertexShaderSourceCode = new String[vertexShaderSourceCode1.length +
                                                                  Model2Camera.model2Camera.length +
                                                                  vertexShaderSourceCode2.length];

      private static final String [] fragmentShaderSourceCode =
      {
         "#version 450 \n",
         "out vec4 color; \n",
         "void main(void) \n",
         "{ \n",
         "\tcolor = vec4(1, 1, 1, 1); \n",
         "} \n"
      };

      /**
         Mutate the {@link FrameBuffer}'s default {@link FrameBuffer.Viewport}
         so that it holds the rendered image of the {@link Scene} object.
         @param scene  {@link Scene} object to render)
         @param fb     {@link FrameBuffer} to hold rendered image of the {@link Scene}
      */
      public static void render(final Scene scene, final FrameBuffer fb)
      {
         render(scene, fb.vp); // render into the default viewport
      }

      /**
         Mutate the {@link FrameBuffer}'s given {@link FrameBuffer.Viewport}
         so that it holds the rendered image of the {@link Scene} object.
         @param scene  {@link Scene} object to render
         @param vp     {@link FrameBuffer.Viewport} to hold rendered image of the {@link Scene}
      */
      public static void render(final Scene scene, final FrameBuffer.Viewport vp)
      {

         glProf = GLProfile.get("GL4");
         glCap  = new GLCapabilities(glProf);

         glCap.setPBuffer(true);              // enable the use of pbuffers
         glCap.setDoubleBuffered(false);

         glFact = GLDrawableFactory.getFactory(glProf);
         glPixelBuffer = glFact.createOffscreenAutoDrawable(null, glCap, null, vp.getWidthVP(), vp.getHeightVP()); // create the pbuffer to be the vp width x vp height
         glPixelBuffer.display();
         glPixelBuffer.getContext().makeCurrent(); // make this pbuffer current

         gl = glPixelBuffer.getGL().getGL4(); // get the gl object associated with the pbuffer

   PipelineChecker.CheckOpenGLError(gl);

         final Color vpBGColor = vp.bgColorVP;
         // clear the pbuffer to be the background color
         gl.glClearColor(vpBGColor.getRed(), vpBGColor.getGreen(), vpBGColor.getBlue(), vpBGColor.getAlpha());
         //gl.glClearColor(255, 255, 255, 255); // clear the pbuffer to be the background color

         gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT); // clear the color buffer and the depth buffer

   System.out.println("CLEARED THE PBUFFER");

         // copy all the glsl code into one big array
         System.arraycopy(vertexShaderSourceCode1,   0, vertexShaderSourceCode, 0, vertexShaderSourceCode1.length);
         System.arraycopy(Model2Camera.model2Camera, 0, vertexShaderSourceCode, verCode1Size, Model2Camera.model2Camera.length);
         System.arraycopy(vertexShaderSourceCode2,   0, vertexShaderSourceCode, verCode1Size + mod2CamSize,     vertexShaderSourceCode2.length);

   System.out.println("COPIED THE CODE: \n");
   System.out.println(Arrays.toString(vertexShaderSourceCode));

         // create the vertex shader and get its id, set the source code, and compile it
         final int vertexShaderID = gl.glCreateShader(GL4.GL_VERTEX_SHADER);

   System.out.println("Created the Shader and is it a shader: " + gl.glIsShader(vertexShaderID));

         gl.glShaderSource(vertexShaderID, vertexShaderSourceCode.length, vertexShaderSourceCode, null);
         //gl.glShaderSource(vertexShaderID, 3, new String[] {"#version 450\n", "void main(void) \n", "{gl_Position = vec4(0, 0, 0, 1);}\n"}, null, 0);
         gl.glCompileShader(vertexShaderID);

   System.out.println("COMPILED THE VERTEX SHADER CODE: " + PipelineChecker.shaderCompiled(gl, vertexShaderID));
   PipelineChecker.CheckOpenGLError(gl);
   //System.out.println(PipelineChecker.shaderCompiled(gl, vertexShaderID));
   //if(!PipelineChecker.shaderCompiled(gl, vertexShaderID))
   PipelineChecker.printShaderLog(gl, vertexShaderID);

         // create the fragment shader and get its id, set the source code, and compile it
         final int fragmentShaderID = gl.glCreateShader(gl.GL_FRAGMENT_SHADER);
         gl.glShaderSource(fragmentShaderID, fragmentShaderSourceCode.length, fragmentShaderSourceCode, null);
         gl.glCompileShader(fragmentShaderID);

   System.out.println("COMPILED THE FRAGMENT SHADER CODE: " + PipelineChecker.shaderCompiled(gl, fragmentShaderID));
   PipelineChecker.CheckOpenGLError(gl);
   //System.out.println(PipelineChecker.shaderCompiled(gl, fragmentShaderID));
   //if(!PipelineChecker.shaderCompiled(gl, fragmentShaderID))
   PipelineChecker.printShaderLog(gl, fragmentShaderID);

         // create the program and save its id, attach the compiled vertex and fragment shader, and link it all together
         int gpuProgramID = gl.glCreateProgram();

   System.out.println("CREATED THE PROGRAM");
   PipelineChecker.CheckOpenGLError(gl);

         gl.glAttachShader(gpuProgramID, vertexShaderID);

   System.out.println("ADDED VERTEX SHADER");
   PipelineChecker.CheckOpenGLError(gl);


         gl.glAttachShader(gpuProgramID, fragmentShaderID);

   System.out.println("ADDED FRAGMENT SHADER");
   PipelineChecker.CheckOpenGLError(gl);


         gl.glLinkProgram(gpuProgramID);

   System.out.println("LINKED THE PROGRAM");
   PipelineChecker.CheckOpenGLError(gl);
   if(!PipelineChecker.programLinked(gl, gpuProgramID))

         PipelineChecker.printProgramLog(gl, gpuProgramID);

         gl.glGenVertexArrays(vao.length, vao, 0); // generate the id for the vao and store it at index 0
         gl.glBindVertexArray(vao[0]);             // bind the id for the vao, make the 0th vao active

         gl.glGenBuffers(vbo.length, vbo, 0); // generate the id and store them starting at index 0.
         vertexVBOID   = vbo[0];

         transUniformID = gl.glGetUniformLocation(gpuProgramID, "translationVector"); // find the id for the translation vector

   System.out.println("SET UP VAO VBO AND UNIFORMS");

         for(final Position position : scene.positionList)
         {
            final Model model = position.getModel();

            final int numPrimitives = model.primitiveList.size();
            // assume every primitive is a line segment, which requires 2 points which has 3 values, x, y, z
            final double[] vertexCoords = new double[numPrimitives * 2 * 3];
            int vertexCoordIndex = 0;

            for(final Primitive prim : model.primitiveList)
            {
               if(prim instanceof LineSegment)
               {
                  final int vInd0 = prim.vIndexList.get(0);
                  final int vInd1 = prim.vIndexList.get(1);
                  final Vertex v0 = model.vertexList.get(vInd0);
                  final Vertex v1 = model.vertexList.get(vInd1);

                  vertexCoords[vertexCoordIndex + 0] = v0.x;
                  vertexCoords[vertexCoordIndex + 1] = v0.y;
                  vertexCoords[vertexCoordIndex + 2] = v0.z;

                  vertexCoordIndex += 3;

                  vertexCoords[vertexCoordIndex + 0] = v1.x;
                  vertexCoords[vertexCoordIndex + 1] = v1.y;
                  vertexCoords[vertexCoordIndex + 2] = v1.z;

                  vertexCoordIndex += 3;
               }
            }

   System.out.println("CREATED THE VERTEX INFO");

            final Vector transVector = position.getTranslation();
            // copy the translation vector into the uniform
            gl.glUniform3d(transUniformID, transVector.x, transVector.y, transVector.z);

   System.out.println("COPIED THE TRANSLATION UNIFORM");

            // bind the vertex buffer id, make the vertex buffer active
            gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexVBOID);
            // make a buffer from the coordinates
            DoubleBuffer vertBuffer = Buffers.newDirectDoubleBuffer(vertexCoords);
            // copy the buffer data
            gl.glBufferData(gl.GL_ARRAY_BUFFER, vertBuffer.limit() * 4, vertBuffer, gl.GL_STATIC_DRAW);
            // say that the vertex buffer is associated with attribute 0
            gl.glVertexAttribPointer(0, 3, gl.GL_DOUBLE, false, 0, 0);
            // make the vertex variable in the vertex shader active
            gl.glEnableVertexAttribArray(0);
            // draw the primitive which is a line starting from point 0 to the number of points
            gl.glDrawArrays(gl.GL_LINES, 0, numPrimitives * 2);

   System.out.println("ENABLED THE VERTEX STUFF AND DREW LINES");

            // make the buffer to store the gl rendered data
            ByteBuffer pixelBuffer = GLBuffers.newDirectByteBuffer(vp.getWidthVP() * vp.getHeightVP() * 4);
            // read the data starting at x = 0, y = 0, through the width and height into the pixelBuffer
            gl.glReadPixels(0, 0, vp.getWidthVP(), vp.getHeightVP(), GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixelBuffer);

   System.out.println("READ THE PIXEL INFO FROM PBUFFER");

            // create an int view of the pixel buffer for use with the viewport
            final IntBuffer pixelIntBuffer = pixelBuffer.asIntBuffer();

            // copy the pixelBuffer into the framebuffer
            for(int x = 0; x < vp.getWidthVP(); x += 1)
            {
               for(int y = 0; y < vp.getHeightVP(); y += 1)
               {
                  vp.setPixelVP(x, y, pixelIntBuffer.get());
               }
            }

   System.out.println("COPIED THE PIXEL BUFFER INFO INTO THE FRAMEBUFFER");

            /*
            // this is for if the translation is supposed to be treated as something that should be rendered
            // translation sholdn't be rendered so this is wrong
            gl.glBindBuffer(gl.GL_ARRAY_BUFFER, transVBOID); // bind the translation vector id, make that buffer active
            final Vector transVector = position.getTranslation();
            final double[] transValues = {transVector.x, transVector.y, transVector.z};
            DoubleBuffer transBuffer = Buffers.newDirectDoubleBuffer(transValues); // make a buffer from the translation
            gl.glBufferData(gl.GL_ARRAY_BUFFER, transBuffer.limit() * 4, transBuffer, gl.GL_STATIC_DRAW); // copy the buffer data
            gl.glVertexAttribPointer(1, 3, gl.GL_DOUBLE, false, 0, 0); // say that the translation buffer is associated with attribute 1
            gl.glEnableVertexAttribArray(1);
            */
         }

         //Model2Camera.model2camera(position);
            //Projection.project(model, scene.camera);
            //Viewport.imagePlane2pixelPlane(model, vp);
            //Rasterize.rasterize(model, vp);
      }

   // Private default constructor to enforce noninstantiable class.
   // See Item 4 in "Effective Java", 3rd Ed, Joshua Bloch.
   private PipelineGL() {
      throw new AssertionError();
   }
}
