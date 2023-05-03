.PHONY:

# ==============================================================================
# Docker

local:
	@echo Starting local docker compose
	docker-compose -f docker-compose.local.yaml up -d --build

develop:
	@echo Building application
	./gradlew clean build -x test
	@echo Starting docker compose
	docker-compose -f docker-compose.yaml up -d --build


# ==============================================================================
# Docker and k8s support grafana - prom-operator

FILES := $(shell docker ps -aq)

down-local:
	docker stop $(FILES)
	docker rm $(FILES)

clean:
	docker system prune -f

logs-local:
	docker logs -f $(FILES)