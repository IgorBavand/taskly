package app.config

import jakarta.persistence.EntityManagerFactory
import org.hibernate.cfg.AvailableSettings
import org.hibernate.jpa.HibernatePersistenceProvider
import org.slf4j.LoggerFactory
import javax.sql.DataSource

/**
 * Configuração do Hibernate/JPA com EntityManagerFactory.
 *
 * Utiliza HikariCP para pool de conexões e configuração programática
 * ao invés de persistence.xml (mais flexível e compatível com diferentes ambientes).
 */
object HibernateConfig {

    private val logger = LoggerFactory.getLogger(HibernateConfig::class.java)

    /**
     * Cria e configura o EntityManagerFactory com Hibernate
     */
    fun createEntityManagerFactory(dataSource: DataSource): EntityManagerFactory {
        logger.info("Configurando Hibernate EntityManagerFactory...")

        val properties = mutableMapOf<String, Any>()

        // ========================================================================
        // DataSource (HikariCP já configurado)
        // ========================================================================
        properties[AvailableSettings.DATASOURCE] = dataSource

        // ========================================================================
        // Dialect
        // ========================================================================
        properties[AvailableSettings.DIALECT] = "org.hibernate.dialect.PostgreSQLDialect"

        // ========================================================================
        // DDL Strategy (VALIDATE - Flyway gerencia schema)
        // ========================================================================
        // - validate: valida schema mas não altera (recomendado com Flyway)
        // - update: atualiza schema automaticamente (não usar em produção)
        // - create: cria schema do zero (apaga dados!)
        // - create-drop: cria e dropa ao fechar (apenas para testes)
        properties[AvailableSettings.HBM2DDL_AUTO] = "validate"

        // ========================================================================
        // SQL Logging (apenas em desenvolvimento)
        // ========================================================================
        if (EnvConfig.isDevelopment()) {
            properties[AvailableSettings.SHOW_SQL] = true
            properties[AvailableSettings.FORMAT_SQL] = true
            properties[AvailableSettings.HIGHLIGHT_SQL] = true
            properties[AvailableSettings.USE_SQL_COMMENTS] = true
        }

        // ========================================================================
        // Logging
        // ========================================================================
        properties[AvailableSettings.LOG_SLOW_QUERY] = 1000L // Log queries > 1s

        // ========================================================================
        // Performance
        // ========================================================================
        properties[AvailableSettings.STATEMENT_BATCH_SIZE] = 20
        properties[AvailableSettings.ORDER_INSERTS] = true
        properties[AvailableSettings.ORDER_UPDATES] = true
        properties[AvailableSettings.BATCH_VERSIONED_DATA] = true

        // ========================================================================
        // Second Level Cache (preparado para futuro)
        // ========================================================================
        properties[AvailableSettings.USE_SECOND_LEVEL_CACHE] = false
        properties[AvailableSettings.USE_QUERY_CACHE] = false

        // ========================================================================
        // Statistics (útil para monitoring)
        // ========================================================================
        properties[AvailableSettings.GENERATE_STATISTICS] = EnvConfig.isDevelopment()

        // ========================================================================
        // JDBC Settings
        // ========================================================================
        properties[AvailableSettings.STATEMENT_FETCH_SIZE] = 50
        properties[AvailableSettings.USE_GET_GENERATED_KEYS] = true

        // ========================================================================
        // Timezone
        // ========================================================================
        properties[AvailableSettings.JDBC_TIME_ZONE] = "UTC"

        // ========================================================================
        // Criar EntityManagerFactory
        // ========================================================================
        val persistenceProvider = HibernatePersistenceProvider()

        val entityManagerFactory = persistenceProvider.createContainerEntityManagerFactory(
            PersistenceUnitInfoImpl(
                persistenceUnitName = "taskly-persistence-unit",
                managedClassNames = listOf(
                    "app.infrastructure.persistence.entity.UserEntity",
                    "app.infrastructure.persistence.entity.TodoEntity",
                    "app.infrastructure.persistence.entity.RefreshTokenEntity",
                    "app.infrastructure.persistence.entity.FaceValidationTokenEntity"
                ),
                properties = properties
            ),
            properties
        )

        logger.info("✅ Hibernate EntityManagerFactory configurado com sucesso")

        return entityManagerFactory
    }
}
