package org.ehrbase.api.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.Test;

public class VersionUidHelperTest {

  static final String VALID_VERSION_UID =
      "1234abcd-5678-ef12-ab34-cd56ef78ab90::test.ehrbase.org::10";

  @Test
  public void acceptsValidVersionUid() {
    assertThat(VersionUidHelper.isVersionUid(VALID_VERSION_UID)).isTrue();
  }

  @Test
  public void rejectsInvalidVersionUid() {
    String testString = "1234invalid";
    assertThat(VersionUidHelper.isVersionUid(testString)).isFalse();
  }

  @Test
  public void acceptsValidUUID() {
    String testString = "abcd1234-ef56-ab78-cd90-ef12ab34cd56";
    assertThat(VersionUidHelper.isUUID(testString)).isTrue();
  }

  @Test
  public void rejectsInvalidUUID() {
    String testString = "invalid-uuid";
    assertThat(VersionUidHelper.isUUID(testString)).isFalse();
  }

  @Test
  public void acceptsValidSystemId() {
    String testString = "::test123.ehrbase.org";
    assertThat(VersionUidHelper.isSystemId(testString)).isTrue();
  }

  @Test
  public void rejectsInvalidSystemId() {
    String testString = "this/is/not/a/valid/system/id";
    assertThat(VersionUidHelper.isSystemId(testString)).isFalse();
  }

  @Test
  public void acceptsValidVersion() {
    String testString = "::562";
    assertThat(VersionUidHelper.isVersion(testString)).isTrue();
  }

  @Test
  public void rejectsInvalidVersion() {
    String testString = "::invalid123";
    assertThat(VersionUidHelper.isVersion(testString)).isFalse();
  }

  @Test
  public void extractsUUID() {
    UUID expected = UUID.fromString("1234abcd-5678-ef12-ab34-cd56ef78ab90");
    assertThat(VersionUidHelper.extractUUID(VALID_VERSION_UID)).isEqualTo(expected);
  }

  @Test
  public void extractsSystemId() {
    String expected = "test.ehrbase.org";
    assertThat(VersionUidHelper.extractSystemId(VALID_VERSION_UID)).isEqualTo(expected);
  }

  @Test
  public void extractsVersion() {
    int expected = 10;
    assertThat(VersionUidHelper.extractVersion(VALID_VERSION_UID)).isEqualTo(expected);
  }
}
