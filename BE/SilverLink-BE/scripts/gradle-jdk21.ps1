param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArgs
)

$ErrorActionPreference = "Stop"

$candidateHomes = @(@(
    $env:JAVA_HOME,
    "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot",
    "C:\Program Files\Eclipse Adoptium\jdk-21",
    "C:\Program Files\Java\jdk-21",
    "C:\Program Files\Microsoft\jdk-21"
) | Where-Object { $_ -and (Test-Path (Join-Path $_ "bin\java.exe")) })

if (-not $candidateHomes) {
    $searchRoots = @(
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Java",
        "C:\Program Files\Microsoft"
    )

    foreach ($root in $searchRoots) {
        if (-not (Test-Path $root)) {
            continue
        }

        $found = Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match "^(jdk-)?21" -and (Test-Path (Join-Path $_.FullName "bin\java.exe")) } |
            Select-Object -First 1 -ExpandProperty FullName

        if ($found) {
            $candidateHomes += $found
            break
        }
    }
}

if (-not $candidateHomes) {
    throw "JDK 21 was not found. Set JAVA_HOME to a JDK 21 install path, then rerun this script."
}

$env:JAVA_HOME = $candidateHomes[0]
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

& .\gradlew.bat @GradleArgs
exit $LASTEXITCODE
