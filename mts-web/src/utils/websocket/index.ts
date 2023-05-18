import { loadEnv } from "@build/index";
import { useAgentStoreHook } from "/@/store/modules/agent";
import { usePermissionStoreHook1 } from "/@/store/modules/permission1";

// 环境变量
const { VITE_PROXY_DOMAIN_REAL } = loadEnv();

class SocketService {
  // 和服务端连接的socket对象
  ws = null;
  // 存储回调函数
  callBackMapping = {};
  // 标识是否连接成功
  connected = false;
  // 记录重试的次数
  sendRetryCount = 0;
  // 最大重新连接尝试的次数
  connectRetryCount = 5;
  //  定义连接服务器的方法
  close() {
    this.connected = false;
    this.connectRetryCount = 0;
    this.ws.close();
    console.log("close websocket");
  }
  connect() {
    this.connectRetryCount--;
    // 连接服务器
    if (!window.WebSocket) {
      console.log("您的浏览器不支持WebSocket");
      return;
    }
    // let token = $.cookie('123');
    // let token = '4E6EF539AAF119D82AC4C2BC84FBA21F';
    //const url = "ws://127.0.0.1:8090/ws/1";
    const url =
      VITE_PROXY_DOMAIN_REAL.replace(new RegExp("http", "g"), "ws") + "/ws/1";
    this.ws = new WebSocket(url);
    // 连接成功的事件
    this.ws.onopen = () => {
      console.log("websocket connected!");
      this.connected = true;
      // 重置重新连接的次数
      this.connectRetryCount = 0;
    };
    // 1.连接服务端失败
    // 2.当连接成功之后, 服务器关闭的情况
    this.ws.onclose = () => {
      console.log("websocket closed!");
      this.connected = false;
      this.connectRetryCount++;
      if (this.connectRetryCount > 0) {
        setTimeout(() => {
          this.connect();
        }, 500 * this.connectRetryCount);
      }
    };
    // 得到服务端发送过来的数据
    this.ws.onmessage = msg => {
      const { type, data } = JSON.parse(msg.data);
      switch (type) {
        case msgType.AGENT:
          useAgentStoreHook().updateAgent(data);
          break;
      }
    };
  }
  // 回调函数的注册
  registerCallBack(socketType, callBack) {
    this.callBackMapping[socketType] = callBack;
  }
  // 取消某一个回调函数
  unRegisterCallBack(socketType) {
    this.callBackMapping[socketType] = null;
  }
  // 发送数据的方法
  send(data) {
    // 判断此时此刻有没有连接成功
    if (this.connected) {
      this.sendRetryCount = 0;
      try {
        this.ws.send(JSON.stringify(data));
      } catch (e) {
        this.ws.send(data);
      }
    } else {
      this.sendRetryCount++;
      setTimeout(() => {
        this.send(data);
      }, this.sendRetryCount * 500);
    }
  }
}

enum msgType {
  AGENT,
  LOG,
  ACCT,
  ACCT_DETAIL
}

export const websocket = new SocketService();
