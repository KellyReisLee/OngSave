# =====================================================================
#  OngSave · imagem Docker (Tomcat 10.1 + Java 17)
#
#  Construir:  docker build -t ongsave .
#  Executar:   docker run -p 8080:8080 -e DATABASE_URL="postgresql://...neon.tech/neondb?sslmode=require" ongsave
#  Abrir:      http://localhost:8080/
#
#  Variáveis de ambiente (todas opcionais, menos DATABASE_URL):
#    DATABASE_URL               connection string do Neon (obrigatória)
#    ONGSAVE_APP_SIMULACAO      true = modo apresentação (frota e GPS simulados) · false = produção
#    ONGSAVE_APP_DADOSDEMO      true = cria contas e dados de demonstração num banco vazio
#    ONGSAVE_APP_ADMINSENHA     nova senha do admin@ongsave.com (mín. 8 caracteres)
#    PORT                       porta HTTP (Render, Railway, Fly e afins definem-na sozinhos)
# =====================================================================

# ---------- 1) Build com Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 2) Execução no Tomcat ----------
FROM tomcat:10.1-jre17-temurin
ENV ONGSAVE_APP_SIMULACAO=true \
    ONGSAVE_APP_DADOSDEMO=true \
    TZ=America/Sao_Paulo \
    CATALINA_OPTS="-Djava.awt.headless=true -XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"

# Remove as apps de exemplo do Tomcat e publica o OngSave na raiz (/)
RUN rm -rf "$CATALINA_HOME"/webapps/* "$CATALINA_HOME"/webapps.dist
COPY --from=build /app/target/ongsave.war "$CATALINA_HOME"/webapps/ROOT.war

EXPOSE 8080
# Usa a porta de $PORT quando a plataforma a define (senão 8080)
CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/\" \"$CATALINA_HOME/conf/server.xml\" && exec catalina.sh run"]
