package messagetest.nav;

import static messagetest.utils.Utils.*;
import messagetest.utils.*;
import battlecode.common.*;

public class DStar extends GradientMover {

  /**
   * Starting locations of DStar.
   */
  public ArraySet<MapLocation> sources;

  public MapLocation dest;

  /**
   * Direction in which we traveled to get to this location.
   */
  public Direction[][] from = new Direction[WRAP_X][WRAP_Y];

  // public boolean[][][] to = new boolean[WRAP_X][WRAP_Y][8];

  /**
   * Conjectured distance to this location.
   */
  public int distance[][] = new int[WRAP_X][WRAP_Y];

  /**
   * Estimated distance from source + distance to dest.
   */
  // private final int estimate[][] = new int[MAP_WIDTH][MAP_HEIGHT];

  private final boolean expanded[][] = new boolean[WRAP_X][WRAP_Y];

  private final BucketQueue<MapLocation> queue = new BucketQueue<MapLocation>(MAP_MAX_SIZE, 8);

  public DStar() {
    // hack to go around the hqs
    /*
     * int x = wrapX(ALLY_HQ.x);
     * int y = wrapY(ALLY_HQ.y);
     * distance[x][y] = Integer.MAX_VALUE;
     * expanded[x][y] = true;
     */

    // TODO: HQs and towers are not invincible, but still scary
    /*
     * ArraySet<MapLocation> unsafeLocs = getUnsafeLocs();
     * for (int i = unsafeLocs.size; --i >= 0;) {
     * MapLocation loc = unsafeLocs.get(i);
     * x = wrapX(loc.x);
     * y = wrapY(loc.y);
     * distance[x][y] = Integer.MAX_VALUE;
     * expanded[x][y] = true;
     * }
     */
  }

  public DStar(ArraySet<MapLocation> sources, int[] distances) {
    this();
    this.sources = sources;

    for (int i = sources.size; --i >= 0;) {
      insert(sources.get(i), distances[i]);
    }
  }

  public int heuristic(MapLocation loc) {
    // TODO: min over sources?
    return octile(loc, sources.get(0));
  }

  public void insert(MapLocation loc, int distance) {
    int x = loc.x % WRAP_X;
    int y = loc.y % WRAP_Y;

    int e = distance + heuristic(loc);
    queue.insert(e, loc);
    this.distance[x][y] = distance;
    // leave as null to cause exceptions if we accidentally try to use it?
    from[x][y] = Direction.NONE;
  }

  public void insert(MapLocation loc, int distance, Direction dir) {
    int x = loc.x % WRAP_X;
    int y = loc.y % WRAP_Y;

    expanded[x][y] = false;
    this.distance[x][y] = distance;
    // TODO: min over sources?
    int e = distance + heuristic(loc);
    queue.insert(e, loc);
    from[x][y] = dir;
  }

  /**
   * Discover that a tile is blocked.
   * @param loc The tile's location.
   */
  public void remove(MapLocation blocked) {
    // TODO: speed this up?
    for (Direction dir : REGULAR_DIRECTIONS) {
      MapLocation loc = blocked.add(dir);
      int x = wrapX(loc.x);
      int y = wrapY(loc.y);

      if (from[x][y] == dir) {
        from[x][y] = null;
        expanded[x][y] = false;
        remove(loc);
      }
    }
  }

  @Override
  public void compute(int bytecodes) {
    // cache variables
    int d, w, e, x, y;
    MapLocation next, nbr;
    Direction dir;
    final BucketQueue<MapLocation> queue = this.queue;
    final int[][] distance = this.distance;
    final Direction[][] from = this.from;

    // int iters = 0;
    // int bc = Clock.getBytecodeNum();

    while (!done()) {
      // iters++;
      if (Clock.getBytecodeNum() >= bytecodes - 600) {
        break;
      }

      // RC.setIndicatorString(0, Integer.toString(min));
      // ALERT: queue.min is valid only after a call to deleteMin()!
      next = queue.deleteMin();

      x = next.x % WRAP_X;
      y = next.y % WRAP_Y;

      dir = from[x][y];

      // node is no longer valid due to map updates
      if (dir == null) continue;

      // check if node has already been expanded
      if (expanded[x][y]) continue;
      expanded[x][y] = true;

      d = distance[x][y];

      /*
       * if (broadcast) {
       * try {
       * messagingSystem.writePathingDirection(next, from[x][y]);
       * } catch (GameActionException ex) {
       * ex.printStackTrace();
       * }
       * }
       */

      Direction[] nbrs = Neighbors.getNeighborsOpt(next, dir);

      w = d + 1;
      for (Direction ndir : nbrs) {
        nbr = next.add(ndir);
        // TODO: how are UNKNOWN tiles handled?
        if (RC.senseTerrainTile(nbr).isTraversable()) {
          e = w + heuristic(nbr);

          x = nbr.x % WRAP_X;
          y = nbr.y % WRAP_Y;

          if (from[x][y] == null) {
            queue.insert(e, nbr);
            distance[x][y] = w;
            // estimate[x][y] = e;
            from[x][y] = ndir;
          } else {
            if (w < distance[x][y] || from[x][y] == ndir) {
              queue.insert(e, nbr);
              distance[x][y] = w;
              // estimate[x][y] = e;
              from[x][y] = ndir;
              expanded[x][y] = false;
            }
          }
        }
      }
    }

    // bc = Clock.getBytecodeNum() - bc;
    // RC.setIndicatorString(2, "average DStar bytecodes: " + (iters > 0 ? bc / iters : bc));
  }

  public int getDistance(int x, int y) {
    return from[x][y] != null ? distance[x][y] : Integer.MAX_VALUE;
  }

  public int getDistance(MapLocation loc) {
    return getDistance(loc.x, loc.y);
  }

  @Override
  public int getWeight(MapLocation loc) {
    return getDistance(loc.x, loc.y);
  }

  public boolean visited(MapLocation loc) {
    return from[wrapX(loc.x)][wrapY(loc.y)] != null;
  }

  /**
   * @return Whether we have found ourselves.
   */
  public boolean done() {
    return visited(currentLocation);
  }

  @Override
  public void setTarget(MapLocation finish) {
    // TODO Auto-generated method stub

  }
}
