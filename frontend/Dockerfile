# ════════════════════════════════════════════
# STAGE 1 — BUILD
# Compile l'application Angular en mode
# production avec Node.js
# ════════════════════════════════════════════
FROM node:22-alpine AS build

WORKDIR /app

# Copie uniquement package.json/package-lock.json
# d'abord — permet de mettre en cache npm install
# tant que les dépendances ne changent pas
COPY package*.json ./
RUN npm ci

# Copie le reste du code source et build
COPY . .
RUN npm run build -- --configuration production

# ════════════════════════════════════════════
# STAGE 2 — RUNTIME
# Sert les fichiers statiques compilés via
# nginx — image finale légère, sans Node.js
# ════════════════════════════════════════════
FROM nginx:alpine

# Copie la configuration nginx personnalisée
# (gère le routing Angular — voir nginx.conf)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copie les fichiers compilés depuis le stage
# précédent
COPY --from=build /app/dist/kineai-frontend/browser /usr/share/nginx/html

EXPOSE 4200

CMD ["nginx", "-g", "daemon off;"]