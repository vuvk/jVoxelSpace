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

/**
 *
 * @author Anton "Vuvk" Shcherbatykh
 */
public final class Global {
    
    public final static int THREADS_COUNT = Runtime.getRuntime().availableProcessors() + 1;
    
    public static boolean fogEnabled = true;
    public static float fogStart = 10.0f;
    public static float fogEnd   = 100.0f;
    public static int fogColor = 0xFFCCCCFF;
    public static int skyColor = 0xFF9999FF;
    public static float detalization = 0.0025f;
    public static boolean multithreading = true;
    public static float deltaTime = 0;    
    public static long curTick;
    public static long lastTick;
    public static int fps = 0;
    private static int frames = 0;
    private static float fpsDelay;
    
    public static void update() {
        curTick = System.currentTimeMillis();
        
        deltaTime = (curTick - lastTick) / 1000.0f;
        
        if (fpsDelay < 1.0) {
            fpsDelay += deltaTime;
            ++frames;
        } else {
            fpsDelay = 0.0f;
            fps = frames;
            frames = 0;
        }
                
        lastTick = curTick;
    }
    
    private Global() {}
}
