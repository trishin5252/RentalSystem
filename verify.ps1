$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
    New-Item -ItemType Directory -Force out | Out-Null
    $taskMain = @(Get-ChildItem src/main/java -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    $taskTests = @(Get-ChildItem src/test/java -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    & javac -encoding UTF-8 --release 17 -cp 'lib/*' -d out @taskMain
    if ($LASTEXITCODE -ne 0) { throw 'Ошибка компиляции приложения' }
    if (Test-Path src/main/resources) { Copy-Item src/main/resources/* out -Recurse -Force }
    & javac -encoding UTF-8 --release 17 -cp 'out;lib/*' -d out @taskTests
    if ($LASTEXITCODE -ne 0) { throw 'Ошибка компиляции проверок' }
    & java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp 'out;lib/*' ru.mirea.project.RegressionChecks
    if ($LASTEXITCODE -ne 0) { throw 'Проверки не пройдены' }
} finally { Pop-Location }
