from collections import deque
from datetime import datetime
import threading
import time
from typing import Dict, Any, Optional
from dataclasses import dataclass

from qts.common.log import get_logger

log = get_logger(__name__)

@dataclass
class Message:
    """消息体结构"""
    timestamp: datetime    # 消息创建时间
    ttl: float            # 存活时间(秒)
    data: Any             # 消息数据
    
    def is_expired(self) -> bool:
        """检查消息是否过期"""
        return (datetime.now() - self.timestamp).total_seconds() > self.ttl

class TTLQueue:
    def __init__(self, cleanup_interval: float = 1.0):
        """
        初始化TTL队列
        Args:
            cleanup_interval: 清理检查间隔(秒)
        """
        self.queue = deque()
        self.lock = threading.Lock()
        self.running = True
        self.cleanup_interval = cleanup_interval
        
        
        # 启动清理线程
        self.cleaner_thread = threading.Thread(target=self._cleanup_worker, daemon=True)
        self.cleaner_thread.start()
    
    def put(self, data: Any, ttl: Optional[float] = None) -> None:
        """
        添加消息到队列
        Args:
            data: 消息数据
            ttl: 消息的TTL(秒)，如果不指定则永不过期
        """
        message = Message(
            timestamp=datetime.now(),
            ttl=float('inf') if ttl is None else ttl,
            data=data
        )
        
        with self.lock:
            self.queue.append(message)
    
    def get_all(self) -> list:
        """获取所有未过期的消息数据"""
        with self.lock:
            return [msg.data for msg in self.queue if not msg.is_expired()]
    
    def get_messages(self) -> list:
        """获取所有未过期的完整消息对象"""
        with self.lock:
            return [msg for msg in self.queue if not msg.is_expired()]
    
    def _cleanup_worker(self) -> None:
        """后台清理工作线程"""
        while self.running:
            try:
                with self.lock:
                    # 计算需要删除的数量
                    delete_count = 0
                    for msg in self.queue:
                        if msg.is_expired():
                            delete_count += 1
                        else:
                            break
                    
                    # 批量删除过期消息
                    for _ in range(delete_count):
                        self.queue.popleft()
                    
                    if delete_count > 0:
                        log.debug(f"Cleaned {delete_count} expired messages")
                
                time.sleep(self.cleanup_interval)
                
            except Exception as e:
                log.error(f"Error in cleanup worker: {e}")
                time.sleep(1)
    
    def size(self) -> int:
        """获取当前队列大小"""
        with self.lock:
            return len(self.queue)
    
    def shutdown(self) -> None:
        """关闭队列和清理线程"""
        self.running = False
        self.cleaner_thread.join()
        log.info("TTLQueue shutdown completed")