; ── VaultDesk Server Installer ────────────────────────────
#define MyAppName "VaultDesk Server"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "Saurashtra Cement Ltd"
#define ServiceName "VaultDeskServer"
#define ServiceDisplayName "VaultDesk Server"

[Setup]
AppId={{B2C3D4E5-F6A7-8901-BCDE-F12345678901}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName=C:\VaultDesk-Server
DefaultGroupName={#MyAppName}
DisableDirPage=yes
OutputDir=..\server-installer-output
OutputBaseFilename=VaultDesk-Server-Setup-v{#MyAppVersion}
SetupIconFile=icon.ico
WizardImageFile=wizard.bmp
WizardSmallImageFile=banner.bmp
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin
MinVersion=10.0

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Dirs]
Name: "{app}"
Name: "{app}\logs"

[Files]
Source: "..\target\server-0.0.1-SNAPSHOT.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "nssm.exe"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\Uninstall {#MyAppName}"; Filename: "{uninstallexe}"

[Code]
procedure InstallService();
var
  ResultCode: Integer;
  NssmPath: String;
  AppDir: String;
begin
  NssmPath := ExpandConstant('{app}\nssm.exe');
  AppDir   := ExpandConstant('{app}');

  Exec(NssmPath, 'stop ' + '{#ServiceName}',
       '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(NssmPath, 'remove ' + '{#ServiceName}' + ' confirm',
       '', SW_HIDE, ewWaitUntilTerminated, ResultCode);

  Exec(NssmPath, 'install ' + '{#ServiceName}' + ' java',
       '', SW_HIDE, ewWaitUntilTerminated, ResultCode);

  Exec(NssmPath,
    'set ' + '{#ServiceName}' +
    ' AppParameters "-jar server-0.0.1-SNAPSHOT.jar --server.port=2008"',
    '', SW_HIDE, ewWaitUntilTerminated, ResultCode);

  Exec(NssmPath,
    'set ' + '{#ServiceName}' + ' AppDirectory "' + AppDir + '"',
    '', SW_HIDE, ewWaitUntilTerminated, ResultCode);

  Exec(NssmPath,
    'set ' + '{#ServiceName}' + ' DisplayName "' +
    '{#ServiceDisplayName}' + '"',
    '', SW_HIDE, ewWaitUntilTerminated, ResultCode);

  Exec(NssmPath,
    'set ' + '{#ServiceName}' +
    ' Description "VaultDesk IT Helpdesk Server - Port 2008"',
    '', SW_HIDE, ewWaitUntilTerminated, ResultCode);

  Exec(NssmPath,
    'set ' + '{#ServiceName}' + ' AppStdout "' +
    AppDir + '\logs\output.log"',
    '', SW_HIDE, ewWaitUntilTerminated, ResultCode);

  Exec(NssmPath,
    'set ' + '{#ServiceName}' + ' AppStderr "' +
    AppDir + '\logs\error.log"',
    '', SW_HIDE, ewWaitUntilTerminated, ResultCode);

  Exec(NssmPath,
    'set ' + '{#ServiceName}' + ' Start SERVICE_AUTO_START',
    '', SW_HIDE, ewWaitUntilTerminated, ResultCode);

  Exec(NssmPath, 'start ' + '{#ServiceName}',
       '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;

procedure RemoveService();
var
  ResultCode: Integer;
  NssmPath: String;
begin
  NssmPath := ExpandConstant('{app}\nssm.exe');
  Exec(NssmPath, 'stop ' + '{#ServiceName}',
       '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(NssmPath, 'remove ' + '{#ServiceName}' + ' confirm',
       '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;

function InitializeSetup(): Boolean;
var
  ResultCode: Integer;
begin
  if not Exec('java', '-version', '', SW_HIDE,
              ewWaitUntilTerminated, ResultCode) then
  begin
    MsgBox(
      'Java 21 is required but was not found.' + #13#10 + #13#10 +
      'Please install Java 21 from: https://adoptium.net' + #13#10 +
      'Then run this installer again.',
      mbError, MB_OK);
    Result := False;
    Exit;
  end;
  Result := True;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
    InstallService();
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usUninstall then
    RemoveService();
end;

procedure InitializeWizard();
begin
  WizardForm.WelcomeLabel1.Caption := 'Welcome to VaultDesk Server Setup';
  WizardForm.WelcomeLabel2.Caption :=
    'This installs VaultDesk Server v{#MyAppVersion} as a Windows Service.' + #13#10 + #13#10 +
    'The server runs on port 2008 and starts automatically on boot.' + #13#10 + #13#10 +
    'Requirement: Java 21 must be installed on this machine.' + #13#10 + #13#10 +
    'Click Next to continue.';
end;