package com.atomist.javatooling.listusedapi.parsing

data class ApiDefinition(
    val methods: Set<String>,
    val classes: Set<String>,
    val annotations: Set<String>) {

    fun merge(apiDefinition: ApiDefinition): ApiDefinition {
        return ApiDefinition(
                this.methods.plus(apiDefinition.methods),
                this.classes.plus(apiDefinition.classes),
                this.annotations.plus(apiDefinition.annotations)
        )
    }
}
