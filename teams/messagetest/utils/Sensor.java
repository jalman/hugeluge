package messagetest.utils;

import java.util.*;

import battlecode.common.*;

public class Sensor {
  /**
   * Auto-generated with radius 24.
   */
  public static final EnumMap<Direction, int[][]> newLocs;

  static {
    newLocs = new EnumMap<>(Direction.class);
    newLocs.put(Direction.EAST, new int[][] { {4, 2}, {4, 1}, {4, 0}, {4, -1}, {4, -2}, {3, 3},
        {2, 4}, {2, -4}, {3, -3},});
    newLocs.put(Direction.NORTH_EAST, new int[][] { {4, 2}, {4, 1}, {4, 0}, {4, -1}, {4, -2},
        {1, -4}, {0, -4}, {2, -3}, {-1, -4}, {2, -4}, {3, -2}, {3, -3}, {-2, -4},});
    newLocs.put(Direction.NORTH, new int[][] { {-4, -2}, {0, -4}, {2, -4}, {-1, -4}, {-2, -4},
        {-3, -3}, {4, -2}, {1, -4}, {3, -3},});
    newLocs.put(Direction.NORTH_WEST, new int[][] { {-4, -2}, {-4, -1}, {-4, 0}, {-4, 1}, {-4, 2},
        {-3, -3}, {-3, -2}, {-2, -3}, {2, -4}, {1, -4}, {0, -4}, {-1, -4}, {-2, -4},});
    newLocs.put(Direction.WEST, new int[][] { {-4, -2}, {-4, -1}, {-4, 0}, {-4, 1}, {-4, 2},
        {-3, -3}, {-2, -4}, {-2, 4}, {-3, 3},});
    newLocs.put(Direction.SOUTH_WEST, new int[][] { {-4, -2}, {-4, -1}, {-4, 0}, {-4, 1}, {-4, 2},
        {-2, 3}, {-2, 4}, {2, 4}, {1, 4}, {-1, 4}, {-3, 2}, {-3, 3}, {0, 4},});
    newLocs.put(Direction.SOUTH, new int[][] { {4, 2}, {0, 4}, {-2, 4}, {-3, 3}, {-4, 2}, {1, 4},
        {3, 3}, {-1, 4}, {2, 4},});
    newLocs.put(Direction.SOUTH_EAST, new int[][] { {4, 2}, {4, 1}, {4, 0}, {4, -1}, {4, -2},
        {3, 3}, {3, 2}, {2, 3}, {1, 4}, {-1, 4}, {-2, 4}, {0, 4}, {2, 4},});
  }

  public static void main(String[] args) {
    MapLocation origin = new MapLocation(0, 0);

    LocSet prev =
        new LocSet(MapLocation.getAllMapLocationsWithinRadiusSq(origin, Utils.ROBOT_SENSOR_RADIUS2));

    StringBuilder s = new StringBuilder();

    for (Direction dir : Utils.REGULAR_DIRECTIONS) {
      s.append("newLocs.put(Direction." + dir + ", new int[][] {");

      MapLocation current = origin.add(dir);
      LocSet visible =
          new LocSet(MapLocation.getAllMapLocationsWithinRadiusSq(current,
              Utils.ROBOT_SENSOR_RADIUS2));

      for (int i = 0; i < prev.size; ++i) {
        visible.remove(prev.get(i));
      }

      for (int i = 0; i < visible.size; ++i) {
        MapLocation loc = visible.get(i);

        // move origin to current
        loc = loc.add(dir.opposite());

        s.append("{" + loc.x + "," + loc.y + "}, ");
      }

      s.append("});\n");
    }

    System.out.println(s);
  }
}
