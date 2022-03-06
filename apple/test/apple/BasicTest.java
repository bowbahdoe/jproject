package apple;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public final class BasicTest {
    @Test
    public void basicTest() {
        assertEquals(8, Main.x());
    }
}
