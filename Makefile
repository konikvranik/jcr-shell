INSTALL_PREFIX="/opt/jcr-shell"
VERSION="0.0.2-SNAPSHOT"
LIB_PERMISSONS="755"
BIN_PERMISSONS="755"

TARGETS=target/jcr_shell-${VERSION}-jar-with-dependencies.jar target/jcr_shell-${VERSION}.jar

default: all

all: build install

build: ${TARGETS}

install: ${TARGETS}
	mkdir -p ${INSTALL_PREFIX}/bin
	mkdir -p ${INSTALL_PREFIX}/lib
	cp bin/jcr-shell.sh ${INSTALL_PREFIX}/bin
	cp target/jcr_shell-${VERSION}-jar-with-dependencies.jar ${INSTALL_PREFIX}/lib
	chmod ${BIN_PERMISSIONS} ${INSTALL_PREFIX}/bin/*
	chmod ${LIB_PERMISSIONS} ${INSTALL_PREFIX}/lib/*

clean:
	mvn clean

${TARGETS}: src/main/**/*
	mvn assembly:assembly

deploy:
	mvn install

.PHONY: deploy clean install build all default
