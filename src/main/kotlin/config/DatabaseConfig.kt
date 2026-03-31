package app.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object DatabaseConfig {

    fun dataSource(): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://localhost:5432/taskly"
            username = "teste"
            password = "teste"
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
        }

        return HikariDataSource(config)
    }

    fun migrate(dataSource: DataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}