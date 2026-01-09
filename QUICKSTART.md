# Quick Start Guide

## 🚀 Getting Started in 3 Steps

### Step 1: Start the Application

```bash
# Navigate to project directory
cd URL-Shortner

# Start all services with Docker Compose
docker-compose up

# Wait for services to be healthy (30-60 seconds)
# You'll see: "Application startup complete"
```

### Step 2: Access the Application

- **Frontend**: http://localhost:3000
- **Backend API Docs**: http://localhost:8000/docs
- **Health Check**: http://localhost:8000/health

### Step 3: Test the API

#### Create a Short URL (No Auth Required)

```bash
curl -X POST "http://localhost:8000/api/urls" \
  -H "Content-Type: application/json" \
  -d '{
    "original_url": "https://github.com/your-username/url-shortener",
    "custom_alias": "my-project"
  }'
```

#### Test the Redirect

```bash
# Visit in browser or use curl
curl -L "http://localhost:8000/my-project"
```

## 📝 Next Steps

1. **Register a User**
   - Go to http://localhost:8000/docs
   - Try `POST /auth/register`
   - Then `POST /auth/login` to get a JWT token

2. **Create Authenticated URLs**
   - Use the JWT token in Authorization header
   - Create URLs with custom aliases
   - View your URLs with `GET /api/urls`

3. **View Analytics**
   - Click your short URL a few times
   - Check analytics with `GET /api/urls/{id}/analytics`

4. **Generate QR Codes**
   - Visit `GET /api/urls/{id}/qr` in browser
   - Download the QR code image

## 🛠️ Development Commands

```bash
# Backend only
cd backend
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
bash create_env.sh
alembic upgrade head
uvicorn app.main:app --reload

# Frontend only
cd frontend
npm install
npm run dev

# Run tests (when implemented)
cd backend && pytest
cd frontend && npm test
```

## 🐛 Troubleshooting

### Port Already in Use
```bash
# Stop existing containers
docker-compose down

# Or change ports in docker-compose.yml
```

### Database Connection Error
```bash
# Reset database
docker-compose down -v
docker-compose up
```

### Redis Connection Error
```bash
# Check Redis is running
docker-compose ps

# Restart Redis
docker-compose restart redis
```

## 📚 Learn More

- [README.md](README.md) - Full documentation
- [ARCHITECTURE.md](ARCHITECTURE.md) - System design
- API Docs: http://localhost:8000/docs (when running)
