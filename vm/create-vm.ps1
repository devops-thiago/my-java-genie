#requires -Version 5
# =============================================================================
# create-vm.ps1 - Creates the TDC workshop VM (Xubuntu 24.04) for My Java Genie
# =============================================================================
# Creates a VirtualBox VM that meets the TDC spec (6GB RAM, 2 vCPU, 60GB dynamic
# disk, NAT, Guest Additions) and runs an unattended Ubuntu install.
#
# Usage (from an elevated / normal PowerShell on the Windows host):
#   .\vm\create-vm.ps1                       # create + install only
#   .\vm\create-vm.ps1 -Provision            # create + install + auto-provision
#   .\vm\create-vm.ps1 -VmName foo -Password 'secret' -Provision
#
# The VM user is created with passwordless sudo (NOPASSWD) so provisioning can
# run non-interactively. The app image is NOT started here.
# =============================================================================
[CmdletBinding()]
param(
  [string]$VmName   = "my-java-genie-workshop",
  [string]$User     = "workshop",
  [string]$Password = "workshop123",
  [int]   $MemoryMB = 6144,
  [int]   $Cpus     = 2,
  [int]   $DiskGB   = 60,
  [string]$IsoDir   = "$env:USERPROFILE\Downloads",
  [string]$IsoUrl   = "https://cdimage.ubuntu.com/xubuntu/releases/24.04/release/xubuntu-24.04.3-desktop-amd64.iso",
  [string]$RepoRaw  = "https://raw.githubusercontent.com/devops-thiago/my-java-genie/workshop-vm",
  [switch]$Provision,
  [switch]$Headless
)

$ErrorActionPreference = "Stop"
$VBox = "C:\Program Files\Oracle\VirtualBox\VBoxManage.exe"
if (-not (Test-Path $VBox)) { throw "VBoxManage not found at $VBox. Is VirtualBox installed?" }

# --- 1. Download the ISO if needed -----------------------------------------
$IsoName = Split-Path $IsoUrl -Leaf
$Iso = Join-Path $IsoDir $IsoName
if (-not (Test-Path $Iso)) {
  Write-Host "Downloading Xubuntu ISO to $Iso (this is ~3GB)..." -ForegroundColor Cyan
  Start-BitsTransfer -Source $IsoUrl -Destination $Iso
}
Write-Host "Using ISO: $Iso"

# --- 2. Create the VM ------------------------------------------------------
$VmDir = Join-Path "${env:USERPROFILE}\VirtualBox VMs" $VmName
$Vdi   = Join-Path $VmDir "$VmName.vdi"

Write-Host "Creating VM '$VmName' ($MemoryMB MB, $Cpus vCPU, $DiskGB GB)..." -ForegroundColor Cyan
& $VBox createvm --name $VmName --ostype Ubuntu_64 --register | Out-Null
& $VBox modifyvm $VmName --memory $MemoryMB --cpus $Cpus --vram 128 `
  --acpi on --ioapic on --rtc-useutc on --nic1 nat --audio none `
  --boot1 dvd --boot2 disk

& $VBox createmedium disk --filename $Vdi --size ($DiskGB * 1024) --variant Standard | Out-Null
& $VBox storagectl $VmName --name SATA --add sata --controller IntelAhci | Out-Null
& $VBox storageattach $VmName --storagectl SATA --port 0 --device 0 --type hdd --medium $Vdi | Out-Null
& $VBox storagectl $VmName --name IDE --add ide | Out-Null
& $VBox storageattach $VmName --storagectl IDE --port 0 --device 0 --type dvddrive --medium $Iso | Out-Null

# --- 3. Unattended install (OS + Guest Additions + user + passwordless sudo) -
Write-Host "Starting unattended install (this can take 15-30 min)..." -ForegroundColor Cyan
$PostInstall = 'install -d /etc/sudoers.d && printf "workshop ALL=(ALL) NOPASSWD:ALL\n" > /etc/sudoers.d/workshop && chmod 440 /etc/sudoers.d/workshop'
$StartMode = if ($Headless) { "headless" } else { "gui" }
& $VBox unattended install $VmName `
  --iso="$Iso" `
  --user=$User --password=$Password --full-user-name="Workshop" `
  --install-additions `
  --time-zone=America/Sao_Paulo --locale=pt_BR --country=BR `
  --post-install-command=$PostInstall `
  --start-vm=$StartMode

Write-Host "Unattended install finished." -ForegroundColor Green

# --- 4. Optional: provision via Guest Additions ----------------------------
if ($Provision) {
  Write-Host "Starting VM '$VmName' for provisioning..." -ForegroundColor Cyan
  & $VBox startvm $VmName --type headless | Out-Null

  Write-Host "Waiting for Guest Additions to be ready..." -ForegroundColor Cyan
  $ready = $false
  for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 5
    & $VBox guestcontrol $VmName run --username $User --password $Password `
      --wait-stdout --wait-stderr -- /bin/true 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    Write-Host "  ...waiting for guest ($($i+1)/60)"
  }
  if (-not $ready) {
    Write-Warning "Guest Additions did not become ready in time."
    Write-Host "Fallback: log into the VM GUI and run:" -ForegroundColor Yellow
    Write-Host "  curl -fsSL $RepoRaw/vm/provision.sh | sudo WORKSHOP_VM=true bash" -ForegroundColor Yellow
    return
  }

  Write-Host "Launching provisioning in the background (logs: /var/log/workshop-provision.log inside the VM)..." -ForegroundColor Cyan
  $provCmd = "nohup bash -c 'curl -fsSL $RepoRaw/vm/provision.sh | sudo WORKSHOP_VM=true bash' > /var/log/workshop-provision.log 2>&1 & disown"
  & $VBox guestcontrol $VmName run --username $User --password $Password `
    --wait-stdout --wait-stderr -- /bin/bash -c $provCmd | Out-Null

  Write-Host "Provisioning is running. Polling progress (look for 'finished')..." -ForegroundColor Cyan
  for ($i = 0; $i -lt 120; $i++) {
    Start-Sleep -Seconds 30
    $done = & $VBox guestcontrol $VmName run --username $User --password $Password `
      --wait-stdout --wait-stderr -- /bin/bash -c 'test -f /var/lib/workshop-provision.done && echo DONE || tail -n 3 /var/log/workshop-provision.log' 2>$null
    Write-Host "  [$($i+1)] $done"
    if ($done -match 'DONE') {
      Write-Host "Provisioning complete!" -ForegroundColor Green
      break
    }
  }
  if ($done -notmatch 'DONE') {
    Write-Warning "Provisioning did not signal completion within the polling window."
    Write-Host "Check progress with:" -ForegroundColor Yellow
    Write-Host "  & `"$VBox`" guestcontrol $VmName run --username $User --password $Password --wait-stdout --wait-stderr -- /bin/bash -c 'tail -n 40 /var/log/workshop-provision.log'" -ForegroundColor Yellow
  }
} else {
  Write-Host "VM created and installed. To provision it:" -ForegroundColor Green
  Write-Host "  Either re-run with -Provision, or log into the VM GUI and run:" -ForegroundColor Yellow
  Write-Host "  curl -fsSL $RepoRaw/vm/provision.sh | sudo WORKSHOP_VM=true bash" -ForegroundColor Yellow
}

Write-Host "Done. VM: $VmName  (user: $User / password: $Password)" -ForegroundColor Green
