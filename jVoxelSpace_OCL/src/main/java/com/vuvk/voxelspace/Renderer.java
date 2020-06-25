/**
    Copyright 2020 Anton "Vuvk" Shcherbatykh

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.vuvk.voxelspace;

import com.vuvk.JOCLSample;
import com.vuvk.voxelspace.forms.FormMain;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import org.jocl.CL;
import static org.jocl.CL.CL_CONTEXT_PLATFORM;
import static org.jocl.CL.CL_DEVICE_TYPE_ALL;
import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.CL_MEM_WRITE_ONLY;
import static org.jocl.CL.CL_MEM_READ_WRITE;
import static org.jocl.CL.CL_MEM_USE_HOST_PTR;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueueWithProperties;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clReleaseProgram;
import static org.jocl.CL.clSetKernelArg;
import org.jocl.NativePointerObject;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;
import org.jocl.cl_queue_properties;

/**
 *
 * @author Anton "Vuvk" Shcherbatykh
 */
public class Renderer {
    //DataBuffer buffer;
    int[] color_map;
    int[] height_map;
    int[] output_image;
    Camera camera;
    float[] camera_params = new float[9];
    int[] global_params = new int[10];

    Pointer p_color_map;
    Pointer p_height_map;
    Pointer p_output_image;
    Pointer p_camera_params;
    Pointer p_global_params;

    // The platform, device type and device number
    // that will be used
    int platformIndex = 0;
    //final long deviceType = CL_DEVICE_TYPE_ALL;
    final long deviceType = CL.CL_DEVICE_TYPE_GPU;
    int deviceIndex = 0;

    // Obtain the number of platforms
    int[] numPlatformsArray;
    int numPlatforms;

    // Obtain a platform ID
    cl_platform_id[] platforms;
    cl_platform_id platform;

    // Initialize the context properties
    cl_context_properties contextProperties;

    // Obtain the number of devices for the platform
    int[] numDevicesArray;
    int numDevices;

    // Obtain a device ID 
    cl_device_id[] devices;
    cl_device_id device;

    // Create a context for the selected device
    cl_context context;

    // Create a command-queue for the selected device
    cl_queue_properties properties;
    cl_command_queue commandQueue;

    // Allocate the memory objects for the input- and output data
    cl_mem srcMemColorMap;
    cl_mem srcMemHeightMap;
    cl_mem dstMemOutputImage;
    cl_mem srcMemCameraParams;
    cl_mem srcMemGlobalParams;

    // Create the program from the source code
    cl_program program;

    // Create the kernel
    cl_kernel kernel;
        
    // Set the work-item dimensions
    final int n = 640;
    long[] global_work_size = new long[]{n};   
        
    public void init() throws URISyntaxException, IOException {         
        InputStream is = getClass().getResourceAsStream("/opencl/render.cl");
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        String lines = new String(buffer);       
        String[] programSource = new String[] { lines };
        
        FormMain frmMain = FormMain.INSTANCE;
        //DataBuffer buffer = frmMain.getScreenBuffer();
        color_map  = frmMain.getMap().get_color_map ();
        height_map = frmMain.getMap().get_height_map();
        //output_image = new int[frmMain.getWidth() * frmMain.getHeight()]; 
        output_image = ((DataBufferInt)frmMain.getScreen().getRaster().getDataBuffer()).getData();
        camera = frmMain.getCamera();
        
        p_color_map    = Pointer.to(color_map);
        p_height_map   = Pointer.to(height_map);
        p_output_image = Pointer.to(output_image);
        p_camera_params= Pointer.to(camera_params);
        p_global_params= Pointer.to(global_params);

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        numPlatformsArray = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        platforms = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        platform = platforms[platformIndex];

        // Initialize the context properties
        contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
        
        // Obtain the number of devices for the platform
        numDevicesArray = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        numDevices = numDevicesArray[0];
        
        // Obtain a device ID 
        devices = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        device = devices[deviceIndex];
        
        // Create a context for the selected device
        context = clCreateContext(
            contextProperties, 1, new cl_device_id[]{device}, 
            null, null, null);
        
        // Create a command-queue for the selected device
        properties = new cl_queue_properties();
        commandQueue = clCreateCommandQueueWithProperties(
            context, device, properties, null);        
        
        // Create the program from the source code
        program = clCreateProgramWithSource(context,
            1, programSource, null, null);
        
        // Allocate the memory objects for the input- and output data
        /*srcMemCameraParams = clCreateBuffer(context, 
            CL_MEM_READ_ONLY | CL_MEM_USE_HOST_PTR,
            Sizeof.cl_float * camera_params.length, p_camera_params, null);
        srcMemGlobalParams = clCreateBuffer(context, 
            CL_MEM_READ_ONLY | CL_MEM_USE_HOST_PTR,
            Sizeof.cl_int * global_params.length, p_global_params, null);*/
        srcMemColorMap = clCreateBuffer(context, 
            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
            Sizeof.cl_int * color_map.length, p_color_map, null);
        srcMemHeightMap = clCreateBuffer(context, 
            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
            Sizeof.cl_int * height_map.length, p_height_map, null);
        dstMemOutputImage = clCreateBuffer(context, 
            CL_MEM_WRITE_ONLY | CL_MEM_USE_HOST_PTR,
            Sizeof.cl_int * output_image.length, p_output_image, null);
        
        // Build the program
        clBuildProgram(program, 0, null, null, null, null);
        
        // Create the kernel
        kernel = clCreateKernel(program, "render", null);  
        
        // Set the arguments for the kernel
        /*int a = 0;
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(srcMemColorMap));
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(srcMemHeightMap));
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(dstMemOutputImage));
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(srcMemCameraParams));
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(srcMemGlobalParams));   */
    }
    
    public void deinit() {        
        // Release kernel, program, and memory objects
        clReleaseMemObject(srcMemColorMap);
        clReleaseMemObject(srcMemHeightMap);
        clReleaseMemObject(dstMemOutputImage);
        clReleaseMemObject(srcMemCameraParams);
        clReleaseMemObject(srcMemGlobalParams);
        
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);        
    }
    
    public void render() throws URISyntaxException, IOException {
        
        long start = System.currentTimeMillis();
        
        FormMain frmMain = FormMain.INSTANCE;
        
        camera_params[0] = (float) camera.getX();
        camera_params[1] = (float) camera.getY();
        camera_params[2] = (float) camera.getTarget().x;
        camera_params[3] = (float) camera.getTarget().y;
        camera_params[4] = (float) camera.getAngle();
        camera_params[5] = (float) camera.getDistance();
        camera_params[6] = (float) camera.getFov();
        camera_params[7] = (float) camera.getHeight();
        camera_params[8] = (float) camera.getHorizon();
                
        global_params[0] = Map.WIDTH;
        global_params[1] = Map.HEIGHT;
        global_params[2] = frmMain.getWidth();
        global_params[3] = frmMain.getHeight();
        global_params[4] = (Global.fogEnabled) ? 1 : 0;
        global_params[5] = Global.fogColor;
        global_params[6] = (int) Global.fogStart;
        global_params[7] = (int) Global.fogEnd;
        global_params[8] = Global.skyColor;
        global_params[9] = (int)(Global.detalization * 10000);
        

        // Allocate the memory objects for the input- and output data
        /*srcMemColorMap = clCreateBuffer(context, 
            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
            Sizeof.cl_int * color_map.length, p_color_map, null);
        srcMemHeightMap = clCreateBuffer(context, 
            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
            Sizeof.cl_int * height_map.length, p_height_map, null);
        dstMemOutputImage = clCreateBuffer(context, 
            CL_MEM_WRITE_ONLY | CL_MEM_COPY_HOST_PTR,
            Sizeof.cl_int * output_image.length, p_output_image, null);*/
        
        srcMemCameraParams = clCreateBuffer(context, 
            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
            Sizeof.cl_float * camera_params.length, p_camera_params, null);
        srcMemGlobalParams = clCreateBuffer(context, 
            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
            Sizeof.cl_int * global_params.length, p_global_params, null);
                
        // Set the arguments for the kernel
        int a = 0;
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(srcMemColorMap));
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(srcMemHeightMap));
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(dstMemOutputImage));
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(srcMemCameraParams));
        clSetKernelArg(kernel, a++, Sizeof.cl_mem, Pointer.to(srcMemGlobalParams)); 
        
        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
            global_work_size, null, 0, null, null);
        
        // Read the output data
        CL.clEnqueueReadBuffer(commandQueue, dstMemOutputImage, true, 0,
            output_image.length * Sizeof.cl_int, p_output_image, 0, null, null);
        //clEnqueueReadBuffer(commandQueue, dstMem, CL_TRUE, 0,
        //    n * Sizeof.cl_float, dst, 0, null, null);
        
        // Verify the result
        /*boolean passed = true;
        final float epsilon = 1e-7f;
        for (int i=0; i<n; i++)
        {
            float x = dstArray[i];
            float y = srcArrayA[i] * srcArrayB[i];
            boolean epsilonEqual = Math.abs(x - y) <= epsilon * Math.abs(x);
            if (!epsilonEqual)
            {
                passed = false;
                break;
            }
        }
        System.out.println("Test "+(passed?"PASSED":"FAILED"));
        if (n <= 10)
        {
            System.out.println("Result: "+Arrays.toString(dstArray));
        }*/
        
        //System.out.println("time elapsed = " + (System.currentTimeMillis() - start) + " ms");
        
        
        //BufferedImage out = new BufferedImage(frmMain.getWidth(), frmMain.getHeight(), BufferedImage.TYPE_INT_ARGB);
        /*for (int i = 0; i < out.getRaster().getDataBuffer().getSize(); ++i) {
            out.getRaster().getDataBuffer().setElem(i, output_image[i]);
        }*/
        //int[] raster = ((DataBufferInt)out.getRaster().getDataBuffer()).getData();
        int[] raster = ((DataBufferInt)frmMain.getScreenBuffer()).getData();
        System.arraycopy(output_image, 0, raster, 0, raster.length);
        //ImageIO.write(out, "PNG", new File("output.png"));
        
        /*for (int i = 0; i < buffer.getSize(); ++i) {
            buffer.setElem(i, output_image[i]);
        }*/
        
        //frmMain.dispose();
        //clReleaseMemObject(srcMemColorMap);
        //clReleaseMemObject(srcMemHeightMap);
        //clReleaseMemObject(dstMemOutputImage);
        clReleaseMemObject(srcMemCameraParams);
        clReleaseMemObject(srcMemGlobalParams);
    }
}
