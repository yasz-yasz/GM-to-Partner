package com.gmtopartner.app.data

import android.content.Context
import org.json.JSONArray

class RouteRepository private constructor(
    val airlines: List<Airline>,
    val airlineMap: Map<String, Airline>,
    val airports: List<Airport>,
    val airportMap: Map<String, Airport>,
    val routes: List<RouteEdge>
) {

    val fifthFreedomRoutes: List<RouteEdge> by lazy {
        routes.filter { it.isFifthFreedom }
            .sortedWith(compareBy<RouteEdge> { it.airlineCode }.thenBy { it.distanceMiles })
    }

    fun resolveAirportCode(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null

        val upperFirst = trimmed.take(3).uppercase()
        if (upperFirst.length == 3 && airportMap.containsKey(upperFirst)) {
            if (trimmed.length == 3 || !trimmed[3].isLetter()) {
                return upperFirst
            }
        }

        val lower = trimmed.lowercase()
        val exactCity = airports.firstOrNull { it.city.lowercase() == lower }
        if (exactCity != null) return exactCity.code

        val partial = airports.firstOrNull { it.searchKey().contains(lower) }
        return partial?.code
    }

    companion object {
        @Volatile
        private var instance: RouteRepository? = null

        fun getInstance(context: Context): RouteRepository {
            return instance ?: synchronized(this) {
                instance ?: loadFromAssets(context.applicationContext).also { instance = it }
            }
        }

        private fun loadFromAssets(context: Context): RouteRepository {
            val airlinesJson = context.assets.open("airlines.json").bufferedReader().use { it.readText() }
            val airportsJson = context.assets.open("airports.json").bufferedReader().use { it.readText() }
            val routesJson = context.assets.open("routes.json").bufferedReader().use { it.readText() }

            val airlinesList = mutableListOf<Airline>()
            val airlinesArr = JSONArray(airlinesJson)
            for (i in 0 until airlinesArr.length()) {
                val obj = airlinesArr.getJSONObject(i)
                val hubsArr = obj.getJSONArray("hubs")
                val hubs = List(hubsArr.length()) { idx -> hubsArr.getString(idx) }
                airlinesList.add(
                    Airline(
                        code = obj.getString("code"),
                        name = obj.getString("name"),
                        alliance = obj.getString("alliance"),
                        country = obj.getString("country"),
                        hubs = hubs,
                        suspended = obj.optBoolean("suspended", false),
                        notes = obj.optString("notes", "")
                    )
                )
            }

            val airportsList = mutableListOf<Airport>()
            val airportsArr = JSONArray(airportsJson)
            for (i in 0 until airportsArr.length()) {
                val obj = airportsArr.getJSONObject(i)
                airportsList.add(
                    Airport(
                        code = obj.getString("code"),
                        name = obj.getString("name"),
                        city = obj.getString("city"),
                        country = obj.getString("country"),
                        region = obj.getString("region"),
                        lat = obj.getDouble("lat"),
                        lon = obj.getDouble("lon")
                    )
                )
            }

            val routesList = mutableListOf<RouteEdge>()
            val routesArr = JSONArray(routesJson)
            for (i in 0 until routesArr.length()) {
                val obj = routesArr.getJSONObject(i)
                routesList.add(
                    RouteEdge(
                        airlineCode = obj.getString("al"),
                        from = obj.getString("a"),
                        to = obj.getString("b"),
                        distanceMiles = obj.getInt("d"),
                        tpmGroup = obj.getString("g"),
                        isFifthFreedom = obj.optBoolean("ff", false),
                        isEligibleGm = obj.optBoolean("el", true),
                        note = obj.optString("n", "")
                    )
                )
            }

            return RouteRepository(
                airlines = airlinesList,
                airlineMap = airlinesList.associateBy { it.code },
                airports = airportsList,
                airportMap = airportsList.associateBy { it.code },
                routes = routesList
            )
        }
    }
}
