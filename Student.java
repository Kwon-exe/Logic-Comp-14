package org.example;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import net.jqwik.api.*;

class Student {
  private String name;
  private List<String> interests;
  private String address;

  public Student() {}

  public Student(String name, List<String> interests, String address) {
    this.name = name;
    this.interests = interests;
    this.address = address;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getInterests() {
    return interests;
  }

  public void setInterests(List<String> interests) {
    this.interests = interests;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Student student = (Student) o;
    return Objects.equals(name, student.name) &&
            Objects.equals(interests, student.interests) &&
            Objects.equals(address, student.address);
  }

  @Override
  public String toString() {
    return "Student{" +
            "name='" + name + '\'' +
            ", interests=" + interests +
            ", address='" + address + '\'' +
            '}';
  }
}

class StudentMapSerializer {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static String serialize(Map<String, Student> studentMap) throws Exception {
    return objectMapper.writeValueAsString(studentMap);
  }

  public static Map<String, Student> deserialize(String json) throws Exception {
    return objectMapper.readValue(json, new TypeReference<Map<String, Student>>() {});
  }
}

class StudentMapSerializationTests {

  @Provide
  Arbitrary<String> studentIds() {
    return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
  }

  @Provide
  Arbitrary<String> names() {
    return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
  }

  @Provide
  Arbitrary<List<String>> interests() {
    return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
            .list().ofMinSize(0).ofMaxSize(10);
  }

  @Provide
  Arbitrary<String> addresses() {
    return Arbitraries.strings().alpha().numeric().withChars(' ', ',', '.').ofMinLength(5).ofMaxLength(100);
  }

  @Provide
  Arbitrary<Student> students() {
    return Combinators.combine(
            names(),
            interests(),
            addresses()
    ).as(Student::new);
  }

  @Provide
  Arbitrary<Map<String, Student>> studentMaps() {
    return Arbitraries.maps(
            studentIds(),
            students()
    ).ofMinSize(0).ofMaxSize(20);
  }

  @Property(tries = 100)
  boolean serializationDeserializationAreInverses(@ForAll("studentMaps") Map<String, Student> original) throws Exception {
    String serialized = StudentMapSerializer.serialize(original);
    Map<String, Student> deserialized = StudentMapSerializer.deserialize(serialized);
    return original.equals(deserialized);
  }
}