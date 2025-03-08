import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.Color;

public class BakerMapEncryption {

    // Funktion zur Umwandlung eines Bildes in ein 8-Bit-Graustufenbild
    public static BufferedImage toGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                int gray = (int) (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
                grayImage.setRGB(x, y, new Color(gray, gray, gray).getRGB());
            }
        }
        return grayImage;
    }

    // Bildgröße auf gerade Werte setzen
    public static BufferedImage resizeToEven(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int newWidth = (width % 2 == 0) ? width : width - 1;
        int newHeight = (height % 2 == 0) ? height : height - 1;

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, image.getType());
        resizedImage.getGraphics().drawImage(image, 0, 0, newWidth, newHeight, null);

        return resizedImage;
    }

    // Funktion zur Anwendung der Baker Map Transformation mit zufälliger Permutation
    public static BufferedImage bakerMapTransform(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage transformedImage = new BufferedImage(width, height, image.getType());

        for (int y = 0; y < height/2; y++) {
            for (int x = 0; x < width; x++) {
                // First half moves to even rows
                transformedImage.setRGB(x, 2 * y, image.getRGB(x, y));

                // Second half moves to odd rows but also gets a small random shift
                int newX = (width - x - 1 + (int) (Math.random() * 3 - 1)) % width;
                transformedImage.setRGB(newX, 2 * y + 1, image.getRGB(x, height - y - 1));
            }
        }
        return transformedImage;
    }

    // Funktion zur Berechnung der Bildentropie
    public static double calculateEntropy(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        int[] histogram = new int[256];

        for (int i = 0; i < width - 1; i++) {
            for (int j = 0; j < height - 1; j++) {
                int pixel1 = new Color(image.getRGB(i, j)).getRed();
                int pixel2 = new Color(image.getRGB(i + 1, j)).getRed();
                int pixel3 = new Color(image.getRGB(i, j + 1)).getRed();

                int diff1 = Math.abs(pixel1 - pixel2);
                int diff2 = Math.abs(pixel1 - pixel3);

                histogram[diff1]++;
                histogram[diff2]++;
            }
        }

        // Shannon Formel - Entropie berechnen
        double entropy = 0.0;
        for (int count : histogram) {
            if (count > 0) {
                double probability = (double) count / (2 * totalPixels);
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }
        return entropy;
    }


    public static void main(String[] args) throws IOException {
        // Sicherstellen, dass der Ausgabeordner existiert
        File outputFolder = new File("output/");
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        // Bilder aus dem Ordner "images/" laden
        File folder = new File("images/");
        File[] files = folder.listFiles((_, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));

        if (files == null || files.length == 0) {
            System.out.println("Keine Bilder gefunden.");
            return;
        }


        for (File file : files) {
            BufferedImage originalImage = ImageIO.read(file);
            BufferedImage grayImage = toGrayscale(originalImage);
            BufferedImage transformedImage = resizeToEven(grayImage);

            // Speichern und Anzeigen des Graustufenbilds
            ImageIO.write(grayImage, "png", new File("output/" + file.getName().replace(".", "_gray.")));
            //ImageViewer.displayImage(grayImage, "Original Graustufenbild - " + file.getName());

            System.out.println("Verarbeite: " + file.getName());
            System.out.println("Entropie des Originalbildes: " + calculateEntropy(grayImage));

            long startTime = System.currentTimeMillis();

            int iteration = 0;
            double currEntropy = calculateEntropy(grayImage);

            while (iteration < 10) {
                transformedImage = bakerMapTransform(transformedImage);
                double newEntropy = calculateEntropy(transformedImage);
                // ImageViewer.displayImage(transformedImage, "Baker Map Iteration " + i);
                System.out.println("Iteration " + (iteration + 1) + " - Entropie: " + newEntropy);
                ImageIO.write(transformedImage, "png", new File("output/" + file.getName().replace(".", "_iter" + (iteration + 1) + ".")));

                if (newEntropy <= currEntropy) {
                    System.out.println("Entropie hat sich zweimal hintereinander nicht geändert oder ist gesunken. Beende Iterationen.");
                    break;
                }

                currEntropy = newEntropy;
                iteration++;
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Gesamte Laufzeit für " + file.getName() + ": " + (endTime - startTime) + " ms\n");
        }
    }
}