package pt.tecnico.supplier;

import com.google.protobuf.ByteString;
import com.google.type.Money;
import io.grpc.stub.StreamObserver;
import pt.tecnico.supplier.domain.Supplier;
import pt.tecnico.supplier.contract.*;

import static javax.xml.bind.DatatypeConverter.printHexBinary;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Date;

public class SupplierServiceImpl extends SupplierGrpc.SupplierImplBase {

  /**
   * Set flag to true to print debug messages.
   * The flag can be set using the -Ddebug command line option.
   */
  private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

  /** Helper method to print debug messages. */
  private static void debug(String debugMessage) {
    if (DEBUG_FLAG) System.err.println(debugMessage);
  }

  /** Domain object. */
  private final Supplier supplier = Supplier.getInstance();

  /** Key. */
  private final Key key;

  /** Digest algorithm. */
  private static final String DIGEST_ALG = "SHA-256";

  /** Symmetric cipher: combination of algorithm, block processing, and padding. */
  private static final String SYM_CIPHER = "AES/ECB/PKCS5Padding";

  /** Constructor */
  public SupplierServiceImpl() throws Exception {
    debug("Loading demo data...");
    key = readKey("secret.key");
    debug("Algorithm: " + key.getAlgorithm());
    supplier.demoData();
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

  /** Helper method to convert domain product to message product. */
  private Product buildProductFromProduct(pt.tecnico.supplier.domain.Product p) {
    Product.Builder productBuilder = Product.newBuilder();
    productBuilder.setIdentifier(p.getId());
    productBuilder.setDescription(p.getDescription());
    productBuilder.setQuantity(p.getQuantity());
    productBuilder.setLikes(p.getLikes());

    Money.Builder moneyBuilder = Money.newBuilder();
    moneyBuilder.setCurrencyCode("EUR").setUnits(p.getPrice());
    productBuilder.setPrice(moneyBuilder.build());

    return productBuilder.build();
  }

  @Override
  public void listProducts(
      ProductsRequest request, StreamObserver<SignedResponse> responseObserver) {
    debug("listProducts called");

    debug("Received request:");
    debug(request.toString());
    debug("in binary hexadecimals:");
    byte[] requestBinary = request.toByteArray();
    debug(String.format("%d bytes%n", requestBinary.length));

    // build response
    ProductsResponse.Builder responseBuilder = ProductsResponse.newBuilder();
    responseBuilder.setSupplierIdentifier(supplier.getId());
    for (String pid : supplier.getProductsIds()) {
      pt.tecnico.supplier.domain.Product p = supplier.getProduct(pid);
      Product product = buildProductFromProduct(p);
      responseBuilder.addProduct(product);
    }
    ProductsResponse response = responseBuilder.build();

    debug("Response to send:");
    debug(response.toString());
    debug("in binary hexadecimals:");
    byte[] responseBinary = response.toByteArray();
    debug(printHexBinary(responseBinary));
    debug(String.format("%d bytes%n", responseBinary.length));

    Signature.Builder sig = Signature.newBuilder();
    try {
      sig.setSignerId(supplier.getId()).setValue(getSignerIdBytes(responseBinary));
    } catch (Exception e) {
      System.err.println("getSignerIdBytes Failed!");
    }
    sig.setTs((new Date()).getTime());
    // send single response back
    SignedResponse sigResponse =
        SignedResponse.newBuilder().setSignature(sig.build()).setResponse(response).build();

    responseObserver.onNext(sigResponse);
    // complete call
    responseObserver.onCompleted();
  }

  private ByteString getSignerIdBytes(byte[] bytes) throws Exception {

    MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALG);
    messageDigest.update(bytes);
    byte[] digest = messageDigest.digest();
    debug("New digest: " + printHexBinary(digest));

    Cipher cipher = Cipher.getInstance(SYM_CIPHER);
    cipher.init(Cipher.ENCRYPT_MODE, key);
    return ByteString.copyFrom(cipher.doFinal(digest));
  }
}
