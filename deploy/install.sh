#!/usr/bin/env bash
set -euo pipefail

##############################################################################
#  MaxKB4j — One-click Install & Deploy Script (Linux/macOS)
#  Supports: Docker-Compose (prebuilt image) | Source build -> image -> compose
##############################################################################

# ------------------------------ Colors ------------------------------------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; NC='\033[0m' # No Color
log_info()  { echo -e "${CYAN}[INFO]${NC}  $*"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ------------------------------ Constants ---------------------------------
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
START_DIR="${PROJECT_DIR}/maxkb4j-start"
JAR_PATH="${START_DIR}/target/maxkb4j-start.jar"
DOCKERFILE_PATH="${START_DIR}/Dockerfile"
LOG_DIR="${PROJECT_DIR}/logs"
DOCKER_COMPOSE_FILE="${PROJECT_DIR}/docker-compose.yml"
DOCKER_COMPOSE_DEV_FILE="${PROJECT_DIR}/docker-compose.dev.yml"
DEFAULT_PORT=8080
APP_IMAGE="registry.cn-hangzhou.aliyuncs.com/tarzanx/maxkb4j:latest"

# Resolve docker compose command once
COMPOSE_CMD="docker compose"
if ! docker compose version &>/dev/null 2>&1; then
    if command -v docker-compose &>/dev/null; then
        COMPOSE_CMD="docker-compose"
    fi
fi

# ===========================================================================
#  Prerequisite Checks
# ===========================================================================
check_docker() {
    echo ""
    log_info "Checking Docker..."
    if ! command -v docker &>/dev/null; then
        log_error "Docker not found. Please install Docker first."
        log_info "  https://docs.docker.com/engine/install/"
        exit 1
    fi
    log_ok "Docker found: $(docker --version)"
}

check_java() {
    log_info "Checking Java..."
    if ! command -v java &>/dev/null; then
        log_error "Java not found. Please install JDK 21+."
        log_info "  https://adoptium.net/"
        exit 1
    fi
    log_ok "Java found: $(java -version 2>&1 | head -1)"
}

check_maven() {
    log_info "Checking Maven..."
    if ! command -v mvn &>/dev/null; then
        log_error "Maven not found. Please install Maven 3.8+."
        log_info "  https://maven.apache.org/download.cgi"
        exit 1
    fi
    log_ok "Maven found: $(mvn --version 2>&1 | head -1)"
}

# ===========================================================================
#  Helper: Prepare host directories used as bind mounts
# ===========================================================================
prepare_dirs() {
    mkdir -p "${LOG_DIR}"
    mkdir -p "${PROJECT_DIR}/postgres/data"
    mkdir -p "${PROJECT_DIR}/mongo/data"
    mkdir -p "${PROJECT_DIR}/mongo/configdb"
}

# ===========================================================================
#  Helper: Bring up the docker-compose stack and report status
#     Arg $1 = "pull" to pull image first, "nopull" to use local image only
# ===========================================================================
compose_up() {
    cd "${PROJECT_DIR}"
    if [[ "${1:-}" == "pull" ]]; then
        log_info "Pulling images..."
        if ! ${COMPOSE_CMD} -f docker-compose.yml pull; then
            log_warn "Pull failed, will use existing images if available."
        fi
    fi

    log_info "Starting services..."
    if ! ${COMPOSE_CMD} -f docker-compose.yml up -d; then
        log_error "docker compose up failed."
        exit 1
    fi

    log_info "Waiting for services..."
    sleep 10

    for c in "maxkb4j-pgvector" "maxkb4j-mongo" "maxkb4j-app"; do
        if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${c}$"; then
            log_ok "Container ${c} is running."
        else
            log_warn "Container ${c} not running. Check: docker logs ${c}"
        fi
    done
}

# ===========================================================================
#  Mode 1: Docker Compose with prebuilt remote image
# ===========================================================================
deploy_docker() {
    clear 2>/dev/null || true
    echo "============================================="
    echo " Mode: Docker Compose (Prebuilt Image)"
    echo "============================================="
    echo ""

    check_docker

    if [ ! -f "${DOCKER_COMPOSE_FILE}" ]; then
        log_error "docker-compose.yml not found."
        exit 1
    fi

    prepare_dirs
    compose_up pull

    echo ""
    log_ok "=== Deployment complete! ==="
    echo ""
    log_info " Web UI:  http://localhost:${DEFAULT_PORT}/admin/login"
    log_info " Default: admin / tarzan@123456"
    echo ""
    log_info " View logs:  ${COMPOSE_CMD} -f docker-compose.yml logs -f"
    log_info " Stop all:   ${COMPOSE_CMD} -f docker-compose.yml down"
    echo ""
}

# ===========================================================================
#  Mode 2: Build source -> local image -> deploy via docker-compose
# ===========================================================================
deploy_build() {
    clear 2>/dev/null || true
    echo "============================================="
    echo " Mode: Source Build -> Image -> Compose"
    echo "============================================="
    echo ""

    check_docker
    check_java
    check_maven

    if [ ! -f "${DOCKER_COMPOSE_FILE}" ]; then
        log_error "docker-compose.yml not found."
        exit 1
    fi

    if [ ! -f "${DOCKERFILE_PATH}" ]; then
        log_error "Dockerfile not found at ${DOCKERFILE_PATH}"
        exit 1
    fi

    echo ""
    log_info "Step 1: Building project with Maven..."
    log_info "This may take a few minutes..."
    cd "${PROJECT_DIR}"
    if ! mvn clean package -pl maxkb4j-start -am -DskipTests; then
        log_error "Build failed. Check the output above."
        exit 1
    fi
    log_ok "Maven build successful."

    if [ ! -f "${JAR_PATH}" ]; then
        log_error "JAR not found at ${JAR_PATH}"
        exit 1
    fi

    echo ""
    log_info "Step 2: Building Docker image ${APP_IMAGE} ..."
    cd "${START_DIR}"
    if ! docker build -t "${APP_IMAGE}" .; then
        log_error "Docker image build failed."
        exit 1
    fi
    log_ok "Docker image built: ${APP_IMAGE}"

    echo ""
    log_info "Step 3: Deploying via docker-compose (using local image)..."
    prepare_dirs
    compose_up nopull

    echo ""
    log_ok "=== Deployment complete! ==="
    echo ""
    log_info " Web UI:  http://localhost:${DEFAULT_PORT}/admin/login"
    log_info " Default: admin / tarzan@123456"
    echo ""
    log_info " View logs:  ${COMPOSE_CMD} -f docker-compose.yml logs -f"
    log_info " Stop all:   ${COMPOSE_CMD} -f docker-compose.yml down"
    echo ""
}

# ===========================================================================
#  Uninstall
# ===========================================================================
uninstall() {
    clear 2>/dev/null || true
    echo "============================================="
    echo " Uninstall MaxKB4j"
    echo "============================================="
    echo ""
    log_warn "This will stop all containers and optionally remove data."
    read -r -p "Continue? [y/N]: " confirm
    if [[ "${confirm,,}" != "y" ]]; then
        log_info "Uninstall cancelled."
        return 0
    fi

    log_info "Stopping Docker Compose services..."
    cd "${PROJECT_DIR}" 2>/dev/null || true
    if [ -f "${DOCKER_COMPOSE_FILE}" ]; then
        ${COMPOSE_CMD} -f docker-compose.yml down 2>/dev/null || true
    fi
    if [ -f "${DOCKER_COMPOSE_DEV_FILE}" ]; then
        ${COMPOSE_CMD} -f docker-compose.dev.yml down 2>/dev/null || true
    fi

    read -r -p "Remove database data volumes? [y/N]: " rm_data
    if [[ "${rm_data,,}" == "y" ]]; then
        log_info "Removing data volumes..."
        ${COMPOSE_CMD} -f docker-compose.yml down -v 2>/dev/null || true
        ${COMPOSE_CMD} -f docker-compose.dev.yml down -v 2>/dev/null || true
        rm -rf "${PROJECT_DIR}/postgres/data" "${PROJECT_DIR}/mongo" 2>/dev/null || true
    fi

    read -r -p "Remove logs? [y/N]: " rm_logs
    if [[ "${rm_logs,,}" == "y" ]]; then
        rm -rf "${LOG_DIR}" 2>/dev/null || true
    fi

    log_ok "Uninstall complete."
}

# ===========================================================================
#  Main menu (loop until user picks a valid action)
# ===========================================================================
main() {
    while true; do
        clear 2>/dev/null || true
        echo "=============================================="
        echo "   MaxKB4j -- One-click Install & Deploy"
        echo "=============================================="
        echo ""
        echo "Select deployment mode:"
        echo ""
        echo "  1) Docker Compose (Prebuilt Image)"
        echo "      -- Fast, pulls remote image (recommended)"
        echo ""
        echo "  2) Source Build -> Image -> Docker Compose"
        echo "      -- Build from source into a local image,"
        echo "         then deploy via docker-compose"
        echo ""
        echo "  3) Uninstall"
        echo "      -- Stop and remove"
        echo ""
        read -r -p "Enter choice [1-3]: " mode_choice

        case "${mode_choice}" in
            1) deploy_docker; exit 0 ;;
            2) deploy_build;  exit 0 ;;
            3) uninstall;     continue ;;
            *) log_error "Invalid choice."; sleep 2 ;;
        esac
    done
}

main "$@"
