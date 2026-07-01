# ════════════════════════════════════════════
# STAGE 1 — BUILD
# Compile l'application avec Maven et le JDK,
# sans embarquer ces outils dans l'image finale
# ════════════════════════════════════════════
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copie uniquement le pom.xml d'abord — permet
# à Docker de mettre en cache les dépendances
# Maven tant que le pom.xml ne change pas
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copie le reste du code source et compile
COPY src ./src
RUN mvn clean package -DskipTests -B

# ════════════════════════════════════════════
# STAGE 2 — RUNTIME
# Image finale légère, contient uniquement le
# JRE et le .jar compilé — pas Maven, pas le
# code source
# ════════════════════════════════════════════
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Récupère uniquement le .jar compilé depuis
# le stage précédent
COPY --from=build /app/target/*.jar app.jar

# Port exposé par Spring Boot
EXPOSE 8080

# Utilisateur non-root pour la sécurité —
# évite de faire tourner l'application en root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

ENTRYPOINT ["java", "-jar", "app.jar"]