package funnav.beavers;

import static funnav.utils.Utils.*;

import funnav.funnav.NavigationMode;

import battlecode.common.Direction;
import funnav.RobotBehavior;
import funnav.funnav.MoveInfo;
import funnav.funnav.NavigationSystem;
import battlecode.common.*;
import funnav.funnav.MovementStateMachine.MovementState;
import static funnav.utils.Utils.*;
import funnav.*;
import funnav.hq.*;
import funnav.messaging.*;
import funnav.messaging.MessagingSystem.MessageType;
import funnav.nav.*;
import funnav.utils.*;
import battlecode.common.*;

public class BeaverBehavior extends RobotBehavior {

  enum Mode {
    COMBAT, MOVE, EXPLORE, BUILD, MINE
  };

  // state machine stuff
  Mode mode = Mode.EXPLORE;
  static MapLocation target;

  // basic data
  static int bornRound = Clock.getRoundNum();
  //  HybridMover hybrid = new HybridMover();
  //  Mover mover = new Mover(NavType.DUMB);

  static ArraySet<MapLocation> messagedEnemyRobots = new ArraySet<MapLocation>(100);
  //  static int[][] enemyLastSeen = new int[2*MAX_MAP_WIDTH][2*MAX_MAP_HEIGHT];

  static ArraySet<MapLocation> attackLocations = new ArraySet<MapLocation>(100);
  static ArraySet<MapLocation> microLocations = new ArraySet<MapLocation>(100);
  // private int buildPastureRound;

  // private final Micro micro = new Micro(this);

  private MovementState curMovementState = MovementState.IDLE;
  private MoveInfo nextMove;
  // private int turnsStuck;
  private Direction dirToSense;
  private RobotType buildingType;

  public BeaverBehavior() {
    navSystem.setNavigationMode(NavigationMode.BUG);
  }

  @Override
  protected void initMessageHandlers() {
    super.initMessageHandlers();

    handlers[MessageType.ATTACK_LOCATION.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        MapLocation loc = new MapLocation(message[0], message[1]);
        attackLocations.insert(loc);
      }
    };
    handlers[MessageType.MICRO_INFO.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        MapLocation loc = new MapLocation(message[0], message[1]);
        microLocations.insert(loc);
        attackLocations.insert(loc);
      }
    };
    /*

    handlers[MessageType.ENEMY_BOT.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        MapLocation loc = new MapLocation(message[0], message[1]);
        enemyLastSeen[loc.x][loc.y] = currentRound;
        messagedEnemyRobots.insert(loc);
      }
    };
     */
  }

  @Override
  public void beginRound() throws GameActionException {
    updateUnitUtils();
    attackLocations.clear();
    microLocations.clear();
    messagedEnemyRobots.clear();
    messagingSystem.beginRound(handlers);
  }

  @Override
  public void endRound() throws GameActionException {
    // sendEnemyMessages();
    messagingSystem.endRound();
  }

  /*
  private void sendEnemyMessages() throws GameActionException {
    for (RobotInfo info : enemyRobots) {
      if (info.type == RobotType.HQ) continue;
      MapLocation loc = info.location;
      if (enemyLastSeen[loc.x][loc.y] < currentRound) {
        // enemyLastSeen[loc.x][loc.y] = currentRound;
        messagingSystem.writeEnemyBotMessage(loc);
      }
    }
  }
   */

  @Override
  public void run() throws GameActionException {
    this.curMovementState = move();
    RC.setIndicatorString(1, curMovementState.name());
    otherActions();
  }

  private void setMode(Mode m) {
    mode = m;
    RC.setIndicatorString(0, m.toString());
    if(target != null) {
      RC.setIndicatorLine(currentLocation, target, 255, 0, 0);
    }
  }

  private void setMode(Mode m, MapLocation t) {
    mode = m;
    target = t;
    RC.setIndicatorString(0, m + " " + target);
    if(target != null) {
      RC.setIndicatorLine(currentLocation, target, 255, 0, 0);
    }
    System.out.println("new target " + t );
  }

  private MapLocation closestTarget() {
    MapLocation closest = null;
    int min = Integer.MAX_VALUE;

    for (int i = attackLocations.size; --i >= 0;) {
      MapLocation loc = attackLocations.get(i);
      int d = currentLocation.distanceSquaredTo(loc);
      if (d < min) {
        min = d;
        closest = loc;
      }
    }

    if (closest != null) return closest;

    for (int i = messagedEnemyRobots.size; --i >= 0;) {
      MapLocation loc = messagedEnemyRobots.get(i);
      int d = currentLocation.distanceSquaredTo(loc);
      if (d < min) {
        min = d;
        closest = loc;
      }
    }

    if (closest != null) return closest;

    if(enemyTowerLocs.length > 0) {
      return closestLocation(enemyTowerLocs, currentLocation);
    } else {
      return ENEMY_HQ;
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

  private MapLocation findNearbyOre() throws GameActionException {
    MapLocation bestLoc = currentLocation;
    double bestOre = RC.senseOre(currentLocation);
    for(int[] offset : unitSenseOffsets) {
      MapLocation loc = currentLocation.add(offset[0], offset[1]);
      double ore = RC.senseOre(loc);
      if(ore > bestOre && !RC.isLocationOccupied(loc)) {
        bestOre = ore;
        bestLoc = loc;
      }
    }
    // null if no ore found
    return bestLoc;
  }
  
  private void tryToFindNewMine() throws GameActionException{
      MapLocation oreLoc = findNearbyOre();
      System.out.println("curLoc = " + currentLocation + ", oreLoc = " + oreLoc);
      if(oreLoc == null) {
        setMode(Mode.EXPLORE);
      } else if(oreLoc != currentLocation) {
        setMode(Mode.MOVE, oreLoc);
      } else {
        setMode(Mode.MINE);
      }
  }

  @Override
  public MoveInfo think() throws GameActionException {
    if(mode != Mode.MINE) {
      tryToFindNewMine();
    }
    System.out.println("mode: " + mode);
    switch(mode) {
      case MOVE:
        navSystem.setDestination(target);
        return new MoveInfo(navSystem.navigateToDestination());
      case EXPLORE:
        Direction exploreDir = getExploreDirection();
        target = currentLocation.add(exploreDir, 10);
        navSystem.setDestination(target);
        return new MoveInfo(navSystem.navigateToDestination());
      case COMBAT:
        // run away
        navSystem.setDestination(ALLY_HQ);
        return new MoveInfo(navSystem.navigateToDestination());
      case MINE:
        if(RC.senseOre(currentLocation) < 0.1) {
          tryToFindNewMine();
        }
        return new MoveInfo(true);
      case BUILD:
        return new MoveInfo(buildingType, target);
      default:
        return null;
    }
  }

  private boolean build(RobotType type, MapLocation loc) throws GameActionException {
    Direction dir = currentLocation.directionTo(loc);
    if (RC.canBuild(dir, type)) {
      RC.build(dir, type);
      return true;
    }
    return false;
  }

  public MovementState move() throws GameActionException {
    if (!RC.isCoreReady()) {
      return MovementState.COOLDOWN;
    }
    switch (curMovementState) {
      case ABOUT_TO_FLASH:
        return MovementState.IDLE;
      case ABOUT_TO_BUILD:
        if (build(nextMove.buildingType, nextMove.buildingLoc)) {
          return MovementState.COOLDOWN;
        }
        return MovementState.IDLE;
      case COOLDOWN:
        if (!RC.isCoreReady()) {
          return MovementState.COOLDOWN;
        }
        // fall through, no break
      case IDLE:
        nextMove = think();
        System.out.println(nextMove.toString());
        if (nextMove == null) {
          return MovementState.IDLE;
        }
        if (nextMove.shouldMine) {
          RC.mine();
          return MovementState.COOLDOWN;
        }
        if (nextMove.buildingType != null) {
          if (currentLocation.isAdjacentTo(nextMove.buildingLoc)) {
            if (build(nextMove.buildingType, nextMove.buildingLoc)) {
              return MovementState.COOLDOWN;
            } else {
              return MovementState.ABOUT_TO_BUILD;
            }
          }
        }
        if (nextMove.dir == null || nextMove.dir == Direction.NONE
            || nextMove.dir == Direction.OMNI) {
          RC.mine();
          return MovementState.IDLE;
        }
        // move!
        if (RC.canMove(nextMove.dir)) {
          RC.move(nextMove.dir);
        } else {
          Direction dir = navSystem.wiggleToMovableDirection(nextMove.dir);
          if (dir != null) {
            RC.move(nextMove.dir);
          } else {
            return MovementState.IDLE;
          }
        }
        // fall through to JUST_MOVED
      case JUST_MOVED:
        // turnsStuck = 0;
        mapCacheSystem.senseAfterMove(dirToSense);
        return MovementState.COOLDOWN;
      default:
        return MovementState.IDLE;
    }
  }

  public void otherActions() throws GameActionException {
    if(curMovementState == MovementState.IDLE && mode == Mode.COMBAT) { // cornered
      for(RobotInfo enemy : enemyRobots) {
        if(RC.canAttackLocation(enemy.location)) {
          RC.attackLocation(enemy.location);
          break;
        }
      }
    }
  }
}
