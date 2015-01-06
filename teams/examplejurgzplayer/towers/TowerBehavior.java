package examplejurgzplayer.towers;

import static examplejurgzplayer.utils.Utils.*;

import java.util.*;

import static examplejurgzplayer.utils.Utils.*;

import battlecode.common.GameActionException;
import examplejurgzplayer.*;
import examplejurgzplayer.Strategy.GamePhase;
import examplejurgzplayer.messaging.*;
import examplejurgzplayer.messaging.MessagingSystem.MessageType;
import examplejurgzplayer.messaging.MessagingSystem.ReservedMessageType;
import examplejurgzplayer.nav.*;
import examplejurgzplayer.utils.*;
import examplejurgzplayer.utils.Utils.SymmetryType;
import battlecode.common.*;

public class TowerBehavior extends RobotBehavior {

  // HQAction[] buildOrder;
  // int buildOrderProgress = 0;

  public TowerBehavior() {
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
    if(enemyRobots.length > 0) {
      MapLocation loc = enemyRobots[0].location;
      if(RC.isWeaponReady() && RC.canAttackLocation(loc)) {
        RC.attackLocation(loc);
      }
    }
    
  }

  @Override
  public void endRound() throws GameActionException {    
  }

}
