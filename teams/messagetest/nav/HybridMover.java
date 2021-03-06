package messagetest.nav;

import static messagetest.utils.Utils.*;
import battlecode.common.*;

public class HybridMover {
  public static MapLocation DIJKSTRA_CENTER = ALLY_HQ;

  NavAlg simple = new DumbMover();

  MapLocation dest = null;

  private MapLocation simpleTarget;

  private class Computation {
    DStar dstar = new DStar();
    MapLocation next = dest;
    boolean outPathDone = false;
    int length = 0;
    int distance = 0;

    Computation() {
      dstar.dest = currentLocation;
      dstar.insert(next, distance);
    }

    void computeOutPath(int bytecodes) throws GameActionException {
      if (outPathDone) return;

      Direction dir = messagingSystem.readPathingInfo(next).first;

      if (dir == null) return;

      // RC.setIndicatorString(2, "Computing outPath");

      while (Clock.getBytecodeNum() <= bytecodes) {
        next = next.subtract(dir);

        if (next.equals(DIJKSTRA_CENTER)) break;

        dir = messagingSystem.readPathingInfo(next).first;

        int diff = 1;// getActionDelay(next, dir);
        diff = Math.max(1, diff / (1 + (++length) / 10));

        distance += diff;

        dstar.insert(next, distance, dir.opposite());
        // loc = messagingSystem.readParent(loc);
      }

      // RC.setIndicatorString(2, "outPath done");
      outPathDone = true;
    }

    void compute() throws GameActionException {
      dstar.dest = currentLocation;

      if (!outPathDone) {
        computeOutPath(5000);
      }

      if (!dstar.visited(currentLocation)) {
        dstar.compute(8000);
      } else {
        dstar.compute(3000);
      }
    }

  }

  private final Computation[][] cache = new Computation[WRAP_X][WRAP_Y];

  private Computation getComputation() {
    int x = dest.x % WRAP_X;
    int y = dest.y % WRAP_Y;

    if (cache[x][y] == null) {
      cache[x][y] = new Computation();
    }
    return cache[x][y];
  }

  private void simpleMove(MapLocation loc) throws GameActionException {
    if (!loc.equals(simpleTarget)) {
      simpleTarget = loc;
      simple.setTarget(loc);
    }

    Direction dir = simple.getNextDir();
    move(dir);
  }

  public void move(MapLocation target) throws GameActionException {
    this.dest = target;
    move();
  }

  private boolean move(Direction dir) throws GameActionException {
    if (!RC.isCoreReady() || !RC.canMove(dir)) return false;
    RC.move(dir);
    return true;
  }

  public void move() throws GameActionException {
    if (currentLocation.equals(dest)) return;

    simpleMove(dest);

    /*
     * Computation computation = getComputation();
     * computation.compute();
     * DStar dstar = computation.dstar;
     * if (!dstar.visited(currentLocation)) {
     * Pair<Direction, Integer> pathingInfo = messagingSystem.readPathingInfo(currentLocation);
     * if (pathingInfo.first != null && computation.length > 1 &&
     * pathingInfo.second <= naiveDistance(currentLocation, dest)) {
     * // RC.setIndicatorString(1, "move to hq");
     * DijkstraMover.getDijkstraMover().move();
     * } else {
     * // RC.setIndicatorString(1, "simple move");
     * simpleMove(dest);
     * }
     * } else {
     * // RC.setIndicatorString(1, "dstar move");
     * dstar.move();
     * }
     */
  }

  public boolean arrived() {
    return currentLocation.equals(dest);
  }
}
