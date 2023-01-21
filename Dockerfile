FROM  --platform=linux/amd64 openjdk:17

#镜像的制作人
MAINTAINER csuduan@qq.com

ENV LD_LIBRARY_PATH=/opt/mts/lib

RUN mkdir /opt/mts/lib/ -p
RUN mkdir /opt/mts/app/ -p
#工作目录
#WORKDIR /opt/mts/app/

RUN set -x && \
echo "#!/bin/bash" > entrypoint.sh && \
echo "java \$JAVA_OPTS \$JAVA_AGENT -Djava.library.path=/opt/mts/lib -jar app.jar" >> entrypoint.sh && \
chmod +x entrypoint.sh

#声明了容器应该打开的端口并没有实际上将它打开
EXPOSE 8091

#拷贝本地文件到镜像中
COPY mts.sqlite  /opt/mts/data/mts.sqlite
COPY mts-admin/target/mts-admin-*.jar app.jar
COPY mts-adapter/src/main/resources/lib/ctp19/* /opt/mts/lib

ENTRYPOINT ["./entrypoint.sh"]
