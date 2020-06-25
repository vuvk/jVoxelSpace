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

import com.vuvk.Utils;
import java.awt.Point;
import java.awt.geom.Point2D;

/**
 *
 * @author Anton "Vuvk" Shcherbatykh
 */
public class Camera {
    
    public final static double MOVE_SPEED = 50.0;
    public final static double ROT_SPEED  = 90.0;
    
    /** position on the map */
    final private Point2D.Double position = new Point.Double();
    final private Point2D.Double target   = new Point.Double();
    
    private double fov,
                   height,   // height of the camera
                   angle,    // direction of the camera
                   horizon,  // horizon position (look up and down)
                   distance; // distance of map
    
    private boolean rotL = false,
                    rotR = false,
                    moveF = false,
                    moveB = false,
                    moveL = false,
                    moveR = false,
                    moveUp   = false,
                    moveDown = false,
                    lookUp   = false,
                    lookDown = false;
    
    private boolean enableGravity = false;
    private boolean enableMouseLook = false;
    
    public Camera(double x, double y, double fov, double height, double angle, double horizon, double distance) {
        setPosition(x, y);
        this.fov = fov;
        setHeight(height);
        setAngle(angle);
        setHorizon(horizon);
        setDistance(distance);
    }
    
    public double getX() {
        return position.x;
    }
    
    public double getY() {
        return position.y;
    }

    public Point2D.Double getPosition() {
        return position;
    }

    public Point2D.Double getTarget() {
        return target;
    }

    public double getFov() {
        return fov;
    }
    
    public double getHeight() {
        return height;
    }

    public boolean isEnableGravity() {
        return enableGravity;
    }

    public boolean isEnableMouseLook() {
        return enableMouseLook;
    }
    
    public double getAngle() {
        return angle;
    }
    
    public double getHorizon() {
        return horizon;
    }

    public double getDistance() {
        return distance;
    }

    public boolean isRotL() {
        return rotL;
    }

    public boolean isRotR() {
        return rotR;
    }

    public boolean isMoveF() {
        return moveF;
    }
    
    public boolean isMoveB() {
        return moveB;
    }
    
    public boolean isMoveR() {
        return moveR;
    }
    
    public boolean isMoveUp() {
        return moveUp;
    }

    public boolean isMoveDown() {
        return moveDown;
    }

    public boolean isLookDown() {
        return lookDown;
    }
    
    public boolean isLookUp() {
        return lookUp;
    }

    public boolean isMoveL() {
        return moveL;
    }
    
    public void setX(double x) {
        position.x = x;
    }
    
    public void setY(double y) {
        position.y = y;
    }
    
    public void setPosition(double x, double y) {
        setX(x);
        setY(y);
    }

    public void enableGravity(boolean gravity) {
        this.enableGravity = gravity;
        moveDown = gravity;
    }

    public void enableMouseLook(boolean mouseLook) {
        this.enableMouseLook = mouseLook;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public void setHorizon(double horizon) {
        this.horizon = horizon;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setRotL(boolean rotL) {
        this.rotL = rotL;
    }

    public void setRotR(boolean rotR) {
        this.rotR = rotR;
    }

    public void setMoveF(boolean moveF) {
        this.moveF = moveF;
    }

    public void setMoveB(boolean moveB) {
        this.moveB = moveB;
    }

    public void setMoveL(boolean moveL) {
        this.moveL = moveL;
    }

    public void setMoveR(boolean moveR) {
        this.moveR = moveR;
    }

    public void setMoveUp(boolean moveUp) {
        this.moveUp = moveUp;
    }

    public void setMoveDown(boolean moveDown) {
        this.moveDown = moveDown;
    }

    public void setLookUp(boolean lookUp) {
        this.lookUp = lookUp;
    }
    
    public void setLookDown(boolean lookDown) {
        this.lookDown = lookDown;
    }
    
    public void rotate(double angle) {
        this.angle += angle;
    }
    
    public void pitch(double angle) {
        this.horizon += angle;
    }
    
    public void update() {        
        if (rotL) { angle -= ROT_SPEED * Global.deltaTime; }
        if (rotR) { angle += ROT_SPEED * Global.deltaTime; }

        if (angle <    0.0) { angle += 360.0; }
        if (angle >= 360.0) { angle -= 360.0; }
                    
        target.x = Utils.sin(angle);
        target.y = Utils.cos(angle);
        
        if (moveF || moveB || moveR || moveL) {
            double moveX = 0, 
                   moveY = 0;

            if (moveF) {
                moveX = target.x;
                moveY = target.y;
            }

            if (moveB) {
                moveX = -target.x;
                moveY = -target.y;    
            }

            if (moveR) {
                moveX =  target.y;
                moveY = -target.x;       
            }

            if (moveL) {
                moveX = -target.y;
                moveY =  target.x;       
            }
            
            position.x += MOVE_SPEED * Global.deltaTime * moveX;
            position.y += MOVE_SPEED * Global.deltaTime * moveY;
        }
        
        while (position.x <          0) { position.x += Map.WIDTH; }
        while (position.x >= Map.WIDTH) { position.x -= Map.WIDTH; }
        
        while (position.y <           0) { position.y += Map.HEIGHT; }
        while (position.y >= Map.HEIGHT) { position.y -= Map.HEIGHT; }
        
        if (moveUp  ) { height -= MOVE_SPEED * Global.deltaTime; }
        if (moveDown) { height += MOVE_SPEED * Global.deltaTime; }
                
        if (lookUp  ) { horizon -= ROT_SPEED * Global.deltaTime; }
        if (lookDown) { horizon += ROT_SPEED * Global.deltaTime; }
    }
}
