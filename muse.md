C:\android\flutter\bin\flutter.bat run --machine --track-widget-creation --device-id=7040016025040287 --start-paused --dart-define=flutter.inspector.structuredErrors=true --devtools-server-address=http://127.0.0.1:9100 lib\main.dart
Launching lib\main.dart on B160V in debug mode...
Running Gradle task 'assembleDebug'...
WARNING: A restricted method in java.lang.System has been called
WARNING: java.lang.System::load has been called by net.rubygrapefruit.platform.internal.NativeLibraryLoader in an unnamed module (file:/C:/Users/612co/.gradle/wrapper/dists/gradle-9.3.1-all/9ot9r568e8zfvvd4mn8rbu1j0/gradle-9.3.1/lib/native-platform-0.22-milestone-29.jar)
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
WARNING: Restricted methods will be blocked in a future release unless native access is enabled

lib/features/probe/probe_screen.dart:7:8: Error: Error when reading '/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker-12.3.0/lib/file_picker.dart': The system cannot find the path specified
import 'package:file_picker/file_picker.dart';
^
lib/features/probe/probe_screen.dart:10:8: Error: Error when reading '/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/permission_handler-13.0.2/lib/permission_handler.dart': The system cannot find the path specified
import 'package:permission_handler/permission_handler.dart';
^
lib/adapters/storage_probe.dart:7:8: Error: Error when reading '/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/permission_handler-13.0.2/lib/permission_handler.dart': The system cannot find the path specified
import 'package:permission_handler/permission_handler.dart';
^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:6:8: Error: Error when reading '/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_platform_interface-3.4.0/lib/file_picker_platform_interface.dart': The system cannot find the path specified
import 'package:file_picker_platform_interface/file_picker_platform_interface.dart';
^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux_options.dart:1:8: Error: Error when reading '/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_platform_interface-3.4.0/lib/file_picker_platform_interface.dart': The system cannot find the path specified
import 'package:file_picker_platform_interface/file_picker_platform_interface.dart';
^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/filters.dart:2:8: Error: Error when reading '/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_platform_interface-3.4.0/lib/file_picker_platform_interface.dart': The system cannot find the path specified
import 'package:file_picker_platform_interface/file_picker_platform_interface.dart';
^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/linux_platform_file.dart:5:8: Error: Error when reading '/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_platform_interface-3.4.0/lib/file_picker_platform_interface.dart': The system cannot find the path specified
import 'package:file_picker_platform_interface/file_picker_platform_interface.dart';
^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:16:31: Error: Type 'FilePickerPlatform' not found.
class FilePickerLinux extends FilePickerPlatform {
^^^^^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:44:10: Error: Type 'PlatformFile' not found.
Future<PlatformFile?> pickFile({
^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:47:5: Error: Type 'FileType' not found.
FileType type = FileType.any,
^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:51:5: Error: Type 'AndroidOptions' not found.
AndroidOptions androidOptions = const AndroidOptions(),
^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:52:5: Error: Type 'DarwinOptions' not found.
DarwinOptions darwinOptions = const DarwinOptions(),
^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:53:5: Error: Type 'WindowsOptions' not found.
WindowsOptions windowsOptions = const WindowsOptions(),
^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:54:5: Error: Type 'LinuxOptions' not found.
LinuxOptions linuxOptions = const FilePickerLinuxOptions(),
^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:55:5: Error: Type 'WebOptions' not found.
WebOptions webOptions = const WebOptions(),
^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:49:14: Error: Type 'FilePickerStatus' not found.
Function(FilePickerStatus)? onFileLoading,
^^^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:71:15: Error: Type 'PlatformFile' not found.
Future<List<PlatformFile>> pickFiles({
^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:74:5: Error: Type 'FileType' not found.
FileType type = FileType.any,
^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:78:5: Error: Type 'AndroidOptions' not found.
AndroidOptions androidOptions = const AndroidOptions(),
^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:79:5: Error: Type 'DarwinOptions' not found.
DarwinOptions darwinOptions = const DarwinOptions(),
^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:80:5: Error: Type 'WindowsOptions' not found.
WindowsOptions windowsOptions = const WindowsOptions(),
^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:81:5: Error: Type 'LinuxOptions' not found.
LinuxOptions linuxOptions = const FilePickerLinuxOptions(),
^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:82:5: Error: Type 'WebOptions' not found.
WebOptions webOptions = const WebOptions(),
^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:76:14: Error: Type 'FilePickerStatus' not found.
Function(FilePickerStatus)? onFileLoading,
^^^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:96:15: Error: Type 'PlatformFile' not found.
Future<List<PlatformFile>> _pickFilesInternal({
^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:100:5: Error: Type 'FileType' not found.
FileType type = FileType.any,
^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:104:5: Error: Type 'LinuxOptions' not found.
LinuxOptions linuxOptions = const FilePickerLinuxOptions(),
^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:102:14: Error: Type 'FilePickerStatus' not found.
Function(FilePickerStatus)? onFileLoading,
^^^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:164:5: Error: Type 'FileType' not found.
FileType type = FileType.any,
^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:180:5: Error: Type 'AndroidOptions' not found.
AndroidOptions androidOptions = const AndroidOptions(),
^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:181:5: Error: Type 'WindowsOptions' not found.
WindowsOptions windowsOptions = const WindowsOptions(),
^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:182:5: Error: Type 'LinuxOptions' not found.
LinuxOptions linuxOptions = const FilePickerLinuxOptions(),
^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:183:5: Error: Type 'WebOptions' not found.
WebOptions webOptions = const WebOptions(),
^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:237:5: Error: Type 'WindowsOptions' not found.
WindowsOptions windowsOptions = const WindowsOptions(),
^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:238:5: Error: Type 'LinuxOptions' not found.
LinuxOptions linuxOptions = const FilePickerLinuxOptions(),
^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:239:5: Error: Type 'WebOptions' not found.
WebOptions webOptions = const WebOptions(),
^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:236:14: Error: Type 'FilePickerStatus' not found.
Function(FilePickerStatus)? onFileSaving,
^^^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:303:48: Error: Type 'LinuxOptions' not found.
static FilePickerLinuxOptions resolveOptions(LinuxOptions linuxOptions) {
^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux_options.dart:4:44: Error: Type 'LinuxOptions' not found.
final class FilePickerLinuxOptions extends LinuxOptions {
^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/filters.dart:55:10: Error: Type 'FileType' not found.
Filter(FileType type, List<String>? allowedExtensions) {
^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/linux_platform_file.dart:9:38: Error: Type 'PlatformFile' not found.
base class LinuxPlatformFile extends PlatformFile {
^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:47:21: Error: Undefined name 'FileType'.
FileType type = FileType.any,
^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:51:43: Error: Couldn't find constructor 'AndroidOptions'.
AndroidOptions androidOptions = const AndroidOptions(),
^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:52:41: Error: Couldn't find constructor 'DarwinOptions'.
DarwinOptions darwinOptions = const DarwinOptions(),
^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:53:43: Error: Couldn't find constructor 'WindowsOptions'.
WindowsOptions windowsOptions = const WindowsOptions(),
^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:55:35: Error: Couldn't find constructor 'WebOptions'.
WebOptions webOptions = const WebOptions(),
^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:74:21: Error: Undefined name 'FileType'.
FileType type = FileType.any,
^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:78:43: Error: Couldn't find constructor 'AndroidOptions'.
AndroidOptions androidOptions = const AndroidOptions(),
^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:79:41: Error: Couldn't find constructor 'DarwinOptions'.
DarwinOptions darwinOptions = const DarwinOptions(),
^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:80:43: Error: Couldn't find constructor 'WindowsOptions'.
WindowsOptions windowsOptions = const WindowsOptions(),
^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:82:35: Error: Couldn't find constructor 'WebOptions'.
WebOptions webOptions = const WebOptions(),
^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:100:21: Error: Undefined name 'FileType'.
FileType type = FileType.any,
^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:164:21: Error: Undefined name 'FileType'.
FileType type = FileType.any,
^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:180:43: Error: Couldn't find constructor 'AndroidOptions'.
AndroidOptions androidOptions = const AndroidOptions(),
^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:181:43: Error: Couldn't find constructor 'WindowsOptions'.
WindowsOptions windowsOptions = const WindowsOptions(),
^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:183:35: Error: Couldn't find constructor 'WebOptions'.
WebOptions webOptions = const WebOptions(),
^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:237:43: Error: Couldn't find constructor 'WindowsOptions'.
WindowsOptions windowsOptions = const WindowsOptions(),
^^^^^^^^^^^^^^
/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:239:35: Error: Couldn't find constructor 'WebOptions'.
WebOptions webOptions = const WebOptions(),
^^^^^^^^^^
lib/features/probe/probe_screen.dart:46:15: Error: 'Permission' isn't a type.
final Map<Permission, PermissionStatus> out =
^^^^^^^^^^
lib/features/probe/probe_screen.dart:46:27: Error: 'PermissionStatus' isn't a type.
final Map<Permission, PermissionStatus> out =
^^^^^^^^^^^^^^^^
lib/features/probe/probe_screen.dart:47:16: Error: 'Permission' isn't a type.
await <Permission>[Permission.photos, Permission.videos].request();
^^^^^^^^^^
lib/features/probe/probe_screen.dart:53:26: Error: 'Permission' isn't a type.
.map((MapEntry<Permission, PermissionStatus> e) =>
^^^^^^^^^^
lib/features/probe/probe_screen.dart:53:38: Error: 'PermissionStatus' isn't a type.
.map((MapEntry<Permission, PermissionStatus> e) =>
^^^^^^^^^^^^^^^^
lib/features/probe/probe_screen.dart:47:28: Error: The getter 'Permission' isn't defined for the type '_ProbeScreenState'.
- '_ProbeScreenState' is from 'package:vision_engine/features/probe/probe_screen.dart' ('lib/features/probe/probe_screen.dart').
  Try correcting the name to the name of an existing getter, or defining a getter or field named 'Permission'.
  await <Permission>[Permission.photos, Permission.videos].request();
  ^^^^^^^^^^
  lib/features/probe/probe_screen.dart:47:47: Error: The getter 'Permission' isn't defined for the type '_ProbeScreenState'.
- '_ProbeScreenState' is from 'package:vision_engine/features/probe/probe_screen.dart' ('lib/features/probe/probe_screen.dart').
  Try correcting the name to the name of an existing getter, or defining a getter or field named 'Permission'.
  await <Permission>[Permission.photos, Permission.videos].request();
  ^^^^^^^^^^
  lib/features/probe/probe_screen.dart:47:66: Error: The method 'request' isn't defined for the type 'List<invalid-type>'.
- 'List' is from 'dart:core'.
  Try correcting the name to the name of an existing method, or defining a method named 'request'.
  await <Permission>[Permission.photos, Permission.videos].request();
  ^^^^^^^
  lib/features/probe/probe_screen.dart:60:34: Error: The getter 'FilePicker' isn't defined for the type '_ProbeScreenState'.
- '_ProbeScreenState' is from 'package:vision_engine/features/probe/probe_screen.dart' ('lib/features/probe/probe_screen.dart').
  Try correcting the name to the name of an existing getter, or defining a getter or field named 'FilePicker'.
  final String? picked = await FilePicker.getDirectoryPath(
  ^^^^^^^^^^
  lib/adapters/storage_probe.dart:22:9: Error: 'PermissionStatus' isn't a type.
  final PermissionStatus photos = await Permission.photos.status;
  ^^^^^^^^^^^^^^^^
  lib/adapters/storage_probe.dart:22:41: Error: Undefined name 'Permission'.
  final PermissionStatus photos = await Permission.photos.status;
  ^^^^^^^^^^
  lib/adapters/storage_probe.dart:23:9: Error: 'PermissionStatus' isn't a type.
  final PermissionStatus videos = await Permission.videos.status;
  ^^^^^^^^^^^^^^^^
  lib/adapters/storage_probe.dart:23:41: Error: Undefined name 'Permission'.
  final PermissionStatus videos = await Permission.videos.status;
  ^^^^^^^^^^
  lib/adapters/storage_probe.dart:24:9: Error: 'PermissionStatus' isn't a type.
  final PermissionStatus storage = await Permission.storage.status;
  ^^^^^^^^^^^^^^^^
  lib/adapters/storage_probe.dart:24:42: Error: Undefined name 'Permission'.
  final PermissionStatus storage = await Permission.storage.status;
  ^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:19:5: Error: Undefined name 'FilePickerPlatform'.
  FilePickerPlatform.instance = FilePickerLinux();
  ^^^^^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:47:5: Error: 'FileType' isn't a type.
  FileType type = FileType.any,
  ^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:49:14: Error: 'FilePickerStatus' isn't a type.
  Function(FilePickerStatus)? onFileLoading,
  ^^^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:51:5: Error: 'AndroidOptions' isn't a type.
  AndroidOptions androidOptions = const AndroidOptions(),
  ^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:52:5: Error: 'DarwinOptions' isn't a type.
  DarwinOptions darwinOptions = const DarwinOptions(),
  ^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:53:5: Error: 'WindowsOptions' isn't a type.
  WindowsOptions windowsOptions = const WindowsOptions(),
  ^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:54:5: Error: 'LinuxOptions' isn't a type.
  LinuxOptions linuxOptions = const FilePickerLinuxOptions(),
  ^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:55:5: Error: 'WebOptions' isn't a type.
  WebOptions webOptions = const WebOptions(),
  ^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:74:5: Error: 'FileType' isn't a type.
  FileType type = FileType.any,
  ^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:76:14: Error: 'FilePickerStatus' isn't a type.
  Function(FilePickerStatus)? onFileLoading,
  ^^^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:78:5: Error: 'AndroidOptions' isn't a type.
  AndroidOptions androidOptions = const AndroidOptions(),
  ^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:79:5: Error: 'DarwinOptions' isn't a type.
  DarwinOptions darwinOptions = const DarwinOptions(),
  ^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:80:5: Error: 'WindowsOptions' isn't a type.
  WindowsOptions windowsOptions = const WindowsOptions(),
  ^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:81:5: Error: 'LinuxOptions' isn't a type.
  LinuxOptions linuxOptions = const FilePickerLinuxOptions(),
  ^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:82:5: Error: 'WebOptions' isn't a type.
  WebOptions webOptions = const WebOptions(),
  ^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:100:5: Error: 'FileType' isn't a type.
  FileType type = FileType.any,
  ^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:102:14: Error: 'FilePickerStatus' isn't a type.
  Function(FilePickerStatus)? onFileLoading,
  ^^^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:104:5: Error: 'LinuxOptions' isn't a type.
  LinuxOptions linuxOptions = const FilePickerLinuxOptions(),
  ^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:112:36: Error: The getter 'lockParentWindow' isn't defined for the type 'FilePickerLinuxOptions'.
- 'FilePickerLinuxOptions' is from 'package:file_picker_linux/src/file_picker_linux_options.dart' ('/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux_options.dart').
  Try correcting the name to the name of an existing getter, or defining a getter or field named 'lockParentWindow'.
  'modal': DBusBoolean(options.lockParentWindow),
  ^^^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:116:17: Error: The getter 'acceptLabel' isn't defined for the type 'FilePickerLinuxOptions'.
- 'FilePickerLinuxOptions' is from 'package:file_picker_linux/src/file_picker_linux_options.dart' ('/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux_options.dart').
  Try correcting the name to the name of an existing getter, or defining a getter or field named 'acceptLabel'.
  if (options.acceptLabel != null) {
  ^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:117:54: Error: The getter 'acceptLabel' isn't defined for the type 'FilePickerLinuxOptions'.
- 'FilePickerLinuxOptions' is from 'package:file_picker_linux/src/file_picker_linux_options.dart' ('/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux_options.dart').
  Try correcting the name to the name of an existing getter, or defining a getter or field named 'acceptLabel'.
  xdpOption["accept_label"] = DBusString(options.acceptLabel!);
  ^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:164:5: Error: 'FileType' isn't a type.
  FileType type = FileType.any,
  ^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:173:31: Error: The getter 'uri' isn't defined for the type 'Object?'.
- 'Object' is from 'dart:core'.
  Try correcting the name to the name of an existing getter, or defining a getter or field named 'uri'.
  return files.map((e) => e.uri.path).toList();
  ^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:173:41: Error: A value of type 'List<dynamic>' can't be returned from an async function with return type 'Future<List<String>>'.
- 'List' is from 'dart:core'.
- 'Future' is from 'dart:async'.
  return files.map((e) => e.uri.path).toList();
  ^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:180:5: Error: 'AndroidOptions' isn't a type.
  AndroidOptions androidOptions = const AndroidOptions(),
  ^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:181:5: Error: 'WindowsOptions' isn't a type.
  WindowsOptions windowsOptions = const WindowsOptions(),
  ^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:182:5: Error: 'LinuxOptions' isn't a type.
  LinuxOptions linuxOptions = const FilePickerLinuxOptions(),
  ^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:183:5: Error: 'WebOptions' isn't a type.
  WebOptions webOptions = const WebOptions(),
  ^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:190:36: Error: The getter 'lockParentWindow' isn't defined for the type 'FilePickerLinuxOptions'.
- 'FilePickerLinuxOptions' is from 'package:file_picker_linux/src/file_picker_linux_options.dart' ('/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux_options.dart').
  Try correcting the name to the name of an existing getter, or defining a getter or field named 'lockParentWindow'.
  'modal': DBusBoolean(options.lockParentWindow),
  ^^^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:236:14: Error: 'FilePickerStatus' isn't a type.
  Function(FilePickerStatus)? onFileSaving,
  ^^^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:237:5: Error: 'WindowsOptions' isn't a type.
  WindowsOptions windowsOptions = const WindowsOptions(),
  ^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:238:5: Error: 'LinuxOptions' isn't a type.
  LinuxOptions linuxOptions = const FilePickerLinuxOptions(),
  ^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:239:5: Error: 'WebOptions' isn't a type.
  WebOptions webOptions = const WebOptions(),
  ^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:246:36: Error: The getter 'lockParentWindow' isn't defined for the type 'FilePickerLinuxOptions'.
- 'FilePickerLinuxOptions' is from 'package:file_picker_linux/src/file_picker_linux_options.dart' ('/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux_options.dart').
  Try correcting the name to the name of an existing getter, or defining a getter or field named 'lockParentWindow'.
  'modal': DBusBoolean(options.lockParentWindow),
  ^^^^^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/file_picker_linux.dart:303:48: Error: 'LinuxOptions' isn't a type.
  static FilePickerLinuxOptions resolveOptions(LinuxOptions linuxOptions) {
  ^^^^^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/filters.dart:55:10: Error: 'FileType' isn't a type.
  Filter(FileType type, List<String>? allowedExtensions) {
  ^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/filters.dart:63:12: Error: Not a constant expression.
  case FileType.any:
  ^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/filters.dart:65:12: Error: Not a constant expression.
  case FileType.audio:
  ^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/filters.dart:67:12: Error: Not a constant expression.
  case FileType.custom:
  ^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/filters.dart:69:12: Error: Not a constant expression.
  case FileType.image:
  ^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/filters.dart:71:12: Error: Not a constant expression.
  case FileType.media:
  ^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/filters.dart:73:12: Error: Not a constant expression.
  case FileType.video:
  ^^^^^^^^
  /C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/filters.dart:56:17: Error: The getter 'FileType' isn't defined for the type 'Filter'.
- 'Filter' is from 'package:file_picker_linux/src/filters.dart' ('/C:/Users/612co/AppData/Local/Pub/Cache/hosted/pub.dev/file_picker_linux-1.1.0/lib/src/filters.dart').
  Try correcting the name to the name of an existing getter, or defining a getter or field named 'FileType'.
  if (type == FileType.custom &&
  ^^^^^^^^
  Target kernel_snapshot_program failed: Exception


FAILURE: Build failed with an exception.

* What went wrong:
  Execution failed for task ':app:compileFlutterBuildDebug'.
> Process 'command 'C:\android\flutter\bin\flutter.bat'' finished with non-zero exit value 1

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).
> Get more help at https://help.gradle.org.

BUILD FAILED in 9s
Error: Gradle task assembleDebug failed with exit code 1
App failed to start
