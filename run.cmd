@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 --release 17 -cp "lib/*" -sourcepath src/main/java -d out src/main/java/ru/mirea/project/Main.java
if errorlevel 1 goto error
if exist src\main\resources xcopy "src\main\resources" "out" /E /I /Y >nul
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp "out;lib/*" ru.mirea.project.Main
if errorlevel 1 goto error
exit /b 0
:error
echo Ошибка запуска. Проверьте установленный JDK и сообщения выше.
pause
exit /b 1
