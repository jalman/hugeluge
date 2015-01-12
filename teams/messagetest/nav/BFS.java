package messagetest.nav;

import static messagetest.utils.Utils.*;
import messagetest.utils.*;
import battlecode.common.*;

public class BFS {

  public boolean broadcast;

  /**
   * Starting locations of the BFS.
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

  /**
   * Distance to a source location.
   */
  public int distance[][] = new int[WRAP_X][WRAP_Y];

  // private final BucketQueue<MapLocation> queue = new BucketQueue<MapLocation>(2 * MAP_MAX_SIZE,
  // 50);
  private final ArrayQueue<MapLocation> queue = new ArrayQueue<MapLocation>(MAP_MAX_SIZE);

  public BFS(boolean broadcast, MapLocation... sources) {
    this.broadcast = broadcast;
    this.sources = new LocSet(sources);
    for (MapLocation source : sources) {
      try {
        Pair<Direction, Integer> info = messagingSystem.readPathingInfo(source);
        addLocation(source, info.first, info.second);
      } catch (GameActionException e) {
        e.printStackTrace();
      }
    }
  }

  public boolean done() {
    return queue.isEmpty();
  }

  private void addLocation(MapLocation loc, Direction dir, int d) {
    queue.push(loc);

    int x = loc.x % WRAP_X;
    int y = loc.y % WRAP_Y;

    from[x][y] = dir;
    distance[x][y] = d;
  }

  /**
   * Compute until either bytecodes have run out or we find a destination.
   * @param bytecodes The bytecode limit.
   * @param broadcast Whether to broadcast the results (used by the HQ).
   * @param dests Destinations to stop at.
   * @return Whether we found a destination.
   */
  public boolean compute(int bytecodes, MapLocation... dests) {
    final LocSet locs = new LocSet(dests);

    Function<MapLocation, Boolean> stop = new Function<MapLocation, Boolean>() {

      @Override
      public Boolean apply(MapLocation loc) {
        return !locs.contains(loc);
      }

    };

    return compute(bytecodes, stop);
  }

  /**
   * Compute until either bytecodes have run out or we find a destination.
   * @param end Hash-map of destination.
   * @param bytecodes The bytecode limit.
   * @param broadcast Whether to broadcast the results (used by the HQ).
   * @return Whether we found a destination.
   */
  public boolean compute(int bytecodes, Function<MapLocation, Boolean> stop) {
    // cache variables
    int d, x, y;
    MapLocation next, nbr;
    Pair<Direction, Integer> info;
    // MapLocation prev, p;
    Direction dir;
    final ArrayQueue<MapLocation> queue = this.queue;
    final int[][] distance = this.distance;
    final Direction[][] from = this.from;
    // final MapLocation[][] parent = this.parent;
    // final boolean[][] unsafe = getUnsafe();

    // int iters = 0;
    // int bc = Clock.getBytecodeNum();

    while (!queue.isEmpty()) {
      // iters++;
      if (Clock.getBytecodeNum() >= bytecodes - 500) {
        break;
      }

      next = queue.pop();

      x = next.x % WRAP_X;
      y = next.y % WRAP_Y;

      dir = from[x][y];
      d = distance[x][y] + 1;
      // if (unsafe[x][y]) d += 100;

      int i;
      switch (dir) {
        case NONE:
        case OMNI:
          dir = Direction.NORTH;
          i = 4;
          break;
        default:
          dir = dir.rotateLeft().rotateLeft();
          i = 3;
          break;
      }

      for (; --i >= 0; dir = dir.rotateRight().rotateRight()) {
        nbr = next.add(dir);
        if (stop.apply(nbr)) continue;

        TerrainTile tile = RC.senseTerrainTile(nbr);

        switch (tile) {
          case OFF_MAP:
          case VOID:
          case UNKNOWN:
            break;
          case NORMAL:

            try {
              info = messagingSystem.readPathingInfo(next);
              if (info.first != null && info.second < d) break;
            } catch (GameActionException e) {
              e.printStackTrace();
            }

            if (broadcast) {
              try {
                messagingSystem.writePathingInfo(next, dir, d);
              } catch (GameActionException e) {
                e.printStackTrace();
              }
            }

            addLocation(nbr, dir, d);
            break;
        }
      }

    }

    // bc = Clock.getBytecodeNum() - bc;
    // RC.setIndicatorString(2, "average BFS bytecodes: " + bc / iters);

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
