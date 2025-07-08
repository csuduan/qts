from numpy.ma.core import identity
from sqlalchemy import Boolean, Column, Integer,text,String
from sqlalchemy.ext.declarative import declarative_base

from typing import List
from qts.common.model.object import AcctConf

from .sqlite_session import session_scope
import pandas as pd

Base = declarative_base()
class AcctConfPo(Base):
    __tablename__ = 'CONF_ACCT'
    
    id = Column(String, primary_key=True)
    name = Column(String)
    group = Column(String)
    user = Column(String)
    pwd = Column(String)
    broker = Column(String) 
    auth = Column(String) 
    td_addr = Column(String)
    md_addr = Column(String)
    enable = Column(Boolean)
    rpc_addr= Column(String)
    remark= Column(String)

    def __repr__(self):
        return f"AcctConfPo(id='{self.id}', name='{self.name}', group='{self.group}', user='{self.user}', pwd='{self.pwd}', broker='{self.broker}', auth='{self.auth}', td_addr='{self.td_addr}', md_addr='{self.md_addr}', enable={self.enable}, rpc_addr='{self.rpc_addr}', remark='{self.remark}')"

class ConfDao():
    def get_acct_configs(self)->List[AcctConf]:
        #sql=text('SELECT  * FROM CONF_ACCT')
        #result = session.execute(sql).mappings().fetchall()
        with session_scope() as session:
            acct_conf_pos = session.query(AcctConfPo).all()
            return [AcctConf(**vars(po)) for po in acct_conf_pos]

    def get_acct_config(self,id:str)->AcctConf:
        with session_scope() as session:
            po = session.query(AcctConfPo).filter(AcctConfPo.id == id).first()
            return AcctConf(**vars(po))

    def save_acct_config(self,config:AcctConf):
        with session_scope() as session:
            po = AcctConfPo(**vars(config))
            existing = session.query(AcctConfPo).filter_by(id=po.id).first()
            if existing:
                session.merge(po)
            else:
                session.add(po)
