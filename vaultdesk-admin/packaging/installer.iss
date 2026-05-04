; ── VaultDesk Admin — Inno Setup Installer Script ────────
; Saurashtra Cement Ltd | IT Helpdesk + Asset Management
; Version 1.0.0

#define MyAppName "VaultDesk Admin"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "Saurashtra Cement Ltd"
#define MyAppExeName "VaultDesk Admin.exe"
#define MyAppURL "http://localhost:8080"

[Setup]
AppId={{A1B2C3D4-E5F6-7890-ABCD-EF1234567890}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
AllowNoIcons=no
OutputDir=installer-output
OutputBaseFilename=VaultDesk-Admin-Setup-v{#MyAppVersion}
SetupIconFile=icon.ico
WizardImageFile=wizard.bmp
WizardSmallImageFile=banner.bmp
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64
UninstallDisplayIcon={app}\{#MyAppExeName}
UninstallDisplayName={#MyAppName}
VersionInfoVersion={#MyAppVersion}
VersionInfoCompany={#MyAppPublisher}
VersionInfoDescription=IT Helpdesk and Asset Management Platform
MinVersion=10.0

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop shortcut"; GroupDescription: "Additional icons:"; Flags: checked
Name: "quicklaunchicon"; Description: "Create a &Quick Launch shortcut"; GroupDescription: "Additional icons:"; Flags: unchecked

[Files]
; All files from the jpackage app-image output
Source: "output-release\VaultDesk Admin\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\{#MyAppExeName}"
Name: "{group}\Uninstall {#MyAppName}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "Launch {#MyAppName}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: files; Name: "{app}\vaultdesk-config.properties"
Type: files; Name: "{app}\vaultdesk-session.properties"

[Code]
procedure InitializeWizard();
begin
  WizardForm.WelcomeLabel1.Caption := 'Welcome to VaultDesk Admin Setup';
  WizardForm.WelcomeLabel2.Caption :=
    'This will install VaultDesk Admin v{#MyAppVersion} on your computer.' + #13#10 + #13#10 +
    'VaultDesk is the IT Helpdesk and Asset Management platform for Saurashtra Cement Ltd.' + #13#10 + #13#10 +
    'Click Next to continue.';
end;