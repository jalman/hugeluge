package funnav.funnav;

import battlecode.common.*;

/** A data structure containing a command to be processed by the movement state machine. */
public class MoveInfo {
  public RobotType buildingType;
  public Direction dir;
  public boolean shouldMine;
  public MapLocation flashTarget;
  public MapLocation buildingLoc;

  public static final MoveInfo miningMoveInfo = new MoveInfo(true);

  /** Whether to mine or not. */
  public MoveInfo(boolean shouldMine) {
    this.shouldMine = shouldMine;
  }

  /** Move in a direction. Do not move. */
  public MoveInfo(Direction dir) {
    this.dir = dir;
  }

  /** Build a building in a given direction. */
  public MoveInfo(RobotType buildingType, MapLocation buildingLoc) {
    this.buildingType = buildingType;
    this.buildingLoc = buildingLoc;
  }

  /** Flash to a given location. */
  public MoveInfo(MapLocation flashTarget) {
    this.flashTarget = flashTarget;
  }

  @Override
  public String toString() {
    if (flashTarget != null) return "Flashing to " + flashTarget;
    if (buildingType != null) return "Build " + buildingType + " at the " + buildingLoc;
    if (shouldMine) return "Mining";
    if(dir==null || dir==Direction.NONE || dir==Direction.OMNI) return "Do nothing";
    return "Move to the " + dir;
  }
}
