package funnav;

import battlecode.common.*;
import funnav.beavers.*;
import funnav.hq.*;
import funnav.soldiers.*;
import funnav.towers.*;
import funnav.utils.*;

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
      case SOLDIER:
        robot = new SoldierBehavior();
        break;
      case BEAVER:
        robot = new BeaverBehavior();
        break;
      default: // autokill
        robot = new SoldierBehavior();
        return;
    }

    while (true) {
      try {
        robot.beginRound();
        robot.run();
        robot.endRound();
        rc.yield();
      } catch (GameActionException e) {
        e.printStackTrace();
      }
    }
  }
}