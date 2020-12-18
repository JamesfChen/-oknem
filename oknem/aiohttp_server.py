
import asyncio
import threading
from aiohttp import web
port = 8889


async def handle(request):
    print('handle')
    return web.Response(text=('hello,adsf'))


def start_server():

    app = web.Application()
    app.add_routes([web.get('/', handle),
                    web.patch('/', handle),
                    web.post('/', handle),
                    web.put('/', handle),
                    ])
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
