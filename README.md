# gRPC Supplier

This is a gRPC application, composed of three modules:
- [contract](contract) - protocol buffers definition
- [server](server) - implementation of service
- [client](client) - invocation of service

See the README for each module.
Start at [contract](contract/README.md), then go to [server](server/README.md), and finally go to
the [client](client/README.md).

The example can be extended with additional data in the messages and with security protections.

---

# Functionality

- **Functional** variant: Add the likes field to the product, which is a positive integer (uint32). Represents the
  number of likes (social media style).
- **Security** variant: To ensure the freshness, add a timestamp field of type int64 to the signature. The server must
  fill in the field with the current time. The client must verify that its current time does not differ from the
  signature timestamp by more than *X* milliseconds (backwards and forwards; *X*=100).

# To Run:

- In the [contract](contract) folder: `mvn install`;
- In the [server](server) folder: `mvn compile exec:java -Ddebug`;
- In the [client](client) folder: `mvn compile exec:java -Ddebug`.

---

## Authors

**Group T18**

- 86923 [Sara Machado](mailto:sara.f.machado@tecnico.ulisboa.pt)
- 90770 [Rafael Figueiredo](mailto:rafael.alexandre.roberto.figueiredo@tecnico.ulisboa.pt)
- 90774 [Ricardo Grade](mailto:ricardo.grade@tecnico.ulisboa.pt)

---

[SD Faculty](mailto:leic-sod@disciplinas.tecnico.ulisboa.pt)
