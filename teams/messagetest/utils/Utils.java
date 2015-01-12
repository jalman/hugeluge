package messagetest.utils;

import static battlecode.common.Direction.*;

import java.util.*;

import messagetest.messaging.*;
import messagetest.nav.*;
import battlecode.common.*;

public class Utils {
  // actual constants

  // public static final int[] DX = {-1, -1, -1, 0, 0, 1, 1, 1};
  // public static final int[] DY = {-1, 0, 1, -1, 1, -1, 0, 1};
  public static final TerrainTile[] TERRAIN_TILES = TerrainTile.values();
  public static final Direction[] DIRECTIONS = Direction.values();

  public static final Direction[] CARDINAL = new Direction[] {NORTH, WEST, SOUTH, EAST};

  public static final int NORMAL_DIAGONAL = 7;
  public static final int NORMAL_ORTHOGONAL = 5;

  public static int getDirWeight(Direction dir) {
    return dir.isDiagonal() ? NORMAL_DIAGONAL : NORMAL_ORTHOGONAL;
  }

  /**
   * Distance assuming normal terrain and no obstacles.
   */
  public static int naiveDistance(MapLocation loc1, MapLocation loc2) {
    int dx = Math.abs(loc1.x - loc2.x);
    int dy = Math.abs(loc1.y - loc2.y);
    int min, diff;
    if (dx > dy) {
      min = dy;
      diff = dx - dy;
    } else {
      min = dx;
      diff = dy - dx;
    }
    return (min * NORMAL_DIAGONAL + diff * NORMAL_ORTHOGONAL);
  }

  /**
   * Standard Manhattan distance.
   */
  public static int manhattan(MapLocation loc1, MapLocation loc2) {
    int dx = loc1.x - loc2.x;
    dx = dx < 0 ? -dx : dx;
    int dy = loc1.y - loc2.y;
    dy = dy < 0 ? -dy : dy;
    return dx + dy;
  }

  /**
   * Distance assuming all 8 direction have distance 1.
   */
  public static int octile(MapLocation loc1, MapLocation loc2) {
    int dx = loc1.x - loc2.x;
    dx = dx < 0 ? -dx : dx;
    int dy = loc1.y - loc2.y;
    dy = dy < 0 ? -dy : dy;
    return dx > dy ? dx : dy;
  }

  public static final Direction[] REGULAR_DIRECTIONS = new Direction[] {
      EAST, NORTH_EAST, NORTH, NORTH_WEST, WEST, SOUTH_WEST, SOUTH, SOUTH_EAST
  };

  public static final int[][] directions = new int[8][2];
  static {
    for (int i = 0; i < 8; i++) {
      directions[i][0] = DIRECTIONS[i].dx;
      directions[i][1] = DIRECTIONS[i].dy;
    }
  }

  public static final int WRAP_X = GameConstants.MAP_MAX_WIDTH + 6;
  public static final int WRAP_Y = GameConstants.MAP_MAX_HEIGHT + 6;
  public static final int MAP_MAX_SIZE = WRAP_X * WRAP_Y;

  public static final int wrapX(int x) {
    x %= WRAP_X;
    if (x < 0) x += WRAP_X;
    return x;
  }

  public static final int wrapY(int y) {
    y %= WRAP_Y;
    if (y < 0) y += WRAP_Y;
    return y;
  }

  public static final int TOWER_SENSOR_RADIUS2 = 35;
  public static final int ROBOT_SENSOR_RADIUS2 = 24;

  // these are set from the beginning of the game
  public static RobotController RC;
  public static RobotInfo ROBOT;
  public static int ID;
  public static RobotType TYPE;
  // public static int MAP_WIDTH, MAP_HEIGHT, MAP_SIZE;
  /**
   * Estimates honed over time.
   */
  public static int MAP_MIN_X, MAP_MAX_X, MAP_MIN_Y, MAP_MAX_Y;
  public static Team ALLY_TEAM, ENEMY_TEAM;
  public static MapLocation ALLY_HQ, ENEMY_HQ;
  public static Direction ENEMY_DIR;
  public static int HQ_DX, HQ_DY;
  public static int HQ_DIST;
  public static Random random;
  public static int birthRound, currentRound;

  // this is for messaging
  public static MessagingSystem messagingSystem;

  public static MapLocation currentLocation, previousLocation;
  public static int curX, curY;
  public static double currentCowsHere;
  // public static double forward;
  public static int SENSOR_RADIUS2;
  public static RobotInfo[] enemyRobots = new RobotInfo[0];

  // public static RobotInfo[] allyRobots = new RobotInfo[0];

  public static void initUtils(RobotController rc) {
    RC = rc;
    TYPE = rc.getType();
    ID = rc.getID();
    SENSOR_RADIUS2 = TYPE.sensorRadiusSquared;

    ALLY_TEAM = rc.getTeam();
    ENEMY_TEAM = (ALLY_TEAM == Team.A) ? Team.B : Team.A;

    ALLY_HQ = rc.senseHQLocation();
    ENEMY_HQ = rc.senseEnemyHQLocation();
    ENEMY_DIR = ALLY_HQ.directionTo(ENEMY_HQ);

    HQ_DX = ENEMY_HQ.x - ALLY_HQ.x;
    HQ_DY = ENEMY_HQ.y - ALLY_HQ.y;
    // HQ_DIST = naiveDistance(ALLY_HQ,ENEMY_HQ);

    int min_x = Math.min(ALLY_HQ.x, ENEMY_HQ.x);
    int max_x = Math.max(ALLY_HQ.x, ENEMY_HQ.x);
    int min_y = Math.min(ALLY_HQ.y, ENEMY_HQ.y);
    int max_y = Math.max(ALLY_HQ.y, ENEMY_HQ.y);

    MAP_MIN_X = max_x - GameConstants.MAP_MAX_WIDTH;
    MAP_MAX_X = min_x + GameConstants.MAP_MAX_WIDTH;
    MAP_MIN_Y = max_y - GameConstants.MAP_MAX_HEIGHT;
    MAP_MAX_Y = min_y + GameConstants.MAP_MAX_HEIGHT;

    currentLocation = RC.getLocation();
    previousLocation = currentLocation;
    curX = currentLocation.x;
    curY = currentLocation.y;

    birthRound = Clock.getRoundNum();

    random = new Random(((long) ID << 32) ^ birthRound);

    messagingSystem = new MessagingSystem();

    updateUtils();
  }

  /**
   * Called at the beginning of each round by buildings.
   */
  public static void updateUtils() {
    previousLocation = currentLocation;
    currentLocation = RC.getLocation();
    currentRound = Clock.getRoundNum();
    bytecodes = Clock.getBytecodeNum();

    enemyRobots = RC.senseNearbyRobots(currentLocation, SENSOR_RADIUS2, ENEMY_TEAM);
    // allyRobots = RC.senseNearbyRobots(currentLocation, SENSOR_RADIUS2, ALLY_TEAM);

  }

  /**
   * @return whether it's my first round executing or not
   */
  public static boolean isFirstRound() {
    return Clock.getRoundNum() == birthRound;
  }

  /**
   * Finds the closest (by naive distance) map location to the target among a set of map locations.
   * @param locs The set of map locations.
   * @param target The target location.
   * @return The closest map location.
   */
  public static MapLocation closestLocation(MapLocation[] locs, MapLocation target) {
    MapLocation close = null;
    int distance = Integer.MAX_VALUE;

    for (int i = locs.length - 1; i >= 0; i--) {
      int d = locs[i].distanceSquaredTo(target);
      if (d < distance) {
        close = locs[i];
        distance = d;
      }
    }

    return close;
  }

  @SafeVarargs
  public static <T> T[] newArray(int length, T... array) {
    return Arrays.copyOf(array, length);
  }

  /**
   * @param loc
   * @return Whether loc is within range (without accounting for splash...) of enemy HQ
   */
  public static boolean inRangeOfEnemyHQ(MapLocation loc) {
    return loc.distanceSquaredTo(ENEMY_HQ) <= RobotType.HQ.attackRadiusSquared;
  }

  /**
   * Bytecode counting
   */
  private static int bytecodes = 0;

  /**
   * Set an indicator string with # bytecodes used since last invocation of countBytecodes
   * @return # bytecodes used since last invocation of countBytecodes
   */
  public static int countBytecodes() {
    int bc = Clock.getBytecodeNum();
    int d = bc - bytecodes;

    RC.setIndicatorString(2, "Bytecodes: " + d);

    bytecodes = bc;
    return d;
  }

  public static boolean isPathable(MapLocation loc) {
    return RC.senseTerrainTile(loc).isTraversable();
  }

  /**
   * Max radius in which hq can hit us.
   */
  public static final int SAFE_RADIUS_SQUARED = 25;

  /**
   * Whether we can get splashed by the enemy hq.
   */
  public static boolean isSafe(MapLocation loc) {
    return loc.add(loc.directionTo(ENEMY_HQ)).distanceSquaredTo(ENEMY_HQ) > RobotType.HQ.attackRadiusSquared;
  }

  private static ArraySet<MapLocation> unsafeLocs;
  private static boolean[][] unsafe;

  public static ArraySet<MapLocation> getUnsafeLocs() {
    if (unsafeLocs == null) {
      unsafeLocs = new ArraySet<MapLocation>(200);
      for (MapLocation loc : MapLocation.getAllMapLocationsWithinRadiusSq(ENEMY_HQ,
          SAFE_RADIUS_SQUARED)) {
        if (isPathable(loc) && !isSafe(loc)) {
          unsafeLocs.insert(loc);
        }
      }
    }
    return unsafeLocs;
  }

  // TODO: needs to be tweaked and could probably be done better
  public static boolean[][] getUnsafe() {
    if (unsafe == null) {
      unsafe = new boolean[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];
      getUnsafeLocs();
      for (int i = unsafeLocs.size; --i >= 0;) {
        MapLocation loc = unsafeLocs.get(i);
        unsafe[loc.x][loc.y] = true;
      }
    }
    return unsafe;
  }

  public static void doPathing(int bytecodes) throws GameActionException {
    Pair<Direction, Integer> info = messagingSystem.readPathingInfo(currentLocation);

    // if (info.first != null) return;

    BFS bfs = new BFS(true);

    Function<MapLocation, Boolean> stop = new Function<MapLocation, Boolean>() {
      @Override
      public Boolean apply(MapLocation loc) {
        return loc.distanceSquaredTo(currentLocation) > SENSOR_RADIUS2;
      }
    };

    bfs.compute(bytecodes, stop);
  }

  /**
   * Get the MapLocations we can see this round but not last round.
   */
  public static MapLocation[] getNewLocs() {
    if (currentLocation == previousLocation) {
      return new MapLocation[] {};
    }

    Direction moved = previousLocation.directionTo(currentLocation);

    int[][] deltas = Sensor.newLocs.get(moved);

    MapLocation[] locs = new MapLocation[deltas.length];

    for (int i = 0; i < locs.length; ++i) {
      int[] delta = deltas[i];
      locs[i] = currentLocation.add(delta[0], delta[1]);
    }

    return locs;
  }

}
