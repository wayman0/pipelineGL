package renderer.pipelineGL;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.*;
import com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.common.nio.Buffers;

public class TestTransformFeedback 
{
    public static void main(String[] args)
    {
        GLProfile glProf = GLProfile.get("GL4");
        GLCapabilities glCap  = new GLCapabilities(glProf);

        glCap.setPBuffer(true);              // enable the use of pbuffers
        glCap.setDoubleBuffered(false);

        GLDrawableFactory glFact = GLDrawableFactory.getFactory(glProf);
        GLOffscreenAutoDrawable glPixelBuffer = glFact.createOffscreenAutoDrawable(null, glCap, null, 100, 100); // create the pbuffer to be the vp width x vp height
        glPixelBuffer.display();
        glPixelBuffer.getContext().makeCurrent(); // make this pbuffer current
        GL4 gl = glPixelBuffer.getGL().getGL4(); // get the gl object associated with the pbuffer

        String[] vertexCode = {
                                "layout (location=0) in vec4 inValue;\n", 
                                "layout (location=1) out vec4 outValue;\n",
                                "void main()\n",
                                "{\n",
                                "outValue.x = sqrt(inValue.x);\n",
                                "outValue.y = sqrt(inValue.y);\n",
                                "outValue.z = sqrt(inValue.z);\n",
                                "outValue.w = sqrt(inValue.w); \n",
                                "}\n"    
                              };

        int shader = gl.glCreateShader(GL4.GL_VERTEX_SHADER);
        gl.glShaderSource(shader, vertexCode.length, vertexCode, null);
        gl.glCompileShader(shader);

        int program = gl.glCreateProgram();
        gl.glAttachShader(program, shader);

        String[] feedbackVaryings = { "outValue" };
        gl.glTransformFeedbackVaryings(program, feedbackVaryings.length, feedbackVaryings, GL4.GL_INTERLEAVED_ATTRIBS);


        gl.glLinkProgram(program);
        gl.glUseProgram(program);
        
        int[] vao = new int[1];
        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glBindVertexArray(vao[0]);

        float[] dataArray = { 1.0f, 2.0f, 3.0f, 1.0f, 
                              4.0f, 5.0f, 6.0f, 1.0f, 
                              7.0f, 8.0f, 9.0f, 1.0f, 
                            };

        int[] vbo = new int[1];
        gl.glGenBuffers(vbo.length, vbo, 0);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo[0]);
        FloatBuffer data = Buffers.newDirectFloatBuffer(dataArray);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, data.limit() * Buffers.SIZEOF_FLOAT, data, GL4.GL_STATIC_DRAW);
        
        int inputAttrib = gl.glGetAttribLocation(program, "inValue");
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 4, GL4.GL_FLOAT, false, 0, 0);

        int[] indexArray = {0, 1, 0, 2}; 
        int[] vbo2 = new int[1]; 
        gl.glGenBuffers(vbo2.length, vbo2, 0);
        gl.glBindBuffer(GL4.GL_ELEMENT_ARRAY_BUFFER, vbo2[0]); 
        IntBuffer index = Buffers.newDirectIntBuffer(indexArray);
        gl.glBufferData(GL4.GL_ELEMENT_ARRAY_BUFFER, index.limit() * Buffers.SIZEOF_INT, index, GL4.GL_STATIC_DRAW); 

        int[] tbo = new int[1];
        gl.glGenBuffers(tbo.length, tbo, 0);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, tbo[0]);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, data.limit()*Buffers.SIZEOF_FLOAT, null, GL4.GL_STATIC_READ);
       
        gl.glEnable(GL4.GL_RASTERIZER_DISCARD);
        
        gl.glBindBufferBase(GL4.GL_TRANSFORM_FEEDBACK_BUFFER, 0, tbo[0]);
        
        gl.glBeginTransformFeedback(GL4.GL_LINES);

        gl.glDrawElements(GL4.GL_LINES, index.limit(), GL4.GL_UNSIGNED_INT, 0);

        gl.glEndTransformFeedback();

        gl.glFlush();

        float[] feedbackArr = new float[dataArray.length];
        FloatBuffer feedback = Buffers.newDirectFloatBuffer(feedbackArr);
        gl.glGetBufferSubData(GL4.GL_TRANSFORM_FEEDBACK_BUFFER, 0, feedback.limit() * Buffers.SIZEOF_FLOAT, feedback);

        for(int i = 0; i < feedback.limit(); i += 1)
            System.out.println(String.format("%f", feedback.get(i)));
    }
}
