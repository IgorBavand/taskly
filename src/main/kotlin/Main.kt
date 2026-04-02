package app

import app.config.DatabaseConfig
import app.config.EnvConfig
import app.config.HibernateConfig
import app.config.JavalinConfig
import app.presentation.controller.AuthController
import app.presentation.controller.TodoController
import app.presentation.controller.FaceController
import app.presentation.exception.GlobalExceptionHandler
import app.infrastructure.persistence.TransactionManager
import app.infrastructure.persistence.repository.JpaUserRepository
import app.infrastructure.persistence.repository.JpaTodoRepository
import app.infrastructure.persistence.repository.JpaRefreshTokenRepository
import app.infrastructure.persistence.repository.JpaFaceValidationTokenRepository
import app.infrastructure.external.DeepFaceService
import app.application.service.AuthService
import app.application.service.TodoService
import app.application.service.FaceValidationService
import app.security.AuthMiddleware
import app.security.AuthRateLimiter
import app.security.RateLimiter
import app.security.FaceValidationMiddleware
import org.slf4j.LoggerFactory

/**
 * Ponto de entrada da aplicação.
 * Configura dependências e inicia o servidor.
 */
fun main() {
    val logger = LoggerFactory.getLogger("Main")

    logger.info("========================================")
    logger.info("  Taskly API - Inicializando...")
    logger.info("  Ambiente: ${EnvConfig.ENVIRONMENT}")
    logger.info("  Porta: ${EnvConfig.SERVER_PORT}")
    logger.info("========================================")

    // Configuração do banco de dados
    val dataSource = DatabaseConfig.dataSource()
    DatabaseConfig.migrate(dataSource)

    // Configuração do Hibernate/JPA
    val entityManagerFactory = HibernateConfig.createEntityManagerFactory(dataSource)
    val transactionManager = TransactionManager(entityManagerFactory)

    // Dependency Injection Manual (simples e eficiente)
    // Camada de infraestrutura (JPA Repositories)
    val userRepository = JpaUserRepository(transactionManager)
    val todoRepository = JpaTodoRepository(transactionManager)
    val refreshTokenRepository = JpaRefreshTokenRepository(transactionManager)
    val faceValidationTokenRepository = JpaFaceValidationTokenRepository(transactionManager)

    // Serviços externos
    val deepFaceService = DeepFaceService()

    // Camada de aplicação
    val authService = AuthService(userRepository, refreshTokenRepository)
    val todoService = TodoService(todoRepository)
    val faceValidationService = FaceValidationService(
        deepFaceService,
        faceValidationTokenRepository,
        userRepository
    )

    // Camada de apresentação
    val authController = AuthController(authService)
    val todoController = TodoController(todoService)
    val faceController = FaceController(faceValidationService)

    // Middlewares
    val faceValidationMiddleware = FaceValidationMiddleware(faceValidationService)

    // Configuração do Javalin
    val app = JavalinConfig.create()

    // Registrar Global Exception Handler (DEVE ser o primeiro)
    GlobalExceptionHandler.register(app)

    // Rate limiting global
    app.before(RateLimiter())

    // Rate limiting específico para rotas de autenticação (mais restritivo)
    app.before("/api/v1/auth/login", AuthRateLimiter())
    app.before("/api/v1/auth/register", AuthRateLimiter())

    // Rotas que requerem autenticação
    app.before("/api/v1/auth/logout", AuthMiddleware())
    app.before("/api/v1/todos*", AuthMiddleware())
    app.before("/api/v1/face*", AuthMiddleware())

    // Validação facial obrigatória para criar todos (apenas POST)
    // ⚠️ TEMPORARIAMENTE DESABILITADO - aguardando testes do DeepFace service
    // Descomentar após verificar que o serviço DeepFace está rodando
    // app.before("/api/v1/todos", faceValidationMiddleware)

    // Registrar controllers
    authController.register(app)
    todoController.register(app)
    faceController.register(app)

    // Rota de health check
    app.get("/health") { ctx ->
        ctx.json(mapOf(
            "status" to "UP",
            "version" to "1.0.0",
            "environment" to EnvConfig.ENVIRONMENT
        ))
    }

    // Rota raiz
    app.get("/") { ctx ->
        ctx.json(mapOf(
            "message" to "Taskly API",
            "version" to "1.0.0",
            "docs" to "/api/v1"
        ))
    }

    // Iniciar servidor
    app.start(EnvConfig.SERVER_PORT)

    logger.info("========================================")
    logger.info("  ✅ Servidor iniciado com sucesso!")
    logger.info("  🚀 http://localhost:${EnvConfig.SERVER_PORT}")
    logger.info("========================================")

    // Cleanup de refresh tokens expirados ao iniciar
    try {
        refreshTokenRepository.deleteExpired()
        logger.info("Refresh tokens expirados removidos")
    } catch (e: Exception) {
        logger.warn("Erro ao limpar refresh tokens expirados: ${e.message}")
    }

    // Shutdown hook para cleanup de recursos
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Encerrando aplicação...")
        try {
            transactionManager.close()
            logger.info("✅ Recursos liberados com sucesso")
        } catch (e: Exception) {
            logger.error("Erro ao liberar recursos: ${e.message}", e)
        }
    })
}
