package messagetest.utils;

public interface Monad<M extends Monad<M, ?>, A> extends Functor<M, A> {
  public Monad<M, A> return_(A a);

  public Monad<M, A> join(Monad<M, Monad<M, A>> m);
}
