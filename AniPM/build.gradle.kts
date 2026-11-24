version = 1

dependencies {
    implementation("com.github.Blatzar:NiceHttp:0.4.18")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.1")
}

cloudstream {
    description = "AniPM anime provider"
    authors = listOf("Nashon-ops")
    language = "en"
    status = 1
    tvTypes = listOf("Anime")
    iconUrl = "https://ani.pm/favicon.ico"
}
