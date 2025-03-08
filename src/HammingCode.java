import java.util.Scanner;

public class HammingCode {

    // Methode zur Berechnung des Hamming(7,4)-Codes
    public static int[] encode(int[] data) {
        int[] encoded = new int[7];

        // Datenbits setzen
        encoded[2] = data[0]; // D1
        encoded[4] = data[1]; // D2
        encoded[5] = data[2]; // D3
        encoded[6] = data[3]; // D4

        // Paritätsbits berechnen
        encoded[0] = encoded[2] ^ encoded[4] ^ encoded[6]; // P1
        encoded[1] = encoded[2] ^ encoded[5] ^ encoded[6]; // P2
        encoded[3] = encoded[4] ^ encoded[5] ^ encoded[6]; // P3

        return encoded;
    }

    // Methode zur Fehlererkennung und -korrektur
    public static int[] decode(int[] received) {
        int p1 = received[0] ^ received[2] ^ received[4] ^ received[6];
        int p2 = received[1] ^ received[2] ^ received[5] ^ received[6];
        int p3 = received[3] ^ received[4] ^ received[5] ^ received[6];

        int errorPosition = p1 + p2 * 2 + p3 * 4; // Fehlerposition berechnen

        if (errorPosition > 0) {
            System.out.println("Fehler erkannt an Position: " + errorPosition);
            received[errorPosition - 1] ^= 1; // Bit korrgieren
        } else {
            System.out.println("Kein Fehler gefunden.");
        }

        return received;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 4 Datenbits einlesen
        System.out.println("Geben Sie 4 Datenbits ein (z.B. 1 0 1 1): ");
        int[] data = new int[4];
        for (int i = 0; i < 4; i++) {
            data[i] = scanner.nextInt();
        }

        // Datenbits kodieren
        int[] encoded = encode(data);
        System.out.println("Kodierte Bits (Hamming(7,4)): ");
        for (int bit : encoded) {
            System.out.print(bit + " ");
        }
        System.out.println();

        // Fehler einfügen (bitflip an Position 3)
        encoded[2] ^= 1;
        System.out.println("Empfangene Bits mit Fehler: ");
        for (int bit : encoded) {
            System.out.print(bit + " ");
        }
        System.out.println();

        // Fehler erkennen und korrigieren
        int[] corrected = decode(encoded);
        System.out.println("Korrigierte Bits: ");
        for (int bit : corrected) {
            System.out.print(bit + " ");
        }
        System.out.println();

        scanner.close();
    }
}
