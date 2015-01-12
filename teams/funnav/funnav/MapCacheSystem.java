package funnav.funnav;

import static funnav.utils.Utils.*;
import battlecode.common.*;
import funnav.utils.*;

/** This data structure caches the terrain of the world map as sensed by one robot.
 * It stores a 256x256 boolean array representing the tiles that are walls.
 *
 * We set the coordinate (128,128) in the cache to correspond to the location of
 * our team's power core, and linearly shift between cache coordinates and world coordinates
 * using this transformation.
 */
public class MapCacheSystem {
  public final static int MAP_SIZE = 256;
  public final static int HQ_POSITION = 128;
  public final static int MAP_BLOCK_SIZE = 4;
  public final static int PACKED_MAP_SIZE = 64;

  /** True if the tile is a wall, false if the tile is ground or out of bounds. */
  final boolean[][] isWall;
  final int[][] packedIsWall;
  /** True if we have sensed the tile or been told by another robot about the tile. */
  final boolean[][] sensed;
  final int[][] packedSensed;
  final FastUShortSet packedDataUpdated;
  /** The edges of the map, in cache coordinates. <br>
   * These values are all exclusive, so anything with one of these coordinates is out of bounds.
   */
  public int edgeXMin, edgeXMax, edgeYMin, edgeYMax;
  /** Just a magic number to optimize the senseAllTiles() function. */
  int senseRadius;
  /** optimized sensing list of position vectors for each unit */
  // TODO (damien) need to compute this!
  private final int[][][] optimizedSensingList;

  public MapCacheSystem() {
    isWall = new boolean[MAP_SIZE][MAP_SIZE];
    sensed = new boolean[MAP_SIZE][MAP_SIZE];
    packedIsWall = new int[PACKED_MAP_SIZE][PACKED_MAP_SIZE];
    packedSensed = new int[PACKED_MAP_SIZE][PACKED_MAP_SIZE];
    packedDataUpdated = new FastUShortSet();
    initPackedDataStructures();
    edgeXMin = 0;
    edgeXMax = 0;
    edgeYMin = 0;
    edgeYMax = 0;
    senseRadius = (int)Math.sqrt(TYPE.sensorRadiusSquared);
    optimizedSensingList = newlySensedTiles;
  }
  private void initPackedDataStructures() {
    //17,47 are optimized magic numbers from (128-60)/4 and (128+60)/4
    for(int xb=17; xb<47; xb++) for(int yb=17; yb<47; yb++) {
      packedIsWall[xb][yb] = xb*(1<<22)+yb*(1<<16);
    }
    for(int xb=17; xb<47; xb++)
      System.arraycopy(packedIsWall[xb], 17, packedSensed[xb], 17, 30);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("\nSurrounding map data:\n");
    int myX = worldToCacheX(curX);
    int myY = worldToCacheY(curY);
    for(int y=myY-10; y<myY+10; y++) {
      for(int x=myX-10; x<myX+10; x++)
        sb.append((y==myY&&x==myX)?'x':(!sensed[x][y])?'o':(isWall[x][y])?'#':'.');
      sb.append("\n");
    }
    sb.append("Edge data: \nx=["+edgeXMin+","+edgeXMax+"] y=["+edgeYMin+","+edgeYMax+"] \n");
    return sb.toString();
  }

  /** Sense all tiles, all map edges, and all power nodes in the robot's sensing range. */
  public void senseAll() {
    // skip for certain kinds of robots?
    senseAllTiles();
    senseAllMapEdges();
  }
  /** Sense all tiles, all map edges, and all power nodes in the robot's sensing range. <br>
   * Assumes that we just moved in a direction, and we only want to sense the new information.
   */
  public void senseAfterMove(Direction lastMoved) {
    // skip for certain kinds of robots?
    if(lastMoved==null || lastMoved==Direction.NONE || lastMoved==Direction.OMNI) {
      return;
    }
    senseTilesOptimized(lastMoved);
    senseMapEdgesOptimized(lastMoved);
  }

  /** Senses the terrain of all tiles in sensing range of the robot.
   * Should be called when a unit (probably only archon or scout) is newly spawned.
   */
  private void senseAllTiles() {
    int myX = worldToCacheX(curX);
    int myY = worldToCacheY(curY);
    for(int dx=-senseRadius; dx<=senseRadius; dx++) for(int dy=-senseRadius; dy<=senseRadius; dy++) {
      int x = myX+dx;
      int y = myY+dy;
      int xblock = x/MAP_BLOCK_SIZE;
      int yblock = y/MAP_BLOCK_SIZE;
      if(sensed[x][y]) continue;
      MapLocation loc = currentLocation.add(dx, dy);
      TerrainTile tt = RC.senseTerrainTile(loc);
      if(tt!=null) {
        boolean b = (tt!=TerrainTile.NORMAL);
        isWall[x][y] = b;
        if(b) packedIsWall[xblock][yblock] |= (1 << (x%4*4+y%4));
        sensed[x][y] = true;
        packedSensed[xblock][yblock] |= (1 << (x%4*4+y%4));
      }
    }
  }
  /**
   * A more optimized way of sensing tiles.
   * Given a direction we just moved in, senses only the tiles that are new.
   * Assumes that we have sensed everything we could have in the past.
   * @param lastMoved the direction we just moved in
   */
  private void senseTilesOptimized(Direction lastMoved) {
    final int[][] list = optimizedSensingList[lastMoved.ordinal()];
    MapLocation myLoc = currentLocation;
    int myX = worldToCacheX(myLoc.x);
    int myY = worldToCacheY(myLoc.y);
    TangentBug tb = navSystem.tangentBug;
    for (int i=0; i<list.length; i++) {
      int dx = list[i][0];
      int dy = list[i][1];
      int x = myX+dx;
      int y = myY+dy;
      int xblock = x/MAP_BLOCK_SIZE;
      int yblock = y/MAP_BLOCK_SIZE;
      if(sensed[x][y]) continue;
      MapLocation loc = myLoc.add(dx, dy);
      TerrainTile tt = RC.senseTerrainTile(loc);
      if(tt!=null) {
        boolean b = (tt != TerrainTile.NORMAL);
        isWall[x][y] = b;
        if(b) {
          packedIsWall[xblock][yblock] |= (1 << (x%4*4+y%4));
          if(tb.wallCache[x][y] > tb.curWallCacheID * TangentBug.BUFFER_LENGTH)
            tb.reset();
        }
        sensed[x][y] = true;
        packedSensed[xblock][yblock] |= (1 << (x%4*4+y%4));
      }
    }
  }

  /** Combines packed terrain data with existing packed terrain data. */
  public void integrateTerrainInfo(int packedIsWallInfo, int packedSensedInfo) {
    int block = (packedIsWallInfo >> 16);
    int xblock = block / 64;
    int yblock = block % 64;
    if(packedSensed[xblock][yblock]!=packedSensedInfo) {
      packedDataUpdated.add(block);
      packedIsWall[xblock][yblock] |= packedIsWallInfo;
      packedSensed[xblock][yblock] |= packedSensedInfo;
    }
  }
  /** Extracts some packed data in the updated packed fields into the
   * unpacked arrays for terrain data. <br>
   * This must be run repeatedly to extract everything.
   * @return true if we are done extracting
   */
  public boolean extractUpdatedPackedDataStep() {
    if(packedDataUpdated.isEmpty()) return true;
    int block = packedDataUpdated.pop();
    int xblock = block / 64;
    int yblock = block % 64;
    int isWallData = packedIsWall[xblock][yblock];
    int sensedData = packedSensed[xblock][yblock];
    for(int bit=0; bit<16; bit++) {
      int x = xblock*MAP_BLOCK_SIZE+bit/4;
      int y = yblock*MAP_BLOCK_SIZE+bit%4;
      isWall[x][y] = ((isWallData & (1<<bit)) != 0);
      sensed[x][y] = ((sensedData & (1<<bit)) != 0);
    }
    return false;
  }

  /** Updates the edges of the map that we can sense.
   * Checks all four cardinal directions for edges.
   * Should be called in the beginning of the game. */
  private void senseAllMapEdges() {
    MapLocation myLoc = currentLocation;
    if(edgeXMin==0 &&
        RC.senseTerrainTile(myLoc.add(Direction.WEST, senseRadius)) == TerrainTile.OFF_MAP) {
      int d = senseRadius;
      while (RC.senseTerrainTile(myLoc.add(Direction.WEST, d - 1)) == TerrainTile.OFF_MAP) {
        d--;
      }
      edgeXMin = worldToCacheX(myLoc.x) - d;
    }
    if(edgeXMax==0 &&
        RC.senseTerrainTile(myLoc.add(Direction.EAST, senseRadius)) == TerrainTile.OFF_MAP) {
      int d = senseRadius;
      while (RC.senseTerrainTile(myLoc.add(Direction.EAST, d - 1)) == TerrainTile.OFF_MAP) {
        d--;
      }
      edgeXMax = worldToCacheX(myLoc.x) + d;
    }
    if(edgeYMin==0 &&
        RC.senseTerrainTile(myLoc.add(Direction.NORTH, senseRadius)) == TerrainTile.OFF_MAP) {
      int d = senseRadius;
      while (RC.senseTerrainTile(myLoc.add(Direction.NORTH, d - 1)) == TerrainTile.OFF_MAP) {
        d--;
      }
      edgeYMin = worldToCacheY(myLoc.y) - d;
    }
    if(edgeYMax==0 &&
        RC.senseTerrainTile(myLoc.add(Direction.SOUTH, senseRadius)) == TerrainTile.OFF_MAP) {
      int d = senseRadius;
      while (RC.senseTerrainTile(myLoc.add(Direction.SOUTH, d - 1)) == TerrainTile.OFF_MAP) {
        d--;
      }
      edgeYMax = worldToCacheY(myLoc.y) + d;
    }
  }
  /** Updates the edges of the map that we can sense.
   * Only checks for edges in directions that we are moving towards.
   *
   * For example, if we just moved NORTH, we only need to check to the north of us for a new wall,
   * since a wall could not have appeared in any other direction. */
  private void senseMapEdgesOptimized(Direction lastMoved) {
    MapLocation myLoc = currentLocation;
    if(edgeXMin==0 && lastMoved.dx==-1 &&
        RC.senseTerrainTile(myLoc.add(Direction.WEST, senseRadius)) == TerrainTile.OFF_MAP) {
      int d = senseRadius;
      // Note that some of this code is not necessary if we use the system properly
      // But it adds a little bit of robustness
      while (RC.senseTerrainTile(myLoc.add(Direction.WEST, d - 1)) == TerrainTile.OFF_MAP) {
        d--;
      }
      edgeXMin = worldToCacheX(myLoc.x) - d;
    }
    if(edgeXMax==0 && lastMoved.dx==1 &&
        RC.senseTerrainTile(myLoc.add(Direction.EAST, senseRadius)) == TerrainTile.OFF_MAP) {
      int d = senseRadius;
      while (RC.senseTerrainTile(myLoc.add(Direction.EAST, d - 1)) == TerrainTile.OFF_MAP) {
        d--;
      }
      edgeXMax = worldToCacheX(myLoc.x) + d;
    }
    if(edgeYMin==0 && lastMoved.dy==-1 &&
        RC.senseTerrainTile(myLoc.add(Direction.NORTH, senseRadius)) == TerrainTile.OFF_MAP) {
      int d = senseRadius;
      while (RC.senseTerrainTile(myLoc.add(Direction.NORTH, d - 1)) == TerrainTile.OFF_MAP) {
        d--;
      }
      edgeYMin = worldToCacheY(myLoc.y) - d;
    }
    if(edgeYMax==0 && lastMoved.dy==1 &&
        RC.senseTerrainTile(myLoc.add(Direction.SOUTH, senseRadius)) == TerrainTile.OFF_MAP) {
      int d = senseRadius;
      while (RC.senseTerrainTile(myLoc.add(Direction.SOUTH, d - 1)) == TerrainTile.OFF_MAP) {
        d--;
      }
      edgeYMax = worldToCacheY(myLoc.y) + d;
    }
  }

  /** Does this robot know about the terrain of the given map location? */
  public boolean isSensed(MapLocation loc) {
    return sensed[worldToCacheX(loc.x)][worldToCacheY(loc.y)];
  }
  /** Is the given map location a wall tile (or an off map tile)? <br>
   * Will return false if the robot does not know.
   */
  public boolean isWall(MapLocation loc) {
    return isWall[worldToCacheX(loc.x)][worldToCacheY(loc.y)];
  }
  /** Is the given map location an off map tile? <br>
   *  Will return false if the robot does not know.
   */
  public boolean isOffMap(MapLocation loc) {
    int x = worldToCacheX(loc.x);
    int y = worldToCacheY(loc.y);
    return edgeXMin!=0 && x<=edgeXMin || edgeXMax!=0 && x>=edgeXMax ||
        edgeYMin!=0 && y<=edgeYMin || edgeYMax!=0 && y>=edgeYMax;
  }

  /** Converts from world x coordinates to cache x coordinates. */
  public int worldToCacheX(int worldX) {
    return worldX - HQ_WORLD_X + HQ_POSITION;
  }
  /** Converts from world x coordinates to cache x coordinates. */
  public int cacheToWorldX(int cacheX) {
    return cacheX + HQ_WORLD_X - HQ_POSITION;
  }
  /** Converts from cache y coordinates to world y coordinates. */
  public int worldToCacheY(int worldY) {
    return worldY - HQ_WORLD_Y + HQ_POSITION;
  }
  /** Converts from cache y coordinates to world y coordinates. */
  public int cacheToWorldY(int cacheY) {
    return cacheY + HQ_WORLD_Y - HQ_POSITION;
  }


  private static final int[][][] newlySensedTiles = new int[][][] { //SCOUT
    { //NORTH
      {-5, -2},{-5, -1},{-5, 0},{-5, 1},{-5, 2},{-4, -3},{-4, 3},{-3, -4},{-3, 4}
    }
    ,{ //NORTH_EAST
      {-5, -1},{-5, 0},{-5, 1},{-5, 2},{-5, 3},{-4, 3},{-4, 4},{-3, 4},{-3, 5},{-2, 5},{-1, 5},{0, 5},{1, 5}
    }
    ,{ //EAST
      {-4, 3},{-3, 4},{-2, 5},{-1, 5},{0, 5},{1, 5},{2, 5},{3, 4},{4, 3}
    }
    ,{ //SOUTH_EAST
      {-1, 5},{0, 5},{1, 5},{2, 5},{3, 4},{3, 5},{4, 3},{4, 4},{5, -1},{5, 0},{5, 1},{5, 2},{5, 3}
    }
    ,{ //SOUTH
      {3, -4},{3, 4},{4, -3},{4, 3},{5, -2},{5, -1},{5, 0},{5, 1},{5, 2}
    }
    ,{ //SOUTH_WEST
      {-1, -5},{0, -5},{1, -5},{2, -5},{3, -5},{3, -4},{4, -4},{4, -3},{5, -3},{5, -2},{5, -1},{5, 0},{5, 1}
    }
    ,{ //WEST
      {-4, -3},{-3, -4},{-2, -5},{-1, -5},{0, -5},{1, -5},{2, -5},{3, -4},{4, -3}
    }
    ,{ //NORTH_WEST
      {-5, -3},{-5, -2},{-5, -1},{-5, 0},{-5, 1},{-4, -4},{-4, -3},{-3, -5},{-3, -4},{-2, -5},{-1, -5},{0, -5},{1, -5}
    }
  };
}
