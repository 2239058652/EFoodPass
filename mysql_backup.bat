@echo off
chcp 65001 >nul
:: 设置中文编码，防止控制台乱码

:: ================= 配置信息 =================
set DB_USER=root
set DB_PASS=Dingtong1998
set DB_NAME=e_food
:: 设置备份文件存放的目录 (请确保D盘有这个文件夹，没有会自动创建)
set BACKUP_DIR=C:\Users\22390\Desktop\FamilyFood\mysql

:: ================= 生成时间戳 =================
:: 获取格式如 20260316_110703 的时间戳作为文件名
set d=%date:~0,4%%date:~5,2%%date:~8,2%
set t=%time:~0,2%%time:~3,2%%time:~6,2%
:: 把时间里可能出现的空格替换成0 (比如早上8点)
set t=%t: =0%
set TIMESTAMP=%d%_%t%

set BACKUP_FILE=%BACKUP_DIR%\%DB_NAME%_backup_%TIMESTAMP%.sql

:: ================= 执行备份 =================
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

echo 开始备份数据库 %DB_NAME% ...
echo 备份文件将保存到: %BACKUP_FILE%

:: 执行导出命令 (如果提示找不到 mysqldump，请看下方的避坑指南)
mysqldump -u%DB_USER% -p%DB_PASS% %DB_NAME% > "%BACKUP_FILE%"

echo.
echo 备份完成！
pause