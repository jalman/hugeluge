package messagetest.nav;

import battlecode.common.*;

public class DumbMover extends GradientMover {
  public MapLocation target;

  public DumbMover() {}

  @Override
  public void setTarget(MapLocation dest) {
    this.target = dest;
  }

  @Override
  public int getWeight(MapLocation loc) {
    return loc.distanceSquaredTo(target);
  }
}
