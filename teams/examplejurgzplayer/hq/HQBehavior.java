package examplejurgzplayer.hq;

import static examplejurgzplayer.utils.Utils.*;

import java.util.*;

import examplejurgzplayer.*;
import examplejurgzplayer.Strategy.GamePhase;
import examplejurgzplayer.messaging.*;
import examplejurgzplayer.messaging.MessagingSystem.MessageType;
import examplejurgzplayer.messaging.MessagingSystem.ReservedMessageType;
import examplejurgzplayer.nav.*;
import examplejurgzplayer.utils.*;
import examplejurgzplayer.utils.Utils.SymmetryType;
import battlecode.common.*;

public class HQBehavior extends RobotBehavior {

  // HQAction[] buildOrder;
  // int buildOrderProgress = 0;

  static RobotInfo[] alliedRobots;
  static int numBots; //, numNoiseTowers, numPastrs, numSoldiers;

  static boolean[] knownAlliedIDs = new boolean[1000000]; // knownAlliedIDs[n] is true if n is known
  static int numSoldiersSpawned = 0;
  static Direction spawnDirection = Direction.NONE;
  static int mostRecentlySpawnedSoldierID = -1;

  static int turnsSinceLastSpawn = 0;

  private final AttackSystem attackSystem = new AttackSystem();

  public static final int[] yrangefornoise = { 17, 17, 17, 17, 16, 16, 16, 15, 15, 14, 14, 13, 12, 11, 10, 8, 6, 3 };

  static final Comparator<Pair<MapLocation, Double>> pairMapLocDoubleComp =
      new Comparator<Pair<MapLocation, Double>>() {
        @Override
        public int compare(Pair<MapLocation, Double> a, Pair<MapLocation, Double> b) {
          return Double.compare(b.second, a.second);
        }
      };

//  private final Dijkstra dijkstra = new Dijkstra(HybridMover.DIJKSTRA_CENTER);
  private static MapLocation rally = ALLY_HQ.add(HQ_DX / 2, HQ_DY / 2);

  public static Strategy.GamePhase gamePhase;
  public static Strategy currentStrategy;
  public static Strategy initialStrategy, midgameStrategy;

  public static boolean waitUntilVictory = false;


  public HQBehavior() {
    try {
      messagingSystem.writeRallyPoint(rally);
      // RC.setIndicatorString(1, "Rally " + rally);
    } catch (GameActionException e) {
      // e.printStackTrace();
    }

    knownAlliedIDs[ID] = true;

    macro();
    pickStrategy();
  }

  static final double AVERAGE_COW_WEIGHT = 40.0;
  /**
   * Pick a strategy based on map properties.
   */
  private void pickStrategy() {
    gamePhase = GamePhase.OPENING;
  }


  @Override
  protected void initMessageHandlers() {
  }

  /**
   * To be called only on turn 0.
   */
  /*
  public static void initialGuessMapSymmetry() {
    int ax = ALLY_HQ.x, ay = ALLY_HQ.y;
    int ex = ENEMY_HQ.x, ey = ENEMY_HQ.y;

    if (ax == ex) { // equal x-values of HQs
      if (ax * 2 == MAP_WIDTH - 1) {
        MAP_SYMMETRY = SymmetryType.ROTATION_OR_VERTICAL;
      } else {
        MAP_SYMMETRY = SymmetryType.VERTICAL_REFLECTION;
      }
    } else if (ay == ey) { // equal y-values of HQs
      if (ay * 2 == MAP_HEIGHT - 1) {
        MAP_SYMMETRY = SymmetryType.ROTATION_OR_HORIZONTAL;
      } else {
        MAP_SYMMETRY = SymmetryType.HORIZONTAL_REFLECTION;
      }
    } else if (MAP_WIDTH == MAP_HEIGHT) { // square map; maybe diag reflection
      if (ax == ey && ay == ex) {
        if (ax == ay) {
          MAP_SYMMETRY = SymmetryType.ROTATION_OR_DIAGONAL_SW_NE;
        } else {
          MAP_SYMMETRY = SymmetryType.DIAGONAL_REFLECTION_SW_NE;
        }
      } else if (ax + ey == MAP_WIDTH - 1 && ay + ex == MAP_HEIGHT - 1) {
        if (ax == ay) {
          MAP_SYMMETRY = SymmetryType.ROTATION_OR_DIAGONAL_SE_NW;
        } else {
          MAP_SYMMETRY = SymmetryType.DIAGONAL_REFLECTION_SE_NW;
        }
      } else {
        MAP_SYMMETRY = SymmetryType.ROTATION;
      }
    } else { // that's all, folks (only remaining case)
      MAP_SYMMETRY = SymmetryType.ROTATION;
    }
    try {
      RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
    } catch (GameActionException e) {
      // e.printStackTrace();
    }
  }
  */


  @Override
  public void beginRound() throws GameActionException {
    Utils.updateBuildingUtils();
    alliedRobots = RC.senseNearbyRobots(currentLocation, 10000, ALLY_TEAM);
    numBots = alliedRobots.length;
    turnsSinceLastSpawn++;
    messagingSystem.beginRound(handlers);
  }

  @Override
  public void run() throws GameActionException {
    attackSystem.tryAttack();
    macro();
    executeStrategy();
    considerTeamAttacking();
  }

  private void setRallyPoint() throws GameActionException {
//    while (!dijkstra.visited(rally)) {
//      rally = rally.add(rally.directionTo(ALLY_HQ));
//      if (rally.distanceSquaredTo(ALLY_HQ) < 15) {
    	rally = ENEMY_HQ;
//        break;
//      }
//      // System.out.println(rally);
//    }
    messagingSystem.writeRallyPoint(rally);
    // RC.setIndicatorString(1, "Rally " + rally);
  }

  @Override
  public void endRound() throws GameActionException {
    messagingSystem.endRound();
//    if (!dijkstra.done() && currentRound <= 2000) {
//      dijkstra.compute(9900, true);
//      if (dijkstra.done()) {
//        // System.out.println("Dijkstra finished on round " + currentRound);
//        if (!dijkstra.visited(rally)) {
//          setRallyPoint();
//        }
//      }
//    }
  }

  /**
   * Do the strategic (read: PASTR-related) stuff.
   * @throws GameActionException
   */
  private static void executeStrategy() throws GameActionException {
//    strategyloop: while (true) {
//      switch (gamePhase) {
//        case OPENING:
//          if (ALLY_PASTR_COUNT >= initialStrategy.desiredPASTRNum) {
//            gamePhase = GamePhase.MIDGAME;
//            currentStrategy = midgameStrategy;
//            break;
//          }
//          // RC.setIndicatorString(0, "init: " + initialStrategy + ", mid: " + midgameStrategy
//          // + ", phase " + gamePhase + ", cur: " + currentStrategy);
//          break strategyloop;
//        case MIDGAME:
//          // RC.setIndicatorString(0, "init: " + initialStrategy + ", mid: " + midgameStrategy
//          // + ", phase " + gamePhase + ", cur: " + currentStrategy);
//          break strategyloop;
//        case ENDGAME:
//          // RC.setIndicatorString(0, "init: " + initialStrategy + ", mid: " + midgameStrategy
//          // + ", phase " + gamePhase + ", cur: " + currentStrategy);
//          break strategyloop;
//        default:
//          break strategyloop;
//      }
//    }
  }

  private static void considerTeamAttacking() throws GameActionException {
//      messagingSystem.writeAttackMessage();
  }

  /**
   * Handle upgrades and robots.
   */
  private static void macro() {
    // if we just spawned someone, check what his id is
    if (spawnDirection != Direction.NONE) {
      RobotInfo[] nearbyBots = RC.senseNearbyRobots(currentLocation.add(spawnDirection), 2, ALLY_TEAM);
      for(int i=nearbyBots.length-1; i>=0; --i) {
        int id = nearbyBots[i].ID;
        if (!knownAlliedIDs[id]) {
          mostRecentlySpawnedSoldierID = id;
          knownAlliedIDs[id] = true;
          break;
        }
      }
      spawnDirection = Direction.NONE;
    }

    if (!RC.isCoreReady()) return;

    try {
      buildBeaver();
    } catch (GameActionException e) {
      // e.printStackTrace();
    }
  }

  /**
   * Tries to build a Soldier.
   * @return Whether successful.
   * @throws GameActionException
   */
  private static boolean buildBeaver() throws GameActionException {
    return spawnRobot(ALLY_HQ.directionTo(ENEMY_HQ), RobotType.BEAVER);
  }

  /**
   * Tries to build a robot of Type type.
   * @param dir The direction in which to build.
   * @param type RobotType.
   * @return Whether successful.
   * @throws GameActionException
   */
  private static boolean spawnRobot(Direction dir, RobotType type) throws GameActionException {
    if (RC.isCoreReady()) {
      System.out.println("building " + type.name());
      // Spawn soldier
      for (int i = 0; i < 8; i++) {
        // if square is movable, spawn soldier there and send initial messages
        if (RC.canSpawn(dir, type) && RC.hasSpawnRequirements(type)) {
          // sendMessagesOnBuild();
          RC.spawn(dir, type);
          spawnDirection = dir;
          turnsSinceLastSpawn = 0;
          return true;
        }
        // otherwise keep rotating until this is possible
        dir = dir.rotateRight();
      }
      // message guys to get out of the way??
    }
    return false;
  }

  // private void sendMessagesOnBuild() throws GameActionException {
  // // empty for now
  // }

  public Direction wayToEnemy(MapLocation m) {
//    MapLocation m2 = Utils.getSymmetricSquare(m);
//    Direction fromD = dijkstra.from[m2.x][m2.y];
//    fromD = getSymmetricDirection(fromD);
//    return fromD != null ? fromD : m.directionTo(ENEMY_HQ);
	  return m.directionTo(ENEMY_HQ);
  }

}
