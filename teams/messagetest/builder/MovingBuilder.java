package messagetest.builder;

import messagetest.messaging.*;
import messagetest.messaging.MessagingSystem.MessageType;
import battlecode.common.*;

public class MovingBuilder {
  private boolean active = false;
  private RobotType buildType;
  private MapLocation buildLoc;

  public void initMessageHandlers(MessageHandler[] handlers) {
    handlers[MessageType.BUILD.type] = new MessageHandler() {

      @Override
      public void handleMessage(int[] message) throws GameActionException {

      }

    };
  }

  public boolean active() {
    return active;
  }

  public void run() {

  }
}
