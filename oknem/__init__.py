import http.server
from http.server import HTTPServer, BaseHTTPRequestHandler, SimpleHTTPRequestHandler
import os
import socket


def get_host_ip():
    """
    查询本机ip地址
    :return: ip
    """
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    finally:
        s.close()

    return ip


class RHandler(SimpleHTTPRequestHandler):

    def do_CONNECT(self):
        print("In do_CONNECT, path: %s" % self.path)


def run(server_class=HTTPServer, handler_class=BaseHTTPRequestHandler):
    # print('local ip', get_host_ip())
    server_address = ('', 8889)
    # print('listening %s' % server_address)
    httpd = server_class(server_address, handler_class)
    httpd.serve_forever()


def create():
    run(handler_class=RHandler)
