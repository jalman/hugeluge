package funnav.funnav;

import static funnav.utils.Utils.*;
import battlecode.common.*;

public class SharedExplorationSystem {
  public SharedExplorationSystem() {
  }

  /** Broadcasts robot's knowledge of one column of the map.
   * The column is decided by modding the turn number by robot's guess of the number
   * of columns on the map.
   */
  public void broadcastMapFragment() {
    int startRow;
    int numRowBlocks;
    int startCol;
    int numColBlocks;

    if(Clock.getRoundNum()/6%2==0) {
      if (mapCacheSystem.edgeXMin != 0) {
        startCol = (mapCacheSystem.edgeXMin + 1) / MapCacheSystem.MAP_BLOCK_SIZE;
        if (mapCacheSystem.edgeXMax != 0) {
          numColBlocks =
              mapCacheSystem.edgeXMax / MapCacheSystem.MAP_BLOCK_SIZE
              - (mapCacheSystem.edgeXMin + 1) / MapCacheSystem.MAP_BLOCK_SIZE + 1;
        } else {
          numColBlocks = 16;
        }
      } else if (mapCacheSystem.edgeXMax != 0) {
        numColBlocks = 16;
        startCol = mapCacheSystem.edgeXMax / MapCacheSystem.MAP_BLOCK_SIZE - numColBlocks + 1;
      } else {
        startCol = 0;
        numColBlocks = 64;
      }

      if (mapCacheSystem.edgeYMin != 0) {
        startRow = (mapCacheSystem.edgeYMin + 1) / MapCacheSystem.MAP_BLOCK_SIZE;
        if (mapCacheSystem.edgeYMax != 0) {
          numRowBlocks =
              mapCacheSystem.edgeYMax / MapCacheSystem.MAP_BLOCK_SIZE
              - (mapCacheSystem.edgeYMin + 1) / MapCacheSystem.MAP_BLOCK_SIZE + 1;
        } else {
          numRowBlocks = 16;
        }
      } else if (mapCacheSystem.edgeYMax != 0) {
        numRowBlocks = 16;
        startRow = mapCacheSystem.edgeYMax / MapCacheSystem.MAP_BLOCK_SIZE - numRowBlocks + 1;
      } else {
        startRow = 0;
        numRowBlocks = 64;
      }
    } else {
      int rotation = Clock.getRoundNum()/12%4;
      startRow =
          mapCacheSystem.worldToCacheY(curY) / MapCacheSystem.MAP_BLOCK_SIZE
          - (rotation / 2 * 2);
      startCol =
          mapCacheSystem.worldToCacheX(curX) / MapCacheSystem.MAP_BLOCK_SIZE
          - (rotation % 2 * 2);
      numRowBlocks = 3;
      numColBlocks = 3;
    }
    int xb = startCol + ((Clock.getRoundNum() / 12 + ID) % numColBlocks);

    int[] buffer = new int[256];
    int c=0;
    for(int yb=startRow; yb<startRow+numRowBlocks; yb++) {
      int data = mapCacheSystem.packedSensed[xb][yb];
      if(data % 65536 == 0) continue;
      buffer[c++] = mapCacheSystem.packedIsWall[xb][yb];
      buffer[c++] = data;
    }
    if(c>0) {
      int[] ints = new int[c];
      System.arraycopy(buffer, 0, ints, 0, c);
      // TODO (damien) figure out messaging
      try {
        messagingSystem.writeMapFragments(ints);
        // br.io.sendUInts(BroadcastChannel.EXPLORERS, BroadcastType.MAP_FRAGMENTS, ints);
      } catch (GameActionException e) {
        e.printStackTrace();
      }
    }
  }
  /** Broadcasts robot's knowledge of the four map edges. */
  public void broadcastMapEdges() {
    // TODO (damien) figure out messaging
    try {
      messagingSystem.writeMapEdges(
          mapCacheSystem.edgeXMin,
          mapCacheSystem.edgeXMax,
          mapCacheSystem.edgeYMin,
          mapCacheSystem.edgeYMax);
      // br.io.sendUShorts(BroadcastChannel.ALL, BroadcastType.MAP_EDGES, edges);
    } catch (GameActionException e) {
      e.printStackTrace();
    }
  }

  /** Receive data equivalent to one broadcast of a map fragment. */
  public void receiveMapFragment(int[] data) {
    for(int i=0; i<data.length; i+=2) {
      mapCacheSystem.integrateTerrainInfo(data[i], data[i + 1]);
    }
  }
  /** Receive data equivalent to one broadcast of the four map edges. */
  public void receiveMapEdges(int[] data) {
    if (mapCacheSystem.edgeXMin == 0) mapCacheSystem.edgeXMin = data[0];
    if (mapCacheSystem.edgeXMax == 0) mapCacheSystem.edgeXMax = data[1];
    if (mapCacheSystem.edgeYMin == 0) mapCacheSystem.edgeYMin = data[2];
    if (mapCacheSystem.edgeYMax == 0) mapCacheSystem.edgeYMax = data[3];
  }
}
