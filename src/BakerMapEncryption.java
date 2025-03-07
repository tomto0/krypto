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

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int newX, newY;

                // Correct Baker Map Transformation
                if (x < width / 2) {
                    newX = 2 * x;
                    newY = y / 2;
                } else {
                    newX = 2 * (x - width / 2) + 1;
                    newY = height / 2 + y / 2;
                }

                transformedImage.setRGB(newX, newY, image.getRGB(x, y));
            }
        }
        return transformedImage;
    }

    // Funktion zur Berechnung der Bildentropie
    public static double calculateEntropy(BufferedImage image) {
        int[] histogram = new int[256];
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = new Color(image.getRGB(x, y)).getRed();
                histogram[gray]++;
            }
        }

        double entropy = 0.0;
        for (int count : histogram) {
            if (count > 0) {
                double probability = (double) count / totalPixels;
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

        int iterations = 10;

        for (File file : files) {
            BufferedImage originalImage = ImageIO.read(file);
            BufferedImage grayImage = toGrayscale(originalImage);
            BufferedImage evenSizedImage = resizeToEven(grayImage);
            BufferedImage transformedImage = evenSizedImage;

            // Speichern und Anzeigen des Graustufenbilds
            ImageIO.write(grayImage, "png", new File("output/" + file.getName().replace(".", "_gray.")));
            ImageViewer.displayImage(grayImage, "Original Graustufenbild - " + file.getName());

            System.out.println("Verarbeite: " + file.getName());
            System.out.println("Entropie des Originalbildes: " + calculateEntropy(grayImage));

            long startTime = System.currentTimeMillis();

            for (int i = 1; i <= iterations; i++) {
                transformedImage = bakerMapTransform(transformedImage);
                ImageIO.write(transformedImage, "png", new File("output/" + file.getName().replace(".", "_iter" + i + ".")));

                // Visualisierung der Transformation
                ImageViewer.displayImage(transformedImage, "Baker Map Iteration " + i);

                System.out.println("Iteration " + i + " - Entropie: " + calculateEntropy(transformedImage));
            }

            long endTime = System.currentTimeMillis();
            System.out.println("Gesamte Laufzeit für " + file.getName() + ": " + (endTime - startTime) + " ms\n");
        }
    }
}