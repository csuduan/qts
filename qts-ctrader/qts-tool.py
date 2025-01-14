import  sys,os

accts=['sim-dq','sim-tora']


def start(acctId):
    if  acctId:
        accts = [acctId]

    for acctId in accts:
        pass
    pass

def stop(acctId):
    pass


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: qts-tool.py start|stop [acctId]")
    exit(-1)

    cmd = sys.argv[1]
    acctId = sys.argv[2]

    if cmd == 'start' :
        start(acctId)
    elif cmd == 'stop':
        stop(acctId)
    else:
        print(f'not supported cmd :{cmd}')
        exit(-1)
