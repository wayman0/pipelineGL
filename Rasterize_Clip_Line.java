/*
 * Renderer 1. The MIT License.
 * Copyright (c) 2022 rlkraft@pnw.edu
 * See LICENSE for details.
*/

package renderer.pipeline;

import renderer.scene.*;
import renderer.scene.primitives.LineSegment;
import renderer.framebuffer.*;
import static renderer.pipeline.PipelineLogger.*;

import java.awt.Color;

/**
   Rasterize a projected {@link LineSegment} into pixels in
   a {@link FrameBuffer.Viewport}, but (optionally) do not
   rasterize any part of the {@link LineSegment} that is not
   contained in the {@link Camera}'s view rectangle.
<p>
   This pipeline stage takes a {@link LineSegment} whose vertices
   have been transformed into the logical pixel-plane and rasterizes
   the {@link LineSegment} into pixels in a {@link FrameBuffer.Viewport}.
<p>
   In addition, this rasterizer has the option to "clip" the
   {@link LineSegment} by not rasterizing into the
   {@link FrameBuffer.Viewport} any part of the projected
   {@link LineSegment} that is not within the {@link Camera}'s
   view rectangle.
<p>
   This rasterization algorithm is based on
<pre>
     "Fundamentals of Computer Graphics", 3rd Edition,
      by Peter Shirley, pages 163-165.
</pre>
<p>
   Recall that a {@link FrameBuffer.Viewport} is a two-dimensional array
   of pixel data. So a viewport has an integer "coordinate system". That
   is, we locate a pixel in a viewport using two integers, which we think
   of as row and column. On the other hand, a {@link Camera}'s view rectangle
   has a two-dimensional real number (not integer) coordinate system. Since
   a framebuffer's viewport and a camera's view rectangle have such different
   coordinate systems, rasterizing line segments from a two-dimensional real
   number coordinate system to an two-dimensional integer grid can be tricky.
   The "logical pixel-plane" and the "viewport transformation" try to make
   this rasterization step a bit easier.
<pre>{@code
                                 (0,0)
                                   _____________________________________________
        y-axis                     |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
          |                        |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
          |  (+1,+1)               |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
    +-----|-----+                  |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
    |     |     |                  |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
    |     |     |                  |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
----------+----------- x-axis      |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
    |     |     |                  |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
    |     |     |                  |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
    +-----|-----+                  |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
 (-1,-1)  |                        |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
          |                        |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
                                   |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
 Camera's View Rectangle           |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
  (in the View Plane)                                                      (w-1,h-1)
                                              FrameBuffer's Viewport
}</pre>
<p>
   The viewport transformation places the logical pixel-plane between the
   camera's view rectangle and the framebuffer's viewport. The pixel-plane
   has a real number coordinate system (like the camera's view plane) but
   is has dimensions more like the dimensions of the framebuffer's viewport.
<pre>{@code
                                                    (w+0.5, h+0.5)
           +-------------------------------------------+
           | . . . . . . . . . . . . . . . . . . . . . |
           | . . . . . . . . . . . . . . . . . . . . . |
           | . . . . . . . . . . . . . . . . . . . . . |
           | . . . . . . . . . . . . . . . . . . . . . |
           | . . . . . . . . . . . . . . . . . . . . . |  The "logical pixels"
           | . . . . . . . . . . . . . . . . . . . . . |  are the points in
           | . . . . . . . . . . . . . . . . . . . . . |  the pixel-plane with
           | . . . . . . . . . . . . . . . . . . . . . |  integer coordinates.
           | . . . . . . . . . . . . . . . . . . . . . |
           | . . . . . . . . . . . . . . . . . . . . . |
           | . . . . . . . . . . . . . . . . . . . . . |
           | . . . . . . . . . . . . . . . . . . . . . |
           | . . . . . . . . . . . . . . . . . . . . . |
           +-------------------------------------------+
      (0.5, 0.5)
                 Pixel-plane's "logical viewport"
                   containing "logical pixels"
   }</pre>
<p>
   Notice that we have two uses of the word "viewport",
   <ul>
   <li>The "logical viewport" is a rectangle in the pixel-plane (so
       its points have real number coordinates). The "logical pixels"
       are the points in the logical viewport with integer coordinates.
   <li>The "physical viewport" is part of the {@link FrameBuffer}'s pixel
       array (so its entries have integer coordinates). The "physical
       pixels" are the entries in the physical viewport.
   </ul>
*/
public final class Rasterize_Clip_Line
{
   /**
      Rasterize and (possibly) clip a projected {@link LineSegment} into pixels
      in the {@link FrameBuffer.Viewport}.

      @param model  {@link Model} that the {@link LineSegment} {@code ls} comes from
      @param ls     {@link LineSegment} to rasterize into the {@link FrameBuffer.Viewport}
      @param vp     {@link FrameBuffer.Viewport} to hold rasterized pixels
   */
   public static void rasterize(final Model model,
                                final LineSegment ls,
                                final FrameBuffer.Viewport vp)
   {
      final String     CLIPPED = "Clipped: ";
      final String NOT_CLIPPED = "         ";

      final Color c = Color.white;

      // Make local copies of several values.
      final int w = vp.getWidthVP();
      final int h = vp.getHeightVP();

      final int vIndex0 = ls.vIndexList.get(0);
      final int vIndex1 = ls.vIndexList.get(1);
      final Vertex v0 = model.vertexList.get(vIndex0);
      final Vertex v1 = model.vertexList.get(vIndex1);

      // Round each point's coordinates to the nearest logical pixel.
      double x0 = Math.round(v0.x);
      double y0 = Math.round(v0.y);
      double x1 = Math.round(v1.x);
      double y1 = Math.round(v1.y);

      // Rasterize a degenerate line segment (a line segment
      // that projected onto a single point) as a single pixel.
      if ( (x0 == x1) && (y0 == y1) )
      {
         // We don't know which endpoint of the line segment
         // is in front, so just pick v0.
         final int x0_vp = (int)x0 - 1;  // viewport coordinate
         final int y0_vp = h - (int)y0;  // viewport coordinate

         if ( ! Rasterize.doClipping
           || (x0_vp >= 0 && x0_vp < w && y0_vp >= 0 && y0_vp < h) ) // clipping test
         {
            if (Rasterize.debug)
            {
               logPixel(NOT_CLIPPED, x0, y0, x0_vp, y0_vp, vp);
            }
            // Log the pixel before setting it so that an array out-
            // of-bounds error will be right after the pixel's address.

            vp.setPixelVP(x0_vp, y0_vp, c);
         }
         else if (Rasterize.doClipping && Rasterize.debug)
         {
            logPixel(CLIPPED, x0, y0, x0_vp, y0_vp, vp);
         }
         return;
      }

      // If abs(slope) <= 1, then rasterize this line in
      // the direction of the x-axis. Otherwise, rasterize
      // this line segment in the direction of the y-axis.
      if (Math.abs(y1 - y0) <= Math.abs(x1 - x0)) // if abs(slope) <= 1
      {
         if (x1 < x0) // We want to rasterize along the x-axis from left-to-right,
         {            // so, if necessary, swap (x0, y0) with (x1, y1).
            final double tempX = x0;
            final double tempY = y0;
            x0 = x1;
            y0 = y1;
            x1 = tempX;
            y1 = tempY;
         }

         // Compute this line segment's slope.
         final double m = (y1 - y0) / (x1 - x0);

         if (Rasterize.debug)
         {
            logMessage("Slope m = " + m);
            logMessage(String.format("(x0_vp, y0_vp) = (%9.4f, %9.4f)", x0-1,h-y0));
            logMessage(String.format("(x1_vp, y1_vp) = (%9.4f, %9.4f)", x1-1,h-y1));
         }

         // Rasterize this line segment, along the x-axis, from left-to-right.
         // In the following loop, as x moves across the logical
         // horizontal pixels, we compute a y value for each x.
         double y = y0;
         for (int x = (int)x0; x <= (int)x1; x += 1, y += m)
         {
            // The value of y will almost always be between
            // two vertical pixel coordinates. By rounding off
            // the value of y, we are choosing the nearest logical
            // vertical pixel coordinate.
            final int x_vp = x - 1;                  // viewport coordinate
            final int y_vp = h - (int)Math.round(y); // viewport coordinate

            if ( ! Rasterize.doClipping
              || (x_vp >= 0 && x_vp < w && y_vp >= 0 && y_vp < h) ) // clipping test
            {
               if (Rasterize.debug)
               {
                  logPixel(NOT_CLIPPED, x, y, x_vp, y_vp, vp);
               }
               // Log the pixel before setting it so that an array out-
               // of-bounds error will be right after the pixel's address.

               vp.setPixelVP(x_vp, y_vp, c);
            }
            else if (Rasterize.doClipping && Rasterize.debug)
            {
               logPixel(CLIPPED, x, y, x_vp, y_vp, vp);
            }
         }// Advance (x,y) to the next pixel. Since delta_x = 1, we need delta_y = m.
      }
      else // abs(slope) > 1, so rasterize along the y-axis.
      {
         if (y1 < y0) // We want to rasterize along the y-axis from bottom-to-top,
         {            // so, if necessary, swap (x0, y0) with (x1, y1).
            final double tempX = x0;
            final double tempY = y0;
            x0 = x1;
            y0 = y1;
            x1 = tempX;
            y1 = tempY;
         }

         // Compute this line segment's slope.
         final double m = (x1 - x0) / (y1 - y0);

         if (Rasterize.debug)
         {
            logMessage("Slope m = " + m + " (so 1/m = " + 1/m + ")");
            logMessage(String.format("(x0_vp, y0_vp) = (%9.4f, %9.4f)", x0-1,h-y0));
            logMessage(String.format("(x1_vp, y1_vp) = (%9.4f, %9.4f)", x1-1,h-y1));
         }

         // Rasterize this line segment, along the y-axis, from bottom-to-top.
         // In the following loop, as y moves across the logical
         // vertical pixels, we compute a x value for each y.
         double x = x0;
         for (int y = (int)y0; y <= (int)y1; x += m, y += 1)
         {
            // The value of x will almost always be between
            // two horizontal pixel coordinates. By rounding off
            // the value of x, we are choosing the nearest logical
            // horizontal pixel coordinate.
            final int x_vp = (int)Math.round(x) - 1; // viewport coordinate
            final int y_vp = h - y;                  // viewport coordinate

            if ( ! Rasterize.doClipping
              || (x_vp >= 0 && x_vp < w && y_vp >= 0 && y_vp < h) ) // clipping test
            {
               if (Rasterize.debug)
               {
                  logPixel(NOT_CLIPPED, x, y, x_vp, y_vp, vp);
               }

               vp.setPixelVP(x_vp, y_vp, c);
            }
            else if (Rasterize.doClipping && Rasterize.debug)
            {
               logPixel(CLIPPED, x, y, x_vp, y_vp, vp);
            }
         }// Advance (x,y) to the next pixel. Since delta_y = 1, we need delta_x = m.
      }
   }



   // Private default constructor to enforce noninstantiable class.
   // See Item 4 in "Effective Java", 3rd Ed, Joshua Bloch.
   private Rasterize_Clip_Line() {
      throw new AssertionError();
   }
}
