from flask import  request,Blueprint,jsonify
from core import acct_mgr
acct_app = Blueprint('acct_app', __name__)

@acct_app.route('/conf/list', methods=['GET'])
def get_configs():
    return jsonify({'msg': 'trade'})


@acct_app.route('/acct/list',methods=['GET'])
def get_acct_list():
    pass

@acct_app.route('/acct/detail',methods=['GET'])
def get_acct_detail():
    pass

@acct_app.route('/acct/start',methods=['POST'])
def start_acct():
    pass

@acct_app.route('/acct/stop',methods=['POST'])
def stop_acct():
    pass

@acct_app.route('/acct/operate',methods=['POST'])
def operate_acct():
    msg = request.json()
    acctId = msg['acctId']
    acct_mgr.send_to_client(acctId,msg)
    pass