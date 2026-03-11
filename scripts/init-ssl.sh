set -e

DOMAIN="${DOMAIN:?'DOMAIN 환경변수를 설정하세요. ex) DOMAIN=api.sparring.kr'}"
EMAIL="${EMAIL:?'EMAIL 환경변수를 설정하세요. ex) EMAIL=admin@sparring.kr'}"
DEPLOY_DIR="$HOME/server"

echo "=============================="
echo "  Let's Encrypt SSL 초기 설정"
echo "  도메인: $DOMAIN"
echo "  이메일: $EMAIL"
echo "=============================="

# 1. Certbot 디렉토리 생성
mkdir -p "$DEPLOY_DIR/certbot/www"
mkdir -p "$DEPLOY_DIR/certbot/conf"

# 2. nginx.ssl.conf에서 도메인 치환
echo ">> nginx.ssl.conf 도메인 설정..."
sed "s/\${DOMAIN}/$DOMAIN/g" "$DEPLOY_DIR/nginx/nginx.ssl.conf" > "$DEPLOY_DIR/nginx/nginx.ssl.conf.tmp"
mv "$DEPLOY_DIR/nginx/nginx.ssl.conf.tmp" "$DEPLOY_DIR/nginx/nginx.ssl.conf.applied"

# 3. HTTP용 nginx 먼저 기동 (Certbot 챌린지용)
echo ">> HTTP nginx 기동 (인증서 발급용)..."
docker run --rm -d \
  --name nginx-certbot-temp \
  -p 80:80 \
  -v "$DEPLOY_DIR/certbot/www:/var/www/certbot" \
  -v "$DEPLOY_DIR/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" \
  nginx:alpine

sleep 3

# 4. Certbot으로 인증서 발급
echo ">> Let's Encrypt 인증서 발급..."
docker run --rm \
  -v "$DEPLOY_DIR/certbot/conf:/etc/letsencrypt" \
  -v "$DEPLOY_DIR/certbot/www:/var/www/certbot" \
  certbot/certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    -d "$DOMAIN"

# 5. 임시 nginx 종료
docker stop nginx-certbot-temp 2>/dev/null || true

# 6. SSL 설정으로 nginx 재기동
echo ">> SSL 모드로 전환..."
# .env에 도메인 정보 추가 (없으면 추가)
if ! grep -q "^DOMAIN=" "$DEPLOY_DIR/.env" 2>/dev/null; then
  echo "DOMAIN=$DOMAIN" >> "$DEPLOY_DIR/.env"
fi

# 7. 전체 스택 재기동 (nginx SSL 포함)
docker compose -f "$DEPLOY_DIR/docker-compose-prod.yml" up -d

echo ""
echo "SSL 설정 완료"
echo "   https://$DOMAIN 으로 접근 가능합니다."
echo ""
echo "인증서 자동 갱신은 docker-compose-prod.yml의"
echo "   certbot 컨테이너가 처리합니다. (90일마다 자동 갱신)"
