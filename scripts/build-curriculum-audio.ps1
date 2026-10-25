$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech
$projectRoot = Split-Path -Parent $PSScriptRoot
$curriculumPath = Join-Path $projectRoot 'docs/content/curriculum-a1-c1.json'
$audioDirectory = Join-Path $projectRoot 'uploads/curriculum/audio'
$content = Get-Content -LiteralPath $curriculumPath -Raw -Encoding UTF8 | ConvertFrom-Json
$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer
try {
    $voice = $speaker.GetInstalledVoices() | Where-Object { $_.Enabled -and $_.VoiceInfo.Culture.Name -like 'en-*' } | Select-Object -First 1
    if (-not $voice) { throw 'An English Windows speech voice is required to rebuild the teaching audio.' }
    $speaker.SelectVoice($voice.VoiceInfo.Name)
    New-Item -ItemType Directory -Path $audioDirectory -Force | Out-Null
    foreach ($lesson in $content | Where-Object { $_.skill -eq 'Listening' }) {
        $speaker.Rate = switch ($lesson.level) { 'A2' { -2 } 'B1' { -1 } default { 0 } }
        $target = Join-Path $audioDirectory ('listening-' + $lesson.level.ToLowerInvariant() + '.wav')
        if (Test-Path -LiteralPath $target) {
            Write-Output ('Keeping existing media: ' + [System.IO.Path]::GetFileName($target))
            continue
        }
        $format = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo 16000, ([System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen), ([System.Speech.AudioFormat.AudioChannel]::Mono)
        $speaker.SetOutputToWaveFile($target, $format)
        $speaker.Speak($lesson.transcript)
        $speaker.SetOutputToNull()
        Write-Output ('Created ' + [System.IO.Path]::GetFileName($target) + ' with ' + $voice.VoiceInfo.Name)
    }
} finally { $speaker.Dispose() }
