package messagetest.utils;

import static messagetest.utils.Utils.*;
import battlecode.common.*;

public class LocMap<T> {
  private T[][] map;

  public LocMap() {
    clear();
  }

  @SuppressWarnings("unchecked")
  public void clear() {
    map = (T[][]) new Object[WRAP_X][WRAP_Y];
  }

  public void set(MapLocation loc, T t) {
    map[wrapX(loc.x)][wrapY(loc.y)] = t;
  }

  public T get(MapLocation loc) {
    return map[wrapX(loc.x)][wrapY(loc.y)];
  }
}
