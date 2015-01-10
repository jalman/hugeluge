package funnav;

import funnav.messaging.MessagingSystem.ReservedMessageType;
import battlecode.common.MapLocation;
import funnav.messaging.MessageHandler;
import funnav.messaging.MessagingSystem.MessageType;
import battlecode.common.*;
import funnav.funnav.*;
import funnav.messaging.*;
import static funnav.utils.Utils.*;

public abstract class RobotBehavior {
  protected MessageHandler[] handlers;
  // public MovementStateMachine msm;
  
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

  public RobotBehavior() {
    handlers = new MessageHandler[MessagingSystem.MESSAGE_TYPES.length];
    initMessageHandlers();
  }

  /**
   * Override to specify message handlers.
   */
  protected void initMessageHandlers() {
    handlers[MessageType.MAP_FRAGMENT.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        sharedExplorationSystem.receiveMapFragment(message);
      }
    };
  }

  /**
   * Called at the beginning of each round.
   * @return whether it's worth living to the next round.
   */
  public void beginRound() throws GameActionException {
    sharedExplorationSystem.receiveMapEdges(messagingSystem.readMapEdges());
  }

  public abstract MoveInfo think() throws GameActionException;
  
  /**
   * Called every round.
   */
  public abstract void run() throws GameActionException;

  /**
   * Called at the end of each round.
   * @throws GameActionException
   */
  public abstract void endRound() throws GameActionException;
}
