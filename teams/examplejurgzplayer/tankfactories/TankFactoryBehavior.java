package examplejurgzplayer.tankfactories;

import static examplejurgzplayer.utils.Utils.*;

import java.util.*;

import static examplejurgzplayer.utils.Utils.*;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;
import examplejurgzplayer.*;
import examplejurgzplayer.Strategy.GamePhase;
import examplejurgzplayer.messaging.*;
import examplejurgzplayer.messaging.MessagingSystem.MessageType;
import examplejurgzplayer.messaging.MessagingSystem.ReservedMessageType;
import examplejurgzplayer.nav.*;
import examplejurgzplayer.utils.*;
import examplejurgzplayer.utils.Utils.SymmetryType;
import battlecode.common.*;

public class TankFactoryBehavior extends RobotBehavior {

  // HQAction[] buildOrder;
  // int buildOrderProgress = 0;

  public TankFactoryBehavior() {
  }

  @Override
  protected void initMessageHandlers() {
  }

  @Override
  public void beginRound() throws GameActionException {   
    updateBuildingUtils(); 
  }

  @Override
  public void run() throws GameActionException {
    System.out.println("asdf");
    for(Direction d : Direction.values()) {
      if(spawnRobot(d, RobotType.TANK)) {
        break;
      }
    }
  }
  
  private static boolean spawnRobot(Direction dir, RobotType type) throws GameActionException {
    if (RC.isCoreReady()) {
      System.out.println("building " + type.name());
      // Spawn soldier
      for (int i = 0; i < 8; i++) {
        // if square is movable, spawn soldier there and send initial messages
        if (RC.canSpawn(dir, type) && RC.hasSpawnRequirements(type)) {
          // sendMessagesOnBuild();
          RC.spawn(dir, type);
          return true;
        }
        // otherwise keep rotating until this is possible
        dir = dir.rotateRight();
      }
      // message guys to get out of the way??
    }
    return false;
  }

  @Override
  public void endRound() throws GameActionException {    
  }

}
