#!/bin/bash
#JAVAC='/usr/lib/jvm/jdk1.6.0_34/bin/javac'
JAVAC='/usr/bin/javac'

find -name "*.java" > WeiboSearchSources.temp
$JAVAC -cp lib/libsvm.jar:lib/xmlrpc-server-3.1.3.jar:lib/htmlparser.jar:lib/mongo-2.9.2.jar:lib/htmllexer.jar:lib/xmlrpc-client-3.1.3.jar:lib/xmlrpc-common-3.1.3.jar:lib/ws-commons-util-1.0.2.jar:lib/commons-httpclient-3.1.0.jar:lib/commons-logging-1.1.jar:lib/commons-codec-1.4.jar -d bin/ @WeiboSearchSources.temp
rm WeiboSearchSources.temp
