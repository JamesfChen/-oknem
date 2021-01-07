import socket

s = socket.socket()

port = 8889

s.connect(('127.0.0.1', port))
s.send('hello world'.encode())
s.close()
