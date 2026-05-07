# Build script: test versions 1.21.3 through 1.21.11
$versions = @(
    @{ mc="1.21.3"; yarn="1.21.3+build.2"; fapi="0.114.1+1.21.3" },
    @{ mc="1.21.4"; yarn="1.21.4+build.8"; fapi="0.119.4+1.21.4" },
    @{ mc="1.21.5"; yarn="1.21.5+build.1"; fapi="0.128.2+1.21.5" },
    @{ mc="1.21.6"; yarn="1.21.6+build.1"; fapi="0.128.2+1.21.6" },
    @{ mc="1.21.7"; yarn="1.21.7+build.8"; fapi="0.129.0+1.21.7" },
    @{ mc="1.21.8"; yarn="1.21.8+build.1"; fapi="0.136.1+1.21.8" },
    @{ mc="1.21.9"; yarn="1.21.9+build.1"; fapi="0.134.1+1.21.9" },
    @{ mc="1.21.10"; yarn="1.21.10+build.3"; fapi="0.138.4+1.21.10" },
    @{ mc="1.21.11"; yarn="1.21.11+build.5"; fapi="0.141.3+1.21.11" }
)

$template = @"
# Done to increase the memory available to gradle.
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true

# Fabric Properties
# check these on https://fabricmc.net/develop
minecraft_version={MC}
yarn_mappings={YARN}
loader_version=0.16.14
loom_version=1.10-SNAPSHOT

# Mod Properties
mod_version=1.0.0
maven_group=com.pvpmod
archives_base_name=IWantToHitFromThreeBlocks

# Dependencies
fabric_api_version={FAPI}
"@

foreach ($v in $versions) {
    $mc = $v.mc
    $content = $template -replace '\{MC\}', $mc -replace '\{YARN\}', $v.yarn -replace '\{FAPI\}', $v.fapi
    Set-Content -Path "gradle.properties" -Value $content -NoNewline
    
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Building for $mc..." -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    
    $output = & .\gradlew clean build 2>&1
    $exitCode = $LASTEXITCODE
    
    if ($exitCode -eq 0 -or ($output -match "BUILD SUCCESSFUL")) {
        Write-Host "SUCCESS: $mc" -ForegroundColor Green
    } else {
        Write-Host "FAILED: $mc" -ForegroundColor Red
        Write-Host "Error output:" -ForegroundColor Red
        $output | Select-String "error:" | ForEach-Object { Write-Host $_.Line -ForegroundColor Yellow }
        Write-Host "STOPPING - fix required for $mc" -ForegroundColor Red
        break
    }
}
