# TCP Bulletin Board

## Contributors
Kai Hoenshell<br>
Jaran Chao<br>

## Server

### Language and Build Tools
- [Kotlin 1.7.21](https://kotlinlang.org/docs/home.html)
- [Gradle 7.4.2](https://docs.gradle.org/current/userguide/userguide.html)

### Libraries
- [kotlinx-coroutines 1.6.4](https://github.com/Kotlin/kotlinx.coroutines)
    - First party library developed by Jetbrains in Kotlin for asynchronous and non-blocking programming
    - Used for concurrency in the non-blocking multi-bind server
- [kotlinx-serialization 1.4.1](https://github.com/Kotlin/kotlinx.serialization)
    - First party compiler plugin and library by Jetbrains to handle the generation of code and API for serializing data into multiple formats
    - Used for JSON parsing

### Tests
- [kotest 5.5.4](https://github.com/kotest/kotest)
    - 3rd party powerful, elegant and flexible test framework for Kotlin
    - Used to create unit tests for the JSON parser and serializer

### How to Run
1. Ensure that Java is installed and that the JDK (Java Development Kit) is on PATH
    - OpenJDK or any third party "not official" JDK will work
    - Java installation (`java`) must be under a JDK installation, not a JRE installation
2. run `./gradlew run --args="port"` to start up the server and open the socket connection on localhost at the specified port
    - `gradlew` will download all required jars and code to ensure `gradle` will run smoothly, however, valid java jdk installation is required
3. If all went well, `Task :run` should output `Server is listening at 0.0.0.0/0.0.0.0:some_port on port some_port`, this means that the server is up and running
## Client

### Language
- [Python 3.10.0+](https://docs.python.org/3/)

### Libaries
- [socket](https://docs.python.org/3/library/socket.html)
- [select](https://docs.python.org/3/library/select.htm)
- [sys](https://docs.python.org/3/library/sys.html)
- [json](https://docs.python.org/3/library/json.html)

### How to Run
1. Install python
    - ensure python is **3.10.0** or later
    - does not run on Windows, see [reasoning](https://docs.python.org/3/library/select.html#:~:text=Note%20that%20on%20Windows%2C%20it%20only%20works%20for%20sockets%3B%20on%20other%20operating%20systems%2C%20it%20also%20works%20for%20other%20file%20types%20(in%20particular%2C%20on%20Unix%2C%20it%20works%20on%20pipes).%20It%20cannot%20be%20used%20on%20regular%20files%20to%20determine%20whether%20a%20file%20has%20grown%20since%20it%20was%20last%20read.)
        - `select.select` does not work with `sys.stdin` on Windows
2. run `python3 client.py`
3. Input **your IP** or 127.0.0.1 (Local Host)
4. Input **port number** used to run the socket in backend above ^
5. Input your **Username** for the Bulletin Board

### Useability Instructions
- valid commands are: join, leave, send, show, users, groups, exit, and help
    - [ join group_id ]: joins or switches to group_id
    - [ leave ]: leaves current group and returns to last group joined
    - [ send subject message ]: sends message to current group
    - [ show message_id ]: shows the message with associated message id in the current group
    - [ users ]: shows a list of users in the current group
    - [ groups ]: shows a list of groups able to be joined
    - [ exit ]: disconnects from the server and shuts down the app

## Issues Encountered

### Client side issues
Originally the client side was going to be a React app Kai made for the GUI that has the same functionality `client.py` currently has. The plan was to do this using [socket.io](https://socket.io/) to communicate to the raw sockets hosted by the Kotlin backend. By the time Kai finished most of React app, when Kai tried to test with with the Kotlin backend Jaran and Kai found out that socket.io and Javascript can only communicate with Websockets, not raw sockets ([reason](https://security.stackexchange.com/a/100838)). This was a major problem because the backend Kotlin that Jaran wrote expected a JSON to be recieved from the client through TCP, and the websocket Kai was communicating with in socket.io can only send a HTTP protocol since it was a websocket not a raw socket. This made it very inconvienient to communicate with the backend. Eventually we decided that was possible but very inconvient to get the front-end working with the backend, so instead Kai created the terminal app `client.py` to communicate with the Kotlin backend. In hindsight Kai should have tested with Jaran's backend long before he continued creating the React app. If there had been better communication or more testing between Kai and Jaran's work, this could have been avoided and we could've had a GUI for this project.


### Server side issues
The main issue server side was JSON encoding and decoding. Due to the dynamic nature of receiving JSON and the statically typed nature of Kotlin, the JSON needed to be ensured to be in the correct format upon receiving the JSON from the socket. This could be validated and assumed correct as we had full control over how the JSON would be structured. This limitiation of the statically typed nature of Kotlin led to the JSON structure being very rigid and not flexible leading to many structural redesigns as the scope of the project increased.