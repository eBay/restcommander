@echo off
echo ****************************
echo Go to your browser for http://localhost:9000 to run the program.
echo Open Eclipse, choose import: import existing project
echo ****************************
rmdir AgentMaster\tmp /s /q
play-1.2.4\play eclipsify AgentMaster
pause > nul