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
  public MapLocation[] locs = new MapLocation[WRAP_X * WRAP_Y];
  public int size = 0;

  public LocSet(MapLocation... sources) {
    for (MapLocation loc : sources) {
      insert(loc);
    }
  }

  public void insert(MapLocation loc) {
    // System.out.println(loc);
    int x = loc.x % WRAP_X;
    int y = loc.y % WRAP_Y;

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
    return has[loc.x % WRAP_X][loc.y % WRAP_Y];
  }

  public int getIndex(MapLocation loc) {
    return index[loc.x % WRAP_X][loc.y % WRAP_Y];
  }

  public MapLocation get(int index) {
    return locs[index];
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
