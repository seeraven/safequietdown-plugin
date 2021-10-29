#
# Makefile for development of the safequietdown-plugin
#

M2_CACHEDIR ?= $(abspath $(dir $(lastword $(MAKEFILE_LIST)))/../m2)
SRCDIR = $(abspath $(dir $(lastword $(MAKEFILE_LIST))))
MAVEN_IMAGE = maven:3.8.3-jdk-8

help:
	@echo "Makefile for Development of SafeQuietdown Plugin"
	@echo "-----------------------------------------------"
	@echo
	@echo "Targets:"
	@echo "  build  - Build the plugin"
	@echo "  clean  - Clean the workspace"
	@echo "  test   - Run only the test specified by the TEST variable."
	@echo
	@echo "Examples:"
	@echo "  make clean build site"
	@echo "  M2_CACHEDIR=/tmp/m2 make build"
	@echo "  TEST=com.clemensrabe.jenkins.plugins.safequietdown.SafeQuietdownConfigurationTest#testAllowAllQueuedItemsSetting make test"
	@echo
	@echo "Settings:"
	@echo "  M2_CACHEDIR = $(M2_CACHEDIR)"
	@echo "  SRCDIR      = $(SRCDIR)"
	@echo "  MAVEN_IMAGE = $(MAVEN_IMAGE)"


build:
	@mkdir -p "$(M2_CACHEDIR)"
	@docker run --rm -ti \
		-v "$(M2_CACHEDIR)":/root/.m2 \
		-v "$(SRCDIR)":/usr/src/mymaven \
		-w /usr/src/mymaven \
		$(MAVEN_IMAGE) \
		mvn clean package \
		-Dmaven.compiler.showDeprecation=true \
		-Dmaven.compiler.showWarnings=true


site:
	@mkdir -p "$(M2_CACHEDIR)"
	@docker run --rm -ti \
		-v "$(M2_CACHEDIR)":/root/.m2 \
		-v "$(SRCDIR)":/usr/src/mymaven \
		-w /usr/src/mymaven \
		$(MAVEN_IMAGE) \
		mvn site


test:
	@mkdir -p "$(M2_CACHEDIR)"
	@docker run --rm -ti \
		-v "$(M2_CACHEDIR)":/root/.m2 \
		-v "$(SRCDIR)":/usr/src/mymaven \
		-w /usr/src/mymaven \
		$(MAVEN_IMAGE) \
		mvn -Dtest=$(TEST) test

clean:
	@find . -iname "*~" -exec rm -f {} \;
	@sudo rm -rf target
