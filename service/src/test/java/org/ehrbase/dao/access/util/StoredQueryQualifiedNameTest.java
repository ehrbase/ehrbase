package org.ehrbase.dao.access.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class StoredQueryQualifiedNameTest {

  @Test
  public void testFullName() {
    String name = "org.example.departmentx.test::diabetes-patient-overview/1.0.2";

    StoredQueryQualifiedName storedQueryQualifiedName = new StoredQueryQualifiedName(name);

    assertNotNull(storedQueryQualifiedName);

    assertEquals("org.example.departmentx.test", storedQueryQualifiedName.reverseDomainName());
    assertEquals("diabetes-patient-overview", storedQueryQualifiedName.semanticId());
    assertEquals("1.0.2", storedQueryQualifiedName.semVer());
  }

  @Test
  public void testUncompleteName() {
    String name = "org.example.departmentx.test::diabetes-patient-overview";

    StoredQueryQualifiedName storedQueryQualifiedName = new StoredQueryQualifiedName(name);

    assertNotNull(storedQueryQualifiedName);

    assertEquals("org.example.departmentx.test", storedQueryQualifiedName.reverseDomainName());
    assertEquals("diabetes-patient-overview", storedQueryQualifiedName.semanticId());
    assertNull(storedQueryQualifiedName.semVer());
  }

  @Test
  public void testBadlyformedName() {
    String name = "org.example.departmentx.test/diabetes-patient-overview";

    try {
      new StoredQueryQualifiedName(name);
      fail();
    } catch (Exception e) {
    }
  }
}
