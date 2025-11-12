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
import renderer.pipelineGL.OpenGLChecker;

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
      private static int[] vbo = new int[2]; // the buffer id's for the vertex attributes ie data and indexes 
      private static int vertexVBOID;    // the buffer id for the vertex info
      private static int indexVBOID;     // the buffer id for the index info 
      private static int vertexAttribID;
      private static int transUniformID;     // the uniform id for the translation info
      private static final int numCoordsPerPoint = 3;//4; 

      private static final String[] vertexShaderSourceCode1 =
      {
         "#version 450 \n",
         "layout (location=0) in vec3 vertex; \n",
         "uniform vec3 translationVector; \n",
         "vec4 model2Camera(); \n", 
         "vec4 projection(); \n"
      };

      private static final String[] vertexShaderSourceCode2 =
      {
         "void main(void) \n",
         "{ \n",
         "gl_Position = model2Camera();\n",
         "gl_Position = project();\n",
         "} \n"
      };

      private static final int verCode1Size = vertexShaderSourceCode1.length;
      private static final int verCode2Size = vertexShaderSourceCode2.length;
      private static final int mod2CamSize  = Model2Camera.model2Camera.length;
      private static final int projectSize  = Projection.project.length; 

      private static String[] vertexShaderSourceCode = new String[verCode1Size +
                                                                  mod2CamSize +
                                                                  projectSize +
                                                                  verCode2Size];

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
         createOpenGLFramebuffer(vp);

         int gpuProgramID = createOpenGLShaders();
         
         //https://docs.gl/gl4/glGenVertexArrays
         //https://docs.gl/gl4/glBindVertexArray
         gl.glGenVertexArrays(vao.length, vao, 0); // generate the id for the vao and store it at index 0
         gl.glBindVertexArray(vao[0]);             // bind the id for the vao, make the 0th vao active

         //https://docs.gl/gl4/glGenBuffers
         gl.glGenBuffers(vbo.length, vbo, 0); // generate the id and store them starting at index 0.
         vertexVBOID   = vbo[0];
         indexVBOID    = vbo[1]; 

         vertexAttribID = gl.glGetAttribLocation(gpuProgramID, "vertex"); 

         //https://docs.gl/gl4/glGetUniformLocation
         transUniformID = gl.glGetUniformLocation(gpuProgramID, "translationVector"); // find the id for the translation vector

         for(final Position position : scene.positionList)
         {
            final Model model = position.getModel();
            
            final int      numVertexes  = model.vertexList.size();
            final double[] vertexCoords = new double[numVertexes * numCoordsPerPoint];

            // this will need to be fixed later on assuming every primitive is a line segment 
            //final int   numPrimitives = model.primitiveList.size();
            int numPoints = 0; 
            int numLines  = 0; 
            for(final Primitive p : model.primitiveList)
            {
               if(p instanceof Point)
                  numPoints += 1; 
               else if(p instanceof LineSegment)
                  numLines += 1; 
            }

            //final int[] vertexIndexes = new int[numPrimitives * 2]; 
            final int[] lineIndexes = new int[numLines * 2]; 
            final int[] pointIndexes = new int[numPoints * 2]; 

            // openGL seems to flip the x and y axis 
            int vertexCoordIndex = 0;
            for(final Vertex v : model.vertexList)
            {
               vertexCoords[vertexCoordIndex + 0] = v.x; 
               vertexCoords[vertexCoordIndex + 1] = v.y; 
               vertexCoords[vertexCoordIndex + 2] = v.z; 

               vertexCoordIndex += numCoordsPerPoint; 
            }

            int lineIndex  = 0; 
            int pointIndex = 0; 
            for(final Primitive p : model.primitiveList)
            {
               if(p instanceof Point)
               {
                  pointIndexes[pointIndex] = p.vIndexList.get(0); 
                  pointIndex += 1; 
               }
               else if(p instanceof LineSegment)
               {
                  lineIndexes[lineIndex + 0] = p.vIndexList.get(0); 
                  lineIndexes[lineIndex + 1] = p.vIndexList.get(1);
                  
                  lineIndex += 2; 
               }
            }

            final Vector transVector = position.getTranslation();
            // copy the translation vector into the uniform
            //https://docs.gl/gl4/glUniform
            gl.glUniform3f(transUniformID, (float)transVector.x, (float)transVector.y, (float)transVector.z);
            OpenGLChecker.CheckOpenGLError(gl); 

            // make a buffer from the coordinates
            DoubleBuffer vertBuffer = Buffers.newDirectDoubleBuffer(vertexCoords);

            // bind the vertex buffer id, make the vertex buffer active
            //https://docs.gl/gl4/glBindBuffer
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexVBOID);

            // copy the vertex buffer data
            //https://docs.gl/gl4/glBufferData
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertBuffer.limit() * Buffers.SIZEOF_DOUBLE, vertBuffer, GL4.GL_STATIC_DRAW);
      
            //performTransformFeedback(vertBuffer, indBuffer);

            if(numLines > 0)
            {
               // make a buffer from the indexes 
               IntBuffer    indBuffer  = Buffers.newDirectIntBuffer(lineIndexes);

               // bind the index buffer id, make the index buffer active
               //https://docs.gl/gl4/glBindBuffer
               //https://www.mathematik.uni-marburg.de/~thormae/lectures/graphics1/graphics_8_1_eng_web.html#13
               gl.glBindBuffer(GL4.GL_ELEMENT_ARRAY_BUFFER, indexVBOID);

               // copy the index buffer data
               //https://docs.gl/gl4/glBufferData
               //https://www.mathematik.uni-marburg.de/~thormae/lectures/graphics1/graphics_8_1_eng_web.html#13
               gl.glBufferData(GL4.GL_ELEMENT_ARRAY_BUFFER, indBuffer.limit() * Buffers.SIZEOF_INT, indBuffer, GL4.GL_STATIC_DRAW);

               // make the vertex variable in the vertex shader active
               //https://docs.gl/gl4/glEnableVertexAttribArray
               gl.glEnableVertexAttribArray(vertexAttribID);

               // say that the vertex buffer is associated with attribute 0, layout = 0 
               //https://docs.gl/gl4/glVertexAttribPointer
               gl.glVertexAttribPointer(vertexAttribID, numCoordsPerPoint, GL4.GL_DOUBLE, false, 0, 0);

               gl.glDrawElements(GL4.GL_LINES, indBuffer.limit(), GL4.GL_UNSIGNED_INT, 0);
            }

            if(numPoints > 0)
            {
               // make a buffer from the indexes 
               IntBuffer    indBuffer  = Buffers.newDirectIntBuffer(pointIndexes);

               // bind the index buffer id, make the index buffer active
               //https://docs.gl/gl4/glBindBuffer
               //https://www.mathematik.uni-marburg.de/~thormae/lectures/graphics1/graphics_8_1_eng_web.html#13
               gl.glBindBuffer(GL4.GL_ELEMENT_ARRAY_BUFFER, indexVBOID);

               // copy the index buffer data
               //https://docs.gl/gl4/glBufferData
               //https://www.mathematik.uni-marburg.de/~thormae/lectures/graphics1/graphics_8_1_eng_web.html#13
               gl.glBufferData(GL4.GL_ELEMENT_ARRAY_BUFFER, indBuffer.limit() * Buffers.SIZEOF_INT, indBuffer, GL4.GL_STATIC_DRAW);

               // make the vertex variable in the vertex shader active
               //https://docs.gl/gl4/glEnableVertexAttribArray
               gl.glEnableVertexAttribArray(vertexAttribID);

               // say that the vertex buffer is associated with attribute 0, layout = 0 
               //https://docs.gl/gl4/glVertexAttribPointer
               gl.glVertexAttribPointer(vertexAttribID, numCoordsPerPoint, GL4.GL_DOUBLE, false, 0, 0);
   
               gl.glDrawElements(GL4.GL_POINTS, indBuffer.limit(), GL4.GL_UNSIGNED_INT, 0); 
            }
            
            // make the buffer to store the gl rendered data
            ByteBuffer pixelBuffer = GLBuffers.newDirectByteBuffer(vp.getWidthVP() * vp.getHeightVP() * 4);

            // read the data starting at x = 0, y = 0, through the width and height into the pixelBuffer
            //https://docs.gl/gl4/glReadPixels
            gl.glReadPixels(0, 0, vp.getWidthVP(), vp.getHeightVP(), GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixelBuffer);

            OpenGLChecker.CheckOpenGLError(gl); 

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
         }
      }

      private static void createOpenGLFramebuffer(FrameBuffer.Viewport vp)
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

         final Color vpBGColor = vp.bgColorVP;
         // clear the pbuffer to be the background color
         gl.glClearColor(vpBGColor.getRed(), vpBGColor.getGreen(), vpBGColor.getBlue(), vpBGColor.getAlpha());

         gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT); // clear the color buffer and the depth buffer
      }

      private static int createOpenGLShaders()
      {
         // copy all the glsl code into one big array
         System.arraycopy(vertexShaderSourceCode1,   0, vertexShaderSourceCode,    0,                            verCode1Size);
         System.arraycopy(Model2Camera.model2Camera, 0, vertexShaderSourceCode, verCode1Size,                             mod2CamSize);
         System.arraycopy(Projection.project,        0, vertexShaderSourceCode, verCode1Size + mod2CamSize,               projectSize);
         System.arraycopy(vertexShaderSourceCode2,   0, vertexShaderSourceCode, verCode1Size + mod2CamSize + projectSize, verCode2Size);

         System.out.println(Arrays.toString(vertexShaderSourceCode)); 

         // create the vertex shader and get its id, set the source code, and compile it
         // https://docs.gl/gl4/glCreateShader
         //https://www.mathematik.uni-marburg.de/~thormae/lectures/graphics1/graphics_9_1_eng_web.html#47
         final int vertexShaderID = gl.glCreateShader(GL4.GL_VERTEX_SHADER);

         //https://docs.gl/gl4/glShaderSource     
         //https://docs.gl/gl4/glCompileShader 
         //https://www.mathematik.uni-marburg.de/~thormae/lectures/graphics1/graphics_9_1_eng_web.html#47
         gl.glShaderSource(vertexShaderID, vertexShaderSourceCode.length, vertexShaderSourceCode, null);
         gl.glCompileShader(vertexShaderID);

         // create the fragment shader and get its id, set the source code, and compile it
         final int fragmentShaderID = gl.glCreateShader(GL4.GL_FRAGMENT_SHADER);
         gl.glShaderSource(fragmentShaderID, fragmentShaderSourceCode.length, fragmentShaderSourceCode, null);
         gl.glCompileShader(fragmentShaderID);

         // create the program and save its id, attach the compiled vertex and fragment shader, and link it all together
         //https://docs.gl/gl4/glCreateProgram
         //https://www.mathematik.uni-marburg.de/~thormae/lectures/graphics1/graphics_9_1_eng_web.html#47  
         int gpuProgramID = gl.glCreateProgram();

         //https://docs.gl/gl4/glAttachShader
         gl.glAttachShader(gpuProgramID, vertexShaderID);
         gl.glAttachShader(gpuProgramID, fragmentShaderID);
         
         //https://docs.gl/gl4/glTransformFeedbackVaryings
         String[] vertexShaderOutputVariableName = //{"transVertex"};
                                                   {"gl_Position"}; 
         gl.glTransformFeedbackVaryings(gpuProgramID, vertexShaderOutputVariableName.length, 
                                        vertexShaderOutputVariableName, GL4.GL_INTERLEAVED_ATTRIBS);
         OpenGLChecker.CheckOpenGLError(gl);
         //https://docs.gl/gl4/glLinkProgram
         //https://www.mathematik.uni-marburg.de/~thormae/lectures/graphics1/graphics_9_1_eng_web.html#47
         gl.glLinkProgram(gpuProgramID);
         gl.glUseProgram(gpuProgramID);
      
         return gpuProgramID;
      }
   
      private static void performTransformFeedback(DoubleBuffer vertBuffer, IntBuffer indBuffer)
      {
         performTransformFeedback(vertBuffer, indBuffer, false);
      }

      private static void performTransformFeedback(DoubleBuffer vertBuffer, IntBuffer indBuffer, boolean print)
      {
         int[] transformFeedbackVBO = new int[1];
         gl.glGenBuffers(transformFeedbackVBO.length, transformFeedbackVBO, 0);
         gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, transformFeedbackVBO[0]);
         gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertBuffer.limit()*Buffers.SIZEOF_DOUBLE, null, GL4.GL_STATIC_READ);
         gl.glEnable(GL4.GL_RASTERIZER_DISCARD);
         gl.glBindBufferBase(GL4.GL_TRANSFORM_FEEDBACK_BUFFER, 0, transformFeedbackVBO[0]);
         gl.glBeginTransformFeedback(GL4.GL_LINES);
         gl.glDrawElements(GL4.GL_LINES, indBuffer.limit(), GL4.GL_UNSIGNED_INT, 0);
         gl.glEndTransformFeedback();
         gl.glFlush();

         //                              number of points   * how many lines there are
         //double[] feedbackArr = new double[vertBuffer.limit()/4 * indBuffer.limit()/2];
         //DoubleBuffer feedback = Buffers.newDirectDoubleBuffer(feedbackArr);
         //gl.glGetBufferSubData(GL4.GL_TRANSFORM_FEEDBACK_BUFFER, 0, feedback.limit() * Buffers.SIZEOF_DOUBLE, feedback);
         
         float[] feedbackArr = new float[vertBuffer.limit()/numCoordsPerPoint * indBuffer.limit()/2]; 
         FloatBuffer feedback = Buffers.newDirectFloatBuffer(feedbackArr); 
         gl.glGetBufferSubData(GL4.GL_TRANSFORM_FEEDBACK_BUFFER, 0, feedback.limit() * Buffers.SIZEOF_FLOAT, feedback);
         OpenGLChecker.CheckOpenGLError(gl); 
         gl.glDisable(GL4.GL_RASTERIZER_DISCARD);  
         
         if(print)
         {   
            for(int i = 0; i < feedback.limit(); i += 4)
               System.out.println(feedback.get(i+0) + ", " + feedback.get(i+1) + ", " +
                                  feedback.get(i+2) + ", " + feedback.get(i+3));
         }
      }
   
   
   // Private default constructor to enforce noninstantiable class.
   // See Item 4 in "Effective Java", 3rd Ed, Joshua Bloch.
   private PipelineGL() {
      throw new AssertionError();
   }
}
