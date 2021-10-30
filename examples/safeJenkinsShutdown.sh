#!/bin/bash -eu
#
# Save shutdown of Jenkins server using the safe quietdown plugin.
#

USER=clemens
TOKEN=11852fb70cd9b8c574f0c3abc166e73e42
JENKINS_URL=http://localhost:8080/

TMPDIR=$(mktemp -d)
cd $TMPDIR
wget ${JENKINS_URL}/jnlpJars/jenkins-cli.jar

echo "Activating safe quietdown mode."
java -jar jenkins-cli.jar \
     -s ${JENKINS_URL} \
     -auth ${USER}:${TOKEN} \
     safe-quiet-down -a -m "Jenkins is going to shutdown for maintenance"

echo "Waiting for Jenkins jobs to finish..."

function finishedSafeQuietDown() {
    java -jar jenkins-cli.jar \
         -s ${JENKINS_URL} \
         -auth ${USER}:${TOKEN} \
         finished-safe-quiet-down
}

NUM_OKS=0
while [[ ${NUM_OKS} -lt 3 ]]; do
    sleep 10s
    if finishedSafeQuietDown; then
        echo "There seem to be no jobs left!"
        let NUM_OKS=${NUM_OKS}+1
    else
        echo "... still working ..."
        NUM_OKS=0
    fi
done

echo "Everything finished. Shutting down Jenkins now!"
java -jar jenkins-cli.jar \
     -s ${JENKINS_URL} \
     -auth ${USER}:${TOKEN} \
     shutdown

cd
rm -rf $TMPDIR
