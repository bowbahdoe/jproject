package dev.mccue.library;

public final class LibTest {
    @Test
    public int testValue() {
        return assertEquals(2, Lib.value());
    }
}