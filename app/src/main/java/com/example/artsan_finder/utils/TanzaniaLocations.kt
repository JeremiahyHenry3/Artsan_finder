package com.example.artsan_finder.utils

object TanzaniaLocations {
    val locations = mapOf(
        "Dar es Salaam" to listOf(
            "Upanga", "Kinondoni", "Kariakoo", "Masaki", "Mikocheni", "Ilala", "Temeke", "Kigamboni", "Mbezi", "Sinza"
        ),
        "Mbeya" to listOf(
            "Iyunga", "Mbalizi", "Sisimba", "Mwanjelwa", "Isamilo", "Forest", "Uyole", "Itende"
        ),
        "Arusha" to listOf(
            "Sekei", "Njiro", "Kaloleni", "Kijenge", "Sakina", "Ungu", "Mount Meru"
        ),
        "Dodoma" to listOf(
            "Kizota", "Makole", "Ipagala", "Chamwino", "Majengo", "Mtendeni", "Hazina"
        ),
        "Mwanza" to listOf(
            "Ilemela", "Nyamagana", "Pasiansi", "Kirumba", "Mabatini", "Nyakato"
        ),
        "Morogoro" to listOf(
            "Mazimbu", "Kingolwira", "Kihonda", "Mbuyuni", "Msamvu"
        ),
        "Tanga" to listOf(
            "Ngamiani", "Chumbageni", "Usagara", "Masiwani", "Central"
        )
    )

    fun getRegions(): List<String> = locations.keys.toList().sorted()
    
    fun getWards(region: String): List<String> = locations[region] ?: emptyList()
}
