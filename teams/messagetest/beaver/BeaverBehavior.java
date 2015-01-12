package messagetest.beaver;

import static messagetest.utils.Utils.*;
import messagetest.*;
import messagetest.combat.*;
import messagetest.nav.*;
import battlecode.common.*;

public class BeaverBehavior extends RobotBehavior {

  enum Mode {
    COMBAT, MOVE, FARM, EXPLORE, DEFEND_TOWER, BUILD_BARRACKS, BUILD_TANK_FACTORY
  };

  // state machine stuff
  private Mode mode;
  static MapLocation target;

  // HybridMover hybrid = new HybridMover();
  // Mover mover = new Mover(NavType.DUMB);
  Mover mover = new Mover(new AStarMover());

  // private final Micro micro = new Micro(this);

  public BeaverBehavior() {}

  @Override
  protected void initMessageHandlers() {
    CombatSystem.initMessageHandlers(handlers);


  }

  @Override
  public void beginRound() throws GameActionException {
    updateUtils();
    CombatSystem.beginRound();
    messagingSystem.beginRound(handlers);
  }

  @Override
  public void endRound() throws GameActionException {
    // sendEnemyMessages();
    messagingSystem.endRound();
    // doPathing(5000);
  }

  /*
   * private void sendEnemyMessages() throws GameActionException {
   * for (RobotInfo info : enemyRobots) {
   * if (info.type == RobotType.HQ) continue;
   * MapLocation loc = info.location;
   * if (enemyLastSeen[loc.x][loc.y] < currentRound) {
   * // enemyLastSeen[loc.x][loc.y] = currentRound;
   * messagingSystem.writeEnemyBotMessage(loc);
   * }
   * }
   * }
   */

  @Override
  public void run() throws GameActionException {
    think();
    act();
  }

  private void setMode(Mode m) {
    RC.setIndicatorString(0, m.toString());
    mode = m;
  }

  private void setMode(Mode m, MapLocation target) {
    RC.setIndicatorString(0, m + " " + target);
    mode = m;
  }

  private void think() throws GameActionException {

    if (CombatSystem.shouldMicro()) {
      setMode(Mode.COMBAT);
      return;
    }



    /*
     * int read = RC.readBroadcast(123); // todo: use messaging system for this
     * if (read != 1 && read != 2) {
     * setMode(Mode.BUILD_BARRACKS);
     * return;
     * }
     * if (read == 1) {
     * setMode(Mode.BUILD_TANK_FACTORY);
     * return;
     * }
     */



    // TODO: use priorities for where to be?

    MapLocation closestTarget = CombatSystem.closestTarget();
    if (closestTarget != null) {
      target = closestTarget;
      setMode(Mode.MOVE, target);
      return;
    }

    /*
     * MapLocation closestTower = closestLocation(alliedTowerLocs, currentLocation);
     * if (closestTower != null) {
     * target = closestTower;
     * setMode(Mode.DEFEND_TOWER, target);
     * return;
     * }
     */

    if (mover.arrived() || mode != Mode.MOVE) {
      target = findExploreLocation();
      setMode(Mode.EXPLORE, target);
    }
  }

  /**
   * Rallies to the place chosen by the HQ.
   * @return Place to explore to.
   * @throws GameActionException
   */
  private MapLocation findExploreLocation() throws GameActionException {
    return messagingSystem.readRallyPoint();
  }

  private void buildBuilding(RobotType buildingType) throws GameActionException {
    RC.setIndicatorString(2,
        "Trying to build " + buildingType.toString() + " on turn " + Clock.getRoundNum());
    for (Direction d : Direction.values()) {
      if (RC.canBuild(d, buildingType)) {
        RC.build(d, buildingType);
        RC.broadcast(123, RC.readBroadcast(123) + 1); // atomic operation!! TODO: use messaging
                                                      // system
        return;
      }
    }
    for (Direction d : Direction.values()) {
      if (RC.canMove(d)) {
        RC.move(d);
        return;
      }
    }
  }

  private void act() throws GameActionException {
    // RC.setIndicatorString(0, mode.toString());
    switch (mode) {
      case COMBAT:
        // if (!RC.isActive()) return;
        // micro.micro();
        // if (!NathanMicro.luge(mover)) {
        // micro.micro();
        // }
        // NathanMicro.luge(mover);
        MapLocation loc = enemyRobots[0].location;
        if (RC.isWeaponReady() && RC.canAttackLocation(loc)) {
          RC.attackLocation(loc);
        }
        break;
      case MOVE:
        mover.setTarget(target);
        mover.compute(4000);
        mover.move();
        break;
      case EXPLORE:
        mover.setTarget(target);
        mover.compute(9000);
        mover.move();
        break;
      case DEFEND_TOWER:
        mover.setTarget(target);
        mover.compute(9000);
        mover.move();
        break;
      case BUILD_TANK_FACTORY:
        buildBuilding(RobotType.TANKFACTORY);
        break;
      case BUILD_BARRACKS:
        buildBuilding(RobotType.BARRACKS);
        break;
      default:
        break;
    }
  }

}
