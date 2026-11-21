package net.mazatlab;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ImageChooser {
    /* Carpeta de imagenes incluida en el classpath (src/main/resources/img) */
    static final String BUNDLED_FOLDER = "img";
    static final String DEFAULT_IMAGE = "jugador(0).png";
    private static final String[] IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif"};

    /* Ruta de una carpeta externa; null significa usar las imagenes incluidas en el classpath */
    private final String path;

    /* El constructor sin argumentos usa las imagenes incluidas en la aplicacion, asi que
     * funciona sin importar desde que carpeta se ejecute */
    public ImageChooser() {
        this.path = null;
    }

    //En este constructor se puede definir la ruta de la carpeta contenedora de imagenes
    public ImageChooser(String path) {
        this.path = path;
    }

    /** Returns the default image, or null if the folder doesn't contain it. */
    public URL getDefaultImage() {
        Path folder = this.resolveFolder();
        if (folder == null) {
            return null;
        }
        Path defaultImage = folder.resolve(DEFAULT_IMAGE);
        System.out.println("Ruta de la imagen por defecto " + defaultImage);
        return Files.isRegularFile(defaultImage) ? toUrl(defaultImage) : null;
    }

    public List<URL> getImages() {
        List<URL> images = new ArrayList<URL>();

        Path folder = this.resolveFolder();
        System.out.println("Ruta de la carpeta con las imagenes: " + folder);
        if (folder == null || !Files.isDirectory(folder)) {
            System.out.println("No se encontro la carpeta de imagenes");
            return images;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file) && isImage(file)) {
                    images.add(toUrl(file));
                }
            }
        } catch (IOException e) {
            System.out.println("No se pudo leer la carpeta de imagenes: " + e.getMessage());
        }

        if (images.isEmpty()) {
            System.out.println("La carpeta no tiene imagenes");
        } else {
            System.out.println("La carpeta contiene " + images.size() + " imagenes");
        }
        return images;
    }

    /* Solo formatos que ImageIcon puede mostrar; ignora archivos ocultos como .DS_Store o ._foto.png */
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

    /* Resuelve la carpeta externa, o la carpeta incluida en el classpath (en disco o dentro del jar) */
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
            // Rutas invalidas (InvalidPathException) o sistemas de archivos no soportados
            System.out.println("No se pudo abrir la carpeta de imagenes: " + e.getMessage());
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
