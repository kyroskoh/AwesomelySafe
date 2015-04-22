package tests;

import constants.AuthenticationConstants;
import encryption.EncryptDecryptHelper;
import constants.FilePaths;
import junit.framework.TestCase;
import main.AwesomeFileTransferClient;
import main.AwesomeFileTransferServer;

import javax.crypto.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Created by JiaHao on 19/4/15.
 */
public class TestOfAwesomeness extends TestCase {

    public static final String BIG_FILE_PATH = "src/tests/test_files/testFileBig.txt";
    public static final String SMALL_FILE_PATH = "src/tests/test_files/testFile.txt";

    public static final String BIG_IMAGE_PATH = "src/tests/test_files/bigImage.jpg";

    /**
     * Helper method to get an encrypting cipher for testing
     * @return
     * @throws InvalidKeyException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static Cipher getEncryptCipher() throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IOException {

        return EncryptDecryptHelper.getEncryptCipher(FilePaths.SERVER_PRIVATE_KEY, AuthenticationConstants.ALGORITHM_RSA,0);

    }

    /**
     * Helper method to get a decrypting cipher for testing
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws InvalidKeyException
     */
    public static Cipher getDecryptCipher() throws NoSuchPaddingException, NoSuchAlgorithmException, IOException, InvalidKeyException {


        return EncryptDecryptHelper.getDecryptCipher(FilePaths.SERVER_PUBLIC_KEY, AuthenticationConstants.ALGORITHM_RSA, 1);
    }

    /**
     * Test encryption on small string
     * @throws IOException
     */
    public void testEncryptDecryptString() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {

        String message = "Hello";


        byte[] encryptString = EncryptDecryptHelper.encryptString(message, getEncryptCipher());
        String decryptedMessage = EncryptDecryptHelper.decryptMessage(encryptString, getDecryptCipher());

        assertTrue(message.equals(decryptedMessage));
    }

    /**
     * Test encryption on a small file
     * @throws IOException
     */
    public void testEncryptDecryptSmallBytes() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {

        testEncryptDecryptFile(SMALL_FILE_PATH);

    }

    /**
     * Test encryption on a file size > 117 bytes
     * @throws IOException
     */
    public void testEncryptDecryptBigBytes() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {

        testEncryptDecryptFile(BIG_FILE_PATH);

    }

    public void testEncryptDecryptBigBytesSymmetric() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        File file = new File(BIG_FILE_PATH);

        FileInputStream fileInputStream = new FileInputStream(file);

        byte[] rawBytes = new byte[(int) file.length()];
        fileInputStream.read(rawBytes);

        Key symmetricKey = KeyGenerator.getInstance("DES").generateKey();

        Cipher encryptCipher = EncryptDecryptHelper.getEncryptCipher(symmetricKey, AuthenticationConstants.ALGORITHM_DES);
        byte[] encryptString = encryptCipher.doFinal(rawBytes);


        Cipher decryptCipher = EncryptDecryptHelper.getDecryptCipher(symmetricKey, AuthenticationConstants.ALGORITHM_DES);

        byte[] decryptedBytes = decryptCipher.doFinal(encryptString);

        assertTrue(Arrays.equals(rawBytes, decryptedBytes));



    }

    /**
     * Tests encryption over sockets
     * @throws InterruptedException
     */
    public void testServerClient() throws InterruptedException {

        final byte[][] results = new byte[2][];

        final int port = getRandomPort();

        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TestServer server = new TestServer(port);
                    results[1] = server.getDecryptedBytes();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                }

            }
        });


        Thread clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TestClient client = new TestClient(port);
                    results[0] = client.getRawBytes();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                }
            }
        });

        serverThread.start();
        clientThread.start();

        serverThread.join();
        clientThread.join();

        assertTrue(Arrays.equals(results[0], results[1]));

    }

    /**
     * Helper test method for encryption of a file path
     * @param path
     * @throws IOException
     */
    public static void testEncryptDecryptFile(String path) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        File file = new File(path);

        FileInputStream fileInputStream = new FileInputStream(file);

        byte[] rawBytes = new byte[(int) file.length()];
        fileInputStream.read(rawBytes);

        byte[] encryptString = EncryptDecryptHelper.encryptByte(rawBytes, getEncryptCipher());

        byte[] decryptedBytes = EncryptDecryptHelper.decryptBytes(encryptString, getDecryptCipher());

        assertTrue(Arrays.equals(rawBytes, decryptedBytes));

    }

    /**
     * Tests the file transfer protocol
     * @throws InterruptedException
     */
    public void testFileTransferProtocolCP1() throws InterruptedException {

        final byte[][] results = new byte[2][];

        final int port = getRandomPort();

        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AwesomeFileTransferServer server = new AwesomeFileTransferServer(port, 1);
                    server.start();
                    results[1] = server.getReceivedFile();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                }

            }
        });


        Thread clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AwesomeFileTransferClient client = new AwesomeFileTransferClient(port, BIG_IMAGE_PATH, 1);
                    client.start();
                    results[0] = client.getFileToSend();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                }
            }
        });

        serverThread.start();
        clientThread.start();

        serverThread.join();
        clientThread.join();

        assertTrue(Arrays.equals(results[0], results[1]));

    }

    /**
     * Tests the file transfer protocol
     * @throws InterruptedException
     */
    public void testFileTransferProtocolCP2() throws InterruptedException {


        final byte[][] results = new byte[2][];

        final int port = getRandomPort();

        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AwesomeFileTransferServer server = new AwesomeFileTransferServer(port, 2);
                    server.start();
                    results[1] = server.getReceivedFile();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                }

            }
        });


        Thread clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AwesomeFileTransferClient client = new AwesomeFileTransferClient(port, BIG_IMAGE_PATH, 2);
                    client.start();
                    results[0] = client.getFileToSend();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                }
            }
        });

        serverThread.start();

        clientThread.start();

        serverThread.join();
        clientThread.join();

        assertTrue(Arrays.equals(results[0], results[1]));

    }

    private int getRandomPort() {

        return 4000 + (int)(Math.random()*6000);
    }

}
