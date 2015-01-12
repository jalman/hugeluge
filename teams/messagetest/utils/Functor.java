package messagetest.utils;

public interface Functor<F extends Functor<F, ?>, A> {
  public <B> Functor<F, B> fmap(Function<A, B> f);
}
