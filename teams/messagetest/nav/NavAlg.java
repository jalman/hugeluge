package messagetest.nav;

import battlecode.common.*;

public abstract class NavAlg {
  public static boolean AVOID_ENEMY_HQ = true;

  // protected MapLocation curLoc = null, finish = null;

  // abstract public void recompute();

  abstract public void setTarget(MapLocation finish);

  /**
   * Return the next direction to [attempt to] move in.
   */
  abstract public Direction getNextDir();

  /**
   * Perform computations.
   * @param bytecodes Bytecode limit.
   */
  public void compute(int bytecodes) {}
}
