package messagetest.utils;

public class ArrayQueue<T> {
  private final T[] queue;
  private final int capacity;
  public int start = 0, end = 0;

  @SuppressWarnings("unchecked")
  public ArrayQueue(int capacity) {
    this.capacity = capacity;
    this.queue = (T[]) new Object[capacity];
  }

  public boolean isEmpty() {
    return start == end;
  }

  public void push(T t) {
    queue[(end++) % capacity] = t;
  }

  public T pop() {
    return queue[(start++) % capacity];
  }
}
