package you.yuli.swordtiers.tiers;

import javax.net.ssl.X509TrustManager;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Date;

public class TrustManager implements X509TrustManager {
    private static final String EXPECTED_HOSTNAME = "mctiers.com"; // Hostname to validate against
    private static final String EXPECTED_PUBKEY_HASH = "04:BF:2C:7D:40:59:DF:36:A4:68:9D:9F:23:E5:53:95:97:70:6E:24:A3:D4:7A:09:AC:0C:AA:64:BE:A7:EB:08:D1:DC:C7:64:29:AA:0C:4B:31:79:C8:63:12:08:6A:3B:C3:54:ED:16:85:17:0B:7F:DB:D8:93:B5:62:19:DD:53:24";  // Public key pin

    public X509Certificate[] getAcceptedIssuers() {
        return null; // We will handle certificate validation explicitly
    }

    public void checkClientTrusted(X509Certificate[] certs, String authType) {
        // No client authentication needed
    }

    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        try {
            // Perform hostname verification
            verifyHostname(certs[0]);

            // Check expiration date
            checkExpirationDate(certs[0]);

            // Perform public key pinning check
            matchPublicKey(certs[0]);

            System.out.println("certification verified");
        } catch (Exception e) {
            throw new CertificateException("Server certificate verification failed: " + e.getMessage(), e);
        }
    }

    private void matchPublicKey(X509Certificate cert) throws CertificateException {
        try {
            PublicKey serverPubKey = cert.getPublicKey();
            String serverPubKeyHash = getPublicKey(serverPubKey);

            if (!serverPubKeyHash.equals(EXPECTED_PUBKEY_HASH)) {
                throw new CertificateException("Public key mismatch! Expected: " + EXPECTED_PUBKEY_HASH + ", but got: " + serverPubKeyHash);
            }
        } catch (CertificateException e) {
            throw new CertificateException("Match public key failed: " + e.getMessage(), e);
        }
    }

    private void verifyHostname(X509Certificate cert) throws CertificateException {
        String[] subjectAltNames = cert.getSubjectAlternativeNames()
                .stream()
                .filter(item -> item.getFirst().equals(2)) // Type 2 = DNS Name
                .map(item -> (String) item.get(1))
                .toArray(String[]::new);

        boolean hostnameMatch = Arrays.stream(subjectAltNames)
                .anyMatch(dnsName -> dnsName.equalsIgnoreCase(EXPECTED_HOSTNAME));

        if (!hostnameMatch) {
            throw new CertificateException("Hostname verification failed: Expected hostname " + EXPECTED_HOSTNAME + " but got " + Arrays.toString(subjectAltNames));
        }
    }

    private void checkExpirationDate(X509Certificate cert) throws CertificateException {
        Date currentDate = new Date();
        if (cert.getNotAfter().before(currentDate)) {
            throw new CertificateException("Certificate has expired. Expiration date: " + cert.getNotAfter());
        }

        if (cert.getNotBefore().after(currentDate)) {
            throw new CertificateException("Certificate is not yet valid. Valid from: " + cert.getNotBefore());
        }
    }

    // Expected length of the uncompressed EC public key (65 bytes: 0x04 + 32-byte x-coordinate + 32-byte y-coordinate)
    private static final int EXPECTED_PUBLIC_KEY_LENGTH = 65; // Uncompressed EC key

    private static String getPublicKey(PublicKey publicKey) throws IllegalArgumentException {
        // Check if the public key is of type EC (Elliptic Curve)
        if (!(publicKey instanceof ECPublicKey ecPublicKey)) {
            throw new IllegalArgumentException("The public key is not an EC (Elliptic Curve) key.");
        }

        // Extract the encoded (raw) public key from the EC public key
        byte[] publicKeyBytes = ecPublicKey.getEncoded(); // This includes extra metadata like the algorithm ID, etc.

        // The first byte in the encoded format indicates the algorithm, we expect X.509 encoding
        if (publicKeyBytes[0] == 0x30) { // X.509 Encoding: Sequence start
            // Extract the actual EC public key bytes (which will be inside the encoded structure)
            byte[] ecRawKey = getBytes(publicKeyBytes);

            // Convert the raw public key to a hexadecimal string
            return bytesToHex(ecRawKey);
        }

        throw new IllegalArgumentException("Invalid public key encoding format.");
    }

    private static byte[] getBytes(byte[] publicKeyBytes) {
        byte[] ecRawKey = Arrays.copyOfRange(publicKeyBytes, publicKeyBytes.length - EXPECTED_PUBLIC_KEY_LENGTH, publicKeyBytes.length);

        // ValidateJson key length
        if (ecRawKey.length != EXPECTED_PUBLIC_KEY_LENGTH) {
            throw new IllegalArgumentException("Public key length is invalid. Expected length: " + EXPECTED_PUBLIC_KEY_LENGTH + ", but got: " + ecRawKey.length);
        }

        // Check for the 0x04 prefix (uncompressed EC key format)
        if (ecRawKey[0] != 0x04) {
            throw new IllegalArgumentException("Invalid public key format. Expected uncompressed EC key starting with 0x04.");
        }
        return ecRawKey;
    }

    // Convert a byte array to a hex string
    private static String bytesToHex(byte[] bytes) {
        // Allocate space for hex digits + colons (i.e., 2 hex digits per byte + colon between)
        StringBuilder hexString = new StringBuilder(bytes.length * 3 - 1); // 2 digits per byte + 1 colon per byte, minus one for the last byte (no colon needed)

        for (int i = 0; i < bytes.length; i++) {
            // Convert each byte to its 2-digit hexadecimal representation
            if (i < bytes.length - 1) hexString.append(String.format("%02X:", bytes[i]));
            else hexString.append(String.format("%02X", bytes[i]));
        }

        return hexString.toString();
    }
}