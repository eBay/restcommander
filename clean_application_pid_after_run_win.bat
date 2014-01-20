@echo off
echo ****************************
echo Go to your browser for http://localhost:9000 to run the program.
echo ****************************
rmdir AgentMaster\tmp /s /q
del AgentMaster\server.pid /q
play-1.2.4\play stop AgentMaster
