import java.awt.Color;

import renderer.framebuffer.*;

import renderer.scene.primitives.*;
import renderer.scene.*;

//import renderer.pipeline.*;
import renderer.pipelineGL.*;

public class cubeTest 
{
    public static void main(String[] args)
    {
        Model mod = new Model(); 
        Position pos = new Position(mod); 
        Scene scene = new Scene(); 
        FrameBuffer fb = new FrameBuffer(500, 500, Color.black); 

        mod.addVertex(new Vertex( -1,  -1, 0), 
                      new Vertex( -1, 1, 0), 
                      new Vertex(1, 1, 0), 
                      new Vertex(1,  -1, 0));

        mod.addPrimitive(new LineSegment(0, 1), 
                         new LineSegment(1, 2), 
                         new LineSegment(2, 3), 
                         new LineSegment(3, 0));     
                         
        pos.translate(0, 0, -5); 

        scene.addPosition(pos);
        Camera.projPerspective(); 

        fb.clearFB();
        Pipeline.render(scene, fb); 

        fb.dumpFB2File("cubeTestGL.ppm");

    }    
}
