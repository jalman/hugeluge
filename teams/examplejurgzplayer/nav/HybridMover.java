package examplejurgzplayer.nav;

import static examplejurgzplayer.utils.Utils.*;
import examplejurgzplayer.utils.*;
import battlecode.common.*;

public class HybridMover {
  public static MapLocation DIJKSTRA_CENTER = ALLY_HQ;

  NavAlg simple = new BugMoveFun2();

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

      while (true) {
        if (Clock.getBytecodeNum() > bytecodes) return;

        next = next.subtract(dir);

        if (next.equals(DIJKSTRA_CENTER)) break;

        dir = messagingSystem.readPathingInfo(next).first;

        int diff = getActionDelay(next, dir);
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

  private final Computation[][] cache = new Computation[MAP_WIDTH][MAP_HEIGHT];

  private Computation getComputation() {
    if (cache[dest.x][dest.y] == null) {
      cache[dest.x][dest.y] = new Computation();
    }
    return cache[dest.x][dest.y];
  }


  private void simpleMove(MapLocation loc) throws GameActionException {
    if (!loc.equals(simpleTarget)) {
      simpleTarget = loc;
      simple.recompute(loc);
    }

    Direction dir = simple.getNextDir();
    move(dir);
  }
  
  private boolean move(Direction dir) throws GameActionException {
    if (!RC.isCoreReady() || !RC.canMove(dir)) return false;
        RC.move(dir);
    return true;
  }

  public void move(MapLocation target) throws GameActionException {
    this.dest = target;
    move();
  }

  public void move() throws GameActionException {
    if (currentLocation.equals(dest)) return;

    Computation computation = getComputation();
    computation.compute();

    DStar dstar = computation.dstar;

    if (!dstar.visited(currentLocation)) {
      Pair<Direction, Integer> pathingInfo = messagingSystem.readPathingInfo(currentLocation);
      if (pathingInfo.first != null && computation.length > 1 &&
          pathingInfo.second <= naiveDistance(currentLocation, dest)) {
        // RC.setIndicatorString(1, "move to hq");
        DijkstraMover.getDijkstraMover().move();
      } else {
        // RC.setIndicatorString(1, "simple move");
        simpleMove(dest);
      }
    } else {
      // RC.setIndicatorString(1, "dstar move");
      dstar.move();
    }
  }

  public boolean arrived() {
    return currentLocation.equals(dest);
  }
}
