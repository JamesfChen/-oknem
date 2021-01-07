import socket
import subprocess

import time

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
print("Socket successfully created")
port = 8889
s.bind(('localhost', port))
print("socket binded to %s" % (port))

s.listen(5)
print("socket is listening")
c, addr = s.accept()
print('Got connection from', addr)
b = c.recv(12)
print(b)
c.close()
