# Docker for Verwandlung Online Judge

## Build Docker Image for Web Application

```
cd web
docker build -t "zjhzxhz/voj.web" .
```

You can also pull it from [Docker Hub](https://hub.docker.com/r/zjhzxhz/voj.web/).

```
docker pull zjhzxhz/voj.web
```

## Run Web Application

```
docker run -d --name voj.web -p 8080:8080 zjhzxhz/voj.web
```

The web application is available at [http://localhost:8080/voj](http://localhost:8080/voj).

## Build Docker Image for Judger Application

```
cd judger
docker build -t "zjhzxhz/voj.judger" .
```

You can also pull it from [Docker Hub](https://hub.docker.com/r/zjhzxhz/voj.judger/).

```
docker pull zjhzxhz/voj.judger
```

## Run Judger Application

```
docker run -d --name voj.judger --link voj.web zjhzxhz/voj.judger
```
