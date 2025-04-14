package org.example;
import net.jqwik.api.*;
import java.util.*;
public class Color {

  public enum NamedColor {
    RED, GREEN, BLUE, YELLOW, CYAN, MAGENTA, BLACK;
  }

  private static final int NAMED_TAG = 0;
  private static final int RGB_TAG = 1;
  private static final int CMYK_TAG = 2;

  private final ColorType type;

  private final NamedColor namedColor;
  private final int[] rgbValues;
  private final int[] cmykValues;

  private enum ColorType {
    NAMED, RGB, CMYK
  }

  public Color(NamedColor namedColor) {
    this.type = ColorType.NAMED;
    this.namedColor = namedColor;
    this.rgbValues = null;
    this.cmykValues = null;
  }

  public Color(int red, int green, int blue) {
    validateRGBValues(red, green, blue);
    this.type = ColorType.RGB;
    this.namedColor = null;
    this.rgbValues = new int[] { red, green, blue };
    this.cmykValues = null;
  }

  public Color(int cyan, int magenta, int yellow, int black) {
    validateCMYKValues(cyan, magenta, yellow, black);
    this.type = ColorType.CMYK;
    this.namedColor = null;
    this.rgbValues = null;
    this.cmykValues = new int[] { cyan, magenta, yellow, black };
  }

  private void validateRGBValues(int red, int green, int blue) {
    if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
      throw new IllegalArgumentException("RGB values must be between 0 and 255");
    }
  }

  private void validateCMYKValues(int cyan, int magenta, int yellow, int black) {
    if (cyan < 0 || cyan > 255 || magenta < 0 || magenta > 255 ||
            yellow < 0 || yellow > 255 || black < 0 || black > 255) {
      throw new IllegalArgumentException("CMYK values must be between 0 and 255");
    }
  }

  public int[] pack() {
    switch (type) {
      case NAMED:
        return new int[] { NAMED_TAG, namedColor.ordinal() };
      case RGB:
        return new int[] { RGB_TAG, rgbValues[0], rgbValues[1], rgbValues[2] };
      case CMYK:
        return new int[] { CMYK_TAG, cmykValues[0], cmykValues[1], cmykValues[2], cmykValues[3] };
      default:
        throw new IllegalStateException("Unknown color type");
    }
  }

  public static Color unpack(int[] packed) {
    if (packed == null || packed.length == 0) {
      throw new IllegalArgumentException("Packed array cannot be null or empty");
    }

    int tag = packed[0];

    switch (tag) {
      case NAMED_TAG:
        if (packed.length != 2) {
          throw new IllegalArgumentException("Named color requires exactly 2 values");
        }
        int namedOrdinal = packed[1];
        if (namedOrdinal < 0 || namedOrdinal >= NamedColor.values().length) {
          throw new IllegalArgumentException("Invalid named color ordinal");
        }
        return new Color(NamedColor.values()[namedOrdinal]);

      case RGB_TAG:
        if (packed.length != 4) {
          throw new IllegalArgumentException("RGB color requires exactly 4 values");
        }
        return new Color(packed[1], packed[2], packed[3]);

      case CMYK_TAG:
        if (packed.length != 5) {
          throw new IllegalArgumentException("CMYK color requires exactly 5 values");
        }
        return new Color(packed[1], packed[2], packed[3], packed[4]);

      default:
        throw new IllegalArgumentException("Unknown color tag: " + tag);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Color color = (Color) o;

    if (type != color.type) return false;

    switch (type) {
      case NAMED:
        return namedColor == color.namedColor;
      case RGB:
        return Arrays.equals(rgbValues, color.rgbValues);
      case CMYK:
        return Arrays.equals(cmykValues, color.cmykValues);
      default:
        return false;
    }
  }
}

class ColorPropertyTests {

  @Provide
  Arbitrary<Color.NamedColor> namedColors() {
    return Arbitraries.of(Color.NamedColor.class);
  }

  @Provide
  Arbitrary<Color> namedColorObjects() {
    return namedColors().map(Color::new);
  }

  @Provide
  Arbitrary<Integer> rgbValue() {
    return Arbitraries.integers().between(0, 255);
  }

  @Provide
  Arbitrary<Color> rgbColorObjects() {
    return Combinators.combine(
            rgbValue(), rgbValue(), rgbValue()
    ).as(Color::new);
  }

  @Provide
  Arbitrary<Color> cmykColorObjects() {
    return Combinators.combine(
            rgbValue(), rgbValue(), rgbValue(), rgbValue()
    ).as(Color::new);
  }

  @Provide
  Arbitrary<Color> anyColorObjects() {
    return Arbitraries.oneOf(
            namedColorObjects(),
            rgbColorObjects(),
            cmykColorObjects()
    );
  }

  @Provide
  Arbitrary<int[]> rgbPackedArrays() {
    return Combinators.combine(
            rgbValue(), rgbValue(), rgbValue()
    ).as((r, g, b) -> new int[] { 1, r, g, b });
  }

  @Provide
  Arbitrary<int[]> cmykPackedArrays() {
    return Combinators.combine(
            rgbValue(), rgbValue(), rgbValue(), rgbValue()
    ).as((c, m, y, k) -> new int[] { 2, c, m, y, k });
  }

  @Provide
  Arbitrary<int[]> namedPackedArrays() {
    return Arbitraries.integers()
            .between(0, Color.NamedColor.values().length - 1)
            .map(i -> new int[] { 0, i });
  }

  @Provide
  Arbitrary<int[]> validPackedArrays() {
    return Arbitraries.oneOf(
            namedPackedArrays(),
            rgbPackedArrays(),
            cmykPackedArrays()
    );
  }

  @Provide
  Arbitrary<int[]> invalidTagPackedArrays() {
    return Arbitraries.integers().between(3, 100)
            .map(tag -> new int[] { tag, 0, 0, 0 });
  }

  @Provide
  Arbitrary<int[]> invalidSizePackedArrays() {
    return Arbitraries.integers().between(0, 2).flatMap(tag -> {
      int expectedSize;
      switch (tag) {
        case 0: expectedSize = 2; break;
        case 1: expectedSize = 4; break;
        case 2: expectedSize = 5; break;
        default: expectedSize = 5;
      }

      return Arbitraries.integers().between(1, 10)
              .filter(size -> size != expectedSize)
              .map(size -> {
                int[] arr = new int[size];
                arr[0] = tag;
                return arr;
              });
    });
  }

  @Provide
  Arbitrary<int[]> invalidValueRangeArrays() {
    return Arbitraries.oneOf(
            Arbitraries.integers().between(-100, 350)
                    .filter(v -> v < 0 || v > 255)
                    .map(v -> new int[] { 1, v, 100, 100 }),

            Arbitraries.integers().between(-100, 350)
                    .filter(v -> v < 0 || v > 255)
                    .map(v -> new int[] { 2, v, 100, 100, 100 })
    );
  }

  @Provide
  Arbitrary<int[]> invalidNamedColorOrdinalArrays() {
    return Arbitraries.integers()
            .filter(i -> i < 0 || i >= Color.NamedColor.values().length)
            .map(i -> new int[] { 0, i });
  }

  @Provide
  Arbitrary<int[]> allInvalidPackedArrays() {
    return Arbitraries.oneOf(
            invalidTagPackedArrays(),
            invalidSizePackedArrays(),
            invalidValueRangeArrays(),
            invalidNamedColorOrdinalArrays()
    );
  }

  @Property(tries = 100)
  boolean packThenUnpackIsIdentity(@ForAll("anyColorObjects") Color original) {
    int[] packed = original.pack();
    Color unpacked = Color.unpack(packed);
    return original.equals(unpacked);
  }

  @Property(tries = 100)
  boolean unpackThenPackIsIdentityForValidArrays(@ForAll("validPackedArrays") int[] originalPacked) {
    Color color = Color.unpack(originalPacked);
    int[] packedAgain = color.pack();
    return Arrays.equals(originalPacked, packedAgain);
  }

  @Property(tries = 100)
  boolean unpackThrowsForInvalidArrays(@ForAll("allInvalidPackedArrays") int[] invalidPacked) {
    try {
      Color color = Color.unpack(invalidPacked);
      return false;
    } catch (IllegalArgumentException e) {
      return true;
    }
  }
}