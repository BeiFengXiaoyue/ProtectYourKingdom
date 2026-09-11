@echo off
cd /d %~dp0
echo Starting SizeEditorTool (first compile takes ~15s)...
mvn -q compile exec:java -Dexec.mainClass=com.kingdom.game.util.editor.SizeEditorTool
if errorlevel 1 (
  echo.
  echo Start failed: check Maven is installed and on PATH.
  pause
)
