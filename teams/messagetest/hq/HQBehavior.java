package messagetest.hq;

import static messagetest.utils.Utils.*;

import java.util.*;

import messagetest.*;
import messagetest.nav.*;
import messagetest.utils.*;
import battlecode.common.*;

public class HQBehavior extends RobotBehavior {

  // HQAction[] buildOrder;
  // int buildOrderProgress = 0;

  /**
   * This is arbitrary and should be tuned.
   */
  public static final int MAX_ROBOTS = 10;
  static RobotInfo[] alliedRobots;
  static int numBots; // , numNoiseTowers, numPastrs, numSoldiers;

  static boolean[] knownAlliedIDs = new boolean[100000];

  static int numSoldiersSpawned = 0;
  static Direction soldierSpawnDirection = Direction.NONE;
  static int mostRecentlySpawnedSoldierID = -1;

  static int turnsSinceLastSpawn = 0;

  private final AttackSystem attackSystem = new AttackSystem();

  static final Comparator<Pair<MapLocation, Double>> pairMapLocDoubleComp =
      new Comparator<Pair<MapLocation, Double>>() {
        @Override
        public int compare(Pair<MapLocation, Double> a, Pair<MapLocation, Double> b) {
          return Double.compare(b.second, a.second);
        }
      };

  private final Dijkstra dijkstra = new Dijkstra(HybridMover.DIJKSTRA_CENTER);
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

    // takenPASTRLocs = new FastIterableLocSet();
    knownAlliedIDs[ID] = true;

    macro();
    // pickStrategy();
  }

  @Override
  protected void initMessageHandlers() {}

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
    // executeStrategy();
    // considerTeamAttacking();
  }

  private void setRallyPoint() throws GameActionException {
    while (!dijkstra.visited(rally)) {
      rally = rally.add(rally.directionTo(ALLY_HQ));
      if (rally.distanceSquaredTo(ALLY_HQ) < 15) {
        break;
      }
      // System.out.println(rally);
    }
    messagingSystem.writeRallyPoint(rally);
    // RC.setIndicatorString(1, "Rally " + rally);
  }

  @Override
  public void endRound() throws GameActionException {
    messagingSystem.endRound();
    if (!dijkstra.done() && currentRound <= 2000) {
      dijkstra.compute(9900, true);
      if (dijkstra.done()) {
        System.out.println("Dijkstra finished on round " + currentRound);
        if (!dijkstra.visited(rally)) {
          setRallyPoint();
        }
      }
    }
  }

  public static float defendabilityScore(MapLocation loc, Direction toEnemy) {
    // if (Clock.getRoundNum() != 400) return 0.4f;
    // this is pretty inefficient at the moment
    int ourSquaresFree = 0, ourSquaresTotal = 0, theirSquaresFree = 0, theirSquaresTotal = 0;
    // System.out.println(Clock.getBytecodeNum() + "," + Clock.getRoundNum());
    MapLocation l;
    // System.out.println(toEnemy.dx + "/" + toEnemy.dy);
    // String s1 = "", s2 = "";
    for (int i = -4; i <= 4; ++i) {
      for (int j = -4; j <= 4; ++j) {
        l = loc.add(i, j);
        TerrainTile tile = RC.senseTerrainTile(l);

        if (tile != TerrainTile.OFF_MAP) {
          Direction toL = loc.directionTo(l);
          if (toL == toEnemy || toL.rotateLeft() == toEnemy || toL.rotateRight() == toEnemy) {
            /*
             * if (!s1.equals("")) { s1 += ","; s2 += ","; }
             * s1 += "" + (l.x - loc.x);
             * s2 += "" + (l.y - loc.y);
             */
            if ((i >= -2 && i <= 2) && (j >= -2 && j <= 2)) {
              ourSquaresFree += (RC.senseTerrainTile(l) != TerrainTile.VOID ? 1 : 0);
              ourSquaresTotal++;
            }
            else {
              theirSquaresFree += (RC.senseTerrainTile(l) != TerrainTile.VOID ? 1 : 0);
              theirSquaresTotal++;
            }
          }
        }
      }
    }
    // System.out.println(s1); System.out.println(s2);
    // System.out.println(Clock.getBytecodeNum() + "," + Clock.getRoundNum() + "!");
    // System.out.println("defend (" + loc.x + "," + loc.y + ") -> (" + toEnemy.dx + "," +
    // toEnemy.dy + "): " + ourSquaresFree + "/" + ourSquaresTotal + " vs " + theirSquaresFree + "/"
    // + theirSquaresTotal + " | " +
    // ((float)ourSquaresFree / ourSquaresTotal - (float)theirSquaresFree / theirSquaresTotal) +
    // " | turn " + Clock.getRoundNum());
    return (float) ourSquaresFree / ourSquaresTotal - (float) theirSquaresFree / theirSquaresTotal;
  }

  /**
   * Handle upgrades and robots.
   */
  private static void macro() {
    // if we just spawned someone, check what his id is
    if (soldierSpawnDirection != Direction.NONE) {
      MapLocation spawnLocation = currentLocation.add(soldierSpawnDirection);
      RobotInfo[] nearbyBots = RC.senseNearbyRobots(spawnLocation, 2, ALLY_TEAM);
      for (int i = nearbyBots.length - 1; i >= 0; --i) {
        int id = nearbyBots[i].ID;
        if (!knownAlliedIDs[id]) {
          mostRecentlySpawnedSoldierID = id;
          knownAlliedIDs[id] = true;
          break;
        }
      }
      soldierSpawnDirection = Direction.NONE;
    }

    // if (!RC.isActive()) return;

    try {
      buildSoldier();
    } catch (GameActionException e) {
      // e.printStackTrace();
    }
  }

  /**
   * Tries to build a Soldier.
   * @return Whether successful.
   * @throws GameActionException
   */
  private static boolean buildSoldier() throws GameActionException {
    return buildSoldier(ALLY_HQ.directionTo(ENEMY_HQ));
  }

  /**
   * Tries to build a Soldier.
   * @param dir The direction in which to build.
   * @return Whether successful.
   * @throws GameActionException
   */
  private static boolean buildSoldier(Direction dir) throws GameActionException {
    if (RC.senseNearbyRobots(100000, ALLY_TEAM).length < MAX_ROBOTS) {
      // Spawn soldier
      for (int i = 0; i < 8; i++) {
        // if square is movable, spawn soldier there and send initial messages
        if (RC.canMove(dir)) {
          // sendMessagesOnBuild();
          RC.spawn(dir, RobotType.SOLDIER);
          soldierSpawnDirection = dir;
          numSoldiersSpawned++;

          messagingSystem.writeDeath(numSoldiersSpawned - numBots);
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

}
