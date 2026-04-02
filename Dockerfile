# Multi-stage build para Kotlin/Maven

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copiar pom.xml primeiro para cache de dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fonte
COPY src ./src

# Build da aplicação
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar JAR do stage de build
COPY --from=build /app/target/taskly-backend.jar app.jar

# Variáveis de ambiente (podem ser sobrescritas no docker-compose)
ENV DATABASE_URL=jdbc:postgresql://postgres:5432/taskly
ENV DATABASE_USER=postgres
ENV DATABASE_PASSWORD=postgres
ENV JWT_SECRET=changeme-in-production
ENV DEEPFACE_URL=http://deepface-service:8080
ENV SERVER_PORT=7171

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:7171/health || exit 1

# Expor porta
EXPOSE 7171

# Executar aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
