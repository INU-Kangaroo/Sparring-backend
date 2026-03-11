set -e

DEPLOY_DIR="$HOME/server"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose-prod.yml"
IMAGE_TAG="${IMAGE_TAG}"
GITHUB_REPOSITORY="${GITHUB_REPOSITORY}"
CURRENT_APP_IMAGE=""
ROLLBACK_TAG=""

echo "=============================="
echo "  Sparring 배포 시작"
echo "  TAG: $IMAGE_TAG"
echo "=============================="

# 배포 디렉토리/compose 파일 확인
mkdir -p "$DEPLOY_DIR"

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "ERROR: compose 파일이 없습니다: $COMPOSE_FILE"
  exit 1
fi

REQUIRED_VARS=(
  DB_NAME
  DB_ROOT_PASSWORD
  DB_USERNAME
  DB_PASSWORD
  REDIS_PASSWORD
  JWT_SECRET
  GOOGLE_CLIENT_ID
  GOOGLE_CLIENT_SECRET
  KAKAO_CLIENT_ID
  KAKAO_APP_ID
  MAIL_USERNAME
  MAIL_PASSWORD
  GEMINI_API_KEY
  GEMINI_API_URL
  ML_SERVER_URL
)
for var in "${REQUIRED_VARS[@]}"; do
  if [ -z "${!var}" ]; then
    echo "ERROR: 필수 환경변수가 비어 있습니다: $var"
    exit 1
  fi
done

# 이미지/레포 필수값 확인
if [ -z "$IMAGE_TAG" ]; then
  echo "ERROR: IMAGE_TAG가 비어 있습니다."
  exit 1
fi

if [ -z "$GITHUB_REPOSITORY" ]; then
  echo "ERROR: GITHUB_REPOSITORY가 비어 있습니다."
  exit 1
fi
GITHUB_REPOSITORY="$(echo "$GITHUB_REPOSITORY" | tr '[:upper:]' '[:lower:]')"

# 실행 중인 app 이미지 태그를 롤백 후보로 보관
CURRENT_APP_IMAGE=$(docker ps --filter "name=^/sparring-app$" --format "{{.Image}}" | head -n 1 || true)
if [ -n "$CURRENT_APP_IMAGE" ]; then
  case "$CURRENT_APP_IMAGE" in
    ghcr.io/${GITHUB_REPOSITORY}:*)
      ROLLBACK_TAG="${CURRENT_APP_IMAGE##*:}"
      echo ">> 롤백 후보 태그: $ROLLBACK_TAG"
      ;;
  esac
fi

echo ">> GHCR 로그인..."
echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GITHUB_ACTOR" --password-stdin

echo ">> 이미지 pull..."
docker pull ghcr.io/${GITHUB_REPOSITORY}:${IMAGE_TAG}

# compose에서 참조할 태그 주입
export IMAGE_TAG
export GITHUB_REPOSITORY

# DB/Redis만 먼저 올리고 healthy 대기
echo ">> 의존 서비스 기동..."
docker compose -f "$COMPOSE_FILE" up -d mysql redis

wait_for_health() {
  local service_name="$1"
  local max_retries=24
  local retry_interval=5
  local count=0

  echo ">> ${service_name} 헬스체크 대기..."
  until [ "$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}unknown{{end}}' "sparring-${service_name}" 2>/dev/null)" = "healthy" ]; do
    count=$((count + 1))
    if [ "$count" -ge "$max_retries" ]; then
      echo "ERROR: ${service_name} 헬스체크 실패"
      docker compose -f "$COMPOSE_FILE" logs --tail=100 "${service_name}" || true
      exit 1
    fi
    echo "   대기 중... (${count}/${max_retries})"
    sleep "$retry_interval"
  done
}

wait_for_health "mysql"
wait_for_health "redis"

# app 교체 후 app 자체 health 확인
echo ">> 앱 컨테이너 재시작..."
docker compose -f "$COMPOSE_FILE" up -d --no-deps app

wait_for_health "app"

# app healthy 이후 nginx 기동
echo ">> nginx 기동..."
docker compose -f "$COMPOSE_FILE" up -d --no-deps nginx

# 최종 외부 경로(/actuator/health) 확인
echo ">> 헬스체크 시작..."
MAX_RETRIES=18
RETRY_INTERVAL=5
count=0

until curl -sf http://localhost/actuator/health > /dev/null; do
  count=$((count + 1))
  if [ $count -ge $MAX_RETRIES ]; then
    echo "ERROR: 헬스체크 실패"
    docker compose -f "$COMPOSE_FILE" logs --tail=100 app nginx || true
    if [ -n "$ROLLBACK_TAG" ]; then
      # 실패 시 이전 태그로 app 롤백 후 동일 검증
      echo ">> 이전 버전으로 롤백 시도: $ROLLBACK_TAG"
      export IMAGE_TAG="$ROLLBACK_TAG"
      docker compose -f "$COMPOSE_FILE" up -d --no-deps app
      wait_for_health "app"
      docker compose -f "$COMPOSE_FILE" up -d --no-deps nginx

      echo ">> 롤백 후 헬스체크 재확인..."
      rollback_count=0
      until curl -sf http://localhost/actuator/health > /dev/null; do
        rollback_count=$((rollback_count + 1))
        if [ $rollback_count -ge $MAX_RETRIES ]; then
          echo "ERROR: 롤백 후에도 헬스체크 실패"
          docker compose -f "$COMPOSE_FILE" logs --tail=100 app nginx || true
          exit 1
        fi
        echo "   롤백 대기 중... ($rollback_count/$MAX_RETRIES)"
        sleep $RETRY_INTERVAL
      done
      echo "롤백 성공"
    else
      echo ">> 롤백 가능한 이전 태그가 없어 배포 중단"
    fi
    exit 1
  fi
  echo "   대기 중... ($count/$MAX_RETRIES)"
  sleep $RETRY_INTERVAL
done

echo "배포 완료"

# 사용하지 않는 dangling 이미지 정리
echo ">> 이전 이미지 정리..."
docker image prune -f

echo "=============================="
echo "  배포 성공"
echo "=============================="
