package stateMachinist;

import battlecode.common.*;

public class Action {
    public static boolean attacked=false;
    public static void healLowestAlly(RobotController rc) throws GameActionException {
    RobotInfo[] alliedBots = rc.senseNearbyRobots(4, rc.getTeam());
    if(alliedBots.length>0) {
        RobotInfo lowestHPAlly=alliedBots[0];
        if(alliedBots.length>0) {
            for(RobotInfo a:alliedBots) {
                if(a.health<lowestHPAlly.health) {
                    lowestHPAlly=a;
                }
                if(a.hasFlag&&a.health<750){
                    lowestHPAlly=a;//technically not true, but if less than x HP, we heal ally flag bearers
                    break;//you can change the value of x as you see fit
                }
            }
        }
        if(rc.canHeal(lowestHPAlly.location)) {
            rc.heal(lowestHPAlly.location);
        }
    }
}


    public static void attackState(RobotController rc,MapLocation enemyToAttack) throws GameActionException{
        attacked=false;
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if(enemyRobots.length>0&&RobotPlayer.turnCount>GameConstants.SETUP_ROUNDS) {
            if(enemyToAttack.equals(new MapLocation(-1, -1))) { //we cant attack any enemy, right now, but we see some
                enemyToAttack=enemyRobots[0].location;
            }
            if(RobotPlayer.stateMachine!=3&&RobotPlayer.stateMachine!=2) { //if we are in a less prioritize state, go into combat
                Pathfind.resetBug();
                RobotPlayer.preCombatState=RobotPlayer.stateMachine;
                RobotPlayer.stateMachine = 2;
            }
        }
        else {
            if(RobotPlayer.stateMachine==2) {
                RobotPlayer.stateMachine=RobotPlayer.preCombatState;
                Pathfind.resetBug();
            }
        }
        if(rc.canAttack(enemyToAttack)) {
            attacked=true;
            rc.attack(enemyToAttack);
        }
    } 


    public static MapLocation optimalEnemy(RobotController rc, int range) throws GameActionException {
        RobotInfo[] enemiesNearby = rc.senseNearbyRobots(range, rc.getTeam().opponent());
        if(enemiesNearby.length>0){
            RobotInfo mostThreat = enemiesNearby[0];
            int mostThreatNum = 1001;
            for(RobotInfo a:enemiesNearby){
                int threatNum = a.getHealth() - 10*(a.getAttackLevel()*a.getAttackLevel()+a.getHealLevel()*a.getHealLevel());
                //this formula is completely arbitrary
                if(a.hasFlag){
                    mostThreat = a;//flag bearer is priority target
                    break;
                }
                if(threatNum<mostThreatNum){
                    mostThreat = a;
                    mostThreatNum = threatNum;
                }
            }
            return mostThreat.getLocation();
        }
        else{
            return new MapLocation(-1, -1);
        }
    }

    public static void placeTraps(RobotController rc) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        if(rc.canBuild(TrapType.STUN, currentLocation)){
            rc.build(TrapType.STUN, currentLocation);
        }
    }



}