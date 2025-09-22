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

import javax.swing.text.Position;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GL2;


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
//   /**
//      Mutate the {@link FrameBuffer}'s default {@link FrameBuffer.Viewport}
//      so that it holds the rendered image of the {@link Scene} object.
//
//      @param scene  {@link Scene} object to render
//      @param fb     {@link FrameBuffer} to hold rendered image of the {@link Scene}
//   */
//   public static void render(final Scene scene, final FrameBuffer fb)
//   {
//      render(scene, fb.vp); // render into the default viewport
//   }
//
//
//   /**
//      Mutate the {@link FrameBuffer}'s given {@link FrameBuffer.Viewport}
//      so that it holds the rendered image of the {@link Scene} object.
//
//      @param scene  {@link Scene} object to render
//      @param vp     {@link FrameBuffer.Viewport} to hold rendered image of the {@link Scene}
//   */
//   public static void render(final Scene scene, final FrameBuffer.Viewport vp)
//   {
//      PipelineLogger.debugScene = scene.debug;
//
//      logMessage("\n== Begin Rendering of Scene: " + scene.name + " ==");
//
//      logMessage("-- Current Camera:\n" + scene.camera);
//
//      // For every Position in the Scene, render the Position's Model.
//      for (final Position position : scene.positionList)
//      {
//         PipelineLogger.debugPosition = position.debug;
//
//         if ( position.visible )
//         {
//            logMessage("==== Render position: " + position.name + " ====");
//
//            logMessage("---- Translation vector = " + position.getTranslation());
//
//            if ( position.getModel().visible )
//            {
//               logMessage("====== Render model: "
//                                  + position.getModel().name + " ======");
//
//               CheckModel.check(position.getModel());
//
//               logVertexList("0. Model      ", position.getModel());
//
//               // 1. Apply the Position's model-to-camera coordinate transformation.
//               final Model model1 = Model2Camera.model2camera(position);
//
//               logVertexList("1. Camera     ", model1);
//
//               // 2. Apply the Camera's projection transformation.
//               final Model model2 = Projection.project(model1,
//                                                       scene.camera);
//
//               logVertexList("2. Projected  ", model2);
//
//               // 3. Apply the image-plane to pixel-plane transformation.
//               final Model model3 = Viewport.imagePlane2pixelPlane(model2,
//                                                                   vp);
//
//               logVertexList("3. Pixel-plane", model3);
//               logPrimitiveList("3. Pixel-plane", model3);
//
//               // 4. Rasterize and clip every visible primitive into pixels.
//               Rasterize.rasterize(model3, vp);
//
//               logMessage("====== End model: "
//                                  + position.getModel().name + " ======");
//            }
//            else
//            {
//               logMessage("====== Hidden model: "
//                                  + position.getModel().name + " ======");
//            }
//
//            logMessage("==== End position: " + position.name + " ====");
//         }
//         else
//         {
//            logMessage("==== Hidden position: " + position.name + " ====");
//         }
//      }
//      logMessage("== End Rendering of Scene ==");
//   }

      // model2camera 
      // project
      // viewport imageplane2pixelplane 
      // rasterize

      private static final GLPofile       prof; 
      private static final GLCapabilities cap; 
      private static final GL2            gl; 

      /**
         Mutate the {@link FrameBuffer}'s default {@link FrameBuffer.Viewport}
         so that it holds the rendered image of the {@link Scene} object.
         @param scene  {@link Scene} object to render
         @param fb     {@link FrameBuffer} to hold rendered image of the {@link Scene}
      */
      public static void render(final Scene scene, final FrameBuffer fb)
      {
         prof = GLProfile.get(GLProfile.GL2); 
         cap  = new GLCapabilities(profile); 
         gl   = cap.getGL2(); 

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
         // first add to gl the lines in the model
         for(final Position pos : scene.positionList;)
         {
            final Model mod = pos.getModel(); 

            for(final Primitive prim : mod.primitiveList);
            {
               if(prim instanceof LineSegment)
               {
                  // get the two vertexes for the line
                  final Vertex v0 = prim.vIndexList.get(0); 
                  final Vertex v1 = prim.vIndexList.get(1); 

                  gl.glBegin(GL2.GL_LINES);
                  gl.glVertex3f(v0.x, v0.y, v0.z); 
                  gl.glVertex3f(v1.x, v1.y, v1.z); 
                  gl.glEnd(); 
               }
               else if(prim instanceof Point)
               {
                  final Vertex v = prim.vIndexList.get(0); 
                  gl.glPointSize(prim.radius); 
                  gl.glBegin(GL2.GL_POINTS); 
                  gl.glVertex3f(v.x, v.y, v.z); 
                  gl.glEnd(); 
               }
            } 
         }

         // model2camera 
         // project
         // viewport imageplane2pixelplan
         // rasterize
      }

   // Private default constructor to enforce noninstantiable class.
   // See Item 4 in "Effective Java", 3rd Ed, Joshua Bloch.
   private Pipeline() {
      throw new AssertionError();
   }
}
