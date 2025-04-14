package org.example;

import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

class B1 {

  static <T extends Comparable<T>> List<T> sortList(List<T> list) {
    List<T> sorted = new ArrayList<>(list);
    Collections.sort(sorted);
    return sorted;
  }

  static <T> List<T> addIfNotExists(List<T> list, T element) {
    List<T> result = new ArrayList<>(list);
    if (!result.contains(element)) {
      result.add(element);
    }
    return result;
  }

  @Property
  boolean sortingDoesNotChangeSize(@ForAll List<@IntRange(min = -1000, max = 1000) Integer> input) {
    List<Integer> sorted = sortList(input);
    return sorted.size() == input.size();
  }

  @Property
  boolean addingElementDoesNotDecreaseSize(
      @ForAll List<@IntRange(min = -1000, max = 1000) Integer> input,
      @ForAll @IntRange(min = -1000, max = 1000) Integer element) {
    List<Integer> modified = addIfNotExists(input, element);
    return modified.size() >= input.size();
  }
}