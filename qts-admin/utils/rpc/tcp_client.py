import socket


class TcpClient(object):
    def __init__(self, host: str, port: int):
        self.host = host
        self.port = port
        self.sock = None

    def connect(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.connect((self.host, self.port))

    def close(self):
        pass

    def health_check(self):
        pass

    def send_msg(self, msg):
        # 计算消息体大小并转换为 4 字节的二进制数据
        msg_size = len(msg)
        msg_size_bytes = msg_size.to_bytes(4, byteorder='big')
        # 将消息体大小和消息体拼接成一个字节串，并发送到对端
        self.sock.sendall(msg_size_bytes + msg)

    def recv_msg(self):
        # 接收前 4 个字节，解析出消息体大小
        msg_size_bytes = self.sock.recv(4)
        msg_size = int.from_bytes(msg_size_bytes, byteorder='big')
        # 接收消息体，并返回
        data = self.sock.recv(msg_size)
        return data
