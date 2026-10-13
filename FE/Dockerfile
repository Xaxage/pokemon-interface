# syntax=docker/dockerfile:1

# ---- Build: type-check and bundle the React app ----
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Serve: nginx hosts the static bundle and proxies /api to the backend ----
FROM nginx:1.27-alpine
# The official image renders /etc/nginx/templates/*.template with envsubst on start-up.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

ENV BACKEND_URL=http://backend:8080
EXPOSE 80
