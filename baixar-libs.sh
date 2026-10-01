#!/usr/bin/env sh
# Baixa as bibliotecas que o Tomcat não traz para src/main/webapp/WEB-INF/lib
set -e
cd "$(dirname "$0")/src/main/webapp/WEB-INF/lib"
M=https://repo1.maven.org/maven2
for url in \
  "$M/org/postgresql/postgresql/42.7.13/postgresql-42.7.13.jar" \
  "$M/jakarta/servlet/jsp/jstl/jakarta.servlet.jsp.jstl-api/3.0.1/jakarta.servlet.jsp.jstl-api-3.0.1.jar" \
  "$M/org/glassfish/web/jakarta.servlet.jsp.jstl/3.0.1/jakarta.servlet.jsp.jstl-3.0.1.jar"; do
  f=$(basename "$url")
  if [ -f "$f" ]; then echo "já existe: $f"; continue; fi
  echo "baixando $f"
  if command -v curl >/dev/null 2>&1; then curl -fsSL -o "$f" "$url"; else wget -q -O "$f" "$url"; fi
done
echo "Pronto. JARs em $(pwd)"
