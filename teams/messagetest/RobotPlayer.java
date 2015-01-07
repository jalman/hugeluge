package messagetest;

import messagetest.hq.*;
import messagetest.soldiers.*;
import messagetest.towers.*;
import messagetest.utils.*;
import battlecode.common.*;

public class RobotPlayer {
  public static void run(RobotController rc) {
    Utils.initUtils(rc);
    RobotBehavior robot = null;
    switch (rc.getType()) {
      case HQ:
        robot = new HQBehavior();
        // Strategy strategy = Strategy.decide();
        break;
      case TOWER:
        robot = new TowerBehavior();
        break;
      case BEAVER:
        robot = new SoldierBehavior();
        break;
      case SOLDIER:
        robot = new SoldierBehavior();
        break;
      default: // autokill
        return;
    }

    while (true) {
      try {
        robot.beginRound();
        robot.run();
        robot.endRound();
        rc.yield();
      } catch (GameActionException e) {
        // e.printStackTrace();
      }
    }
  }
}
