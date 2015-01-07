package messagetest.nav;

import static messagetest.utils.Utils.*;
import battlecode.common.*;

/**
 * Moves down a gradient.
 * @author vlad
 *
 */
public abstract class GradientMover {
  public abstract int getWeight(MapLocation loc);

  /**
   * Move down the gradient (towards the source).
   * @return Whether ended up moving.
   */
  public boolean move() throws GameActionException {
    if (!RC.isCoreReady()) return false;

    Direction dir = Direction.SOUTH, best = null;
    int min = Integer.MAX_VALUE;
    // StringBuilder str = new StringBuilder();
    for (int i = 8; --i >= 0;) {
      MapLocation next = currentLocation.add(dir);
      int d = RC.canMove(dir) && isSafe(next) ? getWeight(next) : Integer.MAX_VALUE;
      // str.append(dir + "(" + d + "), ");

      if (d < min) {
        min = d;
        best = dir;
      }
      dir = dir.rotateRight();
    }
    // RC.setIndicatorString(1, str.toString());

    // System.out.println(str);

    if (best != null) {
      RC.move(best);
      return true;
    }
    return false;
  }

  /**
   * Move up the gradient (away from the sources).
   * @return Whether ended up moving.
   */
  public boolean ascend() throws GameActionException {
    return new GradientMover() {
      @Override
      public int getWeight(MapLocation loc) {
        return -GradientMover.this.getWeight(loc);
      }
    }.move();
  }

}
