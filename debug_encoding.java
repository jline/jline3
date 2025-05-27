import java.nio.charset.StandardCharsets;

public class debug_encoding {
    public static void main(String[] args) {
        String s = "café";
        byte[] bytes = s.getBytes(StandardCharsets.ISO_8859_1);
        System.out.print("ISO-8859-1 bytes: ");
        for (byte b : bytes) {
            System.out.printf("0x%02X ", b & 0xFF);
        }
        System.out.println();
        System.out.println("Length: " + bytes.length);
        
        // Test decoding
        String decoded = new String(bytes, StandardCharsets.ISO_8859_1);
        System.out.println("Decoded: " + decoded);
        System.out.println("Equals original: " + s.equals(decoded));
    }
}
