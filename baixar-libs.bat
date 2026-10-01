@echo off
rem Baixa as bibliotecas que o Tomcat nao traz para src\main\webapp\WEB-INF\lib
setlocal
cd /d "%~dp0src\main\webapp\WEB-INF\lib"
set M=https://repo1.maven.org/maven2
call :baixar %M%/org/postgresql/postgresql/42.7.13/postgresql-42.7.13.jar postgresql-42.7.13.jar
call :baixar %M%/jakarta/servlet/jsp/jstl/jakarta.servlet.jsp.jstl-api/3.0.1/jakarta.servlet.jsp.jstl-api-3.0.1.jar jakarta.servlet.jsp.jstl-api-3.0.1.jar
call :baixar %M%/org/glassfish/web/jakarta.servlet.jsp.jstl/3.0.1/jakarta.servlet.jsp.jstl-3.0.1.jar jakarta.servlet.jsp.jstl-3.0.1.jar
echo Pronto. JARs em %CD%
pause
exit /b 0
:baixar
if exist "%2" ( echo ja existe: %2 & exit /b 0 )
echo baixando %2
powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing -Uri '%1' -OutFile '%2'"
exit /b 0
