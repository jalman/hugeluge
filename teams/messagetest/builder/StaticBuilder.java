package messagetest.builder;

import static messagetest.utils.Utils.*;
import battlecode.common.*;

public class StaticBuilder {
  private static Direction buildDir = null;

  /**
   * Tries to build a Robot.
   * @param dir The direction in which to build.
   * @param type The RobotType.
   * @return Whether successful.
   */
  public static boolean buildRobot(RobotType type) {
    // System.out.println("Building " + type);

    if (buildDir == null) {
      buildDir = currentLocation.directionTo(ENEMY_HQ);
    }

    for (int i = 0; i < 8; i++) {
      if (RC.canSpawn(buildDir, type)) {
        // sendMessagesOnBuild();
        try {
          RC.spawn(buildDir, type);
        } catch (GameActionException e) {
          e.printStackTrace();
          return false;
        }
        return true;
      }

      // System.out.println("Couldn't build in " + buildDir);
      // otherwise keep rotating until this is possible
      buildDir = buildDir.rotateRight();
    }

    // message guys to get out of the way??
    return false;
  }

}
