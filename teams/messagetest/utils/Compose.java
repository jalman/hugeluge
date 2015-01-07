package messagetest.utils;

public class Compose<A, B, C> implements Function<A, C> {

  private final Function<B, C> f;
  private final Function<A, B> g;

  public Compose(Function<B, C> f, Function<A, B> g) {
    this.f = f;
    this.g = g;
  }

  @Override
  public C apply(A a) {
    return f.apply(g.apply(a));
  }

  public static <A, B, C> Function<A, C> compose(final Function<B, C> f, final Function<A, B> g) {
    return new Function<A, C>() {
      @Override
      public C apply(A a) {
        return f.apply(g.apply(a));
      }
    };
  }

}
