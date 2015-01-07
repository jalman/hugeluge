package messagetest.soldiers;

import static messagetest.utils.Utils.*;
import messagetest.nav.*;
import messagetest.utils.*;
import battlecode.common.*;

public class Micro {

  private final ArraySet<MapLocation> attackLocations;
  private final Mover mover;

  public Micro(SoldierBehavior soldierBehavior) {
    this.attackLocations = soldierBehavior.attackLocations;
    this.mover = soldierBehavior.mover;
  }

  public void micro() throws GameActionException {
    // if (!RC.isActive()) return;

    RobotInfo closestEnemyRobot = null;
    int minDist2 = Integer.MAX_VALUE;

    for (RobotInfo info : enemyRobots) {
      if (info.type == RobotType.HQ) continue;
      int dist2 = currentLocation.distanceSquaredTo(info.location);
      if (dist2 < minDist2) {
        minDist2 = dist2;
        closestEnemyRobot = info;
      }
    }

    // attack a robot in range
    if (minDist2 <= RobotType.SOLDIER.attackRadiusSquared) {
      MapLocation target = acquireTarget().location;
      messagingSystem.writeAttackMessage(target);
      RC.attackLocation(target);
      RC.setIndicatorString(1, "FIRE " + target);
      return;
    }

    // decide whether to attack or retreat
    RobotInfo[] allyRobots = RC.senseNearbyRobots(SENSOR_RADIUS2, ALLY_TEAM);

    int numEnemySoldiers = 0;
    for (RobotInfo info : allyRobots) {
      if (info.type == RobotType.SOLDIER)
        numEnemySoldiers++;
    }

    if (allyRobots.length >= numEnemySoldiers) {
      // gang up on an enemy
      MapLocation closestAttackLocation = null;
      minDist2 = 36;

      for (int i = attackLocations.size; --i >= 0;) {
        MapLocation loc = attackLocations.get(i);
        int dist2 = currentLocation.distanceSquaredTo(loc);
        if (dist2 < minDist2) {
          minDist2 = dist2;
          closestAttackLocation = loc;
        }
      }

      if (closestAttackLocation != null) {
        mover.setTarget(closestAttackLocation);
        mover.move();
        RC.setIndicatorString(1, "GANG UP " + closestAttackLocation);
        return;
      }

      messagingSystem.writeAttackMessage(closestEnemyRobot.location);
      mover.setTarget(closestEnemyRobot.location);
      mover.move();
      RC.setIndicatorString(1, "ATTACK " + closestEnemyRobot.location);
    } else if (allyRobots.length < enemyRobots.length) {
      // FIXME: do something better here
      mover.setTarget(ALLY_HQ);
      mover.move();
      RC.setIndicatorString(1, "RETREAT");
    } else {
      RC.setIndicatorString(1, "STAND");
    }
  }

  private RobotInfo acquireTarget() throws GameActionException {
    RobotInfo best = null;
    double minHealth = Double.MAX_VALUE;
    int minDist2 = Integer.MAX_VALUE;

    for (RobotInfo info : enemyRobots) {
      int dist2 = currentLocation.distanceSquaredTo(info.location);
      if (dist2 > RobotType.SOLDIER.attackRadiusSquared) continue;

      double health = info.health;
      if (health < minHealth || (health == minHealth && dist2 < minDist2)) {
        minHealth = health;
        minDist2 = dist2;
        best = info;
      }
    }

    return best;
  }
}
