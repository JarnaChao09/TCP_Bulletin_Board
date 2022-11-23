import socket
import select
import sys
import json


def recv_all(socket_to_read_from: socket.socket):
    ret = ""
    while 1:
        data = socket_to_read_from.recv(2048)
        ret += data.decode()
        if not ret or ret[-1] == "\n":
            return ret[:-1]


def help_command(command):
    match command:
        case 'join':
            print("[join group_id]: joins or switches to group_id")
        case 'leave':
            print("[leave]: leaves current group and returns to last group joined")
        case 'send':
            print("[send subject message]: sends message to current group")
        case 'show':
            print("[show message_id]: shows the message with associated message id in the current group")
        case 'users':
            print("[users]: shows a list of users in the current group")
        case 'groups':
            print("[groups]: shows a list of groups able to be joined")
        case 'exit':
            print("[exit]: disconnects from the server and shuts down the app")
        case 'all' | _:
            print("valid commands are: join, leave, send, show, users, groups, exit, and help")

cmds = {
    'join': (1, lambda u, _, c: {'user': u, 'command': 'join', 'content': c}),
    'leave': (0, lambda u, g: {'user': u, 'command': 'leave', 'content': g}),
    'send': (2, lambda u, g, s, m: {'user': u, 'command': 'send', 'content': {'group': g, 'subject': s, 'message': m}}),
    'show': (1, lambda u, g, i: {'user': u, 'command': 'show', 'content': {'type': 'message', 'group': g, 'id': i}}),
    'users': (0, lambda u, g: {'user': u, 'command': 'list', 'content': {'group': g, 'type': 'user', 'amount': -1}}),
    'groups': (0, lambda u, g: {'user': u, 'command': 'list', 'content': {'group': g, 'type': 'group', 'amount': -1}})
}

user = ""
server = None
connected = False
while not connected:
    addr = input("Enter IP address: ")
    port = int(input("Enter port number: "))
    user = input("Enter username: ").strip()
    try:
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server.settimeout(3)
        server.connect((addr, port))
    except socket.timeout:
        print(f"{addr}:{port} took too long to respond")
    except ConnectionRefusedError:
        print(f"Connection refused at {addr}:{port}")
    else:
        server.send(f"{json.dumps({'user': user, 'command': 'connect', 'content': ''})}\n".encode())
        msg = json.loads(recv_all(server))
        print(msg)
        if msg['status'] == 200:
            connected = True
        else:
            print(f"[FAILED TO CONNECT] REASON: {msg['content']}")
            server.close()

current_group = "no group"


def shell_string():
    return f"{user}(You)[{current_group}]>>> "


clear_shell_string = "\033[2K\033[1G"

print(shell_string(), end="")
sys.stdout.flush()

done = False
while not done:
    r, _, _ = select.select([sys.stdin, server], [], [], 1)
    if r:
        for s in r:
            if s == server:
                msg = json.loads(recv_all(s))
                match msg['command']:
                    case 'send':
                        message = msg['content']
                        print(clear_shell_string, message['id'], message['user'], message['date'], message['subject'])
                    case 'list':
                        content = msg['content']
                        if msg['user'] == user and msg['status'] == 200:
                            in_group = f" in {content['group']}" if (content['type'] != 'group') else ""
                            print(f"{clear_shell_string}List of {content['type']}s{in_group}: ")
                            for e in content['data']:
                                print(e)
                        elif msg['user'] == user and msg['status'] == 400:
                            in_group = f"in {content['group']}" if (content['type'] != 'group') else ""
                            print(
                                f"{clear_shell_string}Unable to retrieve list of {content['type']}s {in_group} because {content['reason']}"
                            )
                    case 'join' | 'leave':
                        print(clear_shell_string, end="")
                        content = msg['content']
                        if msg['user'] == user and msg['status'] == 200:
                            current_group = content['group'] or "no group"
                        print(content['content'])
                    case 'show':
                        print(clear_shell_string, end="")
                        content = msg['content']
                        if msg['user'] == user and msg['status'] == 200:
                            print(f"{content['id']}: {content['data']}")
                        elif msg['user'] == user and msg['status'] == 400:
                            print(f"showing {content['id']} failed due to {content['reason']}")
                    case _:
                        print(clear_shell_string, msg['content'])
                print(shell_string(), end="")
                sys.stdout.flush()
            else:
                cmd = sys.stdin.readline().strip().split(" ")
                if cmd[0] == "exit":
                    done = True
                elif cmd[0] == 'help':
                    help_command(cmd[1] if len(cmd) > 1 else "all")
                elif cmd[0] in cmds and len(cmd) - 1 == cmds[cmd[0]][0]:
                    json_to_send = cmds[cmd[0]][1](user, current_group, *cmd[1:])
                    server.send((json.dumps(json_to_send) + "\n").encode())
                elif cmd[0] in cmds:
                    print(
                        f"[SYNTAX ERROR]: {cmd[0]} was expecting {cmds[cmd[0]][0]} arguments but got {len(cmd) - 1} arguments instead"
                    )
                else:
                    print(f"[UNKNOWN COMMAND]: {cmd[0]}")
                    print("VALID COMMANDS: join, leave, send, show, users, exit and help")
                print(shell_string(), end="")
                sys.stdout.flush()
print(f"{clear_shell_string}exiting...")
server.close()
