package com.smartattendance.app

import com.smartattendance.app.security.PinHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun pinHasher_verifiesCorrectPinAndRejectsWrongPin() {
        val salt1 = PinHasher.generateSalt()
        val salt2 = PinHasher.generateSalt()
        val encodedSalt1 = PinHasher.encodeSalt(salt1)
        val encodedSalt2 = PinHasher.encodeSalt(salt2)

        val hash1 = PinHasher.hashPin("123456", salt1)
        val hash2 = PinHasher.hashPin("123456", salt2)

        assertNotEquals(hash1, hash2)
        assertTrue(PinHasher.verifyPin("123456", encodedSalt1, hash1))
        assertTrue(PinHasher.verifyPin("123456", encodedSalt2, hash2))
        assertFalse(PinHasher.verifyPin("654321", encodedSalt1, hash1))
    }
}
