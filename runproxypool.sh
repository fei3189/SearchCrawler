#!/bin/bash
JAVA='/usr/bin/java'

COMMAND="$JAVA -cp bin/:lib/xmlrpc-server-3.1.3.jar:lib/htmlparser.jar:lib/mongo-2.9.2.jar:lib/htmllexer.jar:lib/xmlrpc-client-3.1.3.jar:lib/xmlrpc-common-3.1.3.jar:lib/ws-commons-util-1.0.2.jar:lib/commons-httpclient-3.1.0.jar:lib/commons-logging-1.1.jar:lib/commons-codec-1.4.jar com.felixjiang.main.StartService"
if [ "$1" == "nohup" ]; then
  DATESTR=`date +"%Y%m%d%H%M%S"`
  nohup $COMMAND 1>/dev/null 2>>log/weibosearch.log.$DATESTR &
  touch exe.nohup.$DATESTR
  rm exe.nohup.$DATESTR
else
  $COMMAND
fi
