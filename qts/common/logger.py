import sys
from pathlib import Path
from logging import DEBUG, INFO, WARNING, ERROR, CRITICAL

from loguru import logger
from .config import config 

logger.remove()


__all__ = [
    "DEBUG",
    "INFO",
    "WARNING",
    "ERROR",
    "CRITICAL",
    "logger",
]
# format: str = (
#     "<green>{time:YYYY-MM-DD HH:mm:ss.SSS}</green> "
#     "| <level>{level}</level> "
#     "| <cyan>{extra[gateway_name]}</cyan> "
#     "| <level>{message}</level>"
# )
format: str ="<green>{time:YYYY-MM-DD HH:mm:ss.SSS}</green> - {level} - <cyan>{file}:{line}</cyan> - {message}"

# Add default gateway
#logger.configure(extra={{"gateway_name": "Logger"}})

# Log level
level: int = config.get_config("log.level")
level = level if level else INFO
app_name: str = config.get_config("appName")
log_path: str = config.get_config("log_path")

# Remove default stderr output

# Add console output
logger.add(sink=sys.stdout, level=level, format=format)
# Add file output
#today_date: str = datetime.now().strftime("%Y%m%d")
filename: str = f"{app_name}_{{time:YYYY-MM-DD}}.log"
file_path: Path = Path(log_path).joinpath(filename)
logger.add(sink=file_path, level=level,rotation="00:00", format=format)



def add_custom_sink(sink,filter, level=level):
    logger.add(sink=sink, level=level, format=format, filter=filter)
