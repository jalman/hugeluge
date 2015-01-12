package messagetest.nav;

import battlecode.common.*;

public class AStarMover extends GradientMover {

  private AStar aStar;

  @Override
  public int getWeight(MapLocation loc) {
    return aStar.getWeight(loc);
  }

  @Override
  public void setTarget(MapLocation finish) {
    aStar = new AStar(finish);
  }

  @Override
  public void compute(int bytecodes) {
    aStar.observe();
    aStar.compute(bytecodes);
  }
}
