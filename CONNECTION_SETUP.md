# Connection Setup Guide

## Quick Fix for Connection Issues

### 1. Start the Backend Server
```bash
cd Ai-Application
./mvnw spring-boot:run
# OR if you have Maven installed:
mvn spring-boot:run
```

The backend should start on **http://localhost:8080**

### 2. Start the Frontend Server
```bash
cd Frontend
npm install  # Only needed first time
npm start
```

The frontend should start on **http://localhost:3000**

### 3. Verify Connection

#### Option A: Using Proxy (Recommended)
The `package.json` has been configured with a proxy. The React dev server will automatically proxy API requests to the backend.

- Frontend runs on: http://localhost:3000
- Backend runs on: http://localhost:8080
- API requests from frontend are automatically proxied

#### Option B: Direct Connection
If proxy doesn't work, you can test the backend directly:
```bash
curl http://localhost:8080/api/health
```

Should return: `{"status":"ok","message":"Backend is running"}`

### 4. Test Login
Use the default admin account:
- **Username:** `admin`
- **Password:** `admin`

### 5. Troubleshooting

#### Backend not starting?
- Check if MongoDB is running: `mongod` or `sudo systemctl start mongod`
- Check if port 8080 is available: `lsof -i :8080` or `netstat -an | grep 8080`
- Check backend logs for errors

#### Frontend can't connect?
- Make sure backend is running on port 8080
- Check browser console for CORS errors
- Try accessing http://localhost:8080/api/health directly in browser
- Restart both servers

#### CORS Errors?
- The proxy in `package.json` should handle this automatically
- If still getting CORS errors, check `WebConfig.java` CORS settings
- Make sure both servers are running

### 6. MongoDB Setup (if needed)
```bash
# Start MongoDB (Linux)
sudo systemctl start mongod

# Or if running manually:
mongod --dbpath /path/to/data
```

The application uses MongoDB at: `mongodb://localhost:27017/electrodb`

