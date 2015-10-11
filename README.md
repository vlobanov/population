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

[nl, Amsterdam 12 km](http://ec2-52-29-0-69.eu-central-1.compute.amazonaws.com/city/population?city=nl,amsterdam&radius=12)

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

[us, NY, New York 300 km](http://ec2-52-29-0-69.eu-central-1.compute.amazonaws.com/city/population?city=us,NY,new%20york&radius=300)

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

Usage: send GET requiest `/city/population` with parameters `city` and `radius`.

`city` has one of the following formats:
* `Amsterdam`
* `Nl,Amsterdam`
* `us,NY,new york`

See "bonus" section for more on specifying city.

Launching
----

Install Docker, then exec:

```wget https://raw.githubusercontent.com/vlobanov/population/master/docker/Dockerfile```

```docker build -t lobanovadik/population .```

```docker run -p 80:8080 -it lobanovadik/population```
(or replace `80` with any port for service to listen on)

For full installation process see the Dockerfile (it's a bit ugly and is not optimal, but I'm short on time here)

# Design overview


Assumptions
----
Request “within 30km of Mumbai” means within 30km from centre, including Mumbai itself.

It could also mean within 30km of border of Mumbai, or “everything that is closer than 30km to Mumbai border”. 

This is completely different measurement, it would almost never give same results as “within 30km from centre”. In order to implement it we need data about borders, for instance as pairs of coordinates. Also we would need an algorithm to find all points outside the border (i.e. distinguish points inside and outside, which is not trivial, so normals for each line too) and then calculate its surface, and, eventually, population. The area can have arbitrary shape, which makes it further complicated. Still, it can be implemented using data about population density (which is used in the app).
On the other hand, such request is valid for any area, not only ones that have some “centre”, so it would be possible to tell amount of people living close to country (or countries, like European Union) border.

Coordinates of city are assumed to be coordinates of well established city centre.

How the measurement _is not_ done
----

The most obvious approach would be to do something like wikipedia indexing:

1. Remember all cities for a given country
2. For each city get population and surface area.
3. If given radius (e.g. 30km) covers the whole city, return population. Otherwise (e.g. 3km) compute population proportionally (population * circle area / city area).
4. If the radius also covers other cities (e.g. 100km) add them in same manner
5. We can either assume that cities have circular shape, or additionnaly store shape, which complicates things quite a bit

Pros:

* Can use data from different sources
* Easy to understand and reason about
* Easy to debug, because data is stored in human-understandable format

Cons:

* Does not take into account that 20% of city’s surface area can contain 80% of population, which is often the case. And it might not even be the centre of the city that accomodates 80% of people.
* Does not take into account that there can be some small villages and cities that are not listed on wikipedia (or other source), but contain significant part of population
* Requires accurate and consistent data about countries, cities and population. Some cities can have most recent population estimation for 2012, other for 2015 and some for 1953

Cons (20/80%, villages around, non-circular shape) will result in big errors, mostly overestimations for some queries

How the measurement _is_ done
----

Instead of counting population by cities, [population density grid](http://sedac.ciesin.columbia.edu/data/set/gpw-v3-population-density-future-estimates/data-download) is used. Population counting:

1. City centre coordinates are looked up in some DB
2. Grid is queried for all cells around that fall into given range (e.g. 30km)
3. Cells are sorted by distance to city centre (with respect to cell shape distortion on given latitude)
4. Population is computed then like this:

```
earthR = 6371
Radius = 30
// cell height. 0.0416666666667 is 2.5' in degrees
ch = 0.0416666666667 * 2 * Pi * earthR / 360
// cell width. we can safely assume it doesnt change around one city
cw = sin(latitude) * ch
cell_area = cw * ch
// sorted by distance array of cell densities (people on km squared)
cells = [....]
area = Pi * Radius * Radius
population = 0

while(area >= 0) {
  density = cells.shift() || 0
  if(area > cell_area) {
      population += cell_area * density
    } else {
      population += area * density
    }
    area -= cell_area
}
return population;
```

[NASA population density grid](http://sedac.ciesin.columbia.edu/data/set/gpw-v3-population-density-future-estimates/data-download) future estimate for 2015, resolution 2.5’ is used to get population on given area.

[Maxmind world cities database](https://www.maxmind.com/en/free-world-cities-database) is used to get coordinates of cities.

Both files can be downloaded [from S3](https://s3.amazonaws.com/felixlotok/d.tar.gz)

Pros:

1. Accuracy doesn't depend on city shape
2. Accuracy doesn't depend on unknown villages around, as long as they're counted on the grid
3. Can be used not only for cities, but for any point on the map
4. City DB and population density DB are decoupled

Cons:

1. Hard to debug (grid is 2D array, not really human friendly)
2. There is only one source of such grid, NASA.


Performance
----
Grid is stored in memory, in 2D vector. Reading it is fast and can't actually get faster (maybe a bit by using native Java arrays, not Clojure vectors).

Startup takes about 12 seconds because of reading the grid. Not nice for tests, but okay for production.

Complexity is O(R<sup>2</sup>/sin(latitude)).

Cities are stored in MySQL. City name is indexed, so lookup is pretty fast too.
As city coordinates lookup is separated from population counting, a cache layer for city coordinares can be easily added (or just enable query cache).

Service can be naturally scaled linearly just by running more instances (maybe accessing same MySQL, don't know: need to measure bottlenecks).

Maximum radius is set to 499, but can be much greater if we take change of latitude into account.

Webserver handles 500 concurrent requests per second (Without tuning, and with client JIT compile optimization). With little tweaking I think will speed up several times.

Extending data
----

Both city DB and density grid cover the whole world, so no extending is needed. Maxmind gives a warning that city DB is outdated, but then how often do cities change their coordinates?

Bonus
----
As US didn't bother to make up city names, there are 22 cities named Moscow in US. The app will give you a hint on how to specify which city you need:

```
http://ec2-52-29-0-69.eu-central-1.compute.amazonaws.com/city/population?city=Moscow&radius=12:

{
  "error": {
    "description": "found 23 results, expected 1",
    "results": [
      {
        "full_query": "ru,48,moscow",
        "longitude": 37.615556,
        "latitude": 55.752222,
        "region": "48",
        "city_accented": "Moscow",
        "city": "moscow",
        "country": "ru"
      },
      {
        "full_query": "us,AL,moscow",
        "longitude": -88.1027778,
        "latitude": 33.8697222,
        "region": "AL",
        "city_accented": "Moscow",
        "city": "moscow",
        "country": "us"
      },
      {
        "full_query": "us,AR,moscow",
        "longitude": -91.795,
        "latitude": 34.1463889,
        "region": "AR",
        "city_accented": "Moscow",
        "city": "moscow",
        "country": "us"
      },
      ...
```

Result of `full_query` field should be used:

```
http://ec2-52-29-0-69.eu-central-1.compute.amazonaws.com/city/population?city=us,AL,moscow&radius=12

{
  "population": 5806,
  "radius": 12,
  "city": {
    "longitude": -88.1027778,
    "latitude": 33.8697222,
    "region": "AL",
    "city_accented": "Moscow",
    "city": "moscow",
    "country": "us"
  }
}
```
