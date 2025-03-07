import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.awt.Color;

public class AESImageEncryption {

    // AES-Schlüssel generieren
    private static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128); // 128-Bit Schlüssel
        return keyGen.generateKey();
    }

    // IV für CBC-Modus generieren
    private static IvParameterSpec generateIV() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    // AES-Bildverschlüsselung mit CBC-Modus
    public static BufferedImage encryptImage(BufferedImage image, SecretKey key, IvParameterSpec iv) throws Exception {
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        // 8-Bit Graustufen-Bilddaten extrahieren
        byte[] pixelData = new byte[totalPixels];
        for (int i = 0; i < totalPixels; i++) {
            int x = i % width;
            int y = i / width;
            pixelData[i] = (byte) new Color(image.getRGB(x, y)).getRed();
        }

        // Padding auf ein Vielfaches von 16 Bytes auffüllen
        int paddedLength = ((pixelData.length + 15) / 16) * 16;
        byte[] paddedPixels = Arrays.copyOf(pixelData, paddedLength);

        // AES-CBC Verschlüsselung initialisieren
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] encryptedPixels = cipher.doFinal(paddedPixels);

        // Verschlüsseltes Bild erstellen
        BufferedImage encryptedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int i = 0; i < totalPixels; i++) {
            int x = i % width;
            int y = i / width;
            int gray = encryptedPixels[i] & 0xFF;
            encryptedImage.setRGB(x, y, new Color(gray, gray, gray).getRGB());
        }

        return encryptedImage;
    }

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

    // Bildentropie berechnen
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

    public static void main(String[] args) throws Exception {
        // Sicherstellen, dass der Ausgabeordner existiert
        File outputFolder = new File("output/");
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        // AES-Schlüssel generieren
        SecretKey aesKey = generateAESKey();
        IvParameterSpec iv = generateIV();

        // Bilder aus src/ laden
        File folder = new File("src/");
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));

        if (files == null || files.length == 0) {
            System.out.println("Keine Bilder gefunden.");
            return;
        }

        for (File file : files) {
            BufferedImage originalImage = ImageIO.read(file);
            BufferedImage grayImage = toGrayscale(originalImage);

            ImageViewer.displayImage(grayImage, "Graustufenbild - " + file.getName());

            // Graustufenbild speichern
            ImageIO.write(grayImage, "png", new File("output/" + file.getName().replace(".", "_gray.")));

            System.out.println("Verarbeite: " + file.getName());
            System.out.println("Entropie des Originalbildes: " + calculateEntropy(grayImage));

            long startTime = System.currentTimeMillis();

            // Bild verschlüsseln mit AES-CBC
            BufferedImage encryptedImage = encryptImage(grayImage, aesKey, iv);
            ImageIO.write(encryptedImage, "png", new File("output/" + file.getName().replace(".", "_aes.")));

            long endTime = System.currentTimeMillis();
            System.out.println("Entropie nach AES-Verschlüsselung: " + calculateEntropy(encryptedImage));
            System.out.println("Gesamte Laufzeit für " + file.getName() + ": " + (endTime - startTime) + " ms\n");

            // Visualisierung des verschlüsselten Bildes
            ImageViewer.displayImage(encryptedImage, "AES-Verschlüsseltes Bild - " + file.getName());
        }
    }
}
