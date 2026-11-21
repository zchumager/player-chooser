package net.mazatlab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageChooserTest {

    @TempDir
    Path tempDir;

    private void createFile(String name) throws IOException {
        Files.createFile(this.tempDir.resolve(name));
    }

    private List<Student> loadTempDir() {
        return new ImageChooser(this.tempDir.toString()).getImages();
    }

    @Test
    void bundledDemoImagesLoadFromClasspath() {
        List<Student> students = new ImageChooser().getImages();

        assertEquals(7, students.size());
        assertEquals("player 1", students.get(0).name());
    }

    @Test
    void externalFolderImagesAreLoaded() throws IOException {
        createFile("a.png");
        createFile("b.jpg");

        assertEquals(2, loadTempDir().size());
    }

    @Test
    void studentsAreNamedAfterTheirFilesAndSortedByName() throws IOException {
        createFile("zoe.png");
        createFile("maria_lopez.jpg");
        createFile("Ana.png");

        List<String> names = loadTempDir().stream().map(Student::name).toList();

        assertEquals(List.of("Ana", "maria lopez", "zoe"), names);
    }

    @Test
    void photosWithTheSameNameCountAsOneStudent() throws IOException {
        createFile("ana.png");
        createFile("Ana.jpg");
        createFile("luis.png");

        List<String> names = loadTempDir().stream().map(Student::name).toList();

        // Ties are broken by file name, so the same photo is kept every time
        assertEquals(List.of("Ana", "luis"), names);
    }

    @Test
    void onlyImageFilesAreLoaded() throws IOException {
        createFile("a.png");
        createFile("b.JPG");
        createFile("c.jpeg");
        createFile("d.gif");
        createFile(".DS_Store");
        createFile("notes.txt");
        createFile("png");
        createFile("._e.png"); // macOS metadata file on external drives

        assertEquals(4, loadTempDir().size());
    }

    @Test
    void invalidFolderPathReturnsNoImages() {
        assertTrue(new ImageChooser("bad\u0000path").getImages().isEmpty());
    }

    @Test
    void startupUsesTheCommandLineFolderFirst() throws IOException {
        Files.createDirectories(this.tempDir.resolve("img"));
        Path classFolder = Files.createDirectories(this.tempDir.resolve("class-3B"));

        ImageChooser chooser = ImageChooser.forStartup(new String[] {classFolder.toString()}, this.tempDir);

        assertFalse(chooser.isDemo());
        assertEquals("class-3B", chooser.description());
    }

    @Test
    void startupUsesTheImgFolderInTheWorkingDirectory() throws IOException {
        Files.createDirectories(this.tempDir.resolve("img"));
        Files.createFile(this.tempDir.resolve("img").resolve("ana.png"));

        ImageChooser chooser = ImageChooser.forStartup(new String[0], this.tempDir);

        assertFalse(chooser.isDemo());
        assertEquals("img", chooser.description());
        assertEquals("ana", chooser.getImages().get(0).name());
    }

    @Test
    void startupFallsBackToTheDemoImages() {
        ImageChooser chooser = ImageChooser.forStartup(new String[0], this.tempDir);

        assertTrue(chooser.isDemo());
        assertEquals("Demo images", chooser.description());
        assertEquals(7, chooser.getImages().size());
    }

    @Test
    void missingFolderReturnsNoImages() {
        ImageChooser chooser = new ImageChooser(this.tempDir.resolve("missing").toString());

        assertTrue(chooser.getImages().isEmpty());
    }
}
