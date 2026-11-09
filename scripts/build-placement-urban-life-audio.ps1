param(
    [string] $UploadDirectory = (Join-Path (Split-Path -Parent $PSScriptRoot) 'uploads')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech

$clips = [ordered]@{
    'market-message.wav' = 'Hi, Maya. It is Ben. The market opens at nine fifteen, but I cannot get there that early. Could we meet at nine forty-five instead? The fountain is being repaired, so wait for me by the bus stop on King Street. We can go to the bakery afterwards.'
    'station-announcement.wav' = 'Passengers for the ten twenty service to the airport should go to platform two. The departure board may still show platform four. This service will not stop at North Park today. If you are travelling to North Park, get off at Riverside and take the replacement bus outside the station.'
    'repair-cafe.wav' = 'When we opened the repair cafe, anyone could just turn up with a broken object. We introduced bookings because some visitors were waiting most of the afternoon. There is still no charge. We do fix quite a few things, but for me the best part is watching people who were nervous about touching a tool leave feeling able to try a simple repair at home.'
}

$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer
try {
    $voice = $speaker.GetInstalledVoices() |
        Where-Object { $_.Enabled -and $_.VoiceInfo.Culture.Name -like 'en-*' } |
        Select-Object -First 1
    if (-not $voice) { throw 'Install an English Windows speech voice to generate the placement audio.' }

    $speaker.SelectVoice($voice.VoiceInfo.Name)
    $speaker.Rate = -1
    $format = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo 16000, ([System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen), ([System.Speech.AudioFormat.AudioChannel]::Mono)
    $audioDirectory = Join-Path $UploadDirectory 'questions/placement-urban-life-01'
    New-Item -ItemType Directory -Path $audioDirectory -Force | Out-Null

    foreach ($clip in $clips.GetEnumerator()) {
        $target = Join-Path $audioDirectory $clip.Key
        if (Test-Path -LiteralPath $target) { continue }

        $temporaryAudio = "$target.$([guid]::NewGuid()).tmp"
        try {
            $speaker.SetOutputToWaveFile($temporaryAudio, $format)
            try {
                $speaker.Speak($clip.Value)
            } finally {
                $speaker.SetOutputToNull()
            }
            Move-Item -LiteralPath $temporaryAudio -Destination $target
            Write-Output "Created $target"
        } finally {
            if (Test-Path -LiteralPath $temporaryAudio) {
                Remove-Item -LiteralPath $temporaryAudio
            }
        }
    }
} finally {
    $speaker.Dispose()
}
