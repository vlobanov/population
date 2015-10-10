Population radar
================

Demo
----

[Mumbai 30 km](http://ec2-52-29-0-69.eu-central-1.compute.amazonaws.com/city/population?city=Mumbai&radius=12)

```
{
  "population": 10290537,
  "radius": 12,
  "city": {
    "longitude": 72.825833,
    "latitude": 18.975,
    "region": "16",
    "city_accented": "Mumbai",
    "city": "mumbai",
    "country": "in"
  }
}
```

[nl, amsterdam 12 km](http://ec2-52-29-0-69.eu-central-1.compute.amazonaws.com/city/population?city=nl,amsterdam&radius=12)

```
{
  "population": 848471,
  "radius": 12,
  "city": {
    "longitude": 4.916667,
    "latitude": 52.35,
    "region": "07",
    "city_accented": "Amsterdam",
    "city": "amsterdam",
    "country": "nl"
  }
}
```

[us, NY, new york 300 km](http://ec2-52-29-0-69.eu-central-1.compute.amazonaws.com/city/population?city=us,NY,new%20york&radius=300)

```
{
  "population": 51267817,
  "radius": 300,
  "city": {
    "longitude": -74.0063889,
    "latitude": 40.7141667,
    "region": "NY",
    "city_accented": "New York",
    "city": "new york",
    "country": "us"
  }
}
```

Launching
----

Install Docker, then exec:

```wget https://raw.githubusercontent.com/vlobanov/population/master/docker/Dockerfile```

```docker build -t lobanovadik/population .```

```docker run -p 80:8080 -it lobanovadik/population```
(or replace `80` with any port for service to listen on)