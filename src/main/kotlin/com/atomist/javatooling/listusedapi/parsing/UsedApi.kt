package com.atomist.javatooling.listusedapi.parsing

data class UsedApi(
    val methods: Set<String>,
    val classes: Set<String>,
    val annotations: Set<String>) {

    fun merge(usedApi: UsedApi): UsedApi {
        return UsedApi(
                this.methods.plus(usedApi.methods),
                this.classes.plus(usedApi.classes),
                this.annotations.plus(usedApi.annotations)
        )
    }
}
