package app.infrastructure.persistence

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory

/**
 * Gerenciador centralizado de transações.
 *
 * Fornece métodos utilitários para executar operações dentro de transações,
 * garantindo commit/rollback automático e gerenciamento correto do EntityManager.
 */
class TransactionManager(
    private val entityManagerFactory: EntityManagerFactory
) {

    private val logger = LoggerFactory.getLogger(TransactionManager::class.java)

    /**
     * Executa uma operação dentro de uma transação.
     *
     * - Cria um EntityManager
     * - Inicia uma transação
     * - Executa o bloco de código
     * - Faz commit se bem-sucedido
     * - Faz rollback em caso de exceção
     * - Fecha o EntityManager
     *
     * @param block Bloco de código a ser executado dentro da transação
     * @return Resultado do bloco de código
     * @throws Exception Propaga qualquer exceção que ocorrer no bloco
     */
    fun <T> executeInTransaction(block: (EntityManager) -> T): T {
        val em = entityManagerFactory.createEntityManager()
        val transaction = em.transaction

        try {
            transaction.begin()
            val result = block(em)
            transaction.commit()
            return result
        } catch (e: Exception) {
            if (transaction.isActive) {
                transaction.rollback()
                logger.warn("Transaction rolled back due to exception", e)
            }
            throw e
        } finally {
            em.close()
        }
    }

    /**
     * Executa uma operação read-only (sem transação).
     *
     * - Cria um EntityManager
     * - Executa o bloco de código (sem transação)
     * - Fecha o EntityManager
     *
     * Útil para consultas que não modificam dados, evitando overhead de transação.
     *
     * @param block Bloco de código a ser executado
     * @return Resultado do bloco de código
     * @throws Exception Propaga qualquer exceção que ocorrer no bloco
     */
    fun <T> executeReadOnly(block: (EntityManager) -> T): T {
        val em = entityManagerFactory.createEntityManager()
        try {
            return block(em)
        } finally {
            em.close()
        }
    }

    /**
     * Fecha o EntityManagerFactory.
     *
     * Deve ser chamado ao encerrar a aplicação para liberar recursos.
     */
    fun close() {
        if (entityManagerFactory.isOpen) {
            entityManagerFactory.close()
            logger.info("EntityManagerFactory closed")
        }
    }
}
