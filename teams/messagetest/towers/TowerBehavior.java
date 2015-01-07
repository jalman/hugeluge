package messagetest.towers;

import static messagetest.utils.Utils.*;
import messagetest.*;
import battlecode.common.*;

public class TowerBehavior extends RobotBehavior {

  // HQAction[] buildOrder;
  // int buildOrderProgress = 0;

  public TowerBehavior() {}

  @Override
  protected void initMessageHandlers() {}

  @Override
  public void beginRound() throws GameActionException {
    updateBuildingUtils();
  }

  @Override
  public void run() throws GameActionException {
    if (enemyRobots.length > 0) {
      MapLocation loc = enemyRobots[0].location;
      if (RC.isWeaponReady() && RC.canAttackLocation(loc)) {
        RC.attackLocation(loc);
      }
    }

  }

  @Override
  public void endRound() throws GameActionException {}

}
