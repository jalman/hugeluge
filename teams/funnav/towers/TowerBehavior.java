package funnav.towers;

import static funnav.utils.Utils.*;

import java.util.*;

import funnav.funnav.MoveInfo;

import static funnav.utils.Utils.*;
import battlecode.common.GameActionException;
import funnav.*;
import funnav.Strategy.GamePhase;
import funnav.messaging.*;
import funnav.messaging.MessagingSystem.MessageType;
import funnav.messaging.MessagingSystem.ReservedMessageType;
import funnav.nav.*;
import funnav.utils.*;
import funnav.utils.Utils.SymmetryType;
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

  @Override
  public MoveInfo think() throws GameActionException {
    return null;
  }
}
