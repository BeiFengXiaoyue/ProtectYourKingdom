@echo off
cd /d %~dp0
echo Starting AnimEditorTool (first compile takes ~15s)...
mvn -q compile exec:java -Dexec.mainClass=com.kingdom.game.util.editor.AnimEditorLauncher
if errorlevel 1 (
  echo.
  echo Start failed: check Maven is installed and on PATH.
  pause
)
