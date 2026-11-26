# Geographic data

The Android assets use OpenStreetMap data © OpenStreetMap contributors, licensed under ODbL 1.0. See https://www.openstreetmap.org/copyright and https://opendatacommons.org/licenses/odbl/1-0/.

- Moscow and Moscow Oblast use OSM relations [102269](https://www.openstreetmap.org/relation/102269) and [51490](https://www.openstreetmap.org/relation/51490), retrieved through Nominatim and simplified for local point-in-polygon checks.
- New Moscow uses OSM relations [2263058](https://www.openstreetmap.org/relation/2263058) and [2263059](https://www.openstreetmap.org/relation/2263059).
- Distances use trunk and motorway members of the MKAD route [2094222](https://www.openstreetmap.org/relation/2094222), retrieved through the OSM API. Distances are straight-line approximations to the road.

Feed fields used for geographic restrictions include region, locality, district, address, latitude, longitude and station names. When a feed supplies explicit location or expiration metadata, conflicting and expired records are excluded.
