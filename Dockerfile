# docker build -t registry.cn-hangzhou.aliyuncs.com/tarzanx/maxkb4j:latest .
# docker tag  registry.cn-hangzhou.aliyuncs.com/tarzanx/maxkb4j:latest  registry.cn-hangzhou.aliyuncs.com/tarzanx/maxkb4j:2.0

#基础镜像为java17
FROM amazoncorretto:17

#作者签名
LABEL maintainer="tarzan <1334512682@qq.com>"

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 删除之前的镜像文件
RUN rm -rf /opt/running/MaxKB4j*

#拷贝jar包，到容器内的指定位置
ADD ./target/MaxKB4j.jar  /opt/running/MaxKB4j.jar

#容器对外映射端口
EXPOSE 8080

# 切换到jar包文件夹下
WORKDIR /opt/running/

#运行启动命令
CMD ["java", "-jar", "-Dfile.encoding=UTF-8", "MaxKB4j.jar"]

