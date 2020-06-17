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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Anton "Vuvk" Shcherbatykh
 */
public class Map {
    public final static int WIDTH  = 512,
                            HEIGHT = 512,
                            SHIFT  = 10;  // power of two
    private int[][] colorMap,
                    heightMap;

    public int[][] getColorMap() {
        return colorMap;
    }

    public int[][] getHeightMap() {
        return heightMap;
    }
    
    private BufferedImage resizeImage(BufferedImage src, int newWidth, int newHeight) {
        BufferedImage bmp = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        bmp.getGraphics().drawImage(src, 0, 0, newWidth, newHeight, null);
        return bmp;
    }
    
    public void load(String colorMapName, String heightMapName) {
        BufferedImage cMap, hMap;
        try {
            cMap = ImageIO.read(getClass().getResource("/maps/" + colorMapName));
            hMap = ImageIO.read(getClass().getResource("/maps/" + heightMapName));  
            
            if (cMap.getWidth() != WIDTH || cMap.getHeight() != HEIGHT) {
                cMap = resizeImage(cMap, WIDTH, HEIGHT);
            }
            
            if (hMap.getWidth() != WIDTH || hMap.getHeight() != HEIGHT) {
                hMap = resizeImage(hMap, WIDTH, HEIGHT);
            }
        
            colorMap  = new int[WIDTH][HEIGHT];
            heightMap = new int[WIDTH][HEIGHT];

            for (int x = 0; x < WIDTH; ++x) {
                for (int y = 0; y < HEIGHT; ++y) {
                    colorMap [x][y] = cMap.getRGB(x, y);
                    
                    int height = hMap.getRGB(x, y);
                    //height >>= 16;
                    height &= 0x000000FF;
                    height = 255 - height;
                    
                    heightMap[x][y] = height;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Map.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
