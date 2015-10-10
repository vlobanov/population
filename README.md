Population radar
================

Demo:

[Mumbai 30 km](http://ec2-52-29-0-69.eu-central-1.compute.amazonaws.com/city/population?city=Mumbai&radius=12)
[nl, amsterdam 12 km](http://ec2-52-29-0-69.eu-central-1.compute.amazonaws.com/city/population?city=nl,amsterdam&radius=12)
[us, NY, new york 300 km](http://ec2-52-29-0-69.eu-central-1.compute.amazonaws.com/city/population?city=us,NY,new%20york&radius=300)

Launching:
Install Docker, then exec:
`wget https://raw.githubusercontent.com/vlobanov/population/master/docker/Dockerfile`
`docker build -t lobanovadik/population .`
`docker run -p 80:8080 -it lobanovadik/population`
(or replace `80` with any port for service to listen on)