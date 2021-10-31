#
# Makefile for development of the safequietdown-plugin
#

M2_CACHEDIR ?= $(abspath $(HOME)/.m2)
TMPDIR := $(shell mktemp -u)
SRCDIR = $(abspath $(dir $(lastword $(MAKEFILE_LIST))))
MAVEN_IMAGE = maven:3.8.3-jdk-8

help:
	@echo "Makefile for Development of SafeQuietdown Plugin"
	@echo "-----------------------------------------------"
	@echo
	@echo "Targets:"
	@echo "  build            - Build the plugin"
	@echo "  clean            - Clean the workspace"
	@echo "  help             - Print this help"
	@echo "  site             - Generate the reports"
	@echo "  test             - Run only the test specified by the TEST variable."
	@echo "  release-prepare  - Prepare the release."
	@echo
	@echo "Examples:"
	@echo "  make clean build site"
	@echo "  M2_CACHEDIR=/tmp/m2 make build"
	@echo "  TEST=com.clemensrabe.jenkins.plugins.safequietdown.SafeQuietdownConfigurationTest#testAllowAllQueuedItemsSetting make test"
	@echo
	@echo "Settings:"
	@echo "  M2_CACHEDIR = $(M2_CACHEDIR)"
	@echo "  TMPDIR      = $(TMPDIR)"
	@echo "  SRCDIR      = $(SRCDIR)"
	@echo "  MAVEN_IMAGE = $(MAVEN_IMAGE)"


build: clean
	@mkdir -p "$(M2_CACHEDIR)" "$(TMPDIR)"
	@docker run --rm -ti \
		-v "$(TMPDIR)":"$(HOME)" \
		-v "$(M2_CACHEDIR)":"$(HOME)/.m2" \
		-v "$(SRCDIR)":/usr/src/mymaven \
		-v "$(HOME)/.gitconfig":"$(HOME)/.gitconfig" \
		-u $(shell id -u):$(shell id -g) \
		-e MAVEN_CONFIG="$(HOME)/.m2" \
		-e HOME="$(HOME)" \
		-w /usr/src/mymaven \
		$(MAVEN_IMAGE) \
		mvn -Duser.home="$(HOME)" clean package \
		-Dmaven.compiler.showDeprecation=true \
		-Dmaven.compiler.showWarnings=true
	@rm -rf "$(TMPDIR)"


site:
	@mkdir -p "$(M2_CACHEDIR)" "$(TMPDIR)"
	@docker run --rm -ti \
		-v "$(TMPDIR)":"$(HOME)" \
		-v "$(M2_CACHEDIR)":"$(HOME)/.m2" \
		-v "$(SRCDIR)":/usr/src/mymaven \
		-v "$(HOME)/.gitconfig":"$(HOME)/.gitconfig" \
		-u $(shell id -u):$(shell id -g) \
		-e MAVEN_CONFIG="$(HOME)/.m2" \
		-e HOME="$(HOME)" \
		-w /usr/src/mymaven \
		$(MAVEN_IMAGE) \
		mvn -Duser.home="$(HOME)" site
	@rm -rf "$(TMPDIR)"


test:
	@mkdir -p "$(M2_CACHEDIR)" "$(TMPDIR)"
	@docker run --rm -ti \
		-v "$(TMPDIR)":"$(HOME)" \
		-v "$(M2_CACHEDIR)":"$(HOME)/.m2" \
		-v "$(SRCDIR)":/usr/src/mymaven \
		-v "$(HOME)/.gitconfig":"$(HOME)/.gitconfig" \
		-u $(shell id -u):$(shell id -g) \
		-e MAVEN_CONFIG="$(HOME)/.m2" \
		-e HOME="$(HOME)" \
		-w /usr/src/mymaven \
		$(MAVEN_IMAGE) \
		mvn -Duser.home="$(HOME)" -Dtest=$(TEST) test
	@rm -rf "$(TMPDIR)"


release-prepare: clean
	@mkdir -p "$(M2_CACHEDIR)" "$(TMPDIR)"
	@docker run --rm -ti \
		-v "$(TMPDIR)":"$(HOME)" \
		-v "$(M2_CACHEDIR)":"$(HOME)/.m2" \
		-v "$(SRCDIR)":/usr/src/mymaven \
		-v "$(HOME)/.gitconfig":"$(HOME)/.gitconfig":ro \
		-v "$(HOME)/.git-credentials":"$(HOME)/.git-credentials":ro \
		-u $(shell id -u):$(shell id -g) \
		-e MAVEN_CONFIG="$(HOME)/.m2" \
		-e HOME="$(HOME)" \
		-w /usr/src/mymaven \
		$(MAVEN_IMAGE) \
		mvn -Duser.home="$(HOME)" --batch-mode release:prepare
	@rm -rf "$(TMPDIR)"


clean:
	@find . -iname "*~" -exec rm -f {} \;
	@rm -rf target
