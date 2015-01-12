package messagetest.soldiers;

import static messagetest.utils.Utils.*;
import battlecode.common.*;

public class SoldierUtils {

  public final static double MAX_SOLDIER_HEALTH = RobotType.SOLDIER.maxHealth;

  public static int sensorRadius = SENSOR_RADIUS2;
  public static int closeEnoughToGoToBattleSquared = 64;
  public static RobotInfo[] enemiesFarAway; // enemies within closeEnoughToGoToBattle of a soldier.
                                            // Only
  // used to find farawayEnemyTarget
  public static final int maxNumberOfEnemiesToCheckToFindATarget = 9;

  static RobotInfo tempRobotInfo;

  public static MapLocation enemyTarget;
  public static RobotInfo enemyTargetRobotInfo;
  public static int enemyWeight;
  public static int allyWeight;

  /**
   * Channel for help messages. Message format: 256*X + Y.
   */
  public static final int HELP_CHANNEL = 65500;

  // micro
  public static boolean luge() throws GameActionException {
    RobotInfo[] nearbyEnemies = RC.senseNearbyRobots(35, ENEMY_TEAM);
    RobotInfo[] enemiesInRange = RC.senseNearbyRobots(10, ENEMY_TEAM);

    // sense every round -- maybe send messages on non-active rounds?

    if (nearbyEnemies.length == 0) { // no enemies: don't micro
      return false;
    } else if (RC.isCoreReady()) {
      // RC.setIndicatorString(2, "luging!");
      RobotInfo[] nearbyTeam = RC.senseNearbyRobots(35, ALLY_TEAM);
      int enemyWeight = 0;

      // get robot infos of enemy
      RobotInfo[] nearbyEnemyInfo = new RobotInfo[nearbyEnemies.length];
      RobotInfo ri;

      MapLocation nearestPastrLoc = null;
      int nearestPastrDistance = 1000000;

      // find enemy weight
      for (int i = nearbyEnemies.length - 1; i >= 0; --i) {
        ri = nearbyEnemies[i];
        switch (ri.type) {
          case SOLDIER:
            enemyWeight += 1; // 10 / (currentLocation.distanceSquaredTo(ri.location));
            // weight by how close enemy soldier is?
            break;
          case HQ:
            enemyWeight += 1000;
            break;
          default:
            break;
        }
        nearbyEnemyInfo[i] = ri;
      }
      // RC.setIndicatorString(1, "" + Clock.getRoundNum() + "," + enemyWeight);

      int help_message = RC.readBroadcast(HELP_CHANNEL);
      int callX = help_message / 256, callY = help_message % 256;
      boolean isHelpingOut = false;

      if (help_message > 0) {
        MapLocation callLoc = new MapLocation(callX, callY);
        if (RC.canSenseLocation(callLoc) && RC.senseRobotAtLocation(callLoc) == null) {
          RC.broadcast(65513, -1);
        }

        if ((callX - currentLocation.x) * (callX - currentLocation.x) + (callY - currentLocation.y)
            * (callY - currentLocation.y) < 8 * 8) {
          isHelpingOut = true;
        }
      }

      if (nearbyTeam.length + 1 >= enemyWeight || isHelpingOut) {
        if (isHelpingOut) {
          // RC.setIndicatorString(2, "helping out to kill guy at " + callX + "," + callY);
        }
        if (RC.isWeaponReady()) { // willing to attack!
          if ((nearbyEnemies.length == 0 || nearbyTeam.length - 1 >= enemyWeight || isHelpingOut)
              && enemiesInRange.length == 0) {
            // willing to move forward and attack!
            return false; // jurgz should take a look at this ...
          }

          RobotInfo targetInfo = getHighestPriority(nearbyEnemyInfo);
          MapLocation target = targetInfo == null ? null : targetInfo.location;
          if (target == null) {} else if (RC.canAttackLocation(target)) {
            RC.attackLocation(target);
            if (callX != target.x || callY != target.y) {
              RC.broadcast(HELP_CHANNEL, 256 * target.x + target.y);
            }
          }
        }
      } else {
        int dx = 0, dy = 0;
        for (int i = nearbyEnemyInfo.length - 1; i >= 0; --i) {
          dx += nearbyEnemyInfo[i].location.x;
          dy += nearbyEnemyInfo[i].location.y;
        }
        dx /= nearbyEnemies.length;
        dy /= nearbyEnemies.length;

        Direction newDir =
            currentLocation.directionTo(new MapLocation(2 * curX - dx, 2 * curY - dy));

        if (RC.isCoreReady() && newDir != Direction.NONE && newDir != Direction.OMNI) {
          if (RC.canMove(newDir)) {
            RC.move(newDir);
          }
          else if (RC.canMove(newDir.rotateLeft())) {
            RC.move(newDir.rotateLeft());
          }
          else if (RC.canMove(newDir.rotateRight())) {
            RC.move(newDir.rotateRight());
          }
        }
      }
    }

    return true;
  }

  /**
   * Returns the priority that an enemy robot has, based on distance, health of enemy, and type of enemy.
   *
   * TODO: The priority system right now is arbitrary.
   * @param RobotInfo r
   * @return
   * @throws GameActionException
   */
  // public static int getPriority(RobotInfo r) throws GameActionException
  // {
  // return getPriority(currentLocation, r);
  // }
  public static int getPriority(MapLocation loc, RobotInfo r) throws GameActionException
  {
    if (loc.distanceSquaredTo(r.location) > RobotType.SOLDIER.attackRadiusSquared) {
      return -100;
    } else if (r.type == RobotType.HQ) {
      return -10000;
    }

    int distance = naiveDistance(loc, r.location);
    // int cows = (int) RC.senseCowsAtLocation(r.location);
    // double healthPercent = robotHealthPercent(r);
    int priority = robotTypePriority(r);
    // int roundsUntilActive = 0;
    // if (r.type == RobotType.SOLDIER) {
    // roundsUntilActive = (int) r.actionDelay;
    // }

    int healthFactor = (int) (500 / (r.health / 10 + 1)); // -(int) (healthPercent * 100);
    int distFactor = 200 / distance;
    int delayFactor = 0; /* -5 * roundsUntilActive; */

    // might want to improve efficiency
    // RobotInfo[] bystanders = RC.senseNearbyRobots(loc, 2, ALLY_TEAM);
    // if (bystanders.length > 0) distFactor += 2000;

    return 5000 + priority + healthFactor + distFactor + delayFactor;
  }

  // Helper methods for overallPriority
  private static int robotTypePriority(RobotInfo r) {
    switch (r.type) {
      case SOLDIER:
        return 500;
      default:
        return -1000000;
    }
  }

  // TODO: fill in rest?
  private static double robotHealthPercent(RobotInfo r) {
    switch (r.type) {
      case SOLDIER:
        return r.health / MAX_SOLDIER_HEALTH;
      default:
        return 1;
    }
  }

  /**
   * @param array of (enemy) robot infos
   * @return robot info with highest priority
   * @throws GameActionException
   */
  // public static MapLocation getHighestPriority(RobotInfo[] arr) throws GameActionException
  // {
  // return getHighestPriority(currentLocation, arr);
  // }
  // public static MapLocation getHighestPriority(MapLocation loc, RobotInfo[] arr) throws
  // GameActionException
  public static RobotInfo getHighestPriority(RobotInfo[] arr) throws GameActionException
  {
    if (arr.length == 0) return null;

    int targetInfoIndex = -1;
    int maxPriority = -100, priority;

    for (int i = arr.length - 1; i >= 0; i--) {
      priority = getPriority(currentLocation, arr[i]);
      if (priority < 0) {
        continue;
      } else if (priority > maxPriority) {
        targetInfoIndex = i;
        maxPriority = priority;
      }
    }

    if (targetInfoIndex < 0) {
      return null;
    }
    return arr[targetInfoIndex];
  }
}
