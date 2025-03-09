import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;

public class BakerMapEncryption {

    // Konvertiert ein Bild in 8-Bit Graustufen
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

    // Passt die Bildgröße an, sodass sie gerade Dimensionen hat
    public static BufferedImage resizeToEven(BufferedImage image) {
        int width = (image.getWidth() % 2 == 0) ? image.getWidth() : image.getWidth() - 1;
        int height = (image.getHeight() % 2 == 0) ? image.getHeight() : image.getHeight() - 1;
        BufferedImage resizedImage = new BufferedImage(width, height, image.getType());
        Graphics g = resizedImage.getGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }

    // Wendet die verbesserte Baker-Map-Transformation an
    public static BufferedImage applyBakerMap(BufferedImage img, int key) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage transformed = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        int mid = height / 2;

        // Die obere Hälfte des Bildes wird nach unten gestreckt
        // Die untere Hälfte des Bildes wird gespiegelt und dann auf die freien Stellen gesetzt
        for (int y = 0; y < mid; y++) {
            for (int x = 0; x < width; x++) {
                transformed.setRGB(x, 2 * y, img.getRGB(x, y)); // Pixel von oben nach unten kopieren

                // Kleine zufällige Verschiebung basierend auf dem Schlüssel
                int shift = ((key + x) % 3) - 1;
                int newX = (width - x - 1 + shift + width) % width;
                transformed.setRGB(newX, 2 * y + 1, img.getRGB(x, height - y - 1)); // Gespiegelte Pixel platzieren
            }
        }
        return transformed;
    }

    // Berechnet die Shannon-Entropie des Bildes
    public static double calculateEntropy(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;
        int[] histogram = new int[256]; // Histogramm für Graustufen-Werte (0-255)

        // Histogramm des Bildes berechnen
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = new Color(image.getRGB(x, y)).getRed(); // Grauwert extrahieren
                histogram[gray]++;
            }
        }

        // Entropie berechnen basierend auf der Wahrscheinlichkeitsverteilung der Pixelwerte
        double entropy = 0.0;
        for (int count : histogram) {
            if (count > 0) {
                double probability = (double) count / totalPixels;
                entropy -= probability * (Math.log(probability) / Math.log(2)); // Shannon-Formel anwenden
            }
        }
        return entropy;
    }

    public static void main(String[] args) throws IOException {
        File outputFolder = new File("output/");
        if (!outputFolder.exists()) outputFolder.mkdirs();

        File folder = new File("images/");
        File[] files = folder.listFiles((_, name) -> name.toLowerCase().matches(".*\\.(png|jpg)"));

        if (files == null || files.length == 0) {
            System.out.println("Keine Bilder gefunden.");
            return;
        }

        int encryptionKey = 1234; // Beispiel-Schlüssel für Reproduzierbarkeit

        for (File file : files) {
            BufferedImage originalImage = ImageIO.read(file);
            BufferedImage grayImage = toGrayscale(originalImage);
            BufferedImage transformedImage = resizeToEven(grayImage);

            // Speichern und Anzeigen des Graustufenbilds
            ImageIO.write(grayImage, "png", new File("output/" + file.getName().replace(".", "_gray.")));
            //ImageViewer.displayImage(grayImage, "Original Graustufenbild - " + file.getName());

            System.out.println("Verarbeite: " + file.getName());
            double currEntropy = calculateEntropy(grayImage);
            System.out.println("Entropie des Originalbildes: " + currEntropy);

            long startTime = System.currentTimeMillis();

            for (int iteration = 0; iteration < 10; iteration++) {
                transformedImage = applyBakerMap(transformedImage, encryptionKey);
                double newEntropy = calculateEntropy(transformedImage);

                if (iteration >= 2 && (newEntropy - currEntropy) / currEntropy < 0.02) {
                    System.out.println("Entropie hat sich nicht signifikant erhöht. Beende Iterationen.");
                    break;
                }

                //ImageViewer.displayImage(transformedImage, "Baker Map Iteration " + (iteration + 1));
                System.out.println("Iteration " + (iteration + 1) + " - Entropie: " + newEntropy);
                ImageIO.write(transformedImage, "png", new File("output/" + file.getName().replace(".", "_iter" + (iteration + 1) + ".")));

                currEntropy = newEntropy;
            }

            System.out.println("Gesamte Laufzeit für " + file.getName() + ": " + (System.currentTimeMillis() - startTime) + " ms\n");
        }
    }
}
