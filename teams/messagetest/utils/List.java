package messagetest.utils;

public class List<T> implements Functor<List<?>, T> {
  public T first;
  public List<T> rest;

  public List(T first, List<T> rest) {
    this.first = first;
    this.rest = rest;
  }

  @Override
  public <B> List<B> fmap(Function<T, B> f) {
    return new List<B>(f.apply(first), this.rest.fmap(f));
  }

}
