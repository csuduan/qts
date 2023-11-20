from flask import  request,Blueprint,jsonify
from utils import log_utils

logger = log_utils.get_logger(__name__)
sys_app = Blueprint('sys_app', __name__)

def build_resp(msg):
    resp = {'code':0,'message':'success','data':msg}
    return jsonify(resp)
@sys_app.route('/user/login', methods=['POST'])
def lonin():
    return build_resp('admin-token')


@sys_app.route('/user/logout', methods=['POST'])
def lonout():
    return build_resp('success')

@sys_app.route('/user/info', methods=['GET'])
def get_userInfo():
    useInfo = {
        'avatar':'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif',
        'name':'Admin',
        'roles':['admin']
    }
    return build_resp(useInfo)

@sys_app.route('/router', methods=['GET'])
def get_router():
    #todo
    pass

@sys_app.errorhandler(Exception)
def hanle_error(error):
    logger.exception(str(error))
    return jsonify( {'code':9999,'message':str(error)})



