package net.mazatlab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class StudentPoolTest {

    private static Student student(String name) {
        try {
            return new Student(name, URI.create("file:///class/" + name + ".png").toURL());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Student> classOf(String... names) {
        List<Student> students = new ArrayList<>();
        for (String name : names) {
            students.add(student(name));
        }
        return students;
    }

    @Test
    void newPoolHasEveryStudentRemaining() {
        StudentPool pool = new StudentPool(classOf("ana", "luis", "zoe"), new Random(1));

        assertEquals(3, pool.total());
        assertEquals(3, pool.remainingCount());
        assertFalse(pool.isEmpty());
        assertTrue(pool.picked().isEmpty());
    }

    @Test
    void pickedStudentLeavesThePool() {
        List<Student> students = classOf("ana", "luis", "zoe");
        StudentPool pool = new StudentPool(students, new Random(1));

        pool.pick(students.get(1));

        assertEquals(2, pool.remainingCount());
        assertEquals(List.of(students.get(1)), pool.picked());
    }

    @Test
    void aStudentCannotBePickedTwice() {
        List<Student> students = classOf("ana", "luis");
        StudentPool pool = new StudentPool(students, new Random(1));
        pool.pick(students.get(0));

        assertThrows(IllegalArgumentException.class, () -> pool.pick(students.get(0)));
    }

    @Test
    void candidatesComeOnlyFromRemainingStudents() {
        List<Student> students = classOf("ana", "luis", "zoe");
        StudentPool pool = new StudentPool(students, new Random(7));
        pool.pick(students.get(0));
        pool.pick(students.get(2));

        for (int i = 0; i < 50; i++) {
            assertEquals(students.get(1), pool.randomCandidate());
        }
    }

    @Test
    void aFullSessionPicksEveryStudentExactlyOnce() {
        List<Student> students = classOf("ana", "luis", "zoe", "maria", "pedro");
        StudentPool pool = new StudentPool(students, new Random(42));

        while (!pool.isEmpty()) {
            pool.pick(pool.randomCandidate());
        }

        assertEquals(5, pool.picked().size());
        assertEquals(new HashSet<>(students), Set.copyOf(pool.picked()));
        assertEquals(0, pool.remainingCount());
    }

    @Test
    void emptyPoolHasNoCandidates() {
        StudentPool pool = new StudentPool(List.of(), new Random(1));

        assertTrue(pool.isEmpty());
        assertThrows(IllegalStateException.class, pool::randomCandidate);
    }

    @Test
    void poolDoesNotChangeTheCallersList() {
        List<Student> students = classOf("ana", "luis");
        StudentPool pool = new StudentPool(students, new Random(1));

        pool.pick(students.get(0));

        assertEquals(2, students.size());
    }
}
