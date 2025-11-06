# Tomcatのベースイメージ
FROM tomcat:9.0-jdk21

# ワーキングディレクトリを用意する
WORKDIR /usr/src/app

# ローカルのディレクトリをコンテナにコピー
COPY ./src ./src
COPY ./WEB-INF ./WEB-INF
COPY ./lib ./lib

# 依存ライブラリをWEB-INF/libにコピーする
COPY ./lib ./WEB-INF/lib

# ソースファイルをコンパイルする
RUN javac -cp "/usr/local/tomcat/lib/servlet-api.jar:/usr/src/app/WEB-INF/lib/*" \
    -d WEB-INF/classes src/DBServlet.java

# WEB-INFディレクトリ自体をアーカイブに含めて.warファイルを作成し、直接デプロイ先に配置する
RUN jar cvf /usr/local/tomcat/webapps/Servlet.war WEB-INF

# 待ち受けるポート番号
EXPOSE 8080

# 起動コマンド
CMD ["catalina.sh", "run"]