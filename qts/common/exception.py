class BizException(Exception):
    def __init__(self,err_code,err_msg):
        self.err_code = err_code
        self.err_msg = err_msg

    def __int__(self,ex:Exception):
        self.err_code = 9999
        self.err_msg = str(ex)