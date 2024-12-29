package stateMachinist;

import battlecode.common.FlagInfo;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Communication {
    static boolean hauntingFlag;
    static int deathTurn;
    public static void initializeBroadCastEnemybase(RobotController rc)throws GameActionException {
        MapLocation[] flags=rc.senseBroadcastFlagLocations();
        for(int i=0;i<3;i++) {
            encodeBase(rc,i,0 ,flags[i].x,flags[i].y);
        }
    }
    public static void encodeBase(RobotController rc, int index,int status, int x, int y) throws GameActionException {
        rc.writeSharedArray(index, (status*10000)+(x*100)+y);
    }
    public static int[] deCode(RobotController rc, int index) throws GameActionException {
        int ret[]=new int[3];
        ret[0]=rc.readSharedArray(index)/10000;
        ret[1]=rc.readSharedArray(index)%10000/100;
        ret[2]=rc.readSharedArray(index)%10000%100;
        return ret;
    }
      public static void flagHaunting(RobotController rc) throws GameActionException {
        if(RobotPlayer.stateMachine==3)         //we held flag once but lost it, tell everyone its new location
        {
            int[]baseLoc=deCode(rc, RobotPlayer.flagPursuing);
            int []actualLoc=deCode(rc, RobotPlayer.flagPursuing+3);
            encodeBase(rc, RobotPlayer.flagPursuing,3, baseLoc[0], baseLoc[1]);
            RobotPlayer.stateMachine=-1;
            hauntingFlag=true;
            deathTurn=RobotPlayer.turnCount;
        }
        if(hauntingFlag) {
            int[] flagStatus=deCode(rc, RobotPlayer.flagPursuing);
            if(flagStatus[0]!=3) {
                hauntingFlag=false;
                deathTurn=-1;
            }
            if(RobotPlayer.turnCount==deathTurn+GameConstants.FLAG_DROPPED_RESET_ROUNDS) {
                encodeBase(rc, RobotPlayer.flagPursuing, 1, flagStatus[1], flagStatus[2]);
                encodeBase(rc, RobotPlayer.flagPursuing+3, 0, flagStatus[1], flagStatus[2]);
            }
        }
    }



        public static int idFlag(RobotController rc, FlagInfo newflagLoc) throws GameActionException{
        int potentialNewflag=-1;
        
        for(int i=0;i<3;i++) { //check to ensure it is not at location of another known flag
            int[] currInfo=Communication.deCode(rc,i);
            switch (currInfo[0]) {
                
                case 0:   {          //first time uncovering an new flag to find exact location?
                    potentialNewflag=i;
                    if(newflagLoc.getLocation().isWithinDistanceSquared(new MapLocation(currInfo[1], currInfo[2]), 100)) {
                        break;
                    }
                }
                case 1:  {           //known flag, lets see if location matches
                    int knownFlagInfo=rc.readSharedArray(i+6);
                    if(newflagLoc.getID()==(knownFlagInfo)) { //yep, we 100% know what this is.
                        return i;
                    }
                    break;
                }
                case 2:{
                    int knownHeldFlagInfo=rc.readSharedArray(i+6);  //check ID
                    if(newflagLoc.getID()==(knownHeldFlagInfo)) { //yep, we 100% know what this is.
                        return i;
                    }
                    break;
                }
                case 3:    {            //dropped flag, could this be what we have to recover?
                    int knownDroppedFlagInfo=rc.readSharedArray(i+6);  //check ID
                    if(newflagLoc.getID()==(knownDroppedFlagInfo)) { //yep, we 100% know what this is.
                        return i;
                    }
                    break;
                }
            }
        }
        if(potentialNewflag!=-1) {  //WE FOUND NEW FLAG
            encodeBase(rc, potentialNewflag, 1, newflagLoc.getLocation().x, newflagLoc.getLocation().y);
            encodeBase(rc, potentialNewflag+3, 0, newflagLoc.getLocation().x, newflagLoc.getLocation().y);
            rc.writeSharedArray(potentialNewflag+6, newflagLoc.getID());
            return potentialNewflag;
        }
        return -1;
    }
}