package pt.tecnico.supplier.client;

import static javax.xml.bind.DatatypeConverter.printHexBinary;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import pt.tecnico.supplier.contract.ProductsRequest;
import pt.tecnico.supplier.contract.ProductsResponse;
import pt.tecnico.supplier.contract.SignedResponse;
import pt.tecnico.supplier.contract.SupplierGrpc;

public class SupplierClient {

  /**
   * Set flag to true to print debug messages.
   * The flag can be set using the -Ddebug command line option.
   */
  private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

  /** Digest algorithm. */
  private static final String DIGEST_ALG = "SHA-256";

  /** Symmetric cipher: combination of algorithm, block processing, and padding. */
  private static final String SYM_CIPHER = "AES/ECB/PKCS5Padding";

  /** Helper method to print debug messages. */
  private static void debug(String debugMessage) {
    if (DEBUG_FLAG) System.err.println(debugMessage);
  }

  public static Key readKey(String resourcePath) throws Exception {
    System.out.println("Reading key from resource " + resourcePath + " ...");

    InputStream fis =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    assert fis != null;
    debug("Bytes: " + fis.available());

    byte[] encoded = new byte[fis.available()];
    fis.read(encoded);
    fis.close();
    debug("Key: " + printHexBinary(encoded));

    return new SecretKeySpec(encoded, "AES");
  }

  public static void main(String[] args) throws Exception {
    System.out.println(SupplierClient.class.getSimpleName() + " starting ...");
    Key key = readKey("secret.key");
    debug("Algorithm: " + key.getAlgorithm());

    // Receive and print arguments.
    System.out.printf("Received %d arguments%n", args.length);
    for (int i = 0; i < args.length; i++) {
      System.out.printf("arg[%d] = %s%n", i, args[i]);
    }

    // Check arguments.
    if (args.length < 2) {
      System.err.println("Argument(s) missing!");
      System.err.printf("Usage: java %s host port%n", SupplierClient.class.getName());
      return;
    }

    final String host = args[0];
    final int port = Integer.parseInt(args[1]);
    final String target = host + ":" + port;

    // Channel is the abstraction to connect to a service end-point.
    final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

    // Create a blocking stub for making synchronous remote calls.
    SupplierGrpc.SupplierBlockingStub stub = SupplierGrpc.newBlockingStub(channel);

    // Prepare request.
    ProductsRequest request = ProductsRequest.newBuilder().build();
    System.out.println("Request to send:");
    System.out.println(request);
    debug("in binary hexadecimals:");
    byte[] requestBinary = request.toByteArray();
    debug(printHexBinary(requestBinary));
    debug(String.format("%d bytes%n", requestBinary.length));

    for (int i = 0; i < 10; i++) {
      // Make the call using the stub.
      System.out.println("Remote call...");
      SignedResponse signedResponse = stub.listProducts(request);
      ProductsResponse response = signedResponse.getResponse();

      // Print response.
      System.out.println("Received response:");
      System.out.println(response);
      debug("in binary hexadecimals:");
      byte[] responseBinary = response.toByteArray();
      debug(printHexBinary(responseBinary));
      debug(String.format("%d bytes%n", responseBinary.length));

      if (Math.abs(signedResponse.getSignature().getTs() - (new Date()).getTime()) > 100) {
        System.out.println("Signature is invalid! Message is not Fresh! :(");
      } else if (isMsgAuth(signedResponse.getSignature().getValue(), responseBinary, key)) {
        System.out.println("Signature is valid! Message accepted! :)");
      } else {
        System.out.println("Signature is invalid! Message rejected! :(");
      }
    }
    // A Channel should be shutdown before stopping the process.
    channel.shutdownNow();
  }

  private static boolean isMsgAuth(ByteString cipherDigest, byte[] bytes, Key key)
      throws Exception {

    MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALG);

    messageDigest.update(bytes);
    byte[] digest = messageDigest.digest();
    debug("New digest: " + printHexBinary(digest));

    Cipher cipher = Cipher.getInstance(SYM_CIPHER);
    cipher.init(Cipher.DECRYPT_MODE, key);
    byte[] decipheredDigest = cipher.doFinal(cipherDigest.toByteArray());
    debug("Deciphered digest: " + printHexBinary(decipheredDigest));

    return Arrays.equals(digest, decipheredDigest);
  }
}
