# Build stage
FROM node:20-alpine AS builder
WORKDIR /app

# Copy package files
COPY package.json package-lock.json* ./
RUN npm ci

# Copy source and build (production build, API is proxied by nginx so baseURL stays /api)
COPY . .
RUN npm run build

# Run stage: nginx to serve static files and proxy API
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default static content
RUN rm -rf /usr/share/nginx/html/*

# Copy built assets from builder
COPY --from=builder /app/dist .

# Copy nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
