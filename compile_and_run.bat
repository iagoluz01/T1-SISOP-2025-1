@echo off
echo Compilando projeto...
mkdir bin 2>nul
javac -d bin src/*.java
if %ERRORLEVEL% NEQ 0 (
  echo Erro na compilacao!
  pause
  exit /b %ERRORLEVEL%
)

echo Executando simulador...
java -cp bin Main
