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

    // AES-Schlüssel generieren (128 Bit)
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

    // Bilddaten in Byte-Array umwandeln (8-Bit Graustufen)
    private static byte[] imageToByteArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] pixelData = new byte[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelData[y * width + x] = (byte) new Color(image.getRGB(x, y)).getRed();
            }
        }
        return pixelData;
    }

    // Byte-Array wieder in Bild umwandeln
    private static BufferedImage byteArrayToImage(byte[] data, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = data[y * width + x] & 0xFF;
                image.setRGB(x, y, new Color(gray, gray, gray).getRGB());
            }
        }
        return image;
    }

    // AES-Bildverschlüsselung (CBC-Modus mit Standard-Padding)
    public static BufferedImage encryptImage(BufferedImage image, SecretKey key, IvParameterSpec iv) throws Exception {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] pixelData = imageToByteArray(image);

        // AES-Cipher initialisieren
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        // Verschlüsselung durchführen
        byte[] encryptedData = cipher.doFinal(pixelData);

        // Verschlüsseltes Bild zurückgeben
        return byteArrayToImage(encryptedData, width, height);
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

        File folder = new File("images/");
        File[] files = folder.listFiles((_, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));

        if (files == null || files.length == 0) {
            System.out.println("Keine Bilder gefunden.");
            return;
        }

        for (File file : files) {
            BufferedImage originalImage = ImageIO.read(file);
            BufferedImage grayImage = BakerMapEncryption.toGrayscale(originalImage);

            // ImageViewer.displayImage(grayImage, "Graustufenbild - " + file.getName());
            ImageIO.write(grayImage, "png", new File("output/" + file.getName().replace(".", "_gray.")));

            System.out.println("Verarbeite: " + file.getName());
            System.out.println("Entropie des Originalbildes: " + BakerMapEncryption.calculateEntropy(grayImage));

            long startTime = System.currentTimeMillis();

            // Bild mit AES verschlüsseln
            BufferedImage encryptedImage = encryptImage(grayImage, aesKey, iv);
            ImageIO.write(encryptedImage, "png", new File("output/" + file.getName().replace(".", "_aes.")));

            long endTime = System.currentTimeMillis();
            System.out.println("Entropie nach AES-Verschlüsselung: " + BakerMapEncryption.calculateEntropy(encryptedImage));
            System.out.println("Gesamte Laufzeit für " + file.getName() + ": " + (endTime - startTime) + " ms\n");
            //ImageViewer.displayImage(encryptedImage, "AES-Verschlüsseltes Bild - " + file.getName());
        }
    }
}
