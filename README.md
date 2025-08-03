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
    <version>0.0.4</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github.hoarder:hoarder-spring-boot-starter:0.0.4'
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

## Advanced Configuration

### Selective Column Caching with @HoardedColumn

By default, Hoarder caches entities by their primary key. To enable caching by specific columns, use the
`@HoardedColumn` annotation on fields you want to cache:

```java

@Entity
@Hoarded
public class Element {
    @Id
    private Long atomicNumber;

    @HoardedColumn
    private String element;

    @HoardedColumn
    private String symbol;

    private String discoverer; // Not cached by column

    // ... other fields and methods
}
```

## How it Works

- **Entity Registration**: On application startup, Hoarder scans for entities annotated with `@Hoarded`.
- **Preloading**: All records for hoarded entities are loaded into memory at startup.
- **Method Interception**: AOP aspects intercept repository method calls to cache results.
- **Cache Lookup**: For supported methods, Hoarder first checks the cache.
- **Fallback**: If the entity is not found in the cache, the query proceeds to

## How column caching works

When you annotate fields with `@HoardedColumn`, Hoarder will:

- Cache by Primary Key: Always enabled for `@Hoarded` entities.
- Cache by Annotated Columns: Creates additional cache entries for fields marked with `@HoardedColumn`.
- Intercept Repository Methods: Automatically handles `findBy*` and `findAllBy*` methods for cached columns.

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

## Cache Refresh

Hoarder supports automatic cache refresh to keep cached entities synchronized with the database. This is useful for
scenarios where data might be modified outside your application.

### Configuration

```yaml
hoarder:
  cache:
    enabled: true
    refresh:
      enabled: true           # Enable automatic cache refresh
      intervalMinutes: 60     # Refresh every 60 minutes
      delayMinutes: 60        # Initial delay before first refresh
```

```properties
hoarder.cache.refresh.enabled=true
hoarder.cache.refresh.intervalMinutes=60
hoarder.cache.refresh.delayMinutes=60
```

### How it Works

When cache refresh is enabled:

- **Scheduled Execution**: A background scheduler runs at the configured interval.
- **Entity Detection**: Only entities annotated with `@Hoarded` and currently cached are refreshed.
- **Atomic Refresh**: For each entity type, the cache is cleared and reloaded with fresh data from the database.
- **Error Handling**: Refresh failures are logged but do not affect application functionality.

### Manual Cache Management

You can also manually manage the cache using the HoarderCache bean:

```java

@Autowired
private HoarderCache hoarderCache;

// Clear all cached entities
hoarderCache.clear();

// Clear cache for a specific entity
hoarderCache.clearForEntity(Element .class);

// Check cache status
hoarderCache.printCacheStatus();

// View detailed cache contents
hoarderCache.printCacheDetails();
```

## Cache Analysis

Hoarder provides built-in cache analysis capabilities to monitor cache performance and memory usage.

### Cache Size Information

You can get detailed information about cache sizes using the HoarderCache component:

```java

@Autowired
private HoarderCache hoarderCache;

// Get cache size 
hoarderCache.getCacheSize();
```

### Example Output

When you call `hoarderCache.printCacheDetails()`, it will output something like this:

```plaintext
========== Cache Size Analysis ==========
Entity: Element
Primary Key Cache: 118 entries
Column Caches:
- Symbol: 118 entries
- Element: 118 entries
- Type: 118 entries
Total Cache Entries: 472
Estimated Memory Usage: ~47.2 KB

Entity: User
Primary Key Cache: 1,543 entries
Column Caches:
- username: 1,543 entries
- email: 1,543 entries
Total Cache Entries: 4,629
Estimated Memory Usage: ~462.9 KB

========================================
Total Entities Cached: 2
Total Cache Entries: 5,101
Total Estimated Memory Usage: ~510.1 KB
========================================
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