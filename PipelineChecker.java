package renderer.pipelineGL;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.glu.*;

public class PipelineChecker 
{
    public static boolean CheckOpenGLError(GL4 gl)
    {
        GLU glu = new GLU(); 
        boolean foundErr = false;

        for(int glErr = gl.glGetError(); glErr != gl.GL_NO_ERROR; glErr = gl.glGetError())
        {
            System.err.println("GL Error: " + glu.gluErrorString(glErr));
            foundErr = true;
        }

        if(!foundErr)
            System.out.println("No GL Error"); 

        return foundErr; 
    }

    public static void printShaderLog(GL4 gl, int shaderID)
    {
        int[] logSize = new int[1]; 
        int[] chWrittn = new int[1]; 
        byte[] logMsg; 

        gl.glGetShaderiv(shaderID, gl.GL_INFO_LOG_LENGTH, logSize, 0); 

        System.out.println("Shader info has " + logSize[0] + " characters"); 

        if(logSize[0] > 0)
        {
            logMsg = new byte[logSize[0]]; 
            
            gl.glGetShaderInfoLog(shaderID, logSize[0], chWrittn, 0, logMsg, 0);

            for(int c = 0; c < logMsg.length; c += 1)
                System.out.print((char)logMsg[c]); 
        }
        else
            System.out.println("Shader " + shaderID + " has no info log."); 
    }

    public static void printProgramLog(GL4 gl, int programID)
    {
        int[] logSize = new int[1]; 
        int[] chWrittn = new int[1]; 
        byte[] logMsg; 

        gl.glGetShaderiv(programID, gl.GL_INFO_LOG_LENGTH, logSize, 0); 
        if(logSize[0] > 0)
        {
            logMsg = new byte[logSize[0]]; 
            
            gl.glGetProgramInfoLog(programID, logSize[0], chWrittn, 0, logMsg, 0);

            System.out.println("Program " + programID + " info log: \n"); 
            for(int c = 0; c < logMsg.length; c += 1)
                System.out.print((char)logMsg[c]); 
        }
        else
            System.out.println("Program " + programID + " has no info log."); 
    }

    public static boolean shaderCompiled(GL4 gl, int shaderID)
    {
        int[] shaderComp = new int[1]; 
        gl.glGetProgramiv(shaderID, gl.GL_COMPILE_STATUS, shaderComp, 0); 

        return shaderComp[0] == 1 ? true : false;  
    }

    public static boolean programLinked(GL4 gl, int programID)
    {
        int[]  programLink = new int[1]; 
        gl.glGetProgramiv(programID, gl.GL_COMPILE_STATUS, programLink, 0); 

        return programLink[0] == 1 ? true : false;  
    }

    // Private default constructor to enforce noninstantiable class.
    // See Item 4 in "Effective Java", 3rd Ed, Joshua Bloch.
    private PipelineChecker() 
    {
        throw new AssertionError();
    }
}    

