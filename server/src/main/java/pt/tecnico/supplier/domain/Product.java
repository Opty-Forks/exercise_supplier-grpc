package pt.tecnico.supplier.domain;

/** Product entity. Only the product quantity is mutable so its get/set methods are synchronized. */
public class Product {
  /** Product identifier. */
  private final String productId;
  /** Product description. */
  private final String description;
  /** Available quantity of product. */
  private final int quantity;
  /** Price of product */
  private final int price;
  /** Likes of product */
  private final int likes;

  /** Create a new product */
  public Product(String pid, String description, int quantity, int price, int likes) {
    this.productId = pid;
    this.description = description;
    this.quantity = quantity;
    this.price = price;
    this.likes = likes;
  }

  public String getId() {
    return productId;
  }

  public String getDescription() {
    return description;
  }

  public int getPrice() {
    return price;
  }

  public int getQuantity() {
    return quantity;
  }

  public int getLikes() {
    return likes;
  }

  @Override
  public String toString() {
    return "Product [productId="
        + productId
        + ", description="
        + description
        + ", quantity="
        + quantity
        + ", price="
        + price
        + ", likes="
        + likes
        + "]";
  }
}
