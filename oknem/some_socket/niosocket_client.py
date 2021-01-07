import asyncio


async def tcp_echo_client(ip, port):
    reader, writer = await asyncio.open_connection(ip, port)
    message = 'CONNECT interface.music.163.com:443 HTTP/1.1'
    print(f'Send: {message!r}')
    writer.write(message.encode())

    data = await reader.read(100)
    print(f'Received: {data.decode()!r}')

    print('Close the connection')
    writer.close()


def start_client(ip, port):
    asyncio.run(tcp_echo_client(ip, port))


if __name__ == "__main__":
    start_client('101.71.154.241', '443')
