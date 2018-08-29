# Docker for Verwandlung Online Judge

## Build Docker Image for Web Application

```
cd web
docker build -t "zjhzxhz/voj.web" .
```

## Run Web Application

```
docker run -d -p 3306:3306 -p 8080:8080 -p 61616:61616 zjhzxhz/voj.web
```