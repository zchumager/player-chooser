package net.mazatlab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The students who haven't participated yet in this session. A picked student leaves the pool,
 * so nobody is picked twice until a new pool (a new session) is created.
 */
final class StudentPool {

    private final List<Student> remaining;
    private final List<Student> picked = new ArrayList<>();
    private final Random random;
    private final int total;

    StudentPool(List<Student> students, Random random) {
        this.remaining = new ArrayList<>(students);
        this.random = random;
        this.total = students.size();
    }

    int total() {
        return this.total;
    }

    int remainingCount() {
        return this.remaining.size();
    }

    boolean isEmpty() {
        return this.remaining.isEmpty();
    }

    /** Students picked so far, in the order they were picked. */
    List<Student> picked() {
        return Collections.unmodifiableList(this.picked);
    }

    /** A random student who hasn't been picked yet, without removing them. */
    Student randomCandidate() {
        if (this.remaining.isEmpty()) {
            throw new IllegalStateException("Every student has already been picked");
        }
        return this.remaining.get(this.random.nextInt(this.remaining.size()));
    }

    /** Marks the student as picked, removing them from the pool. */
    void pick(Student student) {
        if (!this.remaining.remove(student)) {
            throw new IllegalArgumentException("Not in the pool: " + student.name());
        }
        this.picked.add(student);
    }
}
