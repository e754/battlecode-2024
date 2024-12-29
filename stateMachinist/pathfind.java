package stateMachinist;

import java.util.HashSet;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.HashSet;

public class Pathfind {

    private static Direction dir;

    private static MapLocation prevDest = null;
    private static HashSet<MapLocation> line = null;
    private static int obstacleStartDist = 0;


    private static int bugState = 0; // 0 head to target, 1 circle obstacle
    private static MapLocation closestObstacle = null;
    private static int closestObstacleDist = 10000;
    private static Direction bugDir = null;


    private static Direction targetDir;
    static Direction facing=Direction.NORTH;

    public static void bugTowards(RobotController rc, MapLocation target) throws GameActionException {
        targetDir=rc.getLocation().directionTo(target);
        if(rc.canMove(targetDir)) { //attempt to walk directly at target
            rc.move(targetDir);
            facing=targetDir; //yes, we are facing this way  now
        }
        else {  //if not, we try to rotate our "face" direction until we can simply move along obstalce (following wall)
            boolean blockedbyBot=false;
            for(int i=0;i<8;i++) {
                
                if(rc.canMove(facing)) {
                    rc.move(facing);
                    break;
                }
                else
                {
                    if(rc.canSenseRobotAtLocation(rc.getLocation().add(facing)))
                    {
                        blockedbyBot=true;
                    }
                    if(i>4&&blockedbyBot)
                    {
                        break;
                    }
                    facing=facing.rotateRight(); 
                }
            }
        }
    }



    public static void resetBug(){
        bugState = 0; // 0 head to target, 1 circle obstacle
        closestObstacle = null;
        closestObstacleDist = 10000;
        bugDir = null;
    }

    public static void bugNavOne(RobotController rc, MapLocation destination) throws GameActionException{
        if(bugState == 0) {
            bugDir = rc.getLocation().directionTo(destination);
            if(rc.canMove(bugDir)){
                rc.move(bugDir);
            } else {
                bugState = 1;
                closestObstacle = null;
                closestObstacleDist = 10000;
            }
        } else {
            if(rc.getLocation().equals(closestObstacle)){
                bugState = 0;
            }

            if(rc.getLocation().distanceSquaredTo(destination) < closestObstacleDist){
                closestObstacleDist = rc.getLocation().distanceSquaredTo(destination);
                closestObstacle = rc.getLocation();
            }

            for(int i = 0; i < 9; i++){
                if(rc.canMove(bugDir)){
                    rc.move(bugDir);
                    bugDir = bugDir.rotateRight();
                    bugDir = bugDir.rotateRight();
                    break;
                } else {
                    bugDir = bugDir.rotateLeft();
                }
            }
        }
    }

    public static void bugNav2(RobotController rc, MapLocation destination) throws GameActionException{
        
        if(!destination.equals(prevDest)) {
            prevDest = destination;
            line = createLine(rc.getLocation(), destination);
        }

        if(bugState == 0) {
            bugDir = rc.getLocation().directionTo(destination);
            if(rc.canMove(bugDir)){
                rc.move(bugDir);
            } else {
                bugState = 1;
                obstacleStartDist = rc.getLocation().distanceSquaredTo(destination);
                bugDir = rc.getLocation().directionTo(destination);
            }
        } else {
            if(line.contains(rc.getLocation()) && rc.getLocation().distanceSquaredTo(destination) < obstacleStartDist) {
                bugState = 0;
            }

            for(int i = 0; i < 9; i++){
                if(rc.canMove(bugDir)){
                    rc.move(bugDir);
                    bugDir = bugDir.rotateRight();
                    bugDir = bugDir.rotateRight();
                    break;
                } else {
                    bugDir = bugDir.rotateLeft();
                }
            }
        }
    }

    public static void bugNavZero(RobotController rc, MapLocation destination) throws GameActionException{
        Direction bugDir = rc.getLocation().directionTo(destination);

        if(rc.canMove(bugDir)){
            rc.move(bugDir);
        } else {
            for(int i = 0; i < 8; i++){
                if(rc.canMove(bugDir)){
                    rc.move(bugDir);
                    break;
                } else {
                    bugDir = bugDir.rotateLeft();
                }
            }
        }
    }

    private static HashSet<MapLocation> createLine(MapLocation a, MapLocation b) {
        HashSet<MapLocation> locs = new HashSet<>();
        int x = a.x, y = a.y;
        int dx = b.x - a.x;
        int dy = b.y - a.y;
        int sx = (int) Math.signum(dx);
        int sy = (int) Math.signum(dy);
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        int d = Math.max(dx,dy);
        int r = d/2;
        if (dx > dy) {
            for (int i = 0; i < d; i++) {
                locs.add(new MapLocation(x, y));
                x += sx;
                r += dy;
                if (r >= dx) {
                    locs.add(new MapLocation(x, y));
                    y += sy;
                    r -= dx;
                }
            }
        }
        else {
            for (int i = 0; i < d; i++) {
                locs.add(new MapLocation(x, y));
                y += sy;
                r += dx;
                if (r >= dy) {
                    locs.add(new MapLocation(x, y));
                    x += sx;
                    r -= dy;
                }
            }
        }
        locs.add(new MapLocation(x, y));
        return locs;
    }
}
Pathfind.java
7 KB