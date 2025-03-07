import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
        byte[] paddedPixels = new byte[paddedLength];
        System.arraycopy(pixelData, 0, paddedPixels, 0, pixelData.length);

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
            BufferedImage grayImage = BakerMapEncryption.toGrayscale(originalImage);

            ImageViewer.displayImage(grayImage, "Graustufenbild - " + file.getName());

            // Graustufenbild speichern
            ImageIO.write(grayImage, "png", new File("output/" + file.getName().replace(".", "_gray.")));

            System.out.println("Verarbeite: " + file.getName());
            System.out.println("Entropie des Originalbildes: " + BakerMapEncryption.calculateEntropy(grayImage));

            long startTime = System.currentTimeMillis();

            // Bild verschlüsseln mit AES-CBC
            BufferedImage encryptedImage = encryptImage(grayImage, aesKey, iv);
            ImageIO.write(encryptedImage, "png", new File("output/" + file.getName().replace(".", "_aes.")));

            long endTime = System.currentTimeMillis();
            System.out.println("Entropie nach AES-Verschlüsselung: " + BakerMapEncryption.calculateEntropy(encryptedImage));
            System.out.println("Gesamte Laufzeit für " + file.getName() + ": " + (endTime - startTime) + " ms\n");

            // Visualisierung des verschlüsselten Bildes
            ImageViewer.displayImage(encryptedImage, "AES-Verschlüsseltes Bild - " + file.getName());
        }
    }
}
