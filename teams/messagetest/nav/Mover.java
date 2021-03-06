package messagetest.nav;

import static messagetest.utils.Utils.*;
import battlecode.common.*;

public class Mover {
  private MapLocation dest;
  private NavAlg navAlg;

  // private NavType navType;

  public Mover() {
    this.dest = null;
    setNavType(NavType.DUMB);
  }

  public Mover(NavType navType) {
    this.dest = null;
    setNavType(navType);
  }

  public Mover(NavAlg navAlg) {
    this.navAlg = navAlg;
  }

  public void setNavType(NavType navType) {
    // this.navType = navType;
    this.navAlg = navType.navAlg;
  }

  public void setTarget(MapLocation dest) {
    // RC.setIndicatorString(2, "Mover target set to: " + dest);
    if (!dest.equals(this.dest)) {
      this.dest = dest;
      navAlg.setTarget(dest);
    }
  }

  public MapLocation getTarget() {
    return dest;
  }

  public boolean arrived() {
    if (dest == null || dest.equals(currentLocation)) {
      return true;
    }
    return false;
  }

  public void move(MapLocation dest) {
    setTarget(dest);
    move();
  }

  /**
   * Try to move.
   * @return
   */
  public void move() {
    // int bc = Clock.getBytecodesLeft();
    // RC.setIndicatorString(1, "my x = " + Integer.toString(RC.getLocation().x) + ", my y = " +
    // Integer.toString(RC.getLocation().y)
    // + "x = " + Integer.toString(dest.x) + ", y = " + Integer.toString(dest.y));
    // RC.setIndicatorString(2, Clock.getRoundNum() + " | dest = " + dest + ", navtype = " +
    // navType);

    if (arrived()) return;

    if (!RC.isCoreReady()) return;

    Direction d;
    d = navAlg.getNextDir();
    if (d != null && d != Direction.NONE && d != Direction.OMNI) {
      if (RC.canMove(d)) {
        try {
          RC.move(d);
        } catch (GameActionException e) {
          e.printStackTrace();
        }
      } else if (currentLocation.distanceSquaredTo(dest) <= 2) {
        setTarget(currentLocation);
      }
    }

    // System.out.println("Bytecodes used by Mover.execute() = " +
    // Integer.toString(bc-Clock.getBytecodesLeft()));
  }

  public void compute(int bytecodes) {
    navAlg.compute(bytecodes);
  }
}
