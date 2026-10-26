$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$folder = Join-Path $root 'artifacts\quapp-civic-paper'
$output = Join-Path $root 'artifacts\quapp-civic-paper.html'
$old = Get-Content -LiteralPath $output -Raw
$match = [regex]::Match($old, '(?s)<script id="artboard-data" type="application/json">(.*?)</script>')
if (-not $match.Success) { throw 'Gallery metadata is missing.' }
$data = $match.Groups[1].Value | ConvertFrom-Json
foreach ($board in $data.boards) {
    $board.html = (Get-Content -LiteralPath (Join-Path $folder $board.file) -Raw).Replace('<link rel="stylesheet" href="fonts.css">', '<!--LOCAL_FONTS-->')
}
$data.notes = Get-Content -LiteralPath (Join-Path $folder 'Design-notes.txt') -Raw
$data.fonts = Get-Content -LiteralPath (Join-Path $folder 'fonts.css') -Raw
$json = ($data | ConvertTo-Json -Depth 8 -Compress).Replace('</script', '<\/script')
$template = Get-Content -LiteralPath (Join-Path $root 'artifacts\quapp-gallery-template.html') -Raw
$built = $template.Replace('PAYLOAD_PLACEHOLDER', $json)
[IO.File]::WriteAllText($output, $built)
[IO.File]::WriteAllText((Join-Path $folder 'index.html'), $built)
Compress-Archive -Path (Join-Path $folder '*') -DestinationPath (Join-Path $root 'artifacts\quapp-civic-paper.zip') -Force
Write-Output "Rebuilt $($data.boards.Count) screens and the ZIP."
