package run;

import java.util.ArrayList;
import java.util.List;
import java.util.AbstractList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Small library for *almost* immutable lists, yay!
public class Lists {
  public static <A> List<A> concat(List<A> first, List<A> second) {
    List<A> copy = new ArrayList<A>(first);
    copy.addAll(second);
    return copy;
  }

  public static <A> List<A> singleton(A a) {
    List<A> l = new ArrayList<A>();
    l.add(a);
    return l;
  }

  public static <A> List<A> cons(A head, List<A> tail) {
    return concat(singleton(head), tail);
  }

  public static <A> List<A> empty() {
    return new ArrayList<A>();
  }

  public static <A> List<A> fromStream(Stream<A> s) {
    return s.collect(Collectors.toList());
  }
}