package app.config

import jakarta.persistence.SharedCacheMode
import jakarta.persistence.ValidationMode
import jakarta.persistence.spi.ClassTransformer
import jakarta.persistence.spi.PersistenceUnitInfo
import jakarta.persistence.spi.PersistenceUnitTransactionType
import java.net.URL
import java.util.Properties
import javax.sql.DataSource

/**
 * Implementação de PersistenceUnitInfo para configuração programática do Hibernate.
 *
 * Necessário quando não se usa persistence.xml, permitindo configuração
 * via código (mais flexível para diferentes ambientes).
 */
class PersistenceUnitInfoImpl(
    private val persistenceUnitName: String,
    private val managedClassNames: List<String>,
    private val properties: Map<String, Any>
) : PersistenceUnitInfo {

    override fun getPersistenceUnitName(): String = persistenceUnitName

    override fun getPersistenceProviderClassName(): String =
        "org.hibernate.jpa.HibernatePersistenceProvider"

    override fun getTransactionType(): PersistenceUnitTransactionType =
        PersistenceUnitTransactionType.RESOURCE_LOCAL

    override fun getJtaDataSource(): DataSource? = null

    override fun getNonJtaDataSource(): DataSource? = null

    override fun getMappingFileNames(): MutableList<String> = mutableListOf()

    override fun getJarFileUrls(): MutableList<URL> = mutableListOf()

    override fun getPersistenceUnitRootUrl(): URL? = null

    override fun getManagedClassNames(): MutableList<String> = managedClassNames.toMutableList()

    override fun excludeUnlistedClasses(): Boolean = true

    override fun getSharedCacheMode(): SharedCacheMode = SharedCacheMode.UNSPECIFIED

    override fun getValidationMode(): ValidationMode = ValidationMode.AUTO

    override fun getProperties(): Properties {
        val props = Properties()
        properties.forEach { (key, value) ->
            props[key] = value
        }
        return props
    }

    override fun getPersistenceXMLSchemaVersion(): String = "3.0"

    override fun getClassLoader(): ClassLoader = Thread.currentThread().contextClassLoader

    override fun addTransformer(transformer: ClassTransformer?) {
        // Não usado em configuração programática simples
    }

    override fun getNewTempClassLoader(): ClassLoader? = null
}
