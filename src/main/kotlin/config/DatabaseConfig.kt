package app.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import javax.sql.DataSource

/**
 * Configuração do pool de conexões do banco de dados.
 * Utiliza HikariCP para gerenciamento eficiente de conexões.
 */
object DatabaseConfig {

    private val logger = LoggerFactory.getLogger(DatabaseConfig::class.java)

    fun dataSource(): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl = EnvConfig.DATABASE_URL
            username = EnvConfig.DATABASE_USER
            password = EnvConfig.DATABASE_PASSWORD
            driverClassName = "org.postgresql.Driver"

            // Pool settings
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 30000 // 30 segundos
            idleTimeout = 600000 // 10 minutos
            maxLifetime = 1800000 // 30 minutos

            // Security & Performance
            isReadOnly = false
            connectionTestQuery = "SELECT 1"
            validationTimeout = 5000
            leakDetectionThreshold = 60000 // Detecta connection leaks

            // PostgreSQL specific
            addDataSourceProperty("prepareThreshold", "3") // Prepared statement caching
            addDataSourceProperty("preparedStatementCacheQueries", "256")
            addDataSourceProperty("preparedStatementCacheSizeMiB", "5")

            // SSL em produção
            if (EnvConfig.isProduction()) {
                addDataSourceProperty("ssl", "true")
                addDataSourceProperty("sslmode", "require")
            }

            poolName = "TasklyHikariPool"
        }

        logger.info("Inicializando pool de conexões: ${EnvConfig.DATABASE_URL}")
        return HikariDataSource(config)
    }

    fun migrate(dataSource: DataSource) {
        logger.info("🚀 Iniciando migrations...")

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .validateOnMigrate(true)
            .load()

        val result = flyway.migrate()

        logger.info("✅ Migrations executadas: ${result.migrationsExecuted}")
    }
}
