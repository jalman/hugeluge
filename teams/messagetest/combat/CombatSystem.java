package messagetest.combat;

import static messagetest.utils.Utils.*;
import messagetest.messaging.*;
import messagetest.messaging.MessagingSystem.MessageType;
import messagetest.utils.*;
import battlecode.common.*;

public class CombatSystem {
  public static ArraySet<MapLocation> messagedEnemyRobots = new ArraySet<MapLocation>(100);
  // static int[][] enemyLastSeen = new int[2*MAX_MAP_WIDTH][2*MAX_MAP_HEIGHT];

  public static ArraySet<MapLocation> attackLocations = new ArraySet<MapLocation>(100);
  public static ArraySet<MapLocation> microLocations = new ArraySet<MapLocation>(100);

  public static void initMessageHandlers(MessageHandler[] handlers) {
    handlers[MessageType.ATTACK_LOCATION.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        MapLocation loc = new MapLocation(message[0], message[1]);
        attackLocations.insert(loc);
      }
    };
    handlers[MessageType.MICRO_INFO.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        MapLocation loc = new MapLocation(message[0], message[1]);
        microLocations.insert(loc);
        attackLocations.insert(loc);
      }
    };
  }

  public static void beginRound() {
    attackLocations.clear();
    microLocations.clear();
    messagedEnemyRobots.clear();
  }

  public static final int MICRO_DIST = 10;
  public static final int MICRO_DIST2 = MICRO_DIST * MICRO_DIST;

  public static boolean shouldMicro() {
    // for answering pleas for help
    boolean hasNearbyPlea = false;
    for (int i = 0; i < microLocations.size; ++i) {
      MapLocation m = microLocations.get(i);

      if (currentLocation.distanceSquaredTo(m) <= MICRO_DIST2) {
        hasNearbyPlea = true;
        break;
      }
    }

    return enemyRobots.length > 0 || hasNearbyPlea;
  }

  public static MapLocation closestTarget() {
    MapLocation closest = null;
    int min = Integer.MAX_VALUE;

    for (int i = attackLocations.size; --i >= 0;) {
      MapLocation loc = attackLocations.get(i);
      int d = currentLocation.distanceSquaredTo(loc);
      if (d < min) {
        min = d;
        closest = loc;
      }
    }

    if (closest != null) return closest;

    for (int i = messagedEnemyRobots.size; --i >= 0;) {
      MapLocation loc = messagedEnemyRobots.get(i);
      int d = currentLocation.distanceSquaredTo(loc);
      if (d < min) {
        min = d;
        closest = loc;
      }
    }

    if (closest != null) return closest;

    MapLocation[] enemyTowers = RC.senseEnemyTowerLocations();

    if (enemyTowers.length > 0) {
      return closestLocation(enemyTowers, currentLocation);
    } else {
      return ENEMY_HQ;
    }
  }

}
