package messagetest.soldiers;

import static battlecode.common.GameConstants.*;
import static messagetest.utils.Utils.*;
import messagetest.*;
import messagetest.messaging.*;
import messagetest.messaging.MessagingSystem.MessageType;
import messagetest.nav.*;
import messagetest.utils.*;
import battlecode.common.*;

public class SoldierBehavior extends RobotBehavior {

  enum Mode {
    COMBAT, MOVE, FARM, EXPLORE, BUILD_PASTURE, DEFEND_PASTURE
  };

  // state machine stuff
  Mode mode;
  static MapLocation target;

  // basic data
  static int bornRound = Clock.getRoundNum();
  HybridMover hybrid = new HybridMover();
  Mover mover = new Mover();

  static ArraySet<MapLocation> messagedEnemyRobots = new ArraySet<MapLocation>(100);
  static int[][] enemyLastSeen = new int[MAP_MAX_WIDTH][MAP_MAX_HEIGHT];

  static ArraySet<MapLocation> attackLocations = new ArraySet<MapLocation>(100);
  static ArraySet<MapLocation> microLocations = new ArraySet<MapLocation>(100);

  // private final Micro micro = new Micro(this);

  public SoldierBehavior() {}

  @Override
  protected void initMessageHandlers() {
    super.initMessageHandlers();

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

    handlers[MessageType.ENEMY_BOT.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        MapLocation loc = new MapLocation(message[0], message[1]);
        enemyLastSeen[loc.x][loc.y] = currentRound;
        messagedEnemyRobots.insert(loc);
      }
    };
  }

  @Override
  public void beginRound() throws GameActionException {
    updateUnitUtils();
    attackLocations.clear();
    microLocations.clear();
    messagedEnemyRobots.clear();
    messagingSystem.beginRound(handlers);
  }

  @Override
  public void endRound() throws GameActionException {
    sendEnemyMessages();
    messagingSystem.endRound();
  }

  private void sendEnemyMessages() throws GameActionException {
    for (RobotInfo info : enemyRobots) {
      if (info.type == RobotType.HQ) continue;
      MapLocation loc = info.location;
      if (enemyLastSeen[loc.x][loc.y] < currentRound) {
        // enemyLastSeen[loc.x][loc.y] = currentRound;
        messagingSystem.writeEnemyBotMessage(loc);
      }
    }
  }

  @Override
  public void run() throws GameActionException {
    think();
    act();
  }

  private void think() throws GameActionException {
    // for answering pleas for help
    boolean hasNearbyPlea = false;
    for (int i = 0; i < SoldierBehavior.microLocations.size; ++i) {
      MapLocation m = SoldierBehavior.microLocations.get(i);

      if (currentLocation.distanceSquaredTo(m) <= 10 * 10) {
        hasNearbyPlea = true;
        break;
      }
    }

    if (enemyRobots.length > (RC.canSenseLocation(ENEMY_HQ) ? 1 : 0) || hasNearbyPlea) {
      setMode(Mode.COMBAT);
      return;
    }

    // TODO: use priorities for where to be?

    MapLocation closestTarget = closestTarget();
    if (closestTarget != null) {
      target = closestTarget;
      setMode(Mode.MOVE, target);
      return;
    }

    if (hybrid.arrived() || mode != Mode.MOVE) {
      target = findExploreLocation();
      setMode(Mode.EXPLORE, target);
    }
  }

  private void setMode(Mode m) {
    // RC.setIndicatorString(0, m.toString());
    mode = m;
  }

  private void setMode(Mode m, MapLocation target) {
    // RC.setIndicatorString(0, m + " " + target + " 2nd? " + buildingSecondPastr);
    mode = m;
  }

  private MapLocation closestTarget() {
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

    return closest;
  }

  /**
   * Rallies to the place chosen by the HQ.
   * @return Place to explore to.
   * @throws GameActionException
   */
  private MapLocation findExploreLocation() throws GameActionException {
    return messagingSystem.readRallyPoint();
  }

  private void act() throws GameActionException {

    switch (mode) {
      case COMBAT:
        SoldierUtils.luge();
        break;
      case MOVE:
        hybrid.move(target);
        break;
      case EXPLORE:
        hybrid.move(target);
        break;
      default:
        break;
    }
  }

}
