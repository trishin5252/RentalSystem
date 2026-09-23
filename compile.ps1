$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
    [Console]::InputEncoding = [Text.UTF8Encoding]::new($false)
    New-Item -ItemType Directory -Force out | Out-Null
    $taskSources = @(Get-ChildItem -LiteralPath 'src/main/java' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    if (Test-Path src/main/resources) { Copy-Item src/main/resources/* out -Recurse -Force }
    & javac -encoding UTF-8 --release 17 -d out -cp 'lib/*' @taskSources
    if ($LASTEXITCODE -ne 0) { throw 'Ошибка компиляции' }
    if (Test-Path src/main/resources) { Copy-Item src/main/resources/* out -Recurse -Force }
    & java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp 'out;lib/*' ru.mirea.project.Main
    if ($LASTEXITCODE -ne 0) { throw 'Программа завершилась с ошибкой' }
} finally { Pop-Location }
