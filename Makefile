.PHONY: run build test infra-up infra-down infra-status docker-build docker-run clean package

# 运行项目（需本机已启动 MySQL/Redis，或先 make infra-up）
run:
	mvn spring-boot:run

# 编译（跳过测试）
build:
	mvn compile

# 打包
package:
	mvn package -DskipTests

# 运行测试
test:
	mvn test

# 启动基础设施（MySQL + Redis）
infra-up:
	@if [ -f .env ]; then \
		docker compose --env-file .env up -d; \
	else \
		docker compose --env-file .env.example up -d; \
	fi

# 停止基础设施
infra-down:
	docker compose down

# 查看基础设施状态
infra-status:
	docker compose ps

# Docker 构建应用镜像
docker-build:
	docker build -t myreport .

# Docker 运行应用（依赖宿主机已映射的 MySQL/Redis 端口，或自行改网络）
docker-run:
	docker run -p 9091:9091 --env-file .env  myreport

# 清理编译产物
clean:
	mvn clean
