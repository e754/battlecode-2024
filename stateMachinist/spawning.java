package stateMachinist;
import battlecode.common.*;
import battlecode.world.Flag;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Spawning {
    public static List getSpawnLocs(RobotController rc) {

        MapLocation[] spawnLocs = rc.getAllySpawnLocations();

        List<MapLocation> spawns= new ArrayList<>();
        for(MapLocation a:spawnLocs) {
            spawns.add(a);
        }
        Collections.shuffle(spawns);
        return spawns;
    }
    public static void initStatesOnSpawn(RobotController rc) throws GameActionException{
        if(RobotPlayer.turnCount<GameConstants.SETUP_ROUNDS+1)
        {
            Pathfind.resetBug();
            RobotPlayer.stateMachine=0;
            RobotPlayer.targetLoc=new MapLocation(RobotPlayer.randInt(0, rc.getMapWidth()),RobotPlayer.randInt(0,rc.getMapHeight()));
        }
        else {

            RobotPlayer.transitionToPursuit(rc);
        }
    }
}