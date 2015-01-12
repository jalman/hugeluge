package messagetest.nav;

public enum NavType {

  DUMB(new DumbMover()),
  HQ_DIJKSTRA(null);

  public final NavAlg navAlg;

  private NavType() {
    this.navAlg = null;
  }

  private NavType(NavAlg alg) {
    this.navAlg = alg;
  }
}
