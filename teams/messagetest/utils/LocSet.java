package messagetest.utils;

import static messagetest.utils.Utils.*;

import java.util.*;

import battlecode.common.*;

/**
 * An (ordered) set of map locations.
 * TODO: Deletion can be done in O(1), same as ArraySet.
 * @author vlad
 *
 */
public class LocSet {

  public boolean[][] has = new boolean[WRAP_X][WRAP_Y];
  public int index[][] = new int[WRAP_X][WRAP_Y];
  public MapLocation[] locs = new MapLocation[MAP_MAX_SIZE];
  public int size = 0;

  public LocSet(MapLocation... sources) {
    for (MapLocation loc : sources) {
      insert(loc);
    }
  }

  public void insert(MapLocation loc) {
    // System.out.println(loc);
    int x = wrapX(loc.x);
    int y = wrapY(loc.y);

    if (!has[x][y]) {
      has[x][y] = true;
      index[x][y] = size;
      locs[size++] = loc;
    }

    // if (!get(getIndex(loc)).equals(loc)) {
    // System.out.println("BUG in LocSet!");
    // }
  }

  public boolean contains(MapLocation loc) {
    return has[wrapX(loc.x)][wrapY(loc.y)];
  }

  public int getIndex(MapLocation loc) {
    return index[wrapX(loc.x)][wrapY(loc.y)];
  }

  public MapLocation get(int index) {
    return locs[index];
  }

  public void delete(int index) {
    MapLocation loc = locs[index];
    has[wrapX(loc.x)][wrapY(loc.y)] = false;

    --size;
    loc = locs[size];
    locs[index] = loc;
    this.index[wrapX(loc.x)][wrapY(loc.y)] = index;
  }

  public void remove(MapLocation loc) {
    if (contains(loc)) {
      delete(getIndex(loc));
    }
  }

  public int size() {
    return size;
  }

  @Override
  public String toString() {
    MapLocation[] temp = Arrays.copyOf(locs, size);
    return Arrays.toString(temp);
  }
}
