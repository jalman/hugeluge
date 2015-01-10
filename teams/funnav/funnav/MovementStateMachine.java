package funnav.funnav;

import static funnav.utils.Utils.*;
import battlecode.common.*;
import funnav.*;

public class MovementStateMachine {
  public enum MovementState {
    /** Movement cooldown is not active. Spawn stuff if desirable.
     * Compute where to move next, and turn appropriately.*/
    IDLE,
    /** Was just idle, and decided to flash somewhere, but couldn't for some reason. */
    ABOUT_TO_FLASH, // not yet used
    /** Was just idle, and decided to spawn something, but couldn't for some reason. */
    ABOUT_TO_BUILD,
    /** Just moved. Need to call mc.senseAfterMove(justMovedDir) here. */
    JUST_MOVED,
    /** Waiting to be idle again. Prepare for the next navigation computation. */
    COOLDOWN;
  }

  // private static final int TURNS_STUCK_UNTIL_ROBOT_STARTS_MOVING_RANDOMLY = 16;
  private MovementState curState;
  private final RobotBehavior rb;
  private final NavigationSystem nav;
  private MoveInfo nextMove;
  // private int turnsStuck;
  private Direction dirToSense;

  public MovementStateMachine(RobotBehavior rb) {
    this.rb = rb;
    this.nav = navSystem;
    curState = MovementState.COOLDOWN;
  }

  public void reset() {
    curState = MovementState.COOLDOWN;
    nextMove = null;
    // turnsStuck = 0;
  }

  public boolean justMoved() {
    return curState == MovementState.JUST_MOVED;
  }

  public void step() throws GameActionException {
    curState = execute();
    // br.dbg.setIndicatorString('h',2, nextMove+", movement_state="+curState);
  }

  private boolean flash(MapLocation loc) throws GameActionException {
    if(RC.senseRobotAtLocation(loc) == null && RC.hasLearnedSkill(CommanderSkillType.FLASH)) {
      RC.castFlash(loc);
      return true;
    }
    return false;
  }

  private boolean build(RobotType type, MapLocation loc) throws GameActionException {
    Direction dir = currentLocation.directionTo(loc);
    if (RC.canBuild(dir, type)) {
      RC.build(dir, type);
      return true;
    }
    return false;
  }

  private MovementState execute() throws GameActionException {
    switch (curState) {
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
        nextMove = rb.think();
        if (nextMove == null) {
          return MovementState.IDLE;
        }
        if (nextMove.shouldMine) {
          RC.mine();
          return MovementState.COOLDOWN;
        }
        if (nextMove.flashTarget != null) {
          if (flash(nextMove.flashTarget)) {
            return MovementState.COOLDOWN;
          }
          return MovementState.IDLE;
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
          return MovementState.IDLE;
        }
        // move!
        if (RC.canMove(nextMove.dir)) {
          RC.move(nextMove.dir);
        } else {
          Direction dir = nav.wiggleToMovableDirection(nextMove.dir);
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
}
