from sqlalchemy import create_engine, Column, Integer, String
from sqlalchemy.orm import sessionmaker
from contextlib import contextmanager
from sqlalchemy.ext.declarative import declarative_base
from qts.common.config import  config

# 创建连接 SQLite 数据库的 engine 对象
engine = create_engine(url= config.get_config('db_url'),pool_size=10,pool_timeout=30)
# 创建 sessionmaker 对象
Session = sessionmaker(bind=engine)
# 创建 Session 对象
# session = Session()

@contextmanager
def session_scope():
    """提供事务范围的上下文管理器"""
    session = Session()
    try:
        yield session
        session.commit()
    except:
        session.rollback()
        raise
    finally:
        session.close()