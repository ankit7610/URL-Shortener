# Copy .env.example to .env and update with your values
cp ../.env.example .env

# Create local .env for development
cat > .env << EOF
DATABASE_URL=postgresql+asyncpg://urlshortener:urlshortener@localhost:5432/urlshortener
REDIS_URL=redis://localhost:6379/0
JWT_SECRET_KEY=$(openssl rand -hex 32)
ENVIRONMENT=development
DEBUG=true
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001
EOF

echo ".env file created successfully!"
