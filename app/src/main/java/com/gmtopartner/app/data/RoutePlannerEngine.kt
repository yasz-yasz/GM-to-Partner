package com.gmtopartner.app.data

class RoutePlannerEngine(private val repo: RouteRepository) {

    private data class DirectedEdge(
        val from: String,
        val to: String,
        val route: RouteEdge
    )

    private data class RawCandidate(
        val edges: List<DirectedEdge>,
        val totalMiles: Int,
        val totalDistance: Int,
        val uniqueAirlinesCount: Int
    )

    fun search(params: SearchParams, maxResults: Int = 60): List<ItineraryOption> {
        val origin = params.originCode.uppercase()
        val dest = params.destinationCode.uppercase()
        if (origin == dest) return emptyList()

        val adj = HashMap<String, MutableList<DirectedEdge>>()
        for (r in repo.routes) {
            val airline = repo.airlineMap[r.airlineCode]
            if (airline?.suspended == true && !params.includeSuspendedAeroflot) continue
            if (r.airlineCode == "SQ" && !r.isEligibleGm && !params.includeSqNonCodeshare) continue

            adj.getOrPut(r.from) { mutableListOf() }.add(DirectedEdge(r.from, r.to, r))
            adj.getOrPut(r.to) { mutableListOf() }.add(DirectedEdge(r.to, r.from, r))
        }

        val originEdges = adj[origin] ?: return emptyList()
        val destEdges = adj[dest] ?: return emptyList()

        val candidates = mutableListOf<RawCandidate>()

        fun edgeCost(edge: DirectedEdge): Int =
            TpmCalculator.milesForGroup(edge.route.tpmGroup, params.cabin, params.useGroupDTypoRate)

        // 1. Direct (1 Leg)
        if (params.mode != SearchMode.MIX_PARTNER) {
            for (e1 in originEdges) {
                if (e1.to == dest) {
                    candidates.add(
                        RawCandidate(
                            edges = listOf(e1),
                            totalMiles = edgeCost(e1),
                            totalDistance = e1.route.distanceMiles,
                            uniqueAirlinesCount = 1
                        )
                    )
                }
            }
        }

        // 2. One Transit (2 Legs)
        if (params.maxLegs >= 2) {
            for (e1 in originEdges) {
                if (e1.to == dest) continue
                val m1 = edgeCost(e1)
                val secondHop = adj[e1.to] ?: continue
                for (e2 in secondHop) {
                    if (e2.to != dest) continue
                    val isMix = e1.route.airlineCode != e2.route.airlineCode
                    if (params.mode == SearchMode.SINGLE_PARTNER && isMix) continue
                    if (params.mode == SearchMode.MIX_PARTNER && !isMix) continue

                    val m2 = edgeCost(e2)
                    candidates.add(
                        RawCandidate(
                            edges = listOf(e1, e2),
                            totalMiles = m1 + m2,
                            totalDistance = e1.route.distanceMiles + e2.route.distanceMiles,
                            uniqueAirlinesCount = if (isMix) 2 else 1
                        )
                    )
                }
            }
        }

        // 3. Two Transits (3 Legs) — pruned meet-in-the-middle
        if (params.maxLegs >= 3) {
            val toDestMap = HashMap<String, MutableList<Pair<DirectedEdge, Int>>>()
            for (rev in destEdges) {
                val mid2 = rev.to
                if (mid2 == origin) continue
                val forwardEdge = DirectedEdge(mid2, dest, rev.route)
                toDestMap.getOrPut(mid2) { mutableListOf() }.add(forwardEdge to edgeCost(forwardEdge))
            }
            for ((_, list) in toDestMap) {
                list.sortWith(compareBy<Pair<DirectedEdge, Int>> { it.second }.thenBy { it.first.route.distanceMiles })
                if (list.size > 4) {
                    list.subList(4, list.size).clear()
                }
            }

            val fromOriginMap = HashMap<String, MutableList<Pair<DirectedEdge, Int>>>()
            for (e1 in originEdges) {
                if (e1.to == dest) continue
                fromOriginMap.getOrPut(e1.to) { mutableListOf() }.add(e1 to edgeCost(e1))
            }
            for ((_, list) in fromOriginMap) {
                list.sortWith(compareBy<Pair<DirectedEdge, Int>> { it.second }.thenBy { it.first.route.distanceMiles })
                if (list.size > 4) {
                    list.subList(4, list.size).clear()
                }
            }

            val seenSig = HashSet<String>()
            for ((mid1, e1List) in fromOriginMap) {
                val midEdges = adj[mid1] ?: continue
                for (e2 in midEdges) {
                    val mid2 = e2.to
                    if (mid2 == origin || mid2 == dest) continue
                    val e3List = toDestMap[mid2] ?: continue
                    val m2 = edgeCost(e2)

                    for ((e1, m1) in e1List) {
                        for ((e3, m3) in e3List) {
                            val a1 = e1.route.airlineCode
                            val a2 = e2.route.airlineCode
                            val a3 = e3.route.airlineCode
                            val distinctCount = setOf(a1, a2, a3).size
                            val isMix = distinctCount > 1
                            if (params.mode == SearchMode.SINGLE_PARTNER && isMix) continue
                            if (params.mode == SearchMode.MIX_PARTNER && !isMix) continue

                            val sig = "$mid1-$mid2-$a1-$a2-$a3"
                            if (!seenSig.add(sig)) continue

                            candidates.add(
                                RawCandidate(
                                    edges = listOf(e1, e2, e3),
                                    totalMiles = m1 + m2 + m3,
                                    totalDistance = e1.route.distanceMiles + e2.route.distanceMiles + e3.route.distanceMiles,
                                    uniqueAirlinesCount = distinctCount
                                )
                            )
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) return emptyList()

        candidates.sortWith(
            compareBy<RawCandidate> { it.totalMiles }
                .thenBy { it.edges.size }
                .thenBy { it.uniqueAirlinesCount }
                .thenBy { it.totalDistance }
        )

        val selected = mutableListOf<RawCandidate>()
        val selectedSigs = HashSet<String>()

        fun sigOf(c: RawCandidate): String =
            c.edges.joinToString("|") { "${it.from}-${it.to}-${it.route.airlineCode}" }

        if (params.mode == SearchMode.ALL) {
            for (c in candidates.take(40)) {
                if (selectedSigs.add(sigOf(c))) selected.add(c)
            }
            for (c in candidates.filter { it.uniqueAirlinesCount > 1 }.take(15)) {
                if (selectedSigs.add(sigOf(c))) selected.add(c)
            }
            for (c in candidates.filter { it.uniqueAirlinesCount == 1 }.take(15)) {
                if (selectedSigs.add(sigOf(c))) selected.add(c)
            }
            selected.sortWith(
                compareBy<RawCandidate> { it.totalMiles }
                    .thenBy { it.edges.size }
                    .thenBy { it.uniqueAirlinesCount }
                    .thenBy { it.totalDistance }
            )
        } else {
            selected.addAll(candidates.take(maxResults))
        }

        return selected.take(maxResults).map { raw ->
            val segs = raw.edges.map { e ->
                val airline = repo.airlineMap[e.route.airlineCode]
                val fromAp = repo.airportMap[e.from]
                val toAp = repo.airportMap[e.to]
                ItinerarySegment(
                    airlineCode = e.route.airlineCode,
                    airlineName = airline?.name ?: e.route.airlineCode,
                    fromCode = e.from,
                    toCode = e.to,
                    fromCity = fromAp?.city ?: e.from,
                    toCity = toAp?.city ?: e.to,
                    distanceMiles = e.route.distanceMiles,
                    tpmGroup = e.route.tpmGroup,
                    milesRequired = edgeCost(e),
                    isFifthFreedom = e.route.isFifthFreedom,
                    isEligibleGm = e.route.isEligibleGm,
                    isSuspended = airline?.suspended ?: false,
                    note = e.route.note
                )
            }
            val warnings = mutableListOf<String>()
            if (segs.any { it.isSuspended }) {
                warnings.add("Mengandung segmen Aeroflot (SkyTeam Suspended — umumnya tidak aktif ditukarkan).")
            }
            if (segs.any { it.airlineCode == "SQ" && !it.isEligibleGm }) {
                warnings.add("Mengandung rute Singapore Airlines di luar codeshare resmi GarudaMiles.")
            }
            if (segs.any { it.tpmGroup == "D" } && params.cabin == CabinClass.BUSINESS) {
                if (params.useGroupDTypoRate) {
                    warnings.add("Menggunakan tarif web typo Group D Business (77.500). Tarif aktual sistem umumnya 47.500.")
                } else {
                    warnings.add("Group D Business dihitung dengan tarif aktual 47.500 miles (konfirmasi ke GASO karena web mencantumkan typo 77.500).")
                }
            }
            ItineraryOption(
                segments = segs,
                totalMilesOneWay = raw.totalMiles,
                totalDistanceMiles = raw.totalDistance,
                isMixPartner = raw.uniqueAirlinesCount > 1,
                hasFifthFreedom = segs.any { it.isFifthFreedom },
                hasWarning = warnings.isNotEmpty(),
                warningText = warnings.joinToString(" ")
            )
        }
    }
}
