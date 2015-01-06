package examplejurgzplayer.utils;

import static battlecode.common.Direction.*;

import java.util.*;

import examplejurgzplayer.messaging.*;
import examplejurgzplayer.messaging.MessagingSystem.ReservedMessageType;
import battlecode.common.*;

public class Utils {
  //actual constants

  // public static final int[] DX = {-1, -1, -1, 0, 0, 1, 1, 1};
  // public static final int[] DY = {-1, 0, 1, -1, 1, -1, 0, 1};
  public static final TerrainTile[] TERRAIN_TILES = TerrainTile.values();
  public static final Direction[] DIRECTIONS = Direction.values();

  public static final int ROAD_DIAGONAL = 7;
  public static final int ROAD_ORTHOGONAL = 5;
  public static final int NORMAL_DIAGONAL = 14;
  public static final int NORMAL_ORTHOGONAL = 10;

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

  public static final int WEIGHT[][] = new int[TERRAIN_TILES.length][8];

  static {
    for (int i = 0; i < 8; i += 2) {
      WEIGHT[TerrainTile.NORMAL.ordinal()][i] = NORMAL_ORTHOGONAL;
    }
    for (int i = 1; i < 8; i += 2) {
      WEIGHT[TerrainTile.NORMAL.ordinal()][i] = NORMAL_DIAGONAL;
    }
  }

  public static int getActionDelay(MapLocation loc, Direction dir) {
    return WEIGHT[RC.senseTerrainTile(loc).ordinal()][dir.ordinal()];
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

  public static final int MAP_MAX_SIZE = GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_HEIGHT;

  //these are set from the beginning of the game
  public static RobotController RC;
  public static int ID;
  public static RobotType TYPE;
//  public static int MAP_WIDTH, MAP_HEIGHT, MAP_SIZE;
  public static final int MIN_MAP_WIDTH = 30, MIN_MAP_HEIGHT = 30;
  public static final int MAX_MAP_WIDTH = 120, MAX_MAP_HEIGHT = 120;
  public static Team ALLY_TEAM, ENEMY_TEAM;
  public static MapLocation ALLY_HQ, ENEMY_HQ;
  public static Direction ENEMY_DIR;
  public static int HQ_DX, HQ_DY;
  public static int HQ_DIST;
  public static Random random;
  public static int birthRound, currentRound;
  public static MapLocation[] alliedTowerLocs, enemyTowerLocs;

  //this is for messaging
  public static MessagingSystem messagingSystem;

  public static MapLocation currentLocation;
  public static int curX, curY;
  public static double currentCowsHere;
  // public static double forward;
  public static int SENSOR_RADIUS2;
  public static RobotInfo[] enemyRobots = new RobotInfo[0];

  // cache all RobotInfos sensed this turn; hash by ID
//  public static RobotInfo[] robotInfoCache;


  /**
   * Type of map symmetry. Reflection orientations describe the line connecting the HQs.
   *
   * Initially computed by HQ. If uncertain, every time we request the symmetric square,
   * we check the possible symmetric squares, and throw a symmetry out if possible. If both
   * squares are identical to the requested square, we'll pretend the map has rotational symmetry
   * when returning, but maintain the uncertain state.
   *
   * This may be inefficient on maps which have both reflectional and rotational symmetry...
   * On the other hand, we can't be certain about symmetry until such maps unless we check EVERY square.
   */
  public enum SymmetryType {
    ROTATION(true),
    HORIZONTAL_REFLECTION(true), // reflect across vertical line
    VERTICAL_REFLECTION(true), // reflect across horizontal line
    // diagonal reflection only occurs if MAP_WIDTH = MAP_HEIGHT = X
    DIAGONAL_REFLECTION_SW_NE(true), // reflect across the line (0, 0)--(X, X)
    DIAGONAL_REFLECTION_SE_NW(true), // reflect across the line (0, X)--(X, 0)
    ROTATION_OR_HORIZONTAL(false), // unsure about rotation or horizontal
    ROTATION_OR_VERTICAL(false), // unsure about rotation or vertical
    ROTATION_OR_DIAGONAL_SW_NE(false), // etc
    ROTATION_OR_DIAGONAL_SE_NW(false),
    ;

    public boolean certainty;
    private SymmetryType(boolean certainty) {
      this.certainty = certainty;
    }
  }

  public static SymmetryType MAP_SYMMETRY;

  public static void initUtils(RobotController rc) {
    RC = rc;
    TYPE = rc.getType();
    ID = rc.getID();
    SENSOR_RADIUS2 = TYPE.sensorRadiusSquared;

//    don't know map size this year!
//    MAP_WIDTH = rc.getMapWidth();
//    MAP_HEIGHT = rc.getMapHeight();
//    MAP_SIZE = MAP_WIDTH * MAP_HEIGHT;

    ALLY_TEAM = rc.getTeam();
    ENEMY_TEAM = (ALLY_TEAM == Team.A) ? Team.B : Team.A;

    ALLY_HQ = rc.senseHQLocation();
    ENEMY_HQ = rc.senseEnemyHQLocation();

    ENEMY_DIR = ALLY_HQ.directionTo(ENEMY_HQ);

    HQ_DX = ENEMY_HQ.x - ALLY_HQ.x;
    HQ_DY = ENEMY_HQ.y - ALLY_HQ.y;
    // HQ_DIST = naiveDistance(ALLY_HQ,ENEMY_HQ);

    currentLocation = RC.getLocation();
    curX = currentLocation.x;
    curY = currentLocation.y;

    birthRound = Clock.getRoundNum();

    random = new Random(((long) ID << 32) ^ birthRound);

    messagingSystem = new MessagingSystem();

    if (TYPE.isBuilding) {
        updateBuildingUtils();
    } else {
        updateUnitUtils();
    }

  }

  /**
   * Called at the beginning of each round by buildings.
   */
  public static void updateBuildingUtils() {
//    robotInfoCache = new RobotInfo[100000];

    enemyRobots =
        RC.senseNearbyRobots(SENSOR_RADIUS2, ENEMY_TEAM);
    currentRound = Clock.getRoundNum();
    bytecodes = Clock.getBytecodeNum();
    
    // inefficient for now
    alliedTowerLocs = RC.senseTowerLocations();
    enemyTowerLocs = RC.senseEnemyTowerLocations();
  }

  /**
   * Called at the beginning of each round by moving units.
   */
  public static void updateUnitUtils() {
//    robotInfoCache = new RobotInfo[100000];

    currentLocation = RC.getLocation();
    curX = currentLocation.x;
    curY = currentLocation.y;

    enemyRobots =
        RC.senseNearbyRobots(SENSOR_RADIUS2, ENEMY_TEAM);
    currentRound = Clock.getRoundNum();
    bytecodes = Clock.getBytecodeNum();

    alliedTowerLocs = RC.senseTowerLocations();
    enemyTowerLocs = RC.senseEnemyTowerLocations();
  }

  /**
   * Get the MapLocation believed to be symmetric with loc.
   * If we are unsure about the map's symmetry (ROTATION_OR_HORIZONTAL or ROTATION_OR_VERTICAL),
   * further tries to determine the map's symmetry and does a bunch of checks.
   *
   * If only one check works, return the appropriate square and broadcast globally the correct symmetry.
   * If both work, choose the rotationally symmetric MapLocation.
   * @param loc
   * @return
   */
  /*
  public static MapLocation getSymmetricSquare(MapLocation loc) {
    try {
      MAP_SYMMETRY =
          SymmetryType.values()[RC.readBroadcast(ReservedMessageType.MAP_SYMMETRY.channel())];
      int x = loc.x, y = loc.y;
      MapLocation rotLoc, refLoc;
      TerrainTile here;
      double cowGrowthHere;
      switch (MAP_SYMMETRY) {
        case ROTATION:
          return new MapLocation(MAP_WIDTH - 1 - x, MAP_HEIGHT - 1 - y);
        case HORIZONTAL_REFLECTION:
          return new MapLocation(MAP_WIDTH - 1 - x, y);
        case VERTICAL_REFLECTION:
          return new MapLocation(x, MAP_HEIGHT - 1 - y);
        case DIAGONAL_REFLECTION_SW_NE:
          return new MapLocation(y, x);
        case DIAGONAL_REFLECTION_SE_NW:
          return new MapLocation(MAP_WIDTH - 1 - y, MAP_HEIGHT - 1 - x);
        case ROTATION_OR_HORIZONTAL:
          rotLoc = new MapLocation(MAP_WIDTH - 1 - x, MAP_HEIGHT - 1 - y);
          refLoc = new MapLocation(MAP_WIDTH - 1 - x, y);
          here = RC.senseTerrainTile(loc);
          cowGrowthHere = COW_GROWTH[x][y];
          if (here != RC.senseTerrainTile(rotLoc)
              || cowGrowthHere != COW_GROWTH[rotLoc.x][rotLoc.y]) {
            // check rotation first
            MAP_SYMMETRY = SymmetryType.HORIZONTAL_REFLECTION;
            RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
            return refLoc;
          } else if (here != RC.senseTerrainTile(refLoc)
              || cowGrowthHere != COW_GROWTH[refLoc.x][refLoc.y]) {
            // then check reflection
            MAP_SYMMETRY = SymmetryType.ROTATION;
            RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
            return rotLoc;
          } else {
            // if both work, return rotationally symmetric guy
            return rotLoc;
          }
        case ROTATION_OR_VERTICAL:
          rotLoc = new MapLocation(MAP_WIDTH - 1 - x, MAP_HEIGHT - 1 - y);
          refLoc = new MapLocation(x, MAP_HEIGHT - 1 - y);
          here = RC.senseTerrainTile(loc);
          cowGrowthHere = COW_GROWTH[x][y];
          if (here != RC.senseTerrainTile(rotLoc)
              || cowGrowthHere != COW_GROWTH[rotLoc.x][rotLoc.y]) {
            // check rotation first
            MAP_SYMMETRY = SymmetryType.VERTICAL_REFLECTION;
            RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
            return refLoc;
          } else if (here != RC.senseTerrainTile(refLoc)
              || cowGrowthHere != COW_GROWTH[refLoc.x][refLoc.y]) {
            // then check reflection
            MAP_SYMMETRY = SymmetryType.ROTATION;
            RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
            return rotLoc;
          } else {
            // if both work, return rotationally symmetric guy
            return rotLoc;
          }
        case ROTATION_OR_DIAGONAL_SW_NE:
          rotLoc = new MapLocation(MAP_WIDTH - 1 - x, MAP_HEIGHT - 1 - y);
          refLoc = new MapLocation(y, x);
          here = RC.senseTerrainTile(loc);
          cowGrowthHere = COW_GROWTH[x][y];
          if (here != RC.senseTerrainTile(rotLoc)
              || cowGrowthHere != COW_GROWTH[rotLoc.x][rotLoc.y]) {
            // check rotation first
            MAP_SYMMETRY = SymmetryType.DIAGONAL_REFLECTION_SW_NE;
            RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
            return refLoc;
          } else if (here != RC.senseTerrainTile(refLoc)
              || cowGrowthHere != COW_GROWTH[refLoc.x][refLoc.y]) {
            // then check reflection
            MAP_SYMMETRY = SymmetryType.ROTATION;
            RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
            return rotLoc;
          } else {
            // if both work, return rotationally symmetric guy
            return rotLoc;
          }
        case ROTATION_OR_DIAGONAL_SE_NW:
          rotLoc = new MapLocation(MAP_WIDTH - 1 - x, MAP_HEIGHT - 1 - y);
          refLoc = new MapLocation(MAP_WIDTH - 1 - y, MAP_HEIGHT - 1 - x);
          here = RC.senseTerrainTile(loc);
          cowGrowthHere = COW_GROWTH[x][y];
          if (here != RC.senseTerrainTile(rotLoc)
              || cowGrowthHere != COW_GROWTH[rotLoc.x][rotLoc.y]) {
            // check rotation first
            MAP_SYMMETRY = SymmetryType.DIAGONAL_REFLECTION_SE_NW;
            RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
            return refLoc;
          } else if (here != RC.senseTerrainTile(refLoc)
              || cowGrowthHere != COW_GROWTH[refLoc.x][refLoc.y]) {
            // then check reflection
            MAP_SYMMETRY = SymmetryType.ROTATION;
            RC.broadcast(ReservedMessageType.MAP_SYMMETRY.channel(), MAP_SYMMETRY.ordinal());
            return rotLoc;
          } else {
            // if both work, return rotationally symmetric guy
            return rotLoc;
          }
        default:
          return null;
      }
    } catch (GameActionException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static Direction getSymmetricDirection(Direction d) {
    if(d == null) return d;
    switch (MAP_SYMMETRY) {
      case ROTATION:
        return d.opposite();
      case VERTICAL_REFLECTION:
        switch (d) {
          case NORTH:
            return Direction.SOUTH;
          case NORTH_EAST:
            return Direction.SOUTH_EAST;
          case EAST:
            return Direction.EAST;
          case SOUTH_EAST:
            return Direction.NORTH_EAST;
          case SOUTH:
            return Direction.NORTH;
          case SOUTH_WEST:
            return Direction.NORTH_WEST;
          case WEST:
            return Direction.WEST;
          case NORTH_WEST:
            return Direction.SOUTH_WEST;
          default:
            return d;
        }
      case HORIZONTAL_REFLECTION:
        switch (d) {
          case NORTH:
            return Direction.NORTH;
          case NORTH_EAST:
            return Direction.NORTH_WEST;
          case EAST:
            return Direction.WEST;
          case SOUTH_EAST:
            return Direction.SOUTH_WEST;
          case SOUTH:
            return Direction.SOUTH;
          case SOUTH_WEST:
            return Direction.SOUTH_EAST;
          case WEST:
            return Direction.EAST;
          case NORTH_WEST:
            return Direction.NORTH_EAST;
          default:
            return d;
        }
      case DIAGONAL_REFLECTION_SW_NE:
        switch (d) {
          case NORTH:
            return Direction.WEST;
          case NORTH_EAST:
            return Direction.SOUTH_WEST;
          case EAST:
            return Direction.SOUTH;
          case SOUTH_EAST:
            return Direction.SOUTH_EAST;
          case SOUTH:
            return Direction.EAST;
          case SOUTH_WEST:
            return Direction.NORTH_EAST;
          case WEST:
            return Direction.NORTH;
          case NORTH_WEST:
            return Direction.NORTH_WEST;
          default:
            return d;
        }
      case DIAGONAL_REFLECTION_SE_NW:
        switch (d) {
          case NORTH:
            return Direction.EAST;
          case NORTH_EAST:
            return Direction.NORTH_EAST;
          case EAST:
            return Direction.NORTH;
          case SOUTH_EAST:
            return Direction.NORTH_WEST;
          case SOUTH:
            return Direction.WEST;
          case SOUTH_WEST:
            return Direction.SOUTH_WEST;
          case WEST:
            return Direction.SOUTH;
          case NORTH_WEST:
            return Direction.SOUTH_EAST;
          default:
            return d;
        }
      case ROTATION_OR_HORIZONTAL: // all fall through
      case ROTATION_OR_VERTICAL:
      case ROTATION_OR_DIAGONAL_SW_NE:
      case ROTATION_OR_DIAGONAL_SE_NW:
        return d.opposite(); // assume rotation for now
      default:
        return null;
    }
  }
  */

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
      if(d < distance) {
        close = locs[i];
        distance = d;
      }
    }

    return close;
  }

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

    // RC.setIndicatorString(2, Integer.toString(d));

    bytecodes = bc;
    return d;
  }

  /**
   * Max radius in which hq can hit us.
   */
  public static final int SAFE_RADIUS_SQUARED = 25;

  private static ArraySet<MapLocation> unsafeLocs;
  private static boolean[][] unsafe;

  /*
  public static ArraySet<MapLocation> getUnsafeLocs() {
    if (unsafeLocs == null) {
      unsafeLocs = new ArraySet<MapLocation>(200);
      for (MapLocation loc : MapLocation.getAllMapLocationsWithinRadiusSq(ENEMY_HQ,
          SAFE_RADIUS_SQUARED)) {
        if (RC.isPathable(TYPE, loc) && !isSafe(loc)) {
          unsafeLocs.insert(loc);
        }
      }
    }
    return unsafeLocs;
  }

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
  */
}
