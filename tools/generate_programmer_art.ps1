param(
    [string]$OutputRoot = (Join-Path $PSScriptRoot '..\src\main\resources\assets\analog_airwaves\textures')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

function Convert-Color([string]$Hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($Hex)
}

function Fill-Rect(
    [System.Drawing.Bitmap]$Bitmap,
    [int]$X,
    [int]$Y,
    [int]$Width,
    [int]$Height,
    [System.Drawing.Color]$Color
) {
    for ($py = $Y; $py -lt $Y + $Height; $py++) {
        for ($px = $X; $px -lt $X + $Width; $px++) {
            $Bitmap.SetPixel($px, $py, $Color)
        }
    }
}

function Save-Png([System.Drawing.Bitmap]$Bitmap, [string]$Path) {
    $directory = Split-Path -Parent $Path
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    $Bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $Bitmap.Dispose()
}

function New-TransmitterTexture([string]$LampColor, [string]$Path) {
    $bitmap = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

    Fill-Rect $bitmap 0 0 16 16 (Convert-Color '#25282b')
    Fill-Rect $bitmap 0 0 16 1 (Convert-Color '#9b5a32')
    Fill-Rect $bitmap 0 15 16 1 (Convert-Color '#563622')
    Fill-Rect $bitmap 0 1 1 14 (Convert-Color '#75452a')
    Fill-Rect $bitmap 15 1 1 14 (Convert-Color '#75452a')

    # Deliberately simple circuit traces and repeater-style contacts.
    Fill-Rect $bitmap 3 3 2 2 (Convert-Color '#bd7440')
    Fill-Rect $bitmap 11 3 2 2 (Convert-Color '#bd7440')
    Fill-Rect $bitmap 4 4 1 7 (Convert-Color '#75452a')
    Fill-Rect $bitmap 11 4 1 7 (Convert-Color '#75452a')
    Fill-Rect $bitmap 5 9 6 1 (Convert-Color '#75452a')
    Fill-Rect $bitmap 6 5 4 4 (Convert-Color '#111315')
    Fill-Rect $bitmap 7 6 2 2 (Convert-Color '#70777b')

    Fill-Rect $bitmap 6 12 4 2 (Convert-Color '#111315')
    Fill-Rect $bitmap 7 12 2 2 (Convert-Color $LampColor)

    Save-Png $bitmap $Path
}

function New-PortableRadioTexture([string]$Path) {
    $bitmap = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $bitmap.MakeTransparent()

    # Antenna.
    Fill-Rect $bitmap 10 0 1 4 (Convert-Color '#b87340')
    Fill-Rect $bitmap 11 0 1 1 (Convert-Color '#d79a62')
    Fill-Rect $bitmap 9 3 2 2 (Convert-Color '#42474a')

    # Copper-edged receiver body.
    Fill-Rect $bitmap 2 4 13 11 (Convert-Color '#8d512e')
    Fill-Rect $bitmap 3 5 11 9 (Convert-Color '#262a2d')
    Fill-Rect $bitmap 3 13 11 1 (Convert-Color '#17191b')

    # Frequency window and one tuning pixel.
    Fill-Rect $bitmap 4 6 6 3 (Convert-Color '#121516')
    Fill-Rect $bitmap 5 7 4 1 (Convert-Color '#d79b3f')
    $bitmap.SetPixel(9, 5, (Convert-Color '#55a8a1'))

    # Speaker grille.
    foreach ($y in 7, 9, 11) {
        $bitmap.SetPixel(12, $y, (Convert-Color '#777d80'))
    }
    foreach ($y in 8, 10, 12) {
        $bitmap.SetPixel(11, $y, (Convert-Color '#53585b'))
        $bitmap.SetPixel(13, $y, (Convert-Color '#53585b'))
    }

    # Bottom controls.
    Fill-Rect $bitmap 4 11 2 2 (Convert-Color '#555b5e')
    Fill-Rect $bitmap 7 11 2 2 (Convert-Color '#555b5e')

    Save-Png $bitmap $Path
}

$blockRoot = Join-Path $OutputRoot 'block'
$itemRoot = Join-Path $OutputRoot 'item'

New-TransmitterTexture '#777d80' (Join-Path $blockRoot 'transmitter_idle.png')
New-TransmitterTexture '#45d15f' (Join-Path $blockRoot 'transmitter_broadcasting.png')
New-TransmitterTexture '#e34b43' (Join-Path $blockRoot 'transmitter_interference.png')
New-PortableRadioTexture (Join-Path $itemRoot 'portable_radio.png')

Write-Host "Generated Analog Airwaves programmer-art textures in $OutputRoot"
