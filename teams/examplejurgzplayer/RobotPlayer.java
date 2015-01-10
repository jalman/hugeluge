package examplejurgzplayer;

import examplejurgzplayer.tankfactories.TankFactoryBehavior;
import examplejurgzplayer.tanks.TankBehavior;
import examplejurgzplayer.towers.TowerBehavior;
import examplejurgzplayer.barrackses.BarracksBehavior;
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
      case TANK:
        robot = new TankBehavior();
        break;
      case BARRACKS:
        robot = new BarracksBehavior();
        break;
      case TANKFACTORY:
        robot = new TankFactoryBehavior();
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