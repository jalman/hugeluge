package messagetest.nav;

import static messagetest.utils.Utils.*;
import messagetest.utils.*;
import battlecode.common.*;

public class Dijkstra {

  /**
   * Starting locations of Dijkstra.
   */
  public final LocSet sources;
  /**
   * First location reached among the destinations.
   */
  public MapLocation reached = null;
  /**
   * Direction in which we traveled to get to this location.
   */
  public Direction[][] from = new Direction[WRAP_X][WRAP_Y];

  public static final int PARENT_DIST = 50;
  /**
   * Used to speed up path lookup.
   */
  // public MapLocation[][] parent = new MapLocation[WRAP_X][WRAP_Y];
  /**
   * Distance (times 5) to this location.
   */
  public int distance[][] = new int[WRAP_X][WRAP_Y];

  private final BucketQueue<MapLocation> queue = new BucketQueue<MapLocation>(2 * MAP_MAX_SIZE, 50);

  public Dijkstra(MapLocation... sources) {
    this.sources = new LocSet(sources);
    for (MapLocation source : sources) {
      queue.insert(0, source);
      distance[source.x % WRAP_X][source.y % WRAP_Y] = 0;
      // parent[source.x][source.y] = source;
      // leave as null to cause exceptions if we accidentally try to use it
      // from[source.x][source.y] = Direction.NONE;
    }

  }

  public boolean done() {
    return queue.size == 0;
  }

  /**
   * Compute until either bytecodes have run out or we find a destination.
   * @param bytecodes The bytecode limit.
   * @param broadcast Whether to broadcast the results (used by the HQ).
   * @param dests Destinations to stop at.
   * @return Whether we found a destination.
   */
  public boolean compute(int bytecodes, boolean broadcast, MapLocation... dests) {
    return compute(new LocSet(dests), bytecodes, broadcast);
  }

  /**
   * Compute until either bytecodes have run out or we find a destination.
   * @param end Hash-map of destination.
   * @param bytecodes The bytecode limit.
   * @param broadcast Whether to broadcast the results (used by the HQ).
   * @return Whether we found a destination.
   */
  public boolean compute(LocSet end, int bytecodes, boolean broadcast) {
    // cache variables
    int min, w, x, y;
    int[] weight;
    MapLocation next, nbr;
    // MapLocation prev, p;
    Direction dir;
    final BucketQueue<MapLocation> queue = this.queue;
    final int[][] distance = this.distance;
    final Direction[][] from = this.from;
    // final MapLocation[][] parent = this.parent;
    final boolean[][] unsafe = getUnsafe();

    // int iters = 0;
    // int bc = Clock.getBytecodeNum();

    while (queue.size > 0) {
      // iters++;
      if (Clock.getBytecodeNum() >= bytecodes - 500) {
        break;
      }

      // RC.setIndicatorString(0, Integer.toString(min));
      // ALERT: queue.min is valid only after a call to deleteMin()!
      next = queue.deleteMin();
      min = queue.min;

      x = next.x % WRAP_X;
      y = next.y % WRAP_Y;

      // check if we have already visited this node
      if (min == distance[x][y]) {
        if (unsafe[x][y]) min += 100;

        dir = from[x][y];

        /*
         * if (dir != null) {
         * prev = next.subtract(dir);
         * p = parent[prev.x][prev.y];
         * if (min <= distance[p.x][p.y] + PARENT_DIST) {
         * parent[x][y] = p;
         * } else {
         * parent[x][y] = prev;
         * }
         * }
         */

        if (broadcast) {
          try {
            messagingSystem.writePathingInfo(next, dir, min, null /* parent[x][y] */);
          } catch (GameActionException e) {
            // e.printStackTrace();
          }
        }

        // if (end[x][y]) {
        // reached = next;
        // break;
        // }


        // TODO: Huge problem here; the terrain tile might be OFF_MAP or UNKNOWN
        weight = WEIGHT[RC.senseTerrainTile(next).ordinal()];

        int i;
        if (dir == null) {
          dir = Direction.NORTH;
          i = 8;
        } else if (dir.isDiagonal()) {
          dir = dir.rotateLeft().rotateLeft();
          i = 5;
        } else {
          dir = dir.rotateLeft();
          i = 3;
        }

        for (; --i >= 0; dir = dir.rotateRight()) {
          nbr = next.add(dir);
          if (RC.senseTerrainTile(nbr).isTraversable()) {
            w = min + weight[dir.ordinal()];

            x = nbr.x % WRAP_X;
            y = nbr.y % WRAP_Y;

            if (from[x][y] == null) {
              queue.insert_fast(w, nbr);
              // System.out.println("inserted " + nbr + " with distance " + w + " from " + dir);
              distance[x][y] = w;
              from[x][y] = dir;
            } else {
              if (w < distance[x][y]) {
                queue.insert_fast(w, nbr);
                distance[x][y] = w;
                from[x][y] = dir;
              }
            }
          }
        }
      }
    }

    // bc = Clock.getBytecodeNum() - bc;
    // RC.setIndicatorString(2, "average Dijkstra bytecodes: " + bc / iters);

    return reached != null;
  }

  /**
   * Computes a path from a location to the nearest source.
   * Contains both start and end points.
   * @param loc The location.
   * @return The path as a LocSet.
   */
  public LocSet getPath(MapLocation loc) {
    LocSet path = new LocSet();
    path.insert(loc);

    while (!sources.contains(loc)) {
      loc = loc.subtract(from[loc.x % WRAP_X][loc.y % WRAP_Y]);
      path.insert(loc);
    }

    return path;
  }

  public boolean visited(MapLocation loc) {
    return from[loc.x % WRAP_X][loc.y % WRAP_Y] != null;
  }
}
