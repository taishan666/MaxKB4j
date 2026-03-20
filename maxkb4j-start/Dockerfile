# docker build -t registry.cn-hangzhou.aliyuncs.com/tarzanx/maxkb4j:latest .

#基础镜像为java21
FROM amazoncorretto:21

#作者签名
LABEL maintainer="tarzan <1334512682@qq.com>"

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 删除之前的镜像文件
RUN rm -rf /opt/running/maxkb4j-start*

#拷贝jar包，到容器内的指定位置
ADD ./target/maxkb4j-start.jar  /opt/running/maxkb4j-start.jar

#容器对外映射端口
EXPOSE 8080

# 切换到jar包文件夹下
WORKDIR /opt/running/

#运行启动命令
CMD ["java", "-jar", "-Dfile.encoding=UTF-8", "maxkb4j-start.jar"]

