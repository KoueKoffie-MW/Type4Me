package com.transcriptor.hid.data.db

import com.transcriptor.hid.engine.KeyLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    @Test
    fun testStringListConverter() {
        val original = listOf("git", "status", "vcs", "terminal")
        val json = converters.fromStringList(original)
        assertTrue(json.contains("git"))

        val decoded = converters.toStringList(json)
        assertEquals(original, decoded)

        // Empty & null handling
        assertEquals("[]", converters.fromStringList(emptyList()))
        assertEquals("[]", converters.fromStringList(null))
        assertEquals(emptyList<String>(), converters.toStringList(null))
        assertEquals(emptyList<String>(), converters.toStringList(""))
        assertEquals(emptyList<String>(), converters.toStringList("   "))
        assertEquals(emptyList<String>(), converters.toStringList("invalid json {"))
    }

    @Test
    fun testSyntaxTypeConverter() {
        for (syntax in SyntaxType.values()) {
            val name = converters.fromSyntaxType(syntax)
            assertEquals(syntax.name, name)
            val parsed = converters.toSyntaxType(name)
            assertEquals(syntax, parsed)
        }

        // Fallbacks
        assertEquals(SyntaxType.SHELL, converters.toSyntaxType(null))
        assertEquals(SyntaxType.SHELL, converters.toSyntaxType(""))
        assertEquals(SyntaxType.SHELL, converters.toSyntaxType("NON_EXISTENT_TYPE"))
        assertEquals("SHELL", converters.fromSyntaxType(null))
    }

    @Test
    fun testHostOsTypeConverter() {
        for (os in HostOsType.values()) {
            val name = converters.fromHostOsType(os)
            assertEquals(os.name, name)
            val parsed = converters.toHostOsType(name)
            assertEquals(os, parsed)
        }

        // Fallbacks
        assertEquals(HostOsType.WINDOWS, converters.toHostOsType(null))
        assertEquals(HostOsType.WINDOWS, converters.toHostOsType(""))
        assertEquals(HostOsType.WINDOWS, converters.toHostOsType("UNKNOWN_OS"))
        assertEquals("WINDOWS", converters.fromHostOsType(null))
    }

    @Test
    fun testKeyLayoutConverter() {
        for (layout in KeyLayout.values()) {
            val name = converters.fromKeyLayout(layout)
            assertEquals(layout.name, name)
            val parsed = converters.toKeyLayout(name)
            assertEquals(layout, parsed)
        }

        // Fallbacks
        assertEquals(KeyLayout.US_QWERTY, converters.toKeyLayout(null))
        assertEquals(KeyLayout.US_QWERTY, converters.toKeyLayout(""))
        assertEquals(KeyLayout.US_QWERTY, converters.toKeyLayout("DVORAK"))
        assertEquals("US_QWERTY", converters.fromKeyLayout(null))
    }
}
