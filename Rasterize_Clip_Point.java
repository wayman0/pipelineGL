/*
 * Renderer 1. The MIT License.
 * Copyright (c) 2022 rlkraft@pnw.edu
 * See LICENSE for details.
*/

package renderer.pipeline;

import renderer.scene.*;
import renderer.scene.primitives.Point;
import renderer.framebuffer.*;
import static renderer.pipeline.PipelineLogger.*;

import java.awt.Color;

/**
   Rasterize a projected {@link Point} into pixels
   in a {@link FrameBuffer.Viewport}, but (optionally)
   do not rasterize any part of the {@link Point} that
   is not contained in the {@link Camera}'s view rectangle.
*/
public class Rasterize_Clip_Point
{
   /**
      Rasterize a {@link Point} into pixels
      in a {@link FrameBuffer.Viewport}.

      @param model  {@link Model} that the {@link Point} {@code pt} comes from
      @param pt     {@code Point} to rasterize into the {@code FrameBuffer.Viewport}
      @param vp     {@link FrameBuffer.Viewport} to hold rasterized pixels
   */
   public static void rasterize(final Model model,
                                final Point pt,
                                final FrameBuffer.Viewport vp)
   {
      final String     CLIPPED = "Clipped: ";
      final String NOT_CLIPPED = "         ";

      final Color c = Color.white;

      final int w = vp.getWidthVP();
      final int h = vp.getHeightVP();

      final int vIndex = pt.vIndexList.get(0);
      final Vertex v = model.vertexList.get(vIndex);

      // Round the point's coordinates to the nearest logical pixel.
      final double x = Math.round( v.x );
      final double y = Math.round( v.y );

      final int radius = pt.radius;

      for (int y_ = (int)y - radius; y_ <= (int)y + radius; ++y_)
      {
         for (int x_ = (int)x - radius; x_ <= (int)x + radius; ++x_)
         {
            if (Rasterize.debug)
            {
               final String clippedMessage;
               if ( ! Rasterize.doClipping
                 || (x_ > 0 && x_ <= w && y_ > 0 && y_ <= h) ) // clipping test
               {
                  clippedMessage = NOT_CLIPPED;
               }
               else
               {
                  clippedMessage = CLIPPED;
               }
               logPixel(clippedMessage, x, y, x_ - 1, h - y_, vp);
            }
            // Log the pixel before setting it so that an array out-
            // of-bounds error will be right after the pixel's address.

            if ( ! Rasterize.doClipping
              || (x_ > 0 && x_ <= w && y_ > 0 && y_ <= h) ) // clipping test
            {
               vp.setPixelVP(x_ - 1, h - y_, Color.white);
            }
         }
      }
   }



   // Private default constructor to enforce noninstantiable class.
   // See Item 4 in "Effective Java", 3rd Ed, Joshua Bloch.
   private Rasterize_Clip_Point() {
      throw new AssertionError();
   }
}
