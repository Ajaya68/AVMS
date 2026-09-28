# Build the React SPA and serve it with nginx.
# Pass the public backend URL at build time:
#   docker build --build-arg VITE_API_URL=https://api.example.com/api -t avms-frontend ./frontend
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_API_URL=http://localhost:8081/api
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
