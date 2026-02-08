# PowerShell script to merge proto files

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$splitDir = Join-Path $scriptDir "split"
$outputFile = Join-Path $scriptDir "msg.proto"

Write-Host "========================================"
Write-Host "Merging proto files"
Write-Host "Split directory: $splitDir"
Write-Host "Output file: $outputFile"
Write-Host "========================================"
Write-Host ""

# Check if split directory exists
if (-not (Test-Path $splitDir)) {
    Write-Error "Error: split directory does not exist: $splitDir"
    exit 1
}

# Delete old output file if exists
if (Test-Path $outputFile) {
    Remove-Item $outputFile
}

# Create UTF-8 encoding without BOM
$utf8NoBom = New-Object System.Text.UTF8Encoding $false

# Write header
$header = @"
// msg.proto
// Merged proto file
syntax = "proto2";

package org.gof.demo.worldsrv.msg;

import "google/protobuf/descriptor.proto";
import "options.proto";

"@

[System.IO.File]::WriteAllText($outputFile, $header, $utf8NoBom)

# Get all proto files and sort them
$protoFiles = Get-ChildItem -Path $splitDir -Filter "*.proto" | Sort-Object Name

# Process each file
foreach ($protoFile in $protoFiles) {
    $filename = $protoFile.Name
    Write-Host "Processing: $filename"

    $content = [System.IO.File]::ReadAllText($protoFile.FullName, $utf8NoBom)
    $lines = $content -split "`r?`n"
    $cleanedLines = @()
    $inHeader = $true

    foreach ($line in $lines) {
        if ($inHeader) {
            # Skip filename comment (allow spaces after //)
            if ($line -match '^//.*\d+_\d+.*\.proto') {
                continue
            }
            # Skip syntax declaration
            if ($line -match '^\s*syntax\s*=') {
                continue
            }
            # Skip package declaration
            if ($line -match '^\s*package\s+') {
                continue
            }
            # Skip import declaration
            if ($line -match '^\s*import\s+') {
                continue
            }
            # Skip empty lines
            if ([string]::IsNullOrWhiteSpace($line)) {
                continue
            }

            # First non-skipped line - exit header mode
            $inHeader = $false
        } else {
            # Even after header mode, skip these lines (in case they appear later)
            # Skip syntax declaration
            if ($line -match '^\s*syntax\s*=') {
                continue
            }
            # Skip package declaration
            if ($line -match '^\s*package\s+') {
                continue
            }
            # Skip import declaration
            if ($line -match '^\s*import\s+') {
                continue
            }
        }

        # Add all non-header lines
        $cleanedLines += $line
    }

    # Write separator and content
    $separator = "`n// ===== $filename =====`n"
    $fileContent = $separator + ($cleanedLines -join "`n") + "`n"

    [System.IO.File]::AppendAllText($outputFile, $fileContent, $utf8NoBom)
}

Write-Host ""
Write-Host "========================================"
Write-Host "Merge completed!"
Write-Host "Output file: $outputFile"
Write-Host "Total files merged: $($protoFiles.Count)"
Write-Host "========================================"
