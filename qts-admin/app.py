from flask import Flask, render_template
from flask_socketio import SocketIO,emit
from utils import configs

from controller import sys_app,acct_app
from core import acct_mgr

app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret!'
socketio = SocketIO(app)


app.register_blueprint(sys_app, url_prefix='/qts/v1/sys')
app.register_blueprint(acct_app, url_prefix='/qts/v1/acct')
acct_mgr.start_all()
@app.route('/')
def hello_world():  # put application's code here
    return 'qts admin!'

@socketio.on('message')
def handle_message(data):
    print(f"Received message: {data}")
    emit('response', f"Server received: {data}")

if __name__ == '__main__':
    app.run(host='0.0.0.0',port=configs['server_port'],debug=True)
