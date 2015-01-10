package examplejurgzplayer.barrackses;

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

public class BarracksBehavior extends RobotBehavior {

  // HQAction[] buildOrder;
  // int buildOrderProgress = 0;

  public BarracksBehavior() {
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

  }

  @Override
  public void endRound() throws GameActionException {    
  }

}
