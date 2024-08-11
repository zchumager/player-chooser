package net.mazatlab;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageChooser {
    private File dir;
    private File defaultImage;

    /* El constructor sin argumentos por defecto define a la carpeta img como la carpeta
     * contenedora de imagenes*/
    public ImageChooser() {
        this.dir = new File("img");//Para java tambien las carpetas son archivos
    }

    //En este constructor se puede definir la ruta de la carpeta contenedora de imagenes
    public ImageChooser(String path) {
        this.dir = new File(path);
    }

    public File getDefaultImage() {
        this.defaultImage = new File(this.dir.getPath()
                + File.separator + "jugador(0).png");
        System.out.println("Ruta de la imagen por defecto "
                + defaultImage.getPath());
        return defaultImage;
    }

    public List<File> getImages(){
        List<File> files = new ArrayList<File>();

        System.out.println("Ruta de la carpeta con las imagenes: "
                + this.dir.getPath());
        if(dir.isDirectory()) {
            System.out.println("Es una carpeta");

            for(File file : dir.listFiles()){
                if(file.isFile()){
                    files.add(file);
                }
            }

            if(files.size() == 0) {
                System.out.println("La carpeta no tiene archivos");
            } else {
                System.out.println("La carpeta contiene los archivos: ");
                for(File file : files) {
                    System.out.println(file.getName());
                }
            }
        } else {
            System.out.println("Es un archivo");
        }
        return files;
    }
}