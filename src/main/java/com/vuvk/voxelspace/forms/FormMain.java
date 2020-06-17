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
package com.vuvk.voxelspace.forms;

import com.vuvk.Utils;
import com.vuvk.voxelspace.Camera;
import com.vuvk.voxelspace.Global;
import com.vuvk.voxelspace.Map;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Anton "Vuvk" Shcherbatykh
 */
public class FormMain extends javax.swing.JFrame {
    
    public static FormMain INSTANCE;

    Camera camera;
    Map map;
    int[] yHeights;
    //int[] ySky;
    
    Robot mouseManager;
    Point mouseLocation;

    BufferedImage screen;
    Graphics screenGraphics;
    DataBuffer screenBuffer;
    
    private class RenderTask implements Runnable {
        CountDownLatch latch;
        final double angleFrom, angleTo;
        final int xFrom, xTo;
        
        RenderTask(final String name, final double angleFrom, final double angleTo, final int xFrom, final int xTo) {
            this.angleFrom = angleFrom;
            this.angleTo   = angleTo;
            this.xFrom = xFrom;
            this.xTo   = xTo;
            //setName(name);
        }
        
        public void setLatch(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            render(angleFrom, angleTo, xFrom, xTo/*, 0xFFFF0000*/);
            latch.countDown();
        }
    }
        
    private final RenderTask[] RENDER_TASKS = new RenderTask[Global.THREADS_COUNT];
    private final ExecutorService EXECUTOR = Executors.newFixedThreadPool(Global.THREADS_COUNT);

    private void initScreen() {        
        screen = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        screenGraphics = screen.getGraphics();
        screenBuffer = screen.getRaster().getDataBuffer();        
        yHeights = new int[getWidth()]; 
        
        mouseLocation = new Point(screen.getWidth() >> 1, screen.getHeight() >> 1);
    }
    
    private void initThreads() {    
        int xStep = screen.getWidth() / Global.THREADS_COUNT;
        int xStart = 0;
        double angleStep = camera.getFov() / Global.THREADS_COUNT;
        double halfFov = camera.getFov() * 0.5;
        double angleStart = -halfFov;
        
        for (int i = 0; i < Global.THREADS_COUNT; ++i, xStart += xStep, angleStart += angleStep) {
            int xFrom = xStart;
            int xTo   = xFrom + xStep;   
            double angleFrom = angleStart;
            double angleTo   = angleStart + angleStep;
            if (i == Global.THREADS_COUNT - 1) {
                xTo = screen.getWidth();
                angleTo = halfFov;
            }
            
            RENDER_TASKS[i] = new RenderTask("render_task " + i, angleFrom, angleTo, xFrom, xTo);
        }
    }
    
    /**
     * Creates new form FrmMain
     */
    public FormMain() {        
        try {
            this.mouseManager = new Robot();
        } catch (AWTException ex) {
            Logger.getLogger(FormMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        initComponents();

        setLocationRelativeTo(null);

        camera = new Camera(256, 256, 50, 66, 0, -240, 512);
        camera.enableGravity(true);
        map = new Map();
        map.load("C1W.png", "D1.png");

        initScreen(); 
        initThreads();

        getRootPane().addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                initScreen();
                initThreads();
            }
        });
        
        INSTANCE = this;
        
        new FormSettings().setVisible(true);
    }
    
    public Camera getCamera() {
        return camera;
    }
    
    public Map getMap() {
        return map;
    }

    void clear(int color) {
        for (int i = 0; i < screenBuffer.getSize(); ++i) {
            screenBuffer.setElem(i, color);
        }
    }
    
    private int addColor(int colorSrc, int colorDst, double power) {
        if (power <= 0.05) {
            return colorSrc;
        } else if (power >= 0.95) {
            return colorDst;
        } else {
            double brS = 1.0 - power;
            double brD = power;
            
            int rS = (int)(((colorSrc >> 16) & 0xFF) * brS);
            int gS = (int)(((colorSrc >>  8) & 0xFF) * brS);
            int bS = (int)(((colorSrc      ) & 0xFF) * brS);
            
            int rD = (int)(((colorDst >> 16) & 0xFF) * brD);
            int gD = (int)(((colorDst >>  8) & 0xFF) * brD);
            int bD = (int)(((colorDst      ) & 0xFF) * brD);
            
            int r = (rS + rD);// % 255;
            int g = (gS + gD);// % 255;
            int b = (bS + bD);// % 255;
            
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    void drawVerticalLine(int x, int ytop, int ybottom/*int height*/, int color) {
        if (ytop > ybottom || ytop + ybottom <= 0) {
            return;
        }

        int scrWidth  = screen.getWidth();
        int scrHeight = screen.getHeight();

        x = x < 0 ? 0 : x;
        x = x >= scrWidth ? scrWidth - 1 : x;

        ytop = ytop < 0 ? 0 : ytop;
        ytop = ytop >= scrHeight ? scrHeight - 1 : ytop;

        ybottom = ybottom < 0 ? 0 : ybottom;
        ybottom = ybottom >= scrHeight ? scrHeight - 1 : ybottom;

        int offset = ytop * scrWidth + x;
        for (int y = ytop; y < ybottom; ++y, offset += scrWidth) {
            screenBuffer.setElem(offset, color);
        }
    }

    void render(double angleFrom, double angleTo, int xFrom, int xTo) {
        int mapWidthPeriod  = Map.WIDTH - 1;
        int mapHeightPeriod = Map.HEIGHT - 1;

        //int screenWidth  = screen.getWidth();
        int screenHeight = screen.getHeight();
        int renderWidth  = xTo - xFrom;

        int[][] cMap = map.getColorMap();
        int[][] hMap = map.getHeightMap();

        for (int x = xFrom; x < xTo; ++x) {
            yHeights[x] = screenHeight;
        }
        
        int skyHeight;

        double sx, sy;
        double cx = camera.getX(),
               cy = camera.getY();
        
        double angleRay = camera.getAngle() + angleFrom;
        double correctAngle = angleFrom;
        double angleStep = (angleTo - angleFrom) / renderWidth;
        double distance = camera.getDistance();
        double cameraHeight = camera.getHeight();
        double horizon = camera.getHorizon();
        
        double fogFactor = 1.0 / (Global.fogEnd - Global.fogStart);

        for (int x = xFrom; x < xTo; ++x, angleRay += angleStep, correctAngle += angleStep) {;  
            sx = Utils.sin(angleRay);  
            sy = Utils.cos(angleRay);
            
            // correct length of ray
            double correct = 1.0 / Utils.cos(correctAngle);
            sx *= correct;
            sy *= correct;
            
            skyHeight = screenHeight;
            
            for (double dz = 0.1, z = 1.0; z < distance; z += dz, dz += Global.detalization) {
                int px = (int)(cx + sx * z),
                    py = (int)(cy + sy * z);

                while (px < 0) { px += Map.WIDTH;  }
                while (py < 0) { py += Map.HEIGHT; }
                if (px >= Map.WIDTH ) { px %= mapWidthPeriod;  }
                if (py >= Map.HEIGHT) { py %= mapHeightPeriod; }

                int heightOnScreen = (int)((cameraHeight - hMap[px][py]) * 240 / z + horizon);
                if (heightOnScreen < 0) {
                    heightOnScreen = -heightOnScreen;
                }

                int color = cMap[px][py];
                
                if (Global.fogEnabled) {
                    if (z >= Global.fogStart) {
                        if (z < Global.fogEnd) {
                            double power = (z - Global.fogStart) * fogFactor;
                            color = addColor(color, Global.fogColor, power);
                        } else {
                            color = Global.fogColor;
                        }
                    }
                }
                
                drawVerticalLine(x, heightOnScreen, (int)yHeights[x], color);
                if (heightOnScreen < yHeights[x]) {
                    yHeights[x] = heightOnScreen;
                }
                
                if (heightOnScreen < skyHeight) {
                    skyHeight = heightOnScreen;
                }
                
                // all height filled - stop ray
                if (skyHeight == 0) {
                    break;
                }
            }
            
            // draw sky
            drawVerticalLine(x, 0, skyHeight, Global.skyColor);
        }
    }

    @Override
    public void paint(Graphics g) {
        Global.update();
        camera.update();

        if (camera.isEnableGravity()) {
            camera.setMoveDown(true);
        }
        
        int cX = (int) camera.getX(),
            cY = (int) camera.getY();
        int minHeight = map.getHeightMap()[cX][cY] - 7;
        if (camera.getHeight() > minHeight) {
            camera.setHeight(minHeight);
        }
        
        // WORLD RENDERING
        if (Global.multithreading) {
            CountDownLatch cdl = new CountDownLatch(RENDER_TASKS.length);
            for (RenderTask task : RENDER_TASKS) {
                task.setLatch(cdl);
                EXECUTOR.execute(task);
            }
            
            while (cdl.getCount() > 0) {
                Thread.yield();
            }
        } else {
            double halfFov = camera.getFov() * 0.5;
            render(-halfFov, halfFov, 0, screen.getWidth());
        }
        

        screenGraphics.setColor(Color.WHITE);
        screenGraphics.drawString("FPS : " + Global.fps, 30, 50);  
        
        g.drawImage(screen, 0, 0, null);
        
        if (camera.isEnableMouseLook()) {
            Point center = new Point(screen.getWidth()  >> 1, screen.getHeight() >> 1);
            Point wndLoc = getLocation();

            int mouseDX = mouseLocation.x - center.x;
            int mouseDY = mouseLocation.y - center.y;        

            if (mouseDX != 0) {
                camera.rotate(5 * mouseDX * Global.deltaTime);

                mouseLocation = center;
                mouseManager.mouseMove(center.x + wndLoc.x, center.y + wndLoc.y);
            }  

            if (mouseDY != 0) {
                camera.pitch(50 * mouseDY * Global.deltaTime);

                mouseLocation = center;
                mouseManager.mouseMove(center.x + wndLoc.x, center.y + wndLoc.y);
            }
        }
        
        repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("swinger VoxelSpace");
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                formMouseMoved(evt);
            }
        });
        addWindowStateListener(new java.awt.event.WindowStateListener() {
            public void windowStateChanged(java.awt.event.WindowEvent evt) {
                formWindowStateChanged(evt);
            }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                formKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 640, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 548, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_LEFT :
                camera.setRotL(true);
                camera.setRotR(false);
                break;

            case KeyEvent.VK_RIGHT :
                camera.setRotL(false);
                camera.setRotR(true);
                break;

            case KeyEvent.VK_W :
            case KeyEvent.VK_UP :
                camera.setMoveF(true);
                camera.setMoveB(false);
                break;

            case KeyEvent.VK_S :
            case KeyEvent.VK_DOWN :
                camera.setMoveF(false);
                camera.setMoveB(true);
                break;                
                
            case KeyEvent.VK_D :
                camera.setMoveR(true);
                camera.setMoveL(false);
                break;
                
            case KeyEvent.VK_A :
                camera.setMoveR(false);
                camera.setMoveL(true);
                break;

            case KeyEvent.VK_Q :
            case KeyEvent.VK_PAGE_UP :
                camera.setMoveUp(true);
                camera.setMoveDown(false);
                break;

            case KeyEvent.VK_Z :
            case KeyEvent.VK_PAGE_DOWN :
                camera.setMoveUp(false);
                camera.setMoveDown(true);
                break;

            case KeyEvent.VK_E :
            case KeyEvent.VK_HOME :
                camera.setLookUp(true);
                camera.setLookDown(false);
                break;

            case KeyEvent.VK_C :
            case KeyEvent.VK_END :
                camera.setLookUp(false);
                camera.setLookDown(true);
                break;

            case KeyEvent.VK_EQUALS :
            case KeyEvent.VK_INSERT :
                Global.detalization += Global.deltaTime;
                if (Global.detalization > 0.15) {
                    Global.detalization = 0.15;
                }
                break;

            case KeyEvent.VK_MINUS :
            case KeyEvent.VK_DELETE :
                Global.detalization -= Global.deltaTime;
                if (Global.detalization < 0.0001) {
                    Global.detalization = 0.0001;
                }
                break;
                
            case KeyEvent.VK_ESCAPE :
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));  
                break;
        }
    }//GEN-LAST:event_formKeyPressed

    private void formKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyReleased
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_LEFT :
                camera.setRotL(false);
                break;

            case KeyEvent.VK_RIGHT :
                camera.setRotR(false);
                break;

            case KeyEvent.VK_W :
            case KeyEvent.VK_UP :
                camera.setMoveF(false);
                break;

            case KeyEvent.VK_S :
            case KeyEvent.VK_DOWN :
                camera.setMoveB(false);
                break;
                
            case KeyEvent.VK_D :
                camera.setMoveR(false);
                break;
                
            case KeyEvent.VK_A :
                camera.setMoveL(false);
                break;

            case KeyEvent.VK_Q :
            case KeyEvent.VK_PAGE_UP :
                camera.setMoveUp(false);
                break;

            case KeyEvent.VK_Z :
            case KeyEvent.VK_PAGE_DOWN :
                camera.setMoveDown(false);
                break;

            case KeyEvent.VK_E :
            case KeyEvent.VK_HOME :
                camera.setLookUp(false);
                break;

            case KeyEvent.VK_C :
            case KeyEvent.VK_END :
                camera.setLookDown(false);
                break;                

            case KeyEvent.VK_M :
                camera.enableMouseLook(!camera.isEnableMouseLook());
                break;           

            case KeyEvent.VK_G :
                camera.enableGravity(!camera.isEnableGravity());
                break;
        }
    }//GEN-LAST:event_formKeyReleased

    private void formWindowStateChanged(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowStateChanged

    }//GEN-LAST:event_formWindowStateChanged

    private void formMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseMoved
        mouseLocation = evt.getPoint();
    }//GEN-LAST:event_formMouseMoved

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(FormMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(FormMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(FormMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(FormMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new FormMain().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
