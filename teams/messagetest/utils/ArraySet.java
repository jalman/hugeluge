package messagetest.utils;

import java.util.*;

public class ArraySet<T> {
  private final T[] array;
  public int size;

  @SuppressWarnings("unchecked")
  public ArraySet(int capacity) {
    array = (T[]) new Object[capacity];
    size = 0;
  }

  @SafeVarargs
  public ArraySet(T... ts) {
    array = ts;
    size = ts.length;
  }

  public void insert(T t) {
    array[size++] = t;
  }

  public T get(int index) {
    return array[index];
  }

  public void set(int index, T t) {
    array[index] = t;
  }

  public void delete(int index) {
    if (--size > 0) {
      array[index] = array[size];
    }
  }

  public void remove(T t) {
    for (int i = size; --i >= 0;) {
      if (t == array[i]) {
        delete(i);
      }
    }
  }

  public void clear() {
    size = 0;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public T[] toArray() {
    return Arrays.copyOf(array, size);
  }

  public void debug() {
    System.out.println(this.toString());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < size; i++) {
      sb.append(", ");
      sb.append(array[i]);
    }
    return sb.toString();
  }
}
