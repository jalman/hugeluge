package messagetest.hq;

import static messagetest.utils.Utils.*;

import java.util.*;

import messagetest.*;
import messagetest.builder.*;
import messagetest.nav.*;
import messagetest.utils.*;
import battlecode.common.*;

public class HQBehavior extends RobotBehavior {

  // HQAction[] buildOrder;
  // int buildOrderProgress = 0;

  /**
   * This is arbitrary and should be tuned.
   */
  public static final int MAX_BEAVERS = 10;
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

    // macro();
    // pickStrategy();
  }

  @Override
  protected void initMessageHandlers() {}

  @Override
  public void beginRound() throws GameActionException {
    Utils.updateUtils();
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

  private void macro() {
    if (!RC.isCoreReady()) return;

    RobotInfo[] allies = RC.senseNearbyRobots(Integer.MAX_VALUE, ALLY_TEAM);

    int numBeavers = 0;
    for (RobotInfo info : allies) {
      if (info.type == RobotType.BEAVER) {
        ++numBeavers;
      }
    }

    if (numBeavers < MAX_BEAVERS) {
      StaticBuilder.buildRobot(RobotType.BEAVER);
    }

    buildBarracks();
  }

  private void buildBarracks() {
    RC.setIndicatorString(1, RC.checkDependencyProgress(RobotType.BARRACKS).toString());
  }
}
