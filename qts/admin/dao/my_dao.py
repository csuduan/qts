from sqlalchemy import Column
from .sqlite_session import session,engine
import pandas as pd

def get_acct_configs():
    return pd.read_sql_query('SELECT  * FROM CONF_ACCT',engine)

def get_stragegy_configs():
    return pd.read_sql_query('SELECT  * FROM CONF_STRATEGY',engine)