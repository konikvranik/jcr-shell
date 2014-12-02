#!/bin/sh

EXECUTABLE=$0
CONFIG=$1
REPOSITORIES="/var/tmp/jcrrepositories"
CONFIG_HOME="/etc/local/jcr"
VERSION="0.0.2-SNAPSHOT"
LD_LIBRARY_PATH=/usr/lib

JCR_HOME="${REPOSITORIES}/${CONFIG}_console1"
JCR_CONFIG="${CONFIG_HOME}/${CONFIG}_console1.xml"

if [ "$1" == "-l" ]; then
	cd "${CONFIG_HOME}"
	ls -1 *.xml | sed -nr 's/^(.*)_console1.xml$/\1/p'
	exit
fi

shift

[ -L "$EXECUTABLE" ] && EXECUTABLE=`file -b $EXECUTABLE | sed -rn "s/^symbolic link to \\\`(.*)'$/\1/p"`
#echo "EXECUTABLE: $EXECUTABLE"
EXE_DIR=`dirname "$EXECUTABLE"`
#echo "EXE_DIR: $EXE_DIR"
JCR_SHELL_HOME="$( cd "$EXE_DIR/..";  pwd )"
#echo "JCR_SHELL_HOME: $JCR_SHELL_HOME"
[ -e "${JCR_HOME}" ] || mkdir -p "${JCR_HOME}"
JAVA_OPT="-Djava.library.path=${LD_LIBRARY_PATH} -Djcr.home=${JCR_HOME} -Djcr.config=${JCR_CONFIG}"
echo "java ${JAVA_OPT} -jar ${JCR_SHELL_HOME}/lib/jcr_shell-${VERSION}-jar-with-dependencies.jar $@"
java ${JAVA_OPT} -jar ${JCR_SHELL_HOME}/lib/jcr_shell-${VERSION}-jar-with-dependencies.jar $@
