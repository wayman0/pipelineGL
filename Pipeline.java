/*
 * Renderer 1. The MIT License.
 * Copyright (c) 2022 rlkraft@pnw.edu
 * See LICENSE for details.
*/

package renderer.pipelineGL;

import renderer.scene.*;
import renderer.scene.primitives.*;
import renderer.scene.util.CheckModel;
import renderer.framebuffer.*;
import static renderer.pipeline.PipelineLogger.*;

import com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;


/**
   This renderer takes as its input a {@link Scene} data structure
   and a {@link FrameBuffer.Viewport} within a {@link FrameBuffer}
   data structure. This renderer mutates the {@link FrameBuffer.Viewport}
   so that it is filled in with the rendered image of the geometric
   scene represented by the {@link Scene} object.
<p>
   This implements our first rendering pipeline. It has just four
   pipeline stages.
   <ol>
   <li>The {@link Model2Camera} transformation stage converts vertex
   coordinates from the {@link Model}'s (private) coordinate system
   to the {@link Camera}'s (shared) coordinate system.</li>
   <li>The {@link Projection} transformation stage projects
   each {@link Vertex} from its camera space location into
   the {@link Camera}'s image plane.
   <li>The {@link Viewport} stage transforms vertices from the
   image-plane to the logical pixel-plane.</li>
   <li>The {@link Rasterize} stage converts each projected
   {@link renderer.scene.primitives.LineSegment} and
   {@link renderer.scene.primitives.Point} in the pixel-plane
   into pixels in the {@link FrameBuffer.Viewport}.
   Rasterized fragments from a {@link renderer.scene.primitives.LineSegment}
   or a {@link renderer.scene.primitives.Point} that are not contained
   in the {@link Camera}'s view rectangle are "clipped off", and not
   placed in the {@link FrameBuffer.Viewport}.
   </ol>
   Notice that the first three stages act on the {@link Model}'s vertices,
   and the fourth stage acts on the {@link Model}'s
   {@link renderer.scene.primitives.Primitive}s. That is, we transform
   vertices and we rasterize line segments and points.
*/
public final class Pipeline
{
      private static GL4 gl; 
      private static final String[] vertexShaderSourceCode1 = 
      {
         "#version 430 \n", 
         "layout (location=0) in vec4 vertex \n", 
         "layout (location=1) in vec4 positionTranslation; \n", 
         "layout (location=2) in bool cameraPerspective; \n"
      }; 

      private static final String[] vertexShaderSourceCode2 = 
      {
         "void main(void) \n", 
         "{ \n", 
         "gl_Position = model2Camera(); \n", 
         "gl_Position = projection(); \n", 
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
         "#version 430 \n", 
         "out vec4 color; \n", 
         "void main(void) \n", 
         "{ \n", 
         "color = vec4(1, 1, 1, 1); \n",
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
         System.arraycopy(vertexShaderSourceCode1,   0, vertexShaderSourceCode, 0,                      vertexShaderSourceCode1.length); 
         System.arraycopy(Model2Camera.model2Camera, 0, vertexShaderSourceCode, verCode1Size,                   Model2Camera.model2Camera.length); 
         System.arraycopy(vertexShaderSourceCode2,   0, vertexShaderSourceCode, verCode1Size + mod2CamSize,     vertexShaderSourceCode2.length); 

         gl = GLContext.getCurrentGL().getGL4(); 

         final int vertexShaderID = gl.glCreateShader(gl.GL_VERTEX_SHADER); 
         gl.glShaderSource(vertexShaderID, vertexShaderSourceCode.length, vertexShaderSourceCode, null);
         gl.glCompileShader(vertexShaderID);

         final int fragmentShaderID = gl.glCreateShader(gl.GL_FRAGMENT_SHADER); 
         gl.glShaderSource(fragmentShaderID, fragmentShaderSourceCode.length, fragmentShaderSourceCode, null);
         gl.glCompileShader(fragmentShaderID);

         int gpuProgramID = gl.glCreateProgram(); 
         gl.glAttachShader(gpuProgramID, vertexShaderID); 
         gl.glAttachShader(gpuProgramID, fragmentShaderID);
         gl.glLinkProgram(gpuProgramID);

         for(final Position position : scene.positionList)
         {
            final Model model = position.getModel(); 
         }

         //Model2Camera.model2camera(position); 
            //Projection.project(model, scene.camera); 
            //Viewport.imagePlane2pixelPlane(model, vp); 
            //Rasterize.rasterize(model, vp);
      }

   // Private default constructor to enforce noninstantiable class.
   // See Item 4 in "Effective Java", 3rd Ed, Joshua Bloch.
   private Pipeline() {
      throw new AssertionError();
   }
}
