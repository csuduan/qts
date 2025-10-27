import sys
from pathlib import Path
from logging import DEBUG, INFO, WARNING, ERROR, CRITICAL
import logging
from typing import Any, Callable, Optional

from loguru import logger
from .config import config 


class Logger:
    """
    日志记录器类，封装了loguru的功能
    
    提供统一的日志接口，支持控制台输出和文件输出
    支持日志级别配置、日志轮转和保留策略
    """
    
    def __init__(self):
        """
        初始化日志记录器
        
        Args:
            name: 日志记录器名称，用于区分不同的日志实例
        """
        self._logger = logger
        self._configured = False
        
        # 移除默认的日志处理器
        self._logger.remove()
        
        # 初始化配置
        self._configure_logger()
    
    def _configure_logger(self) -> None:
        """配置日志记录器"""
        if self._configured:
            return
            
        # 日志格式
        self.format = "<green>{time:YYYY-MM-DD HH:mm:ss.SSS}</green> - {level} - <cyan>{module}:{line}</cyan> - {message}"
        
        # 获取配置
        self.level = config.get_config("log.level") or INFO
        self.log_name = config.get_config("log_name") or "app"
        self.log_path = config.get_config("log_path") or "./logs"
        self.env = config.get_config("env") or "prod"
        
        # 添加控制台输出（仅在开发环境）
        if self.env == 'dev':
            self._logger.add(sink=sys.stdout, level=self.level, format=self.format)
        
        # 添加文件输出
        filename = f"{self.log_name}.log"
        file_path = Path(self.log_path).joinpath(filename)
        self._logger.add(
            sink=file_path, 
            level=self.level,
            rotation="00:00",
            retention="15 days", 
            enqueue=True,
            format=self.format
        )
        
        self._configured = True
    
    def init_config(self):
        LOGGER_NAMES = ("uvicorn.asgi", "uvicorn.access", "uvicorn")

        # change handler for default uvicorn logger
        logging.getLogger().handlers = [InterceptHandler()]
        for logger_name in LOGGER_NAMES:
            logging_logger = logging.getLogger(logger_name)
            logging_logger.handlers = [InterceptHandler()]

 
    def add_sink(self, sink: Any, level: int = None, filter_func: Optional[Callable] = None) -> int:
        """
        添加自定义日志处理器
        
        Args:
            sink: 日志处理器目标（文件、控制台等）
            level: 日志级别，默认为当前配置级别
            filter_func: 过滤器函数
            
        Returns:
            int: 处理器ID
        """
        return self._logger.add(
            sink=sink, 
            level=level or self.level, 
            format=self.format, 
            filter=filter_func
        )
    
    def remove_sink(self, sink_id: int) -> None:
        """
        移除指定的日志处理器
        
        Args:
            sink_id: 要移除的处理器ID
        """
        self._logger.remove(sink_id)
    
    def get_logger(self):
        """获取底层的loguru记录器实例"""
        return self._logger


class InterceptHandler(logging.Handler):
    def emit(self, record: logging.LogRecord) -> None:  # pragma: no cover
        # Get corresponding Loguru level if it exists
        try:
            level = logger.level(record.levelname).name
        except ValueError:
            level = str(record.levelno)
 
        # Find caller from where originated the logged message
        frame, depth = logging.currentframe(), 2
        while frame.f_code.co_filename == logging.__file__:  # noqa: WPS609
            frame = cast(FrameType, frame.f_back)
            depth += 1
 
        logger.opt(depth=depth, exception=record.exc_info).log(
            level, record.getMessage(),
        )
 



# 创建默认的全局日志记录器实例
Loggers = Logger()
# 保持向后兼容性
logger = Loggers.get_logger()


# 导出日志级别常量
__all__ = [
    "DEBUG",
    "INFO", 
    "WARNING",
    "ERROR",
    "CRITICAL",
    "Logger",
    "Loggers",
    "add_custom_sink"
]