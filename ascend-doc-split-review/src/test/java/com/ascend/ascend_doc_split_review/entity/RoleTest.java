package com.ascend.ascend_doc_split_review.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void testRoleEnumValues() {
        // Test that ACCOUNTANT is the only value
        User.Role[] roles = User.Role.values();
        assertEquals(1, roles.length);
        assertEquals(User.Role.ACCOUNTANT, roles[0]);
    }

    @Test
    void testRoleAccountant() {
        User.Role accountant = User.Role.ACCOUNTANT;
        assertNotNull(accountant);
        assertEquals("ACCOUNTANT", accountant.name());
        assertEquals(0, accountant.ordinal());
    }

    @Test
    void testRoleValueOf() {
        User.Role role = User.Role.valueOf("ACCOUNTANT");
        assertEquals(User.Role.ACCOUNTANT, role);
    }

    @Test
    void testRoleToString() {
        assertEquals("ACCOUNTANT", User.Role.ACCOUNTANT.toString());
    }
}