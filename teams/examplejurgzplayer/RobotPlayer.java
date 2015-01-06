package examplejurgzplayer;

import examplejurgzplayer.towers.TowerBehavior;

import examplejurgzplayer.beavers.BeaverBehavior;
import examplejurgzplayer.hq.*;
import examplejurgzplayer.soldiers.*;
import examplejurgzplayer.utils.*;
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
        // e.printStackTrace();
      }
    }
  }
}