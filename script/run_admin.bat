@echo off
call conda activate qts 
start /B pythonw run_admin.py > output-admin.log 2>&1
exit