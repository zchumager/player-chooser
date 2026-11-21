package net.mazatlab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StudentTest {

    @Test
    void nameDropsTheExtension() {
        assertEquals("maria", Student.nameFromFileName("maria.png"));
    }

    @Test
    void underscoresBecomeSpaces() {
        assertEquals("maria lopez", Student.nameFromFileName("maria_lopez.jpeg"));
    }

    @Test
    void onlyTheLastExtensionIsDropped() {
        assertEquals("j.r. smith", Student.nameFromFileName("j.r._smith.png"));
    }

    @Test
    void hyphensAndCaseAreKept() {
        assertEquals("Ana-Sofía", Student.nameFromFileName("Ana-Sofía.gif"));
    }
}
