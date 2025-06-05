PMD_JAVA_OPTS="-javaagent:`$JACOCO_HOME`/lib/jacocoagent.jar=destfile=./jacoco.exec" && \ 
CLASSPATH=./rule.jar ../../../vendor/pmd-bin-6.55.0/bin/run.sh pmd  -f json -d ../after/FQNTest.java -R ./rule.xml

,includes=net.sourceforge.pmd.lang.java.rule.codestyle.UnnecessaryFullyQualifiedNameRule.*