package tests;

import AwesomeSockets.AwesomeClientSocket;
import constants.AuthenticationConstants;
import encryption.EncryptDecryptHelper;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by JiaHao on 20/4/15.
 */
public class TestClient {

    private byte[] rawBytes;

    public TestClient(int port) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {

        AwesomeClientSocket awesomeClientSocket = new AwesomeClientSocket(AuthenticationConstants.SERVER_IP, port);




        File file = new File(TestOfAwesomeness.BIG_FILE_PATH);

        FileInputStream fileInputStream = new FileInputStream(file);

        this.rawBytes = new byte[(int) file.length()];
        fileInputStream.read(rawBytes);

//        System.out.println(Arrays.toString(rawBytes));
        byte[] encryptString = EncryptDecryptHelper.encryptByte(rawBytes, TestOfAwesomeness.getEncryptCipher());

//        System.out.println(encryptString);

        awesomeClientSocket.sendByteArray(encryptString);

    }

    public byte[] getRawBytes() {
        return rawBytes;
    }

    public static void main(String[] args) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {

        TestClient client = new TestClient(AuthenticationConstants.PORT);
    }
}
