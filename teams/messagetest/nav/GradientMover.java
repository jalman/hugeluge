package messagetest.nav;

import static messagetest.utils.Utils.*;
import battlecode.common.*;

/**
 * Moves down a gradient.
 * @author vlad
 *
 */
public abstract class GradientMover extends NavAlg {
  public abstract int getWeight(MapLocation loc);

  /**
   * Move down the gradient (towards the source).
   * @return The best direction, or none if impossible.
   */
  @Override
  public Direction getNextDir() {
    Direction dir = Direction.SOUTH, best = Direction.NONE;
    int min = Integer.MAX_VALUE;

    StringBuilder s = new StringBuilder();

    for (int i = 8; --i >= 0; dir = dir.rotateRight()) {
      int w = getWeight(currentLocation.add(dir));

      s.append("(" + dir + ", " + w + "), ");

      if (!RC.canMove(dir)) continue;

      // System.out.println(w);

      if (w < min) {
        min = w;
        best = dir;
      }
    }

    RC.setIndicatorLine(currentLocation, currentLocation.add(best), 255, 255, 255);
    RC.setIndicatorString(1, s.toString());

    return best;
  }

  /**
   * Move up the gradient.
   * @return Whether ended up moving.
   */
  public Direction ascend() throws GameActionException {
    Direction dir = Direction.SOUTH, best = Direction.NONE;
    int max = Integer.MIN_VALUE;

    for (int i = 8; --i >= 0; dir = dir.rotateRight()) {
      if (!RC.canMove(dir)) continue;

      int w = getWeight(currentLocation.add(dir));

      if (w > max) {
        max = w;
        best = dir;
      }
    }

    return best;
  }
}
