package net.gotev.sipservice;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ObfuscationHelperTest {

    @Test
    public void nullReturnsEmpty() {
        // Previously threw NPE on string.length()
        assertEquals("", ObfuscationHelper.obfuscate(null));
    }

    @Test
    public void emptyReturnsEmpty() {
        // Previously threw NegativeArraySizeException via repeat(-1)
        assertEquals("", ObfuscationHelper.obfuscate(""));
    }

    @Test
    public void singleCharIsFullyMasked() {
        // Previously leaked the whole character via substring(0)
        assertEquals("*", ObfuscationHelper.obfuscate("4"));
    }

    @Test
    public void shortStringKeepsOnlyLastChar() {
        assertEquals("*4", ObfuscationHelper.obfuscate("34"));
        assertEquals("***4", ObfuscationHelper.obfuscate("1234"));
    }

    @Test
    public void longStringKeepsOnlyLastThreeChars() {
        assertEquals("******789", ObfuscationHelper.obfuscate("123456789"));
    }
}
