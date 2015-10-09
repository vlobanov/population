CREATE TABLE cities (
  country char(2) NOT NULL default '',
  city varchar(128) NOT NULL default '',
  city_accented varchar(128) NOT NULL default '',
  region varchar(128) NOT NULL default '',
  latitude decimal(10,7) NOT NULL default '0.0000000',
  longitude decimal(10,7) NOT NULL default '0.0000000'
);

LOAD DATA LOCAL INFILE './data/worldcitiespop.txt' INTO TABLE cities
FIELDS TERMINATED BY ','
IGNORE 1 LINES
(country,city,city_accented,region,@Population,latitude,longitude);

ALTER TABLE cities ADD INDEX index_city(city);
ALTER TABLE cities ADD INDEX index_country_city(country, city);
ALTER TABLE cities ADD INDEX index_country_region_city(country, region, city);
