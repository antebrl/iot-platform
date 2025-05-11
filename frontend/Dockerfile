# Build stage
FROM node:20-alpine as build

# Set working directory
WORKDIR /app

# Copy package.json and package-lock.json
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy the rest of the application code
COPY . .

# Build the application
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy build files from build stage to nginx
COPY --from=build /app/dist /usr/share/nginx/html

# Expose port for the application
EXPOSE 80

# Command to run nginx in foreground
CMD ["nginx", "-g", "daemon off;"]
