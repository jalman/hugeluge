package messagetest.nav;

import static messagetest.utils.Utils.*;
import messagetest.utils.*;
import battlecode.common.*;

public class Neighbors {

  /**
   * Possible directions to expand in.
   * TODO: cache?
   * @param loc
   * @param from
   * @return Array of directions.
   */
  public static Direction[] getNeighbors(MapLocation loc, Direction from) {
    if (from == Direction.NONE) {
      return REGULAR_DIRECTIONS;
    }

    Direction left = from.rotateLeft();
    Direction right = from.rotateRight();

    if (from.isDiagonal()) {
      return new Direction[] {left.rotateLeft(), left, from, right, right.rotateRight()};
    } else {
      return new Direction[] {left, from, right};
    }
  }

  /**
   * Compute possible directions to expand in.
   * Optimization from http://aigamedev.com/open/tutorial/symmetry-in-pathfinding/
   * @param loc
   * @param from
   * @return Array of directions.
   */
  public static Direction[] getNeighborsOpt(MapLocation loc, Direction from) {
    if (from == Direction.NONE) {
      return REGULAR_DIRECTIONS;
    } else {
      ArraySet<Direction> dirs = new ArraySet<Direction>(8);
      dirs.insert(from);

      Direction left, right;

      if (from.isDiagonal()) {
        left = from.rotateLeft();
        right = from.rotateRight();

        dirs.insert(left);
        dirs.insert(right);
      } else {
        left = right = from;
      }

      left = left.rotateLeft();
      if (RC.senseTerrainTile(loc.add(left.rotateLeft())) == TerrainTile.VOID) {
        dirs.insert(left);
      }

      right = right.rotateRight();
      if (RC.senseTerrainTile(loc.add(right.rotateRight())) == TerrainTile.VOID) {
        dirs.insert(right);
      }

      return dirs.toArray();
    }
  }
}
