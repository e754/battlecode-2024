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


public strictfp class RobotPlayer {
    static int turnCount = 0;
    static final Random rng = new Random();
    public static int randInt(int min, int max) {
    

    // nextInt is normally exclusive of the top value,
    // so add 1 to make it inclusive
        int randomNum = rng.nextInt((max - min) + 1) + min;

        return randomNum;
    }
    static int stateMachine=0;
    static int preCombatState=0; //after we leave combat, we should do what we were doing before the fight
    static int flagPursuing=-1;
    /*
    
    0 denotes random exploration
    1 denotes going to enemy base
    2 denotes combat 
    3 denotes bearing a flag to base

    */
    static MapLocation targetLoc;

 
    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };
    @SuppressWarnings("unused")
  
    public static void tryPickupFlag(RobotController rc) throws GameActionException {
        if(rc.canPickupFlag(rc.getLocation())&&turnCount>GameConstants.SETUP_ROUNDS) {
            rc.pickupFlag(rc.getLocation());
            stateMachine=3;                     //we got it, get the hell out of here!
            Pathfind.resetBug();
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            FlagInfo thisFlag=rc.senseNearbyFlags(1,rc.getTeam().opponent())[0];
            int flagID=Communication.idFlag(rc, thisFlag);
            Communication.encodeBase(rc, flagID, 2, rc.getLocation().x, rc.getLocation().y);
            targetLoc = spawnLocs[0];
        }
    }
    public static void transitionToPursuit(RobotController rc) throws GameActionException {
        Pathfind.resetBug();
        int[]flagStatus;
        while(true) {
            flagPursuing=randInt(0,2);

            flagStatus=Communication.deCode(rc, flagPursuing);
            if(flagStatus[0]!=4) {
                break;
            }
        }
        //0=unknown. 1=known, 2=holding, 3=dropped, 4=captured
        stateMachine=1;
        if(flagStatus[0]==0||flagStatus[0]==1) {
            targetLoc=new MapLocation(flagStatus[1], flagStatus[2]);
        } 
    }

    public static void run(RobotController rc) throws GameActionException {
        //flag statuses, 0=unknown. 1=known, 2=holding, 3=dropped, 4=captured
        while (true) {
            turnCount += 1;  // We have now been alive for one more turn!
            try {
                 if (!rc.isSpawned()){ 

                    Communication.flagHaunting(rc);      //if we were a flag bearer who died, we will check up on that flag
                    List<MapLocation> spawns= Spawning.getSpawnLocs(rc);  //retrieve a randomly ordered list of spawn locations
                    for(MapLocation a:spawns)
                    {
                        if (rc.canSpawn(a))
                        { 
                            Spawning.initStatesOnSpawn(rc);
                            rc.spawn(a);
                            break;
                        }
                    }
                }
                else{
                    if(turnCount==GameConstants.SETUP_ROUNDS+1) { //we roll
                        Communication.initializeBroadCastEnemybase(rc);
                    }
                    tryPickupFlag(rc); //check if there are flags around us and pick it up
                    MapLocation enemyToAttack=Action.optimalEnemy(rc,GameConstants.ATTACK_RADIUS_SQUARED); //find where best enemy is, kyle wrote that function
                    Action.attackState(rc, enemyToAttack);  //attack and adjust with combat states
                    Action.healLowestAlly(rc); //if we can, heal an ally

                        /*
                        0 denotes random exploration
                        1 denotes going to enemy base
                        2 denotes combat 
                        3 denotes bearing a flag to base
                        */
                    
                    switch(stateMachine) { //run code based on which state we are in
                        case 0: {//random exploration 
                            if(turnCount>GameConstants.SETUP_ROUNDS+1) { //switch to state 1, pursuing enemy flag
                                transitionToPursuit(rc);
                            }
                            else if(turnCount%20==0||rc.getLocation().equals(targetLoc)) { //we pick another random location to try to head to (part of random exploration)
                                targetLoc=new MapLocation(randInt(0, rc.getMapWidth()),randInt(0,rc.getMapHeight()));
                                Pathfind.resetBug();
                            }
                            Pathfind.bugTowards(rc,targetLoc);
                            break;
                        }
                        case 1: {//just waddling to enemy flag 
                            int[]target=Communication.deCode(rc, flagPursuing);
                            targetLoc=new MapLocation(target[1], target[2]);
                            FlagInfo[]enemyflag=rc.senseNearbyFlags(-1, rc.getTeam().opponent());


                            if(enemyflag.length>0) {   //we see an enemy flag, so we will ID it to identify it, whether it is a known flag or a new discovery
                                int flagFound=Communication.idFlag(rc, enemyflag[0]); 
                                int[]foundStatus=Communication.deCode(rc, flagFound);
                                if(foundStatus[0]!=2) { //flag statuses, 0=unknown. 1=known, 2=holding, 3=dropped, 4=captured
                                    flagPursuing=flagFound;   //no one else is holding it, we will se this as our new targetFlag
                                    targetLoc=enemyflag[0].getLocation();
                                    Pathfind.resetBug();
                                }
                                else { //escort?

                                }
                            }
                            Pathfind.bugTowards(rc, targetLoc);
                            break;
                        }
                        case 2: {//fight or be forgotten!
                            if(Action.attacked) //we have already attacked, so lets move away
                            {
                                if(rc.canMove(rc.getLocation().directionTo(enemyToAttack).opposite())) //kite away
                                {
                                    rc.move(rc.getLocation().directionTo(enemyToAttack).opposite());
                                }
                            }
                            else { //we havne't attacked yet, so we will want to move in and try to attack
                                Pathfind.bugTowards(rc,enemyToAttack);
                                if(rc.canAttack(enemyToAttack)) {
                                    rc.attack(enemyToAttack);
                                }
                                //attack again oops
                            }
                            break;
                        }
                        case 3: { //We carry a flag, so we will bring it back home
                            Pathfind.bugTowards(rc,targetLoc);
                            Communication.encodeBase(rc,flagPursuing+3,2,rc.getLocation().x,rc.getLocation().y);
                            if(!rc.hasFlag())
                            {
                                transitionToPursuit(rc);
                            }
                        }
                        break;
                    }


                    // Rarely attempt placing traps behind the robot.
                    if(rng.nextInt()%5==1){
                        Action.placeTraps(rc);
                    }
                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    ////flag statuses, 0=unknown. 1=known, 2=holding, 3=dropped, 4=captured


}