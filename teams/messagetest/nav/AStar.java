package messagetest.nav;

import static messagetest.utils.Utils.*;
import messagetest.utils.*;
import battlecode.common.*;

public class AStar extends GradientMover {
  public final MapLocation start;
  public MapLocation dest;

  private State initial;

  private static final int MAX_HEURISTIC = 10000;

  private class State {
    MapLocation loc;
    State parent;
    Direction from;
    TerrainTile tile;

    ArraySet<State> children = new ArraySet<State>(8);
    State minChild = null;

    int dStart;
    int heuristic;
    boolean updated;

    State() {
      loc = start;
      parent = null;
      from = Direction.NONE;
      dStart = 0;
      initHeuristic();
    }

    State(State parent, Direction from) {
      this.parent = parent;
      this.from = from;
      parent.children.insert(this);

      this.loc = parent.loc.add(from);
      this.dStart = parent.dStart + getDirWeight(from);
      this.initHeuristic();
    }

    void initHeuristic() {
      tile = RC.senseTerrainTile(loc);
      switch (tile) {
        case NORMAL:
        case UNKNOWN:
          heuristic = naiveDistance(loc, dest);
          break;
        case VOID:
        case OFF_MAP:
          heuristic = MAX_HEURISTIC;

      }
      updated = true;
    }

    int updateHeuristic() {
      if (updated) return heuristic;

      heuristic = MAX_HEURISTIC;

      if (children.size == 0) {
        System.out.println("NO CHILDREN");
      }

      for (int i = children.size; --i >= 0;) {
        State child = children.get(i);
        int h = child.updateHeuristic() + getDirWeight(child.from);
        if (h < heuristic) {
          heuristic = h;
          minChild = child;
        }
      }

      updated = true;

      return heuristic;
    }

    /**
     * @return This node's priority in the queue.
     */
    int priority() {
      return dStart + heuristic;
    }

    /**
     * Invalidate this node and all parents.
     */
    void touch() {
      if (updated) {
        updated = false;
        if (parent != null && parent.minChild == this) {
          parent.touch();
        }
      }
    }

    /**
     * Estimated distance to target.
     * @return
     */
    int worth() {
      int forwards = updateHeuristic();
      int backwards = dStart + initial.updateHeuristic();

      if (forwards <= backwards) {
        return forwards;
      } else {
        if (currentRound < 50) {
          System.out.println("Backtracking @ " + loc);
          System.out.println(forwards + " > " + backwards);
        }
        return backwards;
      }
    }

    @Override
    public String toString() {
      return loc + ", worth " + worth() + ", from " + from;
    }

  }

  private LocMap<State> states = new LocMap<State>();
  private LocSet blocked;

  private BucketQueue<State> queue = new BucketQueue<State>(10000, 8);

  // private ArrayQueue<State> queue = new ArrayQueue<State>(MAP_MAX_SIZE);

  public AStar(MapLocation dest) {
    this.start = currentLocation;
    this.dest = dest;

    // path around fixed points
    blocked = new LocSet(RC.senseTowerLocations());
    blocked.insert(ALLY_HQ);

    initial = new State();
    insert(initial);
  }

  private void insert(State s) {
    switch (s.tile) {
      case NORMAL:
        queue.insert(s.priority(), s);
        states.set(s.loc, s);
        break;
      case UNKNOWN:
        states.set(s.loc, s);
        break;
      default:
        break;
    }
  }

  @Override
  public void compute(int bytecodes) {
    while (queue.size > 0) {
      if (Clock.getBytecodeNum() >= bytecodes - 600) {
        break;
      }

      State current = queue.deleteMin();

      // if (currentRound < 5) {
      // System.out.println(current);
      // }


      MapLocation loc = current.loc;

      // if (current != states.get(loc)) continue;

      for (Direction dir : Neighbors.getNeighbors(loc, current.from)) {
        MapLocation nbrLoc = loc.add(dir);

        if (blocked.contains(nbrLoc)) continue;

        State nbr = states.get(nbrLoc);

        if (nbr == null) {
          // we haven't encountered this tile before

          nbr = new State(current, dir);

          insert(nbr);

          current.touch();
        } else {
          // should check here that we haven't found a shorter path
          // but we're probably ok with this approximate solution?
        }
      }
    }
  }

  boolean done() {
    State s = states.get(dest);
    return s != null && s.tile != TerrainTile.UNKNOWN;
  }

  /**
   * Consider a newly-detectable tile.
   * @param loc
   */
  void observe(MapLocation loc) {
    State s = states.get(loc);

    if (s != null && s.tile == TerrainTile.UNKNOWN) {
      // System.out.println("Observing " + s.loc);
      s.initHeuristic();
      insert(s);
    }
  }

  void observe() {
    for (MapLocation loc : Utils.getNewLocs()) {
      observe(loc);
    }
  }

  @Override
  public int getWeight(MapLocation loc) {
    State node = states.get(loc);
    return node == null ? MAX_HEURISTIC : node.worth();
  }

  @Override
  public void setTarget(MapLocation finish) {
    // TODO: this probably doesn't quite work correctly
    dest = finish;
  }
}
