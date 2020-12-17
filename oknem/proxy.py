
import asyncio
import threading
from aiohttp import web
port = 8889


def start_server():

    app = web.Application()
    # web.run_app(app, port=9000)
    runner = web.AppRunner(app)
    return runner


# kill tcp process :sudo lsof -i tcp:9000
def main():
    server_loop = asyncio.new_event_loop()
    print('listening localhost:%d' % port)
    asyncio.set_event_loop(server_loop)
    runner = start_server()
    server_loop.run_until_complete(runner.setup())
    site = web.TCPSite(runner, '0.0.0.0', port)
    server_loop.run_until_complete(site.start())
    server_loop.run_forever()


if __name__ == "__main__":
    main()
