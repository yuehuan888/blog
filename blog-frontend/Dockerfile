# Stage 1: Build Nuxt
FROM node:22-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ENV NUXT_PUBLIC_API_BASE=
RUN npm run build

# Stage 2: Run Nuxt preview server
FROM node:22-alpine
WORKDIR /app
COPY --from=builder /app/.output /app/.output
EXPOSE 3000
ENV NITRO_HOST=0.0.0.0
ENV NITRO_PORT=3000
CMD ["node", ".output/server/index.mjs"]
