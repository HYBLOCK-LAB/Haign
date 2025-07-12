-include .env

APDU_TEST_SCRIPT := /card-wallet/scripts/test/apdu_test.sh

auth: ## login to docker hub
	@echo "$(DOCKER_TOKEN)" | docker login -u $(DOCKER_USERNAME) --password-stdin

pull: auth ## Pull Containers
	@docker compose pull

build: auth ## Build Containers
	@docker compose build

rebuild: auth ## Build Containers
	@docker compose build --no-cache --pull
	

build-applet: auth  ## Build Applet Containers
	@docker compose build applet
	@docker compose up applet

logs: ## show logs
	@docker compose logs -f

help: ## Show help message
	@awk 'BEGIN {FS = ":.*?## "}; /^[a-zA-Z0-9_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

%:
	@: