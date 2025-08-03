# Hoarder Spring Boot Starter

A lightweight Spring Boot starter that provides automatic caching for JPA entities through AOP (Aspect-Oriented
Programming). Hoarder intercepts repository method calls and caches entities to improve application performance by
reducing database queries.

## Features

- **Automatic Entity Caching**: Preloads and caches entities marked with `@Hoarded` annotation.
- **AOP-Based Interception**: Seamlessly intercepts `findById` and column-based finder methods.
- **Column-Based Lookup**: Supports caching by specific columns (e.g., `findBySymbol`, `findByElement`).
- **Zero Configuration**: Works out of the box with sensible defaults.
- **Configurable**: Flexible configuration options for caching and logging.
- **Thread-Safe**: Uses `ConcurrentHashMap` for safe concurrent access.
- **Installation**: Simple Maven or Gradle setup.
- **Usage**: Easy to integrate with existing Spring Boot applications.

## Installation

### Maven

```xml
<dependency>
    <groupId>com.github.hoarder</groupId>
    <artifactId>hoarder-spring-boot-starter</artifactId>
    <version>0.0.3</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github.hoarder:hoarder-spring-boot-starter:0.0.3'
```

Add this to your `pom.xml` or `build.gradle` file to include the Hoarder Spring Boot Starter in your project.

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/MaheshBabu11/Hoarder</url>
    </repository>
</repositories>
```

## Quick Start

### 1. Mark Your Entity

Annotate your JPA entity with `@Hoarded`:

```java
@Entity
@Table(name = "periodic_table")
@Hoarded
public class Element {
    @Id
    @Column(name = "AtomicNumber")
    private Integer atomicNumber;

    @Column(name = "Element")
    private String element;

    @Column(name = "Symbol")
    private String symbol;

    @Column(name = "Type")
    private String type;

    // getters and setters...
}
```

### 2. Create Your Repository

Create a standard Spring Data JPA repository:

```java
@Repository
public interface ElementRepository extends JpaRepository<Element, Integer> {

    // These methods will be automatically intercepted and cached
    Optional<Element> findBySymbol(String symbol);

    List<Element> findAllByType(String type);

    Optional<Element> findByElement(String elementName);

    // Complex queries still go to database
    Optional<Element> findBySymbolAndType(String symbol, String type);
}
```

### 3. Use the Repository

Use the repository in your service or controller:

```java
@Service
public class ElementService {

    @Autowired
    private ElementRepository elementRepository;

    public Element getElementBySymbol(String symbol) {
        // First call hits database, subsequent calls return from cache
        return elementRepository.findBySymbol(symbol).orElse(null);
    }

    public Element getElementById(Integer id) {
        // Automatically cached by Hoarder
        return elementRepository.findById(id).orElse(null);
    }
}
```

## How it Works

- **Entity Registration**: On application startup, Hoarder scans for entities annotated with `@Hoarded`.
- **Preloading**: All records for hoarded entities are loaded into memory at startup.
- **Method Interception**: AOP aspects intercept repository method calls to cache results.
- **Cache Lookup**: For supported methods, Hoarder first checks the cache.
- **Fallback**: If the entity is not found in the cache, the query proceeds to

## Supported Method Patterns

Hoarder automatically intercepts and caches these repository method patterns:

- `findById(id)` - Primary key lookup
- `findBy{ColumnName}(value)` - Single column lookup
- `findAllBy{ColumnName}(value)` - Multiple records by column

## Method Name to Column Mapping

Hoarder automatically maps method names to database columns:

```java
// Method name → Column name
findBySymbol()     → "Symbol"

findByElement()    → "Element"

findByAtomicMass() → "AtomicMass"

findByType()       → "Type"
```

## Configuration

Add these properties to your `application.yml` or `application.properties`:

### YAML Configuration

```yaml
hoarder:
  cache:
    enabled: true  # Enable/disable caching (default: true)
  logging:
    enabled: true  # Enable/disable logging (default: true)
    level: INFO    # Log level: TRACE, DEBUG, INFO, WARN, ERROR (default: INFO)
```

### Properties Configuration

```properties
hoarder.cache.enabled=true
hoarder.logging.enabled=true
hoarder.logging.level=INFO
```

## Best Practices

- **Use for Reference Data**: Ideal for lookup tables, configuration data, and relatively static entities.
- **Memory Considerations**: Only use `@Hoarded` on entities with manageable data sizes.
- **Cache Warming**: Entities are preloaded on application startup.
- **Monitor Memory Usage**: Large datasets may require additional memory configuration.

## Requirements

- **Java 17+**
- **Spring Boot 3.2.0+**
- **Spring Data JPA**
- **AspectJ** (included via `spring-aspects`)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to
discuss what you would like to change.

## License

This project is licensed under the Apache License 2.0 - see
the [LICENSE](https://github.com/MaheshBabu11/Hoarder/blob/main/LICENSE) file for details

## Author

Mahesh Babu - [maheshbabu11.dev](https://maheshbabu11.dev)