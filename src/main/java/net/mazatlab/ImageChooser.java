package net.mazatlab;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Loads the student photos, either the bundled demo images or a folder of the user's own. */
public class ImageChooser {
    /* Demo images bundled on the classpath (src/main/resources/img) */
    static final String BUNDLED_FOLDER = "img";
    /* Folder of class photos looked for in the working directory at startup */
    static final String CLASS_FOLDER = "img";
    private static final String[] IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif"};

    /* Path of an external folder; null means the bundled demo images */
    private final String path;

    /** Uses the bundled demo images, which works from any directory and from the jar. */
    public ImageChooser() {
        this.path = null;
    }

    /** Uses the images in the given folder. */
    public ImageChooser(String path) {
        this.path = path;
    }

    /**
     * Picks the images to start with: the folder given on the command line, otherwise an "img"
     * folder in the working directory, otherwise the bundled demo images.
     */
    static ImageChooser forStartup(String[] args, Path workingDir) {
        if (args.length > 0) {
            return new ImageChooser(args[0]);
        }
        Path classFolder = workingDir.resolve(CLASS_FOLDER);
        if (Files.isDirectory(classFolder)) {
            return new ImageChooser(classFolder.toString());
        }
        return new ImageChooser();
    }

    public boolean isDemo() {
        return this.path == null;
    }

    /** A short name for the image source, for the window title: "Demo images" or the folder's name. */
    public String description() {
        if (this.path == null) {
            return "Demo images";
        }
        try {
            Path folder = Paths.get(this.path).toAbsolutePath().normalize();
            return folder.getFileName() != null ? folder.getFileName().toString() : folder.toString();
        } catch (RuntimeException e) {
            return this.path;
        }
    }

    /** Returns one student per image file, sorted by name. Empty if the folder is missing or unreadable. */
    public List<Student> getImages() {
        List<Student> students = new ArrayList<Student>();

        Path folder = this.resolveFolder();
        System.out.println("Image folder: " + folder);
        if (folder == null || !Files.isDirectory(folder)) {
            System.out.println("Image folder not found");
            return students;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file) && isImage(file)) {
                    String name = Student.nameFromFileName(file.getFileName().toString());
                    students.add(new Student(name, toUrl(file)));
                }
            }
        } catch (IOException | DirectoryIteratorException e) {
            // DirectoryIteratorException is unchecked and thrown while iterating, e.g. if the
            // folder becomes unreadable midway
            System.out.println("Could not read the image folder: " + e.getMessage());
        }

        // Ties are broken by file name so the order, and which duplicate is kept, never changes
        students.sort(Comparator.comparing(Student::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(student -> student.image().toString()));
        List<Student> unique = withoutDuplicateNames(students);
        System.out.println("Found " + unique.size() + " images");
        return unique;
    }

    /* Two photos with the same name (e.g. ana.png and Ana.jpg) would let a student be picked twice
     * in one session, so only the first one is kept. Expects the list sorted by name. */
    private static List<Student> withoutDuplicateNames(List<Student> sorted) {
        List<Student> unique = new ArrayList<Student>();
        Set<String> seen = new HashSet<String>();
        for (Student student : sorted) {
            if (seen.add(student.name().toLowerCase(Locale.ROOT))) {
                unique.add(student);
            } else {
                System.out.println("Skipped " + student.image() + ": another photo is already named \"" + student.name() + "\"");
            }
        }
        return unique;
    }

    /* Only formats ImageIcon can display; skips hidden files such as .DS_Store or ._photo.png */
    static boolean isImage(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.startsWith(".")) {
            return false;
        }
        for (String extension : IMAGE_EXTENSIONS) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    /* Resolves the external folder, or the bundled folder on the classpath (on disk or inside the jar) */
    private Path resolveFolder() {
        try {
            if (this.path != null) {
                return Paths.get(this.path);
            }
            URL url = ImageChooser.class.getResource("/" + BUNDLED_FOLDER);
            if (url == null) {
                return null;
            }
            URI uri = url.toURI();
            if ("jar".equals(uri.getScheme())) {
                return jarFileSystem(uri).getPath("/" + BUNDLED_FOLDER);
            }
            return Paths.get(uri);
        } catch (URISyntaxException | IOException | RuntimeException e) {
            // Invalid paths (InvalidPathException) or unsupported file systems
            System.out.println("Could not open the image folder: " + e.getMessage());
            return null;
        }
    }

    private static FileSystem jarFileSystem(URI uri) throws IOException {
        try {
            return FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
        } catch (FileSystemAlreadyExistsException e) {
            return FileSystems.getFileSystem(uri);
        }
    }

    private static URL toUrl(Path file) {
        try {
            return file.toUri().toURL();
        } catch (IOException e) {
            throw new IllegalStateException("Invalid image path: " + file, e);
        }
    }
}
