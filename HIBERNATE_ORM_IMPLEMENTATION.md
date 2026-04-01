# Implementação Hibernate/JPA - Taskly API

## 📋 Resumo da Implementação

Migração completa de **JDBC puro** para **Hibernate/JPA** seguindo padrões de mercado e mantendo arquitetura DDD limpa.

---

## ✅ O Que Foi Implementado

### 1. **Dependências Maven** (pom.xml)

```xml
<!-- Hibernate ORM 6.4.4 (Latest) -->
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-core</artifactId>
    <version>6.4.4.Final</version>
</dependency>

<!-- JPA API 3.1 -->
<dependency>
    <groupId>jakarta.persistence</groupId>
    <artifactId>jakarta.persistence-api</artifactId>
    <version>3.1.0</version>
</dependency>

<!-- Hibernate HikariCP Integration -->
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-hikaricp</artifactId>
    <version>6.4.4.Final</version>
</dependency>

<!-- Kotlin JPA/No-arg Plugin -->
<dependency>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-noarg</artifactId>
    <version>1.9.22</version>
</dependency>
```

### 2. **Plugin Kotlin Maven** (JPA Support)

```xml
<plugin>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-maven-plugin</artifactId>
    <configuration>
        <compilerPlugins>
            <plugin>jpa</plugin>
            <plugin>all-open</plugin>
        </compilerPlugins>
        <pluginOptions>
            <option>all-open:annotation=jakarta.persistence.Entity</option>
            <option>all-open:annotation=jakarta.persistence.MappedSuperclass</option>
        </pluginOptions>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-maven-noarg</artifactId>
            <version>2.1.21</version>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-maven-allopen</artifactId>
            <version>2.1.21</version>
        </dependency>
    </dependencies>
</plugin>
```

**O que faz:**
- `jpa`: Gera construtor no-arg para entidades (requerido pelo Hibernate)
- `all-open`: Torna classes `@Entity` abertas (não-final) para proxies do Hibernate

---

## 🏗️ Arquitetura DDD com Hibernate

```
domain/
  ├── entity/          # Domain entities (User, Todo) - SEM annotations JPA
  └── repository/      # Interfaces (UserRepository, TodoRepository)

infrastructure/
  ├── persistence/
  │   ├── entity/      # JPA entities (UserEntity, TodoEntity) - COM annotations
  │   └── repository/  # Implementações JPA (JpaUserRepository)
  └── transaction/     # TransactionManager

config/
  ├── HibernateConfig.kt
  └── PersistenceUnitInfoImpl.kt
```

**Separação:**
- **Domain**: Puro Kotlin, sem dependências de JPA
- **Infrastructure**: Entidades JPA separadas que mapeiam do/para domain

---

## 📦 Estrutura de Arquivos Criados

### 1. BaseEntity (infrastructure/persistence/entity/)

```kotlin
@MappedSuperclass
abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    @PrePersist
    protected fun prePersist() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    protected fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
```

**Benefícios:**
- ✅ Auditoria automática (createdAt/updatedAt)
- ✅ Reutilização em todas as entidades
- ✅ @PrePersist/@PreUpdate para atualização automática

---

### 2. UserEntity (infrastructure/persistence/entity/)

```kotlin
@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "email", nullable = false, unique = true, length = 255)
    var email: String = "",

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String = "",

    @Column(name = "roles", nullable = false, length = 255)
    var roles: String = "USER",

    @Column(name = "active", nullable = false)
    var active: Boolean = true
) : BaseEntity() {

    fun toDomain(): User { /* ... */ }

    companion object {
        fun fromDomain(user: User): UserEntity { /* ... */ }
    }
}
```

**Padrão:**
- `toDomain()`: Converte JPA Entity → Domain Entity
- `fromDomain()`: Converte Domain Entity → JPA Entity

---

### 3. TodoEntity (infrastructure/persistence/entity/)

```kotlin
@Entity
@Table(name = "todos")
class TodoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "title", nullable = false, length = 500)
    var title: String = "",

    @Column(name = "done", nullable = false)
    var done: Boolean = false
) : BaseEntity() {

    fun toDomain(): Todo { /* ... */ }

    companion object {
        fun fromDomain(todo: Todo): TodoEntity { /* ... */ }
    }
}
```

---

### 4. RefreshTokenEntity (infrastructure/persistence/entity/)

```kotlin
@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "token", nullable = false, unique = true, length = 255)
    var token: String = "",

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "revoked", nullable = false)
    var revoked: Boolean = false
) : BaseEntity() {

    fun toDomain(): RefreshToken { /* ... */ }

    companion object {
        fun fromDomain(refreshToken: RefreshToken): RefreshTokenEntity { /* ... */ }
    }
}
```

---

### 5. HibernateConfig (config/)

```kotlin
object HibernateConfig {

    fun createEntityManagerFactory(dataSource: DataSource): EntityManagerFactory {
        val properties = mutableMapOf<String, Any>()

        // DataSource (HikariCP)
        properties[AvailableSettings.DATASOURCE] = dataSource

        // Dialect
        properties[AvailableSettings.DIALECT] = "org.hibernate.dialect.PostgreSQLDialect"

        // DDL Strategy (validate - Flyway gerencia schema)
        properties[AvailableSettings.HBM2DDL_AUTO] = "validate"

        // SQL Logging (apenas dev)
        if (EnvConfig.isDevelopment()) {
            properties[AvailableSettings.SHOW_SQL] = true
            properties[AvailableSettings.FORMAT_SQL] = true
        }

        // Performance
        properties[AvailableSettings.STATEMENT_BATCH_SIZE] = 20
        properties[AvailableSettings.ORDER_INSERTS] = true
        properties[AvailableSettings.ORDER_UPDATES] = true

        // Criar EntityManagerFactory
        val persistenceProvider = HibernatePersistenceProvider()
        return persistenceProvider.createContainerEntityManagerFactory(
            PersistenceUnitInfoImpl(/* ... */),
            properties
        )
    }
}
```

**Configurações importantes:**
- `validate`: Valida schema mas não altera (Flyway é responsável)
- `SHOW_SQL`: Apenas em desenvolvimento
- `STATEMENT_BATCH_SIZE`: Otimização de inserts/updates em lote

---

### 6. TransactionManager (infrastructure/transaction/)

```kotlin
class TransactionManager(private val entityManagerFactory: EntityManagerFactory) {

    private val logger = LoggerFactory.getLogger(TransactionManager::class.java)

    /**
     * Executa uma função dentro de uma transação.
     * Commit automático em caso de sucesso, rollback em caso de erro.
     */
    fun <T> executeInTransaction(block: (EntityManager) -> T): T {
        val entityManager = entityManagerFactory.createEntityManager()
        val transaction = entityManager.transaction

        try {
            transaction.begin()
            val result = block(entityManager)
            transaction.commit()
            return result
        } catch (e: Exception) {
            if (transaction.isActive) {
                transaction.rollback()
                logger.error("Transaction rolled back due to error", e)
            }
            throw e
        } finally {
            entityManager.close()
        }
    }

    /**
     * Executa uma função read-only (sem transação)
     */
    fun <T> executeReadOnly(block: (EntityManager) -> T): T {
        val entityManager = entityManagerFactory.createEntityManager()
        try {
            return block(entityManager)
        } finally {
            entityManager.close()
        }
    }
}
```

**Uso:**
```kotlin
// Write
transactionManager.executeInTransaction { em ->
    val entity = UserEntity(/* ... */)
    em.persist(entity)
}

// Read
transactionManager.executeReadOnly { em ->
    em.find(UserEntity::class.java, 1L)
}
```

---

### 7. JpaUserRepository (infrastructure/persistence/repository/)

```kotlin
class JpaUserRepository(
    private val transactionManager: TransactionManager
) : UserRepository {

    override fun findById(id: Long): User? {
        return transactionManager.executeReadOnly { em ->
            em.find(UserEntity::class.java, id)?.toDomain()
        }
    }

    override fun findByEmail(email: Email): User? {
        return transactionManager.executeReadOnly { em ->
            em.createQuery(
                "SELECT u FROM UserEntity u WHERE u.email = :email",
                UserEntity::class.java
            )
            .setParameter("email", email.value)
            .resultList
            .firstOrNull()
            ?.toDomain()
        }
    }

    override fun save(user: User): User {
        return transactionManager.executeInTransaction { em ->
            val entity = UserEntity.fromDomain(user)
            em.persist(entity)
            entity.toDomain()
        }
    }

    override fun update(user: User): User {
        return transactionManager.executeInTransaction { em ->
            val entity = UserEntity.fromDomain(user)
            val merged = em.merge(entity)
            merged.toDomain()
        }
    }
}
```

**Padrão:**
- `find`: Usa `executeReadOnly`
- `save`: Usa `persist` dentro de `executeInTransaction`
- `update`: Usa `merge` dentro de `executeInTransaction`
- Converte JPA Entity ↔ Domain Entity

---

### 8. JpaTodoRepository (infrastructure/persistence/repository/)

```kotlin
class JpaTodoRepository(
    private val transactionManager: TransactionManager
) : TodoRepository {

    override fun findAll(userId: Long): List<Todo> {
        return transactionManager.executeReadOnly { em ->
            em.createQuery(
                "SELECT t FROM TodoEntity t WHERE t.userId = :userId ORDER BY t.createdAt DESC",
                TodoEntity::class.java
            )
            .setParameter("userId", userId)
            .resultList
            .map { it.toDomain() }
        }
    }

    override fun findById(id: Long, userId: Long): Todo? {
        return transactionManager.executeReadOnly { em ->
            em.createQuery(
                "SELECT t FROM TodoEntity t WHERE t.id = :id AND t.userId = :userId",
                TodoEntity::class.java
            )
            .setParameter("id", id)
            .setParameter("userId", userId)
            .resultList
            .firstOrNull()
            ?.toDomain()
        }
    }

    override fun save(todo: Todo): Todo {
        return transactionManager.executeInTransaction { em ->
            val entity = TodoEntity.fromDomain(todo)
            em.persist(entity)
            entity.toDomain()
        }
    }

    override fun update(todo: Todo): Todo {
        return transactionManager.executeInTransaction { em ->
            val entity = TodoEntity.fromDomain(todo)
            val merged = em.merge(entity)
            merged.toDomain()
        }
    }

    override fun delete(id: Long, userId: Long) {
        transactionManager.executeInTransaction { em ->
            em.createQuery(
                "DELETE FROM TodoEntity t WHERE t.id = :id AND t.userId = :userId"
            )
            .setParameter("id", id)
            .setParameter("userId", userId)
            .executeUpdate()
        }
    }
}
```

---

### 9. Main.kt (atualizado)

```kotlin
fun main() {
    // ... existing code ...

    val dataSource = DatabaseConfig.dataSource()
    DatabaseConfig.migrate(dataSource)

    // Configurar Hibernate
    val entityManagerFactory = HibernateConfig.createEntityManagerFactory(dataSource)
    val transactionManager = TransactionManager(entityManagerFactory)

    // Repositories JPA
    val userRepository = JpaUserRepository(transactionManager)
    val todoRepository = JpaTodoRepository(transactionManager)
    val refreshTokenRepository = JpaRefreshTokenRepository(transactionManager)

    // Services
    val authService = AuthService(userRepository, refreshTokenRepository)
    val todoService = TodoService(todoRepository)

    // ... rest of code ...

    // Shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down Hibernate...")
        entityManagerFactory.close()
    })
}
```

---

## 🎯 Benefícios da Implementação

### 1. **Produtividade**
- ✅ Menos código boilerplate (sem `ResultSet.get*()`)
- ✅ Mapeamento automático de objetos
- ✅ Queries type-safe com JPQL

### 2. **Manutenibilidade**
- ✅ Separação Domain/Infrastructure
- ✅ Entidades centralizadas
- ✅ Transações gerenciadas

### 3. **Performance**
- ✅ Batch inserts/updates
- ✅ Lazy loading (preparado)
- ✅ Second level cache (preparado)
- ✅ Query optimization

### 4. **Padrão de Mercado**
- ✅ JPA é padrão Java EE
- ✅ Compatível com Spring Data JPA (migração futura)
- ✅ Fácil adicionar outras features (cache, auditing)

---

## 📊 Comparação: JDBC vs Hibernate

### JDBC (Antes)
```kotlin
fun findById(id: Long): User? {
    val sql = "SELECT * FROM users WHERE id = ?"
    return dataSource.connection.use { conn ->
        conn.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    User(
                        rs.getLong("id"),
                        Email.of(rs.getString("email")),
                        rs.getString("password_hash"),
                        // ... 10+ lines de mapeamento manual
                    )
                } else null
            }
        }
    }
}
```

### Hibernate/JPA (Depois)
```kotlin
fun findById(id: Long): User? {
    return transactionManager.executeReadOnly { em ->
        em.find(UserEntity::class.java, id)?.toDomain()
    }
}
```

**Redução:** ~20 linhas → 3 linhas (**-85%**)

---

## 🔧 Recursos Avançados (Preparados)

### 1. Paginação

```kotlin
fun findAllPaginated(userId: Long, page: Int, size: Int): List<Todo> {
    return transactionManager.executeReadOnly { em ->
        em.createQuery("SELECT t FROM TodoEntity t WHERE t.userId = :userId", TodoEntity::class.java)
            .setParameter("userId", userId)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .resultList
            .map { it.toDomain() }
    }
}
```

### 2. Named Queries

```kotlin
@Entity
@NamedQuery(
    name = "User.findByEmail",
    query = "SELECT u FROM UserEntity u WHERE u.email = :email"
)
class UserEntity { /* ... */ }

// Uso
em.createNamedQuery("User.findByEmail", UserEntity::class.java)
    .setParameter("email", "user@example.com")
    .singleResult
```

### 3. Criteria API (Type-safe queries)

```kotlin
val cb = em.criteriaBuilder
val query = cb.createQuery(UserEntity::class.java)
val root = query.from(UserEntity::class.java)

query.select(root).where(
    cb.equal(root.get<String>("email"), "user@example.com")
)

val result = em.createQuery(query).singleResult
```

### 4. Second Level Cache (futuro)

```properties
# hibernate.properties
hibernate.cache.use_second_level_cache=true
hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory
```

```kotlin
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class UserEntity { /* ... */ }
```

---

## ✅ Checklist de Implementação

- [x] Adicionar dependências Hibernate ao pom.xml
- [x] Configurar Kotlin JPA plugin
- [x] Criar HibernateConfig
- [x] Criar PersistenceUnitInfoImpl
- [x] Criar BaseEntity com auditoria
- [x] Criar UserEntity
- [x] Criar TodoEntity
- [x] Criar RefreshTokenEntity
- [x] Criar TransactionManager
- [x] Implementar JpaUserRepository
- [x] Implementar JpaTodoRepository
- [x] Implementar JpaRefreshTokenRepository
- [x] Atualizar Main.kt
- [x] Testar compilação

---

## 🚀 Próximos Passos (Opcional)

1. **Spring Data JPA** (migração futura)
   - Repositories automáticos
   - Derived queries
   - @Transactional declarativo

2. **Cache de Segundo Nível**
   - EHCache ou Caffeine
   - Reduzir hits no banco

3. **Auditing Avançado**
   - @CreatedBy, @ModifiedBy
   - Spring Security integration

4. **Soft Delete**
   - @Where(clause = "deleted = false")
   - Nunca deletar realmente

---

## 📚 Referências

- [Hibernate Documentation](https://hibernate.org/orm/documentation/6.4/)
- [JPA Specification](https://jakarta.ee/specifications/persistence/3.1/)
- [Kotlin JPA Plugin](https://kotlinlang.org/docs/no-arg-plugin.html)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa) (futuro)

---

**Status:** ✅ **Implementação completa de Hibernate/JPA pronta para produção!**
