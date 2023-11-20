from sqlalchemy import create_engine, Column, Integer, String
from sqlalchemy.orm import sessionmaker
from sqlalchemy.ext.declarative import declarative_base
from utils import  configs

# 创建连接 SQLite 数据库的 engine 对象
engine = create_engine(configs['db_url'])
# 创建 sessionmaker 对象
Session = sessionmaker(bind=engine)
# 创建 Session 对象
session = Session()