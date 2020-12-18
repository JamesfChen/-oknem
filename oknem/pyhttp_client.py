import http.client


def main():
    # h3 = http.client.HTTPConnection('www.python.org', 80)
    conn = http.client.HTTPSConnection("www.python.org")
    conn.request("GET", "/")
    r1 = conn.getresponse()
    print(r1.status, r1.reason)
    data1 = r1.read()
    conn.request("GET", "/")
    r1 = conn.getresponse()
    chunk = r1.read(200)
    while chunk:
        chunk = r1.read(200)
        print(repr(chunk))


if __name__ == "__main__":
    main()
