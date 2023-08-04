import socket

class SocketClient:
    def __init__(self):
        pass

    def connect_to_server(self):
        # 常规tcp连接写法
        # server_address = ('127.0.0.1', 9999)
        # socket_family = socket.AF_INET
        # socket_type = socket.SOCK_STREAM

        # unix domain sockets 连接写法
        server_address = '/tmp/uds_socket'
        socket_family = socket.AF_UNIX
        socket_type = socket.SOCK_STREAM

        # 其他代码完全一样
        sock = socket.socket(socket_family, socket_type)
        sock.connect(server_address)
        sock.sendall("hello server".encode())
        data = sock.recv(1024)
        print(f"recv data from server '{server_address}': {data.decode()}")
        sock.close()

if __name__ == "__main__":
    socket_client_obj = SocketClient()
    socket_client_obj.connect_to_server()