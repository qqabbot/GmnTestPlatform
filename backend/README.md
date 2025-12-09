
# API Automation Platform

## Startup Commands

### Normal Startup
Run the application using Maven:
```bash
mvn spring-boot:run
```
The application will start on port `7777`.

### Debug Mode
To start the application in debug mode (listening on port `5005`):
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```
You can then attach your IDE's remote debugger to `localhost:5005`.

## Stopping the Application

### If running in foreground
Press `Ctrl + C` in the terminal window where the application is running.

### If running in background or port is occupied
Find the process ID (PID) occupying the port:
```bash
lsof -i :7777
```
Then kill the process:
```bash
kill -9 <PID>
```

## API Documentation
See [api_endpoints.md](file:///Users/xhb/.gemini/antigravity/brain/e77f905b-e4b3-4454-ac29-7438aa607732/api_endpoints.md) for details on available endpoints.
