package main;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import AwesomeSockets.AwesomeServerSocket;
import constants.AuthenticationConstants;
import constants.FilePaths;
import encryption.EncryptDecryptHelper;
import encryption.NonceHelper;
import encryption.SecurityFileReader;

/**
 * Created by JiaHao on 19/4/15.
 */
public class AwesomeFileTransferServer {

    private final AwesomeServerSocket serverSocket;
    private final Cipher encryptCipher;
    private final Cipher  decryptCipher;

    private final int fileTransferProtocol;

    private final String writeToFilePath;

    private Cipher symmetricCipher;

    private byte[] receivedFile;

    public AwesomeFileTransferServer(int port, String writeToFilePath, int fileTransferProtocol) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        this.serverSocket = new AwesomeServerSocket(port);
        this.encryptCipher = EncryptDecryptHelper.getEncryptCipher(FilePaths.SERVER_PRIVATE_KEY, AuthenticationConstants.ALGORITHM_RSA, 0);

        this.decryptCipher = EncryptDecryptHelper.getDecryptCipher(FilePaths.SERVER_PRIVATE_KEY, AuthenticationConstants.ALGORITHM_RSA, 0);

        this.fileTransferProtocol = fileTransferProtocol;

        this.writeToFilePath = writeToFilePath;
    }

    public void start() throws IOException {

        try {
            this.serverSocket.acceptClient();
            authenticationProtocol();
            confidentialityProtocol();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

        this.serverSocket.closeServer();
    }

    public void authenticationProtocol() throws IOException, IllegalAccessException {
        System.out.println("=== AUTHENTICATION PROTOCOL ===");

        waitForClientToSayHello();
        waitForClientToAskForCertificate();
    }

    public void confidentialityProtocol() throws BadPaddingException, IllegalBlockSizeException, IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        System.out.println("=== CONFIDENTIALITY PROTOCOL ===");

        if (fileTransferProtocol == 1) {
            waitForClientToSendFileRSA();

        } else {

            waitForClientToSendSymmetricKey();
            waitForClientToSendFile();

        }

        this.writeBytesToFile(this.receivedFile);

        System.out.println("CP Ended");
    }


    private void waitForClientToSayHello() throws IOException, IllegalAccessException {
        System.out.println("Waiting for client to say hello...");
        String errorMessage = "Client sent unknown hello message";

//        messageToSend = privateKey(nonce + serverReplyToHello)
        dealWithMessage(AuthenticationConstants.CLIENT_HELLO_MESSAGE, AuthenticationConstants.SERVER_REPLY_TO_HELLO.getBytes(), errorMessage, true);

    }

    private void waitForClientToAskForCertificate() throws IOException, IllegalAccessException {
        System.out.println("Waiting for client to ask for certificate...");
        // wait for client to ask for certificate

        String errorMessage = "Client sent unknown ask for certificate message";
        byte[] serverCert = SecurityFileReader.readFileIntoByteArray(FilePaths.SERVER_CERTIFICATE);

//        messageToSend = privateKey(nonce) + serverCert
        dealWithMessage(AuthenticationConstants.CLIENT_ASK_FOR_CERT, serverCert, errorMessage, false);

    }

    /**
     * Helper method to avoid repeating code
     *
     * If encryptNonceResponseOnly, messageToSend = privateKey(receivedNonce + replyMessage)
     * else: messageToSend = privateKey(receivedNonce) + replyMessage
     *
     * @param stringCheck Compare received message with expected message
     * @param replyMessage is always unencrypted
     * @param errorMessage exception to throw if it fails
     * @param encryptNonceResponseOnly
     * @throws IllegalAccessException
     * @throws IOException
     */
    private void dealWithMessage(String stringCheck, byte[] replyMessage, String errorMessage, boolean encryptNonceResponseOnly) throws IllegalAccessException, IOException {

        // Gets the message from the client
        byte[] receivedMessagewithNonce = this.serverSocket.readByteArrayForClient(0);

        // split it into nonce (nonce received is always unencrypted)
        byte[][] splitMessage = ByteArrayHelper.splitMessage(receivedMessagewithNonce, NonceHelper.NONCE_LENGTH);

        byte[] receivedNonce = splitMessage[0];
        String messageString = new String(splitMessage[1]);

        // do a check to see if the message is expected
        if (!messageString.equals(stringCheck)) {
            throw new IllegalAccessException(errorMessage);
        }


        byte[] messageToSend;
        if (encryptNonceResponseOnly) {

            // messageToSend = privateKey(receivedNonce + replyMessage)

            byte[] nonceWithReplyMessage = ByteArrayHelper.concatenateBytes(receivedNonce, replyMessage);
            messageToSend = EncryptDecryptHelper.encryptByte(nonceWithReplyMessage, this.encryptCipher);
        } else {

            // messageToSend = privateKey(receivedNonce) + replyMessage

            byte[] encryptedNonce = EncryptDecryptHelper.encryptByte(receivedNonce, this.encryptCipher);
            messageToSend = ByteArrayHelper.concatenateBytes(encryptedNonce, replyMessage);
        }

        serverSocket.sendByteArrayForClient(0, messageToSend);

    }

    private void waitForClientToSendSymmetricKey() throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {

        System.out.println("Waiting for client to send symmetric key...");
        // wait for client to sent symmetric key
        byte[] receivedEncryptedSymmetricKey = this.serverSocket.readByteArrayForClient(0);
        System.out.println("encryptLength: " + receivedEncryptedSymmetricKey.length);
        System.out.println(Arrays.toString(receivedEncryptedSymmetricKey));

        byte[] receivedDecryptedSymmetricKey = EncryptDecryptHelper.decryptBytes(receivedEncryptedSymmetricKey, this.decryptCipher);
        System.out.println("decryptLength: " + receivedDecryptedSymmetricKey.length);
        System.out.println(Arrays.toString(receivedDecryptedSymmetricKey));

        Key symmetricKey = new SecretKeySpec(receivedDecryptedSymmetricKey, 0, receivedDecryptedSymmetricKey.length, SecurityFileReader.AES_KEY);

//        this.symmetricCipher.init(Cipher.DECRYPT_MODE, key2);

        this.symmetricCipher = EncryptDecryptHelper.getDecryptCipher(symmetricKey, AuthenticationConstants.ALGORITHM_AES);
    }

    private void waitForClientToSendFile() throws IOException, BadPaddingException, IllegalBlockSizeException {
    	System.out.println("Waiting for client to send file...");
    	byte [] finalRawData =  this.serverSocket.readByteArrayForClient(0);

        System.out.println("File received, decrpyting...");

    	byte [] finalDecryptedData = symmetricCipher.doFinal(finalRawData);


        this.receivedFile = finalDecryptedData;

        System.out.println("File decrypted.");


    }
    private void waitForClientToSendFileRSA() throws IOException, BadPaddingException, IllegalBlockSizeException {
        System.out.println("Waiting for client to send file...");
        byte [] finalRawData =  this.serverSocket.readByteArrayForClient(0);
        System.out.println("File received, decrpyting...");
        byte [] finalDecryptedData = EncryptDecryptHelper.decryptBytes(finalRawData, this.decryptCipher);


        this.receivedFile = finalDecryptedData;
        System.out.println("File decrypted.");


    }

    private void writeBytesToFile(byte[] writeToFileBytes) throws IOException {

//        FileOutputStream out = new FileOutputStream("received_file");
//        out.write(writeToFileBytes);
//        out.close();

        System.out.println("writing to file");
        FileOutputStream stream = new FileOutputStream(this.writeToFilePath);
        try {
            stream.write(writeToFileBytes);
        } finally {
            stream.close();
        }
        System.out.println("writing done");
    }

    public byte[] getReceivedFile() {
        return receivedFile;
    }

    public static void main(String[] args) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        AwesomeFileTransferServer server = new AwesomeFileTransferServer(AuthenticationConstants.PORT, "src/received_file", 1);
        server.start();

//        System.out.println("Received file: " + Arrays.toString(server.getReceivedFile()));
    }

}
