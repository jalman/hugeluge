package messagetest.nav;

import static messagetest.utils.Utils.*;
import messagetest.utils.*;
import battlecode.common.*;

/**
 * Moves towards the reflection of the DIJKSTRA_CENTER (that is, the ENEMY_HQ).
 * TODO: Fix this once reflections work again.
 * @author vlad
 *
 */
public class ReverseDijkstraMover extends GradientMover {
  public static final ReverseDijkstraMover reverseDijkstraMover = new ReverseDijkstraMover();

  @Override
  public int getWeight(MapLocation loc) {
    try {
      Pair<Direction, Integer> pathingInfo =
          messagingSystem.readPathingInfo(null);
      if (pathingInfo.first != null) {
        return pathingInfo.second;
      }
    } catch (GameActionException e) {
      // TODO Auto-generated catch block
      // e.printStackTrace();
    }
    return Integer.MAX_VALUE;
  }

}
